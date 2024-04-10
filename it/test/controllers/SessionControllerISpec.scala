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

package controllers

import helpers.ComponentSpecBase
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.incometaxsessiondata.models.SessionData
class SessionControllerISpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with ComponentSpecBase{

    def httpStatus(expectedValue: Int): HavePropertyMatcher[WSResponse, Int] =
        new HavePropertyMatcher[WSResponse, Int] {
            def apply(response: WSResponse) = {
                HavePropertyMatchResult(
                    response.status == expectedValue,
                    "httpStatus",
                    expectedValue,
                    response.status
                )
            }
        }

    val testSessionData: SessionData = SessionData(
        sessionID = "session-123",
        mtditid = "id-123",
        nino = "nino-123",
        saUtr = "utr-123",
        clientFirstName = Some("David"),
        clientLastName = None,
        userType = "Individual"
    )

    "Sending a GET request to the session data service" should {
        "return some session data" in {
            SessionDataHelpers.post("/")(Json.toJson[SessionData](testSessionData))
            val result = SessionDataHelpers.get("/session-123")
            result should have(
                httpStatus(OK)
            )
        }
    }

}
