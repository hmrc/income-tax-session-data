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

import auth.TestHeaderExtractor
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.and
import org.mongodb.scala.result.DeleteResult
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, TestSuite}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.incometaxsessiondata.auth.HeaderExtractor
import uk.gov.hmrc.incometaxsessiondata.config.AppConfig
import uk.gov.hmrc.incometaxsessiondata.models.Session
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import scala.concurrent.Future

trait ComponentSpecBase extends TestSuite with GuiceOneServerPerSuite with ScalaFutures
  with IntegrationPatience with Matchers
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with Eventually{

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .overrides(bind[HeaderExtractor].to[TestHeaderExtractor])
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
      .get().futureValue
  }

  def post(uri: String)(body: JsValue): WSResponse = {
    buildClient(uri)
      .withFollowRedirects(false)
      .withHttpHeaders("Csrf-Token" -> "nocheck",  "X-Session-ID" -> testSessionId)
      .post(body).futureValue
  }

  def clearDb[T](repository: PlayMongoRepository[T], sessionId: String): Future[DeleteResult] = {
    repository.collection.deleteMany(dataFilter(sessionId)).toFuture()
  }

  private def dataFilter(sessionId: String): Bson = {
    and(org.mongodb.scala.model.Filters.equal("sessionId", sessionId))
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
  val testInternalId = "test-internal-id-integration"
  val testInternalIdAlternative = "test-internal-id-integration-alternative"

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

  val testSession: Session = Session(
    sessionId = testSessionId,
    mtditid = "id-123",
    nino = "nino-123",
    utr = "utr-123",
    internalId = testInternalId
  )

  val testSessionDifferentInternalId: Session = Session(
    sessionId = testSessionId,
    mtditid = "id-123",
    nino = "nino-123",
    utr = "utr-123",
    internalId = testInternalIdAlternative
  )

  case class KVPair(key: String, value: String)
  case class Enrolment(key: String, identifiers: Seq[KVPair], state: String)
}
