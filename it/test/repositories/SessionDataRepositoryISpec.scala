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

package repositories

import org.mongodb.scala.bson.BsonDocument
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.incometaxsessiondata.domain.models.Session
import uk.gov.hmrc.incometaxsessiondata.repositories.SessionDataRepository

import scala.concurrent.ExecutionContext

class SessionDataRepositoryISpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with GuiceOneServerPerSuite{

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val repository = app.injector.instanceOf[SessionDataRepository]

  def beforeEach(): Unit = {
    await(repository.collection.deleteMany(BsonDocument()).toFuture())
  }

  val testSessionId = "session-123"

  val dummySession = Session(
    sessionID = testSessionId,
    mtditid = Some("testId"),
    nino = Some("testNino"),
    saUtr = None,
    clientFirstName = Some("John"),
    clientLastName = Some("Smith"),
    userType = None
  )

  "Session Data Repository" should {
    "set some data" in {
      val acknowledged = await(repository.set(dummySession))
      acknowledged shouldBe true
    }
    "get some data" in {
      await(repository.set(dummySession))
      val result = await(repository.get(testSessionId)).get
      result.nino shouldBe Some("testNino")
      result.userType shouldBe None
    }
  }

}
