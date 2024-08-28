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
import play.api.libs.json.{OFormat, Reads, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class Session(mtditid: String,
                   nino: String,
                   utr: String,
                   internalId: String,
                   sessionId: String,
                   lastUpdated: Instant = Instant.now)

object Session {


  def readsWithRequest(request: SessionDataRequest[_]): Reads[Session] = {
    Reads[Session] { json =>
      for {
        mtditid <- (json \ "mtditid").validate[String]
        nino <- (json \ "nino").validate[String]
        utr <- (json \ "utr").validate[String]
      } yield Session(
        mtditid = mtditid,
        nino = nino,
        utr = utr,
        internalId = request.internalId,
        sessionId = request.sessionId
      )
    }
  }

  implicit val format: OFormat[Session] = {

    ((__ \ "mtditid").format[String]
      ~ (__ \ "nino").format[String]
      ~ (__ \ "utr").format[String]
      ~ (__ \ "internalId").format[String]
      ~ (__ \ "sessionId").format[String]
      ~ (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat))(Session.apply, unlift(Session.unapply))
  }
}
