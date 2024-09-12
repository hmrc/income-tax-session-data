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
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, PlainText, SymmetricCryptoFactory}
import uk.gov.hmrc.incometaxsessiondata.models.{EncryptedSession, Session, SessionData, SessionDataRequest}
import utils.TestSupport

import java.time.Instant

object BaseTestConstants extends TestSupport {

  val testMtditid: String               = "testMtditid"
  val testInternalId: String            = "123"
  val testInternalIdAlternative: String = "testInternalIdAlternative"
  val testSessionId: String             = "xsession-12345"
  val testNino: String                  = "testNino123"
  val testUtr: String                   = "testUtr123"
  val testLastUpdated: Instant          = Instant.ofEpochMilli(270899)

  val testRequest: SessionDataRequest[_] = SessionDataRequest(
    internalId = testInternalId,
    sessionId = testSessionId
  )(FakeRequest())

  val testSessionData: SessionData = SessionData(
    sessionId = testSessionId,
    mtditid = testMtditid,
    nino = testNino,
    utr = testUtr
  )

  val testValidRequest: Session = Session(
    mtditid = testMtditid,
    nino = testNino,
    utr = testUtr,
    internalId = testInternalId,
    sessionId = testSessionId
  )

  val testSession: Session = testValidRequest

  private val encryptionKey             = "QmFyMTIzNDVCYXIxMjM0NQ=="
  val crypter: Encrypter with Decrypter = SymmetricCryptoFactory.aesGcmCrypto(encryptionKey)

  val testEncryptedSession: EncryptedSession = EncryptedSession(
    mtditid = testMtditid,
    nino = crypter.encrypt(PlainText("testNino123")),
    utr = crypter.encrypt(PlainText("testUtr123")),
    internalId = testInternalId,
    sessionId = testSessionId
  )

  val testSessionAllA: Session = Session(mtditid = "A", nino = "A", utr = "A", internalId = "A", sessionId = "A")

  val testSessionDifferentInternalId: Session = Session(
    mtditid = testMtditid,
    nino = testNino,
    utr = testUtr,
    internalId = testInternalIdAlternative,
    sessionId = testSessionId
  )

}
