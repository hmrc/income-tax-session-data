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
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model._
import org.mongodb.scala.result
import uk.gov.hmrc.incometaxsessiondata.config.AppConfig
import uk.gov.hmrc.incometaxsessiondata.models.EncryptedSession
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionDataRepository @Inject() (
  mongoComponent: MongoComponent,
  config: AppConfig
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[EncryptedSession](
      collectionName = "session-data",
      mongoComponent = mongoComponent,
      domainFormat = EncryptedSession.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("sessionId", "internalId"),
          IndexOptions()
            .name("compoundIndex")
            .unique(true)
        ),
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIndex")
            .expireAfter(config.cacheTtl, TimeUnit.SECONDS)
            .unique(false)
        )
      ),
      replaceIndexes = true
    ) {

  private def dataFilter(sessionId: String, internalId: String): Bson =
    and(equal("sessionId", sessionId), equal("internalId", internalId))

  def get(sessionId: String, internalId: String): Future[Option[EncryptedSession]] =
    collection
      .find(dataFilter(sessionId, internalId))
      .headOption()

  def set(data: EncryptedSession): Future[result.UpdateResult] =
    collection
      .replaceOne(
        filter = dataFilter(data.sessionId, data.internalId),
        replacement = data,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()

}
