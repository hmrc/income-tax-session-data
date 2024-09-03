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

import com.mongodb.client.result.UpdateResult
import helpers.ComponentSpecBase
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import testConstants.IntegrationTestConstants._
import uk.gov.hmrc.incometaxsessiondata.models.Session
import uk.gov.hmrc.incometaxsessiondata.repositories.SessionDataRepository

import scala.concurrent.ExecutionContext

class SessionDataRepositoryISpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with GuiceOneServerPerSuite
  with BeforeAndAfterEach
  with ComponentSpecBase {

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val repository = app.injector.instanceOf[SessionDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(clearDb(repository, testSessionId))
    await(clearDb(repository, testAlternativeSessionId))
  }

  val testAlternativeSessionId = "session-xxx"

  override val testSession: Session = Session(
    sessionId = testSessionId,
    mtditid = "testId",
    nino = "testNino",
    utr = "testUtr",
    internalId = "test-internal-id"
  )

  val testSession2: Session = Session(
    sessionId = itTestSessionId,
    mtditid = itTestMtditid,
    nino = itTestNino,
    utr = itTestUtr,
    internalId = itTestInternalId
  )

  val testSessionAlternativeInternalId: Session = Session(
    sessionId = testSessionId,
    mtditid = "testId",
    nino = "testNinoOther",
    utr = "testUtrOther",
    internalId = "test-internal-id-other"
  )

  val otherTestSession: Session = Session(
    sessionId = testAlternativeSessionId,
    mtditid = "testIdOther",
    nino = "testNinoOther",
    utr = "testUtrOther",
    internalId = "test-internal-id-other"
  )

  "Session Data Repository get method" should {
    "set some data" in {
      val result = repository.set(testSession)
      result.futureValue.wasAcknowledged() shouldBe true
    }
    "get some data using sessionId and internalId" in {
      await(repository.set(testSession))
      await(repository.set(testSession.copy(sessionId = "different-session-id", nino = "999-test-999")))

      val result = repository.get("different-session-id", testSession.internalId).futureValue.get
      result.nino shouldBe "999-test-999"
    }
    "set some data when data with matching session id and different internal id to on which exists in the database" in {
      await(repository.set(testSession))
      await(repository.set(testSession.copy(internalId = "different-internal-id", utr = "12345-test-54321")))

      val result = repository.get(testSession.sessionId, "different-internal-id").futureValue.get
      result.utr shouldBe "12345-test-54321"
    }
    "set some data when data with matching internal id and different session id to on which exists in the database" in {
      await(repository.set(testSession))

      val result = repository.set(testSession.copy(sessionId = "different-session-id"))
      result.futureValue shouldBe UpdateResult.acknowledged(1, 1, null)
    }
    "overwrite some data, when a session with a matching session id and internal id is set" in {
      await(repository.set(testSession))
      val resultOne = repository.get(testSession.sessionId, testSession.internalId).futureValue.get

      resultOne.utr shouldBe "testUtr"
      resultOne.nino shouldBe "testNino"
      resultOne.mtditid shouldBe "testId"

      await(repository.set(testSession.copy(utr = "diffUtr", nino = "diffNino", mtditid = "diffMtditid")))
      val resultTwo = repository.get(testSession.sessionId, testSession.internalId).futureValue.get

      resultTwo.utr shouldBe "diffUtr"
      resultTwo.nino shouldBe "diffNino"
      resultTwo.mtditid shouldBe "diffMtditid"
    }
    "get an empty list when searching with session id but there are no entries matching that session id but there are items in the database" in {
      await(repository.set(testSession))

      val result = repository.get("xTest1", "xTest2")

      result.futureValue shouldBe None
    }
    "get an empty list when searching with session id but there are no entries matching that session id" in {
      val result = repository.get(testSession.sessionId, testSession.internalId)

      result.futureValue shouldBe None
    }
  }

}
