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

import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model._
import uk.gov.hmrc.incometaxsessiondata.config.AppConfig
import uk.gov.hmrc.incometaxsessiondata.models.UserSessionDetails
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SessionData @Inject()(
                             mongoComponent: MongoComponent,
                             config: AppConfig,
                             clock: Clock
                           )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[UserSessionDetails](
    collectionName = "ui-journey-session-data",
    mongoComponent = mongoComponent,
    domainFormat = UserSessionDetails.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("lastUpdatedIdx")
          .expireAfter(config.cacheTtl, TimeUnit.SECONDS)
      )
    ),
    replaceIndexes = true
  ) {

  private def dataFilter(data: UserSessionDetails): Bson = {
    import Filters._
    and(equal("sessionId", data.sessionId), equal("nino", data.nino))
  }

  def keepAlive(data: UserSessionDetails): Future[Boolean] =
    collection
      .updateOne(
        filter = dataFilter(data),
        update = Updates.set("lastUpdated", Instant.now(clock))
      )
      .toFuture()
      .map(_.wasAcknowledged())

  def get(sessionId: String, nino: String): Future[Option[UserSessionDetails]] = {
    val data = UserSessionDetails(sessionId, nino)
    keepAlive(data).flatMap {
      _ =>
        collection
          .find(dataFilter(data))
          .headOption()
    }
  }

  def set(data: UserSessionDetails): Future[Boolean] = {

    val updatedAnswers = data copy (lastUpdated = Instant.now(clock))

    collection
      .replaceOne(
        filter = dataFilter(data),
        replacement = updatedAnswers,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_.wasAcknowledged())
  }
}
