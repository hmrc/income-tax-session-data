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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.{ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.incometaxsessiondata.models.SessionData
import uk.gov.hmrc.incometaxsessiondata.services.SessionService

import scala.concurrent.{ExecutionContext, Future}

class SessionControllerSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ScalaFutures{

  val mockSessionService: SessionService = mock(classOf[SessionService])
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  object testSessionController extends SessionController(
    app.injector.instanceOf[ControllerComponents],
    mockSessionService
  )

  val testSessionData: SessionData = SessionData(
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
        val result: Future[Result] = testSessionController.getById("123")(FakeRequest())
        status(result) shouldBe OK
      }
    }
    "Return Ok" when {
      "Empty data is returned from service" in {
        when(mockSessionService.get(any())).thenReturn(Future(Right(None)))
        val result: Future[Result] = testSessionController.getById("123")(FakeRequest())
        status(result) shouldBe OK
      }
    }
    "Return an error" when {
      "An error is returned from the service" in {
        when(mockSessionService.get(any())).thenReturn(Future(Left(new Error("Error"))))
        val result: Future[Result] = testSessionController.getById("123")(FakeRequest())
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "SessionController.set" should {
//    "Return successful" when {
//      "the body is correct and the service returns true" in {
//        when(mockSessionService.set(any())).thenReturn(Future(true))
//        val result: Future[Result] = testSessionController.set()(FakeRequest().withSession(
//          "clientFirstName" -> "Test",
//          "clientLastName" -> "User",
//          "mtditid" -> "1234567890",
//          "saUtr" -> "XAIT00000000015",
//          "nino" -> "testNino",
//          "userType" -> "Individual",
//          "sessionID" -> "session-123",
//          "lastUpdated" -> "0000"
//        ))
//        status(result) shouldBe OK
//      }
//    }
    "return a bad request" when {
      "the request body is invalid" in {
                val result: Future[Result] = testSessionController.set()(FakeRequest())
                status(result) shouldBe BAD_REQUEST
      }
    }
  }
}
