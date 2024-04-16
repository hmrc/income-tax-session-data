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

package uk.gov.hmrc.incometaxsessiondata.domain

import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsPath, Reads, Writes}
import uk.gov.hmrc.incometaxsessiondata.models.Session

case class SessionData(sessionID: SessionId,
                       mtditid: String,
                       nino: String,
                       saUtr: String,
                       clientFirstName: Option[String],
                       clientLastName: Option[String],
                       userType: String)


object SessionData {

  implicit def string2SessionID(id: String) = SessionId(id)

  val sessionIdReadsBuilder = (JsPath \ "sessionID").read[SessionId]
  implicit val sessionIdReads: Reads[SessionId] = sessionIdReadsBuilder.map { rawJson =>
    SessionId(rawJson.value)
  }

  implicit val sessionDataReads : Reads[SessionData] = (
      (JsPath \ "sessionID").read[String] and
      (JsPath \ "mtditid").read[String] and
      (JsPath \ "nino").read[String] and
        (JsPath \ "saUtr").read[String] and
      (JsPath \ "clientFirstName").readNullable[String] and
      (JsPath \ "clientLastName").readNullable[String] and
      (JsPath \ "userType").read[String]
    ) ( SessionData( _, _, _, _, _, _, _) )

  implicit val sessionDataWrites: Writes[SessionData] = (
    (JsPath \ "sessionID").write[String] and
      (JsPath \ "mtditid").write[String] and
      (JsPath \ "nino").write[String] and
      (JsPath \ "saUtr").write[String] and
      (JsPath \ "clientFirstName").writeNullable[String] and
      (JsPath \ "clientLastName").writeNullable[String] and
      (JsPath \ "userType").write[String]
    )(s => (s.sessionID.value, s.mtditid, s.nino, s.saUtr, s.clientFirstName, s.clientLastName, s.userType))

  implicit val fromSession: Session => SessionData = session => SessionData(
    sessionID = SessionId(session.sessionID),
    mtditid = session.mtditid,
    nino = session.nino,
    saUtr = session.saUtr,
    clientFirstName = session.clientFirstName,
    clientLastName = session.clientLastName,
    userType = session.userType
  )
}