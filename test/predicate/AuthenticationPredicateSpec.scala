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

package predicate

import auth.TestHeaderExtractor
import mocks.MockMicroserviceAuthConnector
import play.api.http.Status
import play.api.mvc.Results.{Ok, Unauthorized}
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.auth.core.MissingBearerToken
import uk.gov.hmrc.incometaxsessiondata.predicates.AuthenticationPredicate

import scala.concurrent.Future

class AuthenticationPredicateSpec extends MockMicroserviceAuthConnector {

  def fixture(): Object {
    val headerExtractor: TestHeaderExtractor;
    val mockCC: ControllerComponents;
    val predicate: AuthenticationPredicate
  } = new {
    val headerExtractor                   = new TestHeaderExtractor()
    lazy val mockCC: ControllerComponents = stubControllerComponents()
    val predicate                         = new AuthenticationPredicate(mockMicroserviceAuthConnector, mockCC, appConfig, headerExtractor)
  }

  def result(
    authenticationPredicate: AuthenticationPredicate,
    request: FakeRequest[AnyContentAsEmpty.type]
  ): Future[Result] = authenticationPredicate
    .async { _ =>
      Future.successful(Ok)
    }(request)
    .recoverWith {
      case ex if ex.getCause.getMessage.contains("Unable to extract mtditid") =>
        Future.successful(Unauthorized)
      case ex                                                                 =>
        Future.failed(ex)
    }

  "The AuthenticationPredicate.authenticated method" should {

    "called with an Unauthenticated user (No Bearer Token in Header)" in {
      val f            = fixture()
      mockAuth(Future.failed(new MissingBearerToken))
      val futureResult = result(f.predicate, fakeRequestWithActiveSession)
      futureResult.futureValue.header.status shouldBe Status.UNAUTHORIZED
    }

    "called with agent authenticated user (Some Bearer Token in Header)" in {
      val f            = fixture()
      mockAuth()
      val futureResult = result(f.predicate, fakeRequestWithActiveSession)
      futureResult.futureValue.header.status shouldBe Status.OK
    }

    "called with agent authenticated user and empty sessionId" in {
      val f            = fixture()
      mockAuth()
      val futureResult = result(authenticationPredicate = f.predicate, fakeRequestWithActiveSessionAndEmptySessionId)
      futureResult.futureValue.header.status shouldBe Status.UNAUTHORIZED
    }

    "called authenticated agent" in {
      val f            = fixture()
      mockAuth(Future.successful(agentResponse))
      val futureResult = result(f.predicate, fakeRequestWithActiveSession)
      futureResult.futureValue.header.status shouldBe Status.OK
    }

    "called authenticated individual with ConfidenceLevel50" in {
      val f            = fixture()
      mockAuth(Future.successful(individualAuthResponseWithCL50))
      val futureResult = result(f.predicate, fakeRequestWithActiveSession)
      futureResult.futureValue.header.status shouldBe Status.UNAUTHORIZED
    }

    "called authenticated individual with ConfidenceLevel250" in {
      val f            = fixture()
      mockAuth(Future.successful(individualAuthResponseWithCL250))
      val futureResult = result(f.predicate, fakeRequestWithActiveSession)
      futureResult.futureValue.header.status shouldBe Status.OK
    }

  }

}
