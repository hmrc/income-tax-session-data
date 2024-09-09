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

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, OFormat, Reads, Writes, __}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class Session(
                    mtditid: String,
                    nino: Crypted,
                    utr: Crypted,
                    internalId: String,
                    sessionId: String,
                    lastUpdated: Instant = Instant.now
)

object Session {

  private val crypter = SymmetricCryptoFactory.aesGcmCrypto("QmFyMTIzNDVCYXIxMjM0NQ==") //TODO: app.conf key

  def readsWithRequest(request: SessionDataRequest[_]): Reads[Session] =
    Reads[Session] { json =>
      for {
        mtditid <- (json \ "mtditid").validate[String]
        nino    <- (json \ "nino").validate[String]
        utr     <- (json \ "utr").validate[String]
      } yield Session(
        mtditid = mtditid,
        nino = crypter.encrypt(PlainText(nino)),
        utr = crypter.encrypt(PlainText(utr)),
        internalId = request.internalId,
        sessionId = request.sessionId
      )
    }

  private def stringEncrypter(implicit crypto: Encrypter): Writes[String] =
    implicitly[Writes[String]]
      .contramap[String](s => crypto.encrypt(PlainText(s)).value)

  private def stringDecrypter(implicit crypto: Decrypter): Reads[String] =
    implicitly[Reads[String]]
      .map[String](s => crypto.decrypt(Crypted(s)).value)

  private def decrypter(implicit crypto: Decrypter): Reads[Crypted] = {
    stringDecrypter.map(s => Crypted.apply(s))
  }

  private def encrypter(implicit crypto: Encrypter): Writes[Crypted] = {
    stringEncrypter.contramap(s => s.value)
  }

  implicit def encryptedStringFormat(implicit crypto: Encrypter with Decrypter): Format[Crypted] = {
    Format(decrypter, encrypter)
  }

  implicit def format(implicit crypto: Encrypter with Decrypter): OFormat[Session] =
    ((__ \ "mtditid").format[String]
      ~ (__ \ "nino").format[Crypted]
      ~ (__ \ "utr").format[Crypted]
      ~ (__ \ "internalId").format[String]
      ~ (__ \ "sessionId").format[String]
      ~ (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat))(Session.apply, unlift(Session.unapply))
}
