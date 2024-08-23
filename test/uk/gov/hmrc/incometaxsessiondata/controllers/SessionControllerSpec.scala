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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.incometaxsessiondata.auth.HeaderExtractor
import uk.gov.hmrc.incometaxsessiondata.models.SessionData
import uk.gov.hmrc.incometaxsessiondata.predicates.AuthenticationPredicate
import uk.gov.hmrc.incometaxsessiondata.services.SessionService

import scala.concurrent.Future

class SessionControllerSpec extends MockMicroserviceAuthConnector {

  val mockSessionService: SessionService = mock(classOf[SessionService])
  val cc: ControllerComponents           = app.injector.instanceOf[ControllerComponents]
  val headerExtractor: HeaderExtractor   = new TestHeaderExtractor()
  val authPredicate                      = new AuthenticationPredicate(mockMicroserviceAuthConnector, cc, appConfig, headerExtractor)

  object testSessionController extends SessionController(cc, authPredicate, mockSessionService)

  val testSessionData: SessionData = SessionData(
    sessionID = "session-123",
    mtditid = "id-123",
    nino = "nino-123",
    saUtr = "utr-123",
    clientFirstName = Some("David"),
    clientLastName = None,
    userType = "Individual"
  )

  "SessionController.getById" should {
    "Return successful" when {
      "Data is returned from the service" in {
        when(mockSessionService.get(any())).thenReturn(Future(Right(Some(testSessionData))))
        mockAuth()
        val result: Future[Result] = testSessionController.getById("123")(fakeRequestWithActiveSession)
        status(result) shouldBe OK
      }
    }
    "Return Ok" when {
      "Empty data is returned from service" in {
        when(mockSessionService.get(any())).thenReturn(Future(Right(None)))
        mockAuth()
        val result: Future[Result] = testSessionController.getById("123")(fakeRequestWithActiveSession)
        status(result) shouldBe NOT_FOUND
      }
    }
    "Return an error" when {
      "An error is returned from the service" in {
        when(mockSessionService.get(any())).thenReturn(Future(Left(new Error("Error"))))
        mockAuth()
        val result: Future[Result] = testSessionController.getById("123")(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "Recover" when {
      "Unexpected error from service" in {
        when(mockSessionService.get(any())).thenReturn(Future.failed(new Error("")))
        val result: Future[Result] = testSessionController.getById("123")(fakeRequestWithActiveSession)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "Recover" when {
      "Unauthorised error when sessionId empty" in {
        when(mockSessionService.get(any())).thenReturn(Future(Right(Some(testSessionData))))
        val result: Future[Result] = testSessionController.getById("123")(fakeRequestWithActiveSessionAndEmptySessionId)
        status(result) shouldBe UNAUTHORIZED
      }
    }
  }

  "SessionController.set" should {
    "Return successful" when {
      "the body is correct and the service returns true" in {
        when(mockSessionService.set(any())).thenReturn(Future(true))
        val result: Future[Result] = testSessionController.set()(
          fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
        )
        status(result) shouldBe OK
      }
    }
    "Return an error" when {
      "the service fails to set the session" in {
        when(mockSessionService.set(any())).thenReturn(Future(false))
        val result: Future[Result] = testSessionController.set()(
          fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
        )
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
    "return a bad request" when {
      "the request body is invalid" in {
        val result: Future[Result] = testSessionController.set()(fakeRequestWithActiveSession)
        status(result) shouldBe BAD_REQUEST
      }
    }
    "Recover" when {
      "Unexpected error from service" in {
        when(mockSessionService.set(any())).thenReturn(Future.failed(new Error("")))
        val result: Future[Result] = testSessionController.set()(
          fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
        )
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
