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
import play.api.mvc.Results.InternalServerError
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
        when(mockSessionService.set(any())).thenReturn(Future.failed(new Error("")))
        setupMockGenericGetDuplicationStatus()

        val result: Future[Result] = testSessionController.set()(
          fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
        )
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "SessionController.set" when {
    "record is not a duplicate" when {
      "the service adds the record to the database successfully" should {
        "return an Ok response" in {
          when(mockSessionService.set(any())).thenReturn(Future(Right(true)))
          setupMockGetDuplicationStatus(NonDuplicate)

          val result: Future[Result] = testSessionController.set()(
            fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
          )
          status(result) shouldBe OK
        }
      }
      "the repository does not acknowledge the database operation" should {
        "return an error" in {
          when(mockSessionService.set(any())).thenReturn(Future(Right(false)))
          setupMockGetDuplicationStatus(NonDuplicate)

          val result: Future[Result] = testSessionController.set()(
            fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
          )
          result.futureValue shouldBe InternalServerError("Write operation was not acknowledged")
        }
      }
      "the service returns an exception" should {
        "return an error" in {
          when(mockSessionService.set(any())).thenReturn(Future(Left(new Exception("Test error!"))))
          setupMockGetDuplicationStatus(NonDuplicate)

          val result: Future[Result] = testSessionController.set()(
            fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
          )
          result.futureValue shouldBe InternalServerError("Unknown exception")
        }
      }
    }

    "record is a partial duplicate" when {
      "the service adds the record to the database successfully" should {
        "return a Forbidden response" in {
          when(mockSessionService.set(any())).thenReturn(Future(Right(true)))
          setupMockGetDuplicationStatus(PartialDuplicate)

          val result: Future[Result] = testSessionController.set()(
            fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
          )
          status(result) shouldBe FORBIDDEN
        }
      }
    }

    "record is a full duplicate" when {
      "the service adds the record to the database successfully" should {
        "return a Conflict response" in {
          when(mockSessionService.set(any())).thenReturn(Future(Right(true)))
          setupMockGetDuplicationStatus(FullDuplicate)

          val result: Future[Result] = testSessionController.set()(
            fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
          )
          status(result) shouldBe CONFLICT
        }
      }
      "the repository does not acknowledge the database operation" should {
        "return an error" in {
          when(mockSessionService.set(any())).thenReturn(Future(Right(false)))
          setupMockGetDuplicationStatus(FullDuplicate)

          val result: Future[Result] = testSessionController.set()(
            fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
          )
          result.futureValue shouldBe InternalServerError("Write operation was not acknowledged")
        }
      }
      "the service returns an exception" should {
        "return an error" in {
          when(mockSessionService.set(any())).thenReturn(Future(Left(new Exception("Test error!"))))
          setupMockGetDuplicationStatus(FullDuplicate)

          val result: Future[Result] = testSessionController.set()(
            fakeRequestWithActiveSession.withJsonBody(Json.toJson[SessionData](testSessionData))
          )
          result.futureValue shouldBe InternalServerError("Unknown exception")
        }
      }
    }
  }
}
