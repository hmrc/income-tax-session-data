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

package uk.gov.hmrc.incometaxsessiondata.domain.models

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{OFormat, __}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class Session(sessionID: String,
                   mtditid: Option[String] = None,
                   nino: Option[String] = None,
                   saUtr: Option[String] = None,
                   clientFirstName: Option[String] = None,
                   clientLastName: Option[String] = None,
                   userType: Option[String] = None,
                   lastUpdated: Instant = Instant.now)

object Session {
  implicit val format: OFormat[Session] = {

    ((__ \ "sessionID").format[String]
      ~ (__ \ "mtditid").formatNullable[String]
      ~ (__ \ "nino").formatNullable[String]
      ~ (__ \ "saUtr").formatNullable[String]
      ~ (__ \ "clientFirstName").formatNullable[String]
      ~ (__ \ "clientLastName").formatNullable[String]
      ~ (__ \ "userType").formatNullable[String]
      ~ (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat)
      )(Session.apply, unlift(Session.unapply)
    )
  }
}
