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

import mocks.MockMicroserviceAuthConnector
import play.api.http.Status
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, MissingBearerToken}
import uk.gov.hmrc.incometaxsessiondata.predicates.{AuthenticationPredicateV2}

import scala.concurrent.Future

class AuthenticationPredicateSpec extends MockMicroserviceAuthConnector {

  def fixture =
    new {
      lazy val mockCC = stubControllerComponents()
      val predicate = new AuthenticationPredicateV2(mockMicroserviceAuthConnector,
        mockCC, appConfig)
    }

  def result(authenticationPredicate: AuthenticationPredicateV2): Future[Result] = authenticationPredicate.async {
    request =>
      Future.successful(Ok)
  }.apply(FakeRequest())

  "The AuthenticationPredicate.authenticated method" should {

    "called with an Unauthenticated user (No Bearer Token in Header)" in {
      val f = fixture
      mockAuth(Future.failed(new MissingBearerToken))
      val futureResult = result(f.predicate)
      futureResult.futureValue.header.status shouldBe Status.UNAUTHORIZED
    }

    "called with an authenticated user (Some Bearer Token in Header)" in {
      val f = fixture
      mockAuth()
      val futureResult = result(f.predicate)
      futureResult.futureValue.header.status shouldBe Status.OK
    }

    //
    //    "called with low confidence level" when {
    //      mockAuth(Future.successful(Some(AffinityGroup.Individual) and ConfidenceLevel.L50))
    //      val futureResult = result(TestAuthenticationPredicate)
    //      whenReady(futureResult){  res =>
    //        checkStatusOf(res)(Status.UNAUTHORIZED)
    //      }
    //    }
    //
    //    "agent called with low confidence level" when {
    //      mockAuth(Future.successful(Some(AffinityGroup.Agent) and ConfidenceLevel.L50))
    //      val futureResult = result(TestAuthenticationPredicate)
    //      whenReady(futureResult){ res =>
    //        checkStatusOf(res)(Status.OK)
    //      }
    //    }

  }

}
