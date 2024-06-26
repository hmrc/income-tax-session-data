/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.incometaxsessiondata.repositories

import com.google.inject.Singleton
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import uk.gov.hmrc.incometaxsessiondata.config.AppConfig
import uk.gov.hmrc.incometaxsessiondata.models.Session
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionDataRepository @Inject()(
                                       mongoComponent: MongoComponent,
                                       config: AppConfig,
                                       clock: Clock
                                     )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[Session](
    collectionName = "session-data",
    mongoComponent = mongoComponent,
    domainFormat = Session.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("sessionID"),
        IndexOptions()
          .name("sessionIDIndex")
          .unique(true)
      ),
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIndex")
          .expireAfter(config.cacheTtl, TimeUnit.SECONDS)
      )
    ),
    replaceIndexes = true
  ) {

  private def dataFilter(sessionId: String): Bson = {
    import Filters._
    and(equal("sessionID", sessionId))
  }

  def get(sessionId: String): Future[Option[Session]] = {
    collection
      .find(dataFilter(sessionId))
      .headOption()
  }

  def set(data: Session): Future[Boolean] = {

    val updatedAnswers = data copy (lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = dataFilter(data.sessionID),
        replacement = updatedAnswers,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_.wasAcknowledged())
  }

  def deleteOne(sessionId: String): Future[Boolean] = {
    collection.deleteOne(dataFilter(sessionId)).toFuture().map(_.wasAcknowledged())
  }
}
