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
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import testConstants.BaseTestConstants.{testInternalId, testSessionData}
import testConstants.IntegrationTestConstants.{itTestSessionId, testDefaultRequest, testDefaultSession, testValidRequest}
import uk.gov.hmrc.incometaxsessiondata.models.{Session, SessionData}
import uk.gov.hmrc.incometaxsessiondata.services.SessionService

class SessionControllerISpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with GuiceOneServerPerSuite
    with ComponentSpecBase {

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
    await(clearDb(sessionService.repository, testSession.sessionId))
    await(clearDb(sessionService.repository, itTestSessionId))
    await(clearDb(sessionService.repository, testSessionId))
  }

  val testSession: Session = Session(
    sessionId = testSessionId,
    mtditid = "id-123",
    nino = "nino-123",
    utr = "utr-123",
    internalId = testInternalId
  )

  "Sending a GET request to the session data service" should {

    "return some session data when auth as agent, with the user's current session id and internal id" when {
      "there is data in mongo under that id" in {
        UserDetailsStub.stubGetUserDetails()
        AuthStub.stubAuthorised(asAgent = true)
        await(sessionService.set(testDefaultSession))
        val result = get("/")
        result should have(
          httpStatus(OK)
        )
      }
    }

    "return error when auth as Individual" when {
      "there is data in mongo under that id" in {
        UserDetailsStub.stubGetUserDetails()
        AuthStub.stubAuthorised(asAgent = false)
        await(sessionService.set(testValidRequest))
        val result = get("/")
        result should have(
          httpStatus(UNAUTHORIZED)
        )
      }
    }

    "return Not Found" when {
      "there is no data in mongo with that id" in {
        await(sessionService.set(testSession))
        val result = get("/")
        result should have(
          httpStatus(NOT_FOUND)
        )
      }
    }
  }

  "Sending a POST request to the session data service" should {
    "add data to mongo when auth as agent" when {
      "data provided is valid" in {
        UserDetailsStub.stubGetUserDetails()
        AuthStub.stubAuthorised(asAgent = true)
        val result = post("/")(Json.toJson[Session](testValidRequest))

        result should have(
          httpStatus(200)
        )

        sessionService.get(testDefaultRequest).futureValue match {
          case Some(value) =>
            value.sessionId shouldBe testDefaultRequest.sessionId
            value.mtditid shouldBe testValidRequest.mtditid
            value.utr shouldBe testValidRequest.utr
            value.nino shouldBe testValidRequest.nino
          case None => fail("failed because result was None")
        }
      }
    }

    "add data to mongo when auth as individual" when {
      "not data found" in {
        UserDetailsStub.stubGetUserDetails()
        AuthStub.stubAuthorised(asAgent = false)
        val result = post("/")(Json.toJson[SessionData](testSessionData))
        result should have(httpStatus(UNAUTHORIZED))

        sessionService.get(testDefaultRequest).futureValue shouldBe None
      }
    }

    "return BAD_REQUEST when auth as agent" when {
      "data provided in invalid" in {
        UserDetailsStub.stubGetUserDetails()
        AuthStub.stubAuthorised(asAgent = true)

        val result = post("/")(Json.toJson[String]("not a valid session"))
        result should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }

    "return UNAUTHORIZED when auth as individual" when {
      "data provided in invalid" in {
        UserDetailsStub.stubGetUserDetails()
        AuthStub.stubAuthorised(asAgent = false)

        val result = post("/")(Json.toJson[String]("not a valid session"))
        result should have(
          httpStatus(UNAUTHORIZED)
        )
      }
    }


  }

}
