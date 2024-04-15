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

import helpers.{AuthStub, ComponentSpecBase, UserDetailsStub}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.incometaxsessiondata.models.SessionData
import uk.gov.hmrc.incometaxsessiondata.services.SessionService
class SessionControllerISpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with ComponentSpecBase{

    def httpStatus(expectedValue: Int): HavePropertyMatcher[WSResponse, Int] =
        (response: WSResponse) => {
            HavePropertyMatchResult(
                response.status == expectedValue,
                "httpStatus",
                expectedValue,
                response.status
            )
        }


    val sessionService: SessionService = app.injector.instanceOf[SessionService]

    override def beforeEach(): Unit = {
        super.beforeEach()
        await(sessionService.deleteSession(testSessionData.sessionID))
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
        "return some session data" when {
            "there is data in mongo under that id" in {
                UserDetailsStub.stubGetUserDetails()
                AuthStub.stubAuthorised()
                await(sessionService.set(testSessionData))
                val result = get("/session-123")
                result should have(
                    httpStatus(OK)
                )
            }
        }
        "return Not Found" when {
            "there is no data in mongo with that id" in {
                await(sessionService.set(testSessionData))
                val result = get("/session-999")
                result should have(
                    httpStatus(NOT_FOUND)
                )
            }
        }
    }

    "Sending a POST request to the sesison data service" should {
        "add data to mongo" when {
            "data provided is valid" in {
                val result = post("/")(Json.toJson[SessionData](testSessionData))
                sessionService.get(testSessionData.sessionID).futureValue shouldBe Right(Some(testSessionData))
                result should have(
                    httpStatus(OK)
                )
            }
        }
        "return BAD_REQUEST" when {
            "data provided in invalid" in {
                val result = post("/")(Json.toJson[String]("not a valid session"))
                result should have(
                    httpStatus(BAD_REQUEST)
                )
            }
        }
    }

}
