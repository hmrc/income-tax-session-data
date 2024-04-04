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

import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{OFormat, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import uk.gov.hmrc.auth.core.AffinityGroup

import java.time.Instant

case class UserSessionDetails(sessionId: String,
                            nino: String,
                            mtditid: Option[String] = None,
                            utr: Option[String] = None,
                            clientFirstName: Option[String] = None,
                            clientLastName: Option[String] = None,
                            affinityGroup: Option[AffinityGroup] = None,
                            lastUpdated: Instant = Instant.now) {

}

object UserSessionDetails {

  implicit val format: OFormat[UserSessionDetails] = {

    ((__ \ "sessionId").format[String]
      ~ (__ \ "nino").format[String]
      ~ (__ \ "mtditid").formatNullable[String]
      ~ (__ \ "utr").formatNullable[String]
      ~ (__ \ "clientFirstName").formatNullable[String]
      ~ (__ \ "clientLastName").formatNullable[String]
      ~ (__ \ "affinityGroup").formatNullable[AffinityGroup]
      ~ (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat)
      )(UserSessionDetails.apply, unlift(UserSessionDetails.unapply)
    )
  }
}