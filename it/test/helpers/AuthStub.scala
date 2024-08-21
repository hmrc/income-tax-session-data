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

package helpers

import play.api.http.Status
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import play.api.libs.functional.syntax._
import play.api.libs.json._

object AuthStub extends ComponentSpecBase {

  val postAuthoriseUrl = "/auth/authorise"

  val requiredConfidenceLevel = appConfig.confidenceLevel

  // TODO: refactor below imports
  case class KVPair(key: String, value: String)
  case class Enrolment(key: String, identifiers: Seq[KVPair], state: String)

  implicit val kvPairWrites: Writes[KVPair] = (
    (JsPath \ "key").write[String] and
      (JsPath \ "value").write[String]
    ) (unlift(KVPair.unapply))

  implicit val enrolmentWrites: Writes[Enrolment] = (
    (JsPath \ "key").write[String] and
      (JsPath \ "identifiers").write[Seq[KVPair]] and
      (JsPath \ "state").write[String]
    ) (unlift(Enrolment.unapply))

  private def successfulAuthResponse(confidenceLevel: Option[ConfidenceLevel]): JsObject = {
    val es = Seq(Enrolment(key = "HMRC-AS-AGENT", identifiers = Seq(KVPair(key = "AgentReferenceNumber", value = "1")), state = "Activated"))
    val json = Json.toJson[Seq[Enrolment]](es)
    confidenceLevel.fold(Json.obj())(unwrappedConfidenceLevel =>
      Json.obj(
        "confidenceLevel" -> unwrappedConfidenceLevel,
        "internalId" -> "123",
        "affinityGroup" -> "Agent",
        "allEnrolments" -> json
      )
    )
  }

  // TODO: only as Agent at this moment, do we really need to add tests cases for Individuals?
  def stubAuthorised(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.OK,
      successfulAuthResponse( Some(ConfidenceLevel.L250) ).toString()
    )
  }
  def stubUnauthorised(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.UNAUTHORIZED, "{}")
  }

}
