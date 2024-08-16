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

package services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.incometaxsessiondata.models.{Session, SessionData}
import uk.gov.hmrc.incometaxsessiondata.repositories.SessionDataRepository
import uk.gov.hmrc.incometaxsessiondata.services.SessionService

import scala.concurrent.{ExecutionContext, Future}

class SessionServiceSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ScalaFutures{

  val mockRepository: SessionDataRepository = mock(classOf[SessionDataRepository])
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  object testSessionService extends SessionService(
    mockRepository
  )(ec)

  val testSession: Session = Session(
    sessionId = "session-123",
    mtditid = "id-123",
    nino = "nino-123",
    utr = "utr-123",
    internalId = "test-internal-id"
  )

  val testSessionData: SessionData = SessionData(
    sessionId ="session-123",
    mtditid = "id-123",
    nino = "nino-123",
    utr = "utr-123",
    internalId = "test-internal-id"
  )

  "SessionService.get" should {
    "return session data" when {
      "data returned from the repository" in {
        when(mockRepository.get(any(), any(), any())).thenReturn(Future(Some(testSession)))
        val result = testSessionService.get("test-session", "test-internal", "test-mtditid")
        result.futureValue shouldBe Right(Some(testSessionData))
      }
    }
    "return None" when {
      "no data returned from repository" in {
        when(mockRepository.get(any(), any(), any())).thenReturn(Future(None))
        val result = testSessionService.get("test-session", "test-internal", "test-mtditid")
        result.futureValue shouldBe Right(None)
      }
    }
  }

  "SessionService.set" should {
    "return true" when {
      "repository returns true" in {
        when(mockRepository.set(any())).thenReturn(Future(true))
        val result = testSessionService.set(testSession)
        result.futureValue shouldBe true
      }
    }
    "return false" when {
      "repository returns false" in {
        when(mockRepository.set(any())).thenReturn(Future(false))
        val result = testSessionService.set(testSession)
        result.futureValue shouldBe false
      }
    }
  }

}
