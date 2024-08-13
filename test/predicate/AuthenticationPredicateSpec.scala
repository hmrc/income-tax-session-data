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
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.auth.core.MissingBearerToken
import uk.gov.hmrc.incometaxsessiondata.predicates.AuthenticationPredicate

import scala.concurrent.Future

class AuthenticationPredicateSpec extends MockMicroserviceAuthConnector {

  def fixture() = new {
    val headerExtractor = new TestHeaderExtractor()
    lazy val mockCC = stubControllerComponents()
    val predicate = new AuthenticationPredicate(mockMicroserviceAuthConnector,
      mockCC, appConfig, headerExtractor)
  }

  def result(authenticationPredicate: AuthenticationPredicate,
             request: FakeRequest[AnyContentAsEmpty.type]): Future[Result] = authenticationPredicate.async {
    _ => Future.successful(Ok)
  }(request)

  "The AuthenticationPredicate.authenticated method" should {

    "called with an Unauthenticated user (No Bearer Token in Header)" in {
      val f = fixture()
      mockAuth(Future.failed(new MissingBearerToken))
      val futureResult = result(f.predicate, fakeRequestWithActiveSession)
      futureResult.futureValue.header.status shouldBe Status.UNAUTHORIZED
    }

    "called with an authenticated user (Some Bearer Token in Header)" in {
      val f = fixture()
      mockAuth()
      val futureResult = result(f.predicate, fakeRequestWithActiveSession)
      futureResult.futureValue.header.status shouldBe Status.OK
    }

    "called with an authenticated user and empty sessionId" in {
      val f = fixture()
      mockAuth()
      val futureResult = result(authenticationPredicate = f.predicate, fakeRequestWithActiveSessionAndEmptySessionId)
      futureResult.futureValue.header.status shouldBe Status.UNAUTHORIZED
    }

    "called with low confidence level" in {
      val f = fixture()
      mockAuth(Future.successful(individualAuthResponseWithCL50))
      val futureResult = result(f.predicate, fakeRequestWithActiveSession)
      futureResult.futureValue.header.status shouldBe Status.UNAUTHORIZED
    }

    "agent called with low confidence level" in {
      val f = fixture()
      mockAuth(Future.successful(agentResponseWithCL50))
      val futureResult = result(f.predicate, fakeRequestWithActiveSession)
      futureResult.futureValue.header.status shouldBe Status.OK
    }

  }

}
