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
      val result = await(repository.set(testSession))
      result.wasAcknowledged() shouldBe true
    }
    "set some data when data with matching session id and internal id exists in the database" in {
      repository.set(testSession)
      val result = await(repository.set(testSession.copy(sessionId = testSessionId, internalId = "test-internal-id")))
      result shouldBe UpdateResult.acknowledged(1, 1, null)
    }
    "get some data using sessionId and internalId" in {
      await(repository.set(testSession2))
      val result = await(repository.get(testRequest)).get
      result.mtditid shouldBe "it-testMtditid"
      result.utr shouldBe "it-testUtr123"
    }
    "get some data using just session id when there is only one entry" in {
      await(repository.set(testSession))
      val result = await(repository.getBySessionId(testSession.sessionId))
      result.size shouldBe 1
      result.head.mtditid shouldBe "testId"
      result.head.utr shouldBe "testUtr"
    }
    "get some data using just session id when there is multiple entries with the same session id" in {
      await(repository.set(testSession))
      await(repository.set(testSessionAlternativeInternalId))
      val result = await(repository.getBySessionId(testSession.sessionId))
      result.size shouldBe 2
      result.head.mtditid shouldBe "testId"
      result.head.utr shouldBe "testUtr"
      result(1).mtditid shouldBe "testId"
      result(1).utr shouldBe "testUtrOther"
    }
    "get an empty list when searching with session id but there are no entries matching that session id" in {
      val result = await(repository.getBySessionId(testSession.mtditid))

      result.size shouldBe 0
    }
  }

}
