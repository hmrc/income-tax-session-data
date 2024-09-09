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

package uk.gov.hmrc.incometaxsessiondata.models

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.crypto.SymmetricCryptoFactory

case class SessionData(
  mtditid: String,
  nino: String,
  utr: String,
  sessionId: String
)

object SessionData {

  private val crypter = SymmetricCryptoFactory.aesGcmCrypto("QmFyMTIzNDVCYXIxMjM0NQ==") //TODO: app.conf key

  implicit val fromSession: Session => SessionData = session =>
    SessionData(
      mtditid = session.mtditid,
      nino = crypter.decrypt(session.nino).value,
      utr = crypter.decrypt(session.utr).value,
      sessionId = session.sessionId
    )
  implicit val format: Format[SessionData]         = Json.format[SessionData]

}
