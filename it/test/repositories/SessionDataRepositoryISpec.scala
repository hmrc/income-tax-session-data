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
  with GuiceOneServerPerSuite{

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  private val repository = app.injector.instanceOf[SessionDataRepository]

  def beforeEach(): Unit = {
    await(repository.deleteOne(testSessionId, testSession.internalId, testSession.mtditid))
    await(repository.deleteOne(otherTestSessionId, otherTestSession.internalId, otherTestSession.mtditid))
  }

  val testSessionId = "session-123"
  val otherTestSessionId = "session-xxx"

  val testSession = Session(
    sessionId = testSessionId,
    mtditid = "testId",
    nino = "testNino",
    utr = "testUtr",
    internalId = "test-internal-id"
  )
  val otherTestSession = Session(
    sessionId = otherTestSessionId,
    mtditid = "testId",
    nino = "testNino",
    utr = "testUtr",
    internalId = "test-internal-id"
  )

  "Session Data Repository" should {
    "set some data" in {
      val acknowledged = await(repository.set(testSession))
      acknowledged shouldBe true
    }
    "get some data" in {
      await(repository.set(testSession))
      val result = await(repository.get(testSessionId, testSession.internalId, testSession.mtditid)).get
      result.mtditid shouldBe "testId"
      result.utr shouldBe "testUtr"
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
