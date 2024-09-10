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

import play.api.Configuration
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json._
import uk.gov.hmrc.crypto.{Crypted, PlainText, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class EncryptedSession(
                             mtditid: String,
                             nino: Crypted,
                             utr: Crypted,
                             internalId: String,
                             sessionId: String,
                             lastUpdated: Instant = Instant.now
                           )

object EncryptedSession {

  def apply(session: Session, config: Configuration): EncryptedSession = {
    val crypter = SymmetricCryptoFactory.aesGcmCryptoFromConfig("encryption", config.underlying)
    EncryptedSession(
      mtditid = session.mtditid,
        nino = crypter.encrypt(PlainText(session.nino)),
        utr = crypter.encrypt(PlainText(session.utr)),
        internalId = session.internalId,
        sessionId = session.sessionId
    )
  }
  def unapply(encryptedSession: EncryptedSession, config: Configuration): Session = {
    val crypter = SymmetricCryptoFactory.aesGcmCryptoFromConfig("encryption", config.underlying)
    Session(
      mtditid = encryptedSession.mtditid,
      nino = crypter.decrypt(encryptedSession.nino).value,
      utr = crypter.decrypt(encryptedSession.utr).value,
      internalId = encryptedSession.internalId,
      sessionId = encryptedSession.sessionId
    )
  }

  private def decrypter(implicit reader: Reads[String]): Reads[Crypted] = {
    reader.map(s => Crypted.apply(s))
  }

  private def encrypter(implicit writer: Writes[String]): Writes[Crypted] = {
    writer.contramap(s => s.value)
  }

  implicit def encryptedStringFormat: Format[Crypted] = {
    Format(decrypter, encrypter)
  }

  implicit val format: OFormat[EncryptedSession] =
    ((__ \ "mtditid").format[String]
      ~ (__ \ "nino").format[Crypted]
      ~ (__ \ "utr").format[Crypted]
      ~ (__ \ "internalId").format[String]
      ~ (__ \ "sessionId").format[String]
      ~ (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat))(EncryptedSession.apply, unlift(EncryptedSession.unapply))
}
