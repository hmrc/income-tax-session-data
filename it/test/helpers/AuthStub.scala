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
import play.api.libs.functional.syntax._
import play.api.libs.json._

object AuthStub extends ComponentSpecBase {

  val postAuthoriseUrl = "/auth/authorise"

  implicit val kvPairWrites: Writes[KVPair] = (
    (JsPath \ "key").write[String] and
      (JsPath \ "value").write[String]
    )(unlift(KVPair.unapply))

  implicit val enrolmentWrites: Writes[Enrolment] = (
    (JsPath \ "key").write[String] and
      (JsPath \ "identifiers").write[Seq[KVPair]] and
      (JsPath \ "state").write[String]
    )(unlift(Enrolment.unapply))

  private def successfulAuthResponse(asAgent: Boolean): JsObject = {
    val agentEnrolments = Seq(Enrolment(key = "HMRC-AS-AGENT",
      identifiers = Seq(KVPair(key = "AgentReferenceNumber", value = "9999")), state = "Activated"))
    val individualEnrolments =
      Seq(Enrolment(key = "HMRC-MTD-IT",
        identifiers = Seq(KVPair(key = "MTDITID", value = testMtditid)), state = "Activated"))

    val json = {
      if (asAgent)
        Json.toJson[Seq[Enrolment]](agentEnrolments)
      else
        Json.toJson[Seq[Enrolment]](individualEnrolments)
    }
    val x = Json.obj(
      "internalId" -> "123",
      "affinityGroup" -> { if (asAgent) "Agent" else "Individual" },
      "allEnrolments" -> json
    )
    println(s"BALANCING: ${x.value.mkString("\n ")}")
    x
  }

  def stubAuthorised(asAgent: Boolean): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.OK,
      successfulAuthResponse(asAgent).toString()
    )
  }

  def stubUnauthorised(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.UNAUTHORIZED, "{}")
  }

}
