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
import play.api.Configuration
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import testConstants.IntegrationTestConstants.crypter
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.incometaxsessiondata.models.{EncryptedSession, Session}
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
  private val config = app.injector.instanceOf[Configuration]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(clearDb(repository, testSessionId))
    await(clearDb(repository, testAlternativeSessionId))
  }

  val testAlternativeSessionId = "session-xxx"

  val testEncryptedSession: EncryptedSession = EncryptedSession(
    sessionId = testSessionId,
    mtditid = "testId",
    nino = crypter.encrypt(PlainText("testNino")),
    utr = crypter.encrypt(PlainText("testUtr")),
    internalId = "test-internal-id"
  )

  "Session Data Repository get method" should {
    "set some data" in {
      val result = repository.set(testEncryptedSession)
      result.futureValue.wasAcknowledged() shouldBe true
    }
    "get some data using sessionId and internalId" in {
      await(repository.set(testEncryptedSession))
      await(repository.set(testEncryptedSession.copy(sessionId = "different-session-id", nino = crypter.encrypt(PlainText("999-test-999")))))

      val result = repository.get("different-session-id", testEncryptedSession.internalId).futureValue.get
      crypter.decrypt(result.nino).value shouldBe "999-test-999"
    }
    "set some data when data with matching session id and different internal id to on which exists in the database" in {
      await(repository.set(testEncryptedSession))
      await(repository.set(testEncryptedSession.copy(internalId = "different-internal-id", utr = crypter.encrypt(PlainText("12345-test-54321")))))

      val result = repository.get(testEncryptedSession.sessionId, "different-internal-id").futureValue.get
      crypter.decrypt(result.utr).value shouldBe "12345-test-54321"
    }
    "set some data when data with matching internal id and different session id to on which exists in the database" in {
      await(repository.set(testEncryptedSession))

      val result = repository.set(testEncryptedSession.copy(sessionId = "different-session-id"))
      result.futureValue shouldBe UpdateResult.acknowledged(1, 1, null)
    }
    "overwrite some data, when a session with a matching session id and internal id is set" in {
      await(repository.set(testEncryptedSession))
      val resultOne = repository.get(testEncryptedSession.sessionId, testEncryptedSession.internalId).futureValue.get

      crypter.decrypt(resultOne.utr).value shouldBe "testUtr"
      crypter.decrypt(resultOne.nino).value shouldBe "testNino"
      resultOne.mtditid shouldBe "testId"

      await(repository.set(testEncryptedSession.copy(utr = crypter.encrypt(PlainText("diffUtr")), nino = crypter.encrypt(PlainText("diffNino")), mtditid = "diffMtditid")))
      val resultTwo = repository.get(testEncryptedSession.sessionId, testEncryptedSession.internalId).futureValue.get

      crypter.decrypt(resultTwo.utr).value shouldBe "diffUtr"
      crypter.decrypt(resultTwo.nino).value shouldBe "diffNino"
      resultTwo.mtditid shouldBe "diffMtditid"
    }
    "get an empty list when searching with session id but there are no entries matching that session id but there are items in the database" in {
      await(repository.set(testEncryptedSession))

      val result = repository.get("xTest1", "xTest2")

      result.futureValue shouldBe None
    }
    "get an empty list when searching with session id but there are no entries matching that session id" in {
      val result = repository.get(testEncryptedSession.sessionId, testEncryptedSession.internalId)

      result.futureValue shouldBe None
    }

    // AC2: example => MISUV-8389
    "overwrite some data =>" in {
      val plainSession = Session(
        sessionId = testEncryptedSession.sessionId,
        mtditid = "testId",
        nino = "testNino",
        utr = "testUtr",
        internalId = "test-internal-id"
      )

      val plainSession2 = Session(
        sessionId = testEncryptedSession.sessionId,
        mtditid = "testId2",
        nino = "testNino",
        utr = "testUtr",
        internalId = "test-internal-id"
      )
      // Drop calling encryption / decryption directly
      await(repository.set( EncryptedSession(plainSession, config) ) )
      val encSession = repository.get(testEncryptedSession.sessionId, testEncryptedSession.internalId).futureValue.get
      EncryptedSession.unapply(encSession, config) shouldBe plainSession
      EncryptedSession.unapply(encSession, config) !== plainSession2

    }
  }

}
