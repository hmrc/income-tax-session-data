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

package testConstants

import play.api.test.FakeRequest
import uk.gov.hmrc.incometaxsessiondata.models.{Session, SessionData, SessionDataRequest}

import java.time.Instant

object IntegrationTestConstants {

  val itTestSessionId: String = "it-test1123"
  val itTestMtditid: String = "it-testMtditid"
  val itTestInternalId: String = "it-testInternalId"
  val itTestNino: String = "it-testNino123"
  val itTestUtr: String = "it-testUtr123"
  val itTestLastUpdated: Instant = Instant.ofEpochMilli(270899)
  val defaultRequestSessionId = "xsession-12345"
  val defaultRequestInternalId = "123"
  val defaultRequestInternalIdAlternative = "123-alternative"

  val testRequest: SessionDataRequest[_] = SessionDataRequest(
    internalId = itTestInternalId,
    sessionId = itTestSessionId
  )(FakeRequest())

  val testDefaultRequest: SessionDataRequest[_] = SessionDataRequest(
    internalId = defaultRequestInternalId,
    sessionId = defaultRequestSessionId
  )(FakeRequest())

  val testDefaultSession: Session = Session(
    sessionId = defaultRequestSessionId,
    mtditid = itTestMtditid,
    nino = "nino-123",
    utr = "utr-123",
    internalId = defaultRequestInternalId
  )

  val testDefaultSessionAlternativeInternalId: Session = Session(
    sessionId = defaultRequestSessionId,
    mtditid = itTestMtditid,
    nino = "nino-123",
    utr = "utr-123",
    internalId = defaultRequestInternalIdAlternative
  )

  val testSessionData: SessionData = SessionData(
    sessionId = itTestSessionId,
    mtditid = itTestMtditid,
    nino = itTestNino,
    utr = itTestUtr
  )

  val testValidRequest: Session = Session(
    mtditid = itTestMtditid,
    nino = itTestNino,
    utr = itTestUtr,
    internalId = itTestInternalId,
    sessionId = itTestSessionId)

  val testSessionAllB: Session = Session(
    mtditid = "B",
    nino = "B",
    utr = "B",
    internalId = "B",
    sessionId = "B")

}
