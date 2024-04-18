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

package helpers

import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, TestSuite}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.incometaxsessiondata.config.AppConfig

trait ComponentSpecBase extends TestSuite with GuiceOneServerPerSuite with ScalaFutures
  with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually{

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .build()

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  def get(uri: String): WSResponse = {
    buildClient(uri)
      .withHttpHeaders( "X-Session-ID" -> testSessionId)
      .withHttpHeaders("Authorization" -> "Bearer123")
      .get().futureValue
  }

  def post(uri: String)(body: JsValue): WSResponse = {
    buildClient(uri)
      .withFollowRedirects(false)
      .withHttpHeaders("Csrf-Token" -> "nocheck",  "X-Session-ID" -> testSessionId)
      .withHttpHeaders("Authorization" -> "Bearer123")
      .post(body).futureValue
  }

  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val testUserTypeIndividual = Individual
  val testUserTypeAgent = Agent

  val testMtditidEnrolmentKey = "HMRC-MTD-IT"
  val testMtditidEnrolmentIdentifier = "MTDITID"
  val testMtditid = "XAITSA123456"
  val testUserName = "Albert Einstein"

  val testSaUtrEnrolmentKey = "IR-SA"
  val testSaUtrEnrolmentIdentifier = "UTR"
  val testSaUtr = "1234567890"
  val credId = "12345-credId"
  val testSessionId = "xsession-12345"
  val testArn = "XAIT0000123456"

  val testNinoEnrolmentKey = "HMRC-NI"
  val testNinoEnrolmentIdentifier = "NINO"
  val testNino = "AA123456A"
  val testCalcId = "01234567"
  val testCalcId2 = "01234568"

  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString
  val mockUrl: String = s"http://$mockHost:$mockPort"
  val userDetailsUrl = "/user-details/id/5397272a3d00003d002f3ca9"
  val testUserDetailsWiremockUrl: String = mockUrl + userDetailsUrl
}
