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

case class SessionData(sessionID: String,
                       mtditid: String,
                       nino: String,
                       saUtr: String,
                       clientFirstName: Option[String],
                       clientLastName: Option[String],
                       userType: String)

object SessionData {
  implicit val fromSession: Session => SessionData = session => SessionData(
    sessionID = session.sessionID,
    mtditid = session.mtditid,
    nino = session.nino,
    saUtr = session.saUtr,
    clientFirstName = session.clientFirstName,
    clientLastName = session.clientLastName,
    userType = session.userType
  )
  implicit val format: Format[SessionData] = Json.format[SessionData]
}
