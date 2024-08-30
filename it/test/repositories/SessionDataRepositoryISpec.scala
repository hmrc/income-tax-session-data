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

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.incometaxsessiondata.models.Session
import uk.gov.hmrc.incometaxsessiondata.repositories.SessionDataRepository

import scala.concurrent.ExecutionContext

class SessionDataRepositoryISpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with GuiceOneServerPerSuite
  with BeforeAndAfterEach {

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val repository = app.injector.instanceOf[SessionDataRepository]

  override def beforeEach(): Unit = {
    await(repository.deleteOne(testSession.sessionId, testSession.internalId, testSession.mtditid))
    await(repository.deleteOne(otherTestSession.sessionId, otherTestSession.internalId, otherTestSession.mtditid))
    await(repository.deleteOne(testSessionDuplicateMtditid.sessionId, testSessionDuplicateMtditid.internalId, testSessionDuplicateMtditid.mtditid))
  }

  val testSessionId = "session-123"
  val otherTestSessionId = "session-xxx"

  val testSession: Session = Session(
    sessionId = testSessionId,
    mtditid = "testId",
    nino = "testNino",
    utr = "testUtr",
    internalId = "test-internal-id"
  )

  val testSessionDuplicateMtditid: Session = Session(
    sessionId = otherTestSessionId,
    mtditid = "testId",
    nino = "testNinoOther",
    utr = "testUtrOther",
    internalId = "test-internal-id-other"
  )

  val otherTestSession: Session = Session(
    sessionId = otherTestSessionId,
    mtditid = "testIdOther",
    nino = "testNinoOther",
    utr = "testUtrOther",
    internalId = "test-internal-id-other"
  )

  "Session Data Repository get method" should {
    "set some data" in {
      val result = await(repository.set(testSession))
      result shouldBe "true"
    }
    "get some data using sessionId, internalId and mtditid" in {
      await(repository.set(testSession))
      val result = await(repository.get(testSessionId, testSession.internalId, testSession.mtditid)).get
      result.mtditid shouldBe "testId"
      result.utr shouldBe "testUtr"
    }
    "get some data using just mtditid when there is only one entry" in {
      await(repository.set(testSession))
      val result = await(repository.getBySessionId(testSession.mtditid))
      result.size shouldBe 1
      result.head.mtditid shouldBe "testId"
      result.head.utr shouldBe "testUtr"
    }
    "get some data using just mtditid when there is multiple entries with the same mtditid" in {
      await(repository.set(testSession))
      await(repository.set(testSessionDuplicateMtditid))
      val result = await(repository.getBySessionId(testSession.mtditid))
      result.size shouldBe 2
      result.head.mtditid shouldBe "testId"
      result.head.utr shouldBe "testUtr"
      result(1).mtditid shouldBe "testId"
      result(1).utr shouldBe "testUtrOther"
    }
    "get an empty list when searching with mtditid but there are no entries matching that mtditid" in {
      val result = await(repository.getBySessionId(testSession.mtditid))

      result.size shouldBe 0
    }
    "delete specified data" in {
      await(repository.set(testSession))
      await(repository.set(otherTestSession))
      await(repository.deleteOne(testSessionId, testSession.internalId, testSession.mtditid))
      val result = await(repository.get(testSessionId, testSession.internalId, testSession.mtditid))
      val otherResult = await(repository.get(otherTestSessionId, otherTestSession.internalId, otherTestSession.mtditid)).get
      result shouldBe None
      otherResult.sessionId shouldBe "session-xxx"
    }
  }

}
