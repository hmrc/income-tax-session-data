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

object AuthStub extends ComponentSpecBase {

  val postAuthoriseUrl = "/auth/authorise"

  val requiredConfidenceLevel = appConfig.confidenceLevel

  val testAuthInternalId: String = "123"
  private def successfulAuthResponse(confidenceLevel: Option[ConfidenceLevel]): JsObject = {
    confidenceLevel.fold(Json.obj())(unwrappedConfidenceLevel =>
      Json.obj("confidenceLevel" -> unwrappedConfidenceLevel, "internalId" -> testAuthInternalId) )
  }

  def stubAuthorised(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.OK,
      successfulAuthResponse( Some(ConfidenceLevel.L250) ).toString()
    )
  }
  def stubUnauthorised(): Unit = {
    WiremockHelper.stubPost(postAuthoriseUrl, Status.UNAUTHORIZED, "{}")
  }

}
