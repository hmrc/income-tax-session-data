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

package uk.gov.hmrc.incometaxsessiondata.controllers

import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.incometaxsessiondata.models.MtdItUser
import uk.gov.hmrc.incometaxsessiondata.services.UserDetailsService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class UserDetailsController @Inject()(cc: ControllerComponents,
                                      userDetailsService: UserDetailsService
                                     )(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def getUser(mtditid: String): Action[AnyContent] = Action.async {
    userDetailsService.getUserDetails(mtditid) map {
      case Right(userDetails: MtdItUser) => Ok(Json.toJson(userDetails))
      case Left(_)                       => InternalServerError("Failed to retrieve user")
    }
  }

  def createUser(): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson.getOrElse(Json.obj())
      .validate[MtdItUser] match {
        case err: JsError               => Future.successful(BadRequest(s"Json validation error while parsing request: $err"))
        case JsSuccess(validRequest, _) => userDetailsService.createUserDetails(validRequest) map {
          case Right(_) => Ok(Json.toJson("Successfully created user"))
          case Left(_)  => InternalServerError(Json.toJson("Failed to create user"))
        }
    }
  }
}
