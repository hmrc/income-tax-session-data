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

package uk.gov.hmrc.incometaxsessiondata.controllers

import auth.TestHeaderExtractor
import mocks.MockMicroserviceAuthConnector
import mocks.services.MockSessionService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.Json
import play.api.mvc.Results.{Conflict, Forbidden, InternalServerError, Ok}
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.incometaxsessiondata.auth.HeaderExtractor
import uk.gov.hmrc.incometaxsessiondata.models.{FullDuplicate, NonDuplicate, PartialDuplicate, SessionData}
import uk.gov.hmrc.incometaxsessiondata.predicates.AuthenticationPredicate

import scala.concurrent.Future

class SessionControllerSpec extends MockMicroserviceAuthConnector with MockSessionService {

  val cc: ControllerComponents = app.injector.instanceOf[ControllerComponents]
  val headerExtractor: HeaderExtractor = new TestHeaderExtractor()
  val authPredicate = new AuthenticationPredicate(mockMicroserviceAuthConnector, cc, appConfig, headerExtractor)

  object testSessionController extends SessionController(cc, authPredicate, mockSessionService)

  val testSessionData: SessionData = SessionData(
    sessionId = "session-123",
    mtditid = "id-123",
    nino = "nino-123",
    utr = "utr-123"
  )

  "SessionController.get" should {
    "return Ok" when {
      "data is returned from the service" in {
        when(mockSessionService.get(any())).thenReturn(Future(Some(testSessionData)))
        mockAuth()
        val result: Future[Result] = testSessionController.get()(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
    }
    "return not found" when {
      "there is no session data for the given request" in {
        when(mockSessionService.get(any())).thenReturn(Future(None))
        mockAuth()
        val result: Future[Result] = testSessionController.get()(fakeRequestWithActiveSession)
        status(result) shouldBe NOT_FOUND
      }
    }
    "recover" when {
      "unexpected error from service" in {
        when(mockSessionService.get(any())).thenReturn(Future.failed(new Error("")))
        val result: Future[Result] = testSessionController.get()(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "Recover" when {
      "Unauthorised error when sessionId empty" in {
        when(mockSessionService.get(any())).thenReturn(Future(Some(testSessionData)))

        val result: Future[Result] = testSessionController.get()(fakeRequestWithActiveSessionAndEmptySessionId)
        status(result) shouldBe UNAUTHORIZED
      }
    }
  }

  "SessionController.set" when {
    "the request body is invalid" should {
      "return a bad request" in {
        val result: Future[Result] = testSessionController.set()(fakeRequestWithActiveSession)
        status(result) shouldBe BAD_REQUEST
      }
    }
    "there is an unexpected error from service" should {
      "recover with an internal server error" in {
        setupMockHandleValidRequestFutureFailed()

        val result: Future[Result] = testSessionController.set()(
          fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
        )
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "SessionController.set" should {
    "pass through the result from the service" when {
      "the service returns an Ok result" in {
        setupMockHandleValidRequest(Ok("test 123"))

        val result: Future[Result] = testSessionController.set()(
          fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
        )
        status(result) shouldBe OK
      }
      "the service returns an Forbidden result" in {
        setupMockHandleValidRequest(Forbidden("test 123"))

        val result: Future[Result] = testSessionController.set()(
          fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
        )
        status(result) shouldBe FORBIDDEN
      }
      "the service returns an Conflict result" in {
        setupMockHandleValidRequest(Conflict("test 123"))

        val result: Future[Result] = testSessionController.set()(
          fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
        )
        status(result) shouldBe CONFLICT
      }
      "the service returns an InternalServerError result" in {
        setupMockHandleValidRequest(InternalServerError("test 123"))

        val result: Future[Result] = testSessionController.set()(
          fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
        )
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
