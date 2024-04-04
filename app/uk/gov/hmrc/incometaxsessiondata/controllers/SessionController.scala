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

import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.incometaxsessiondata.domain.models.Session
import uk.gov.hmrc.incometaxsessiondata.services.SessionService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class SessionController @Inject()(cc: ControllerComponents,
                                  sessionService: SessionService
                                 )(implicit ec: ExecutionContext)
    extends BackendController(cc) with Logging {

  def getById(sessionID: String): Action[AnyContent] = Action.async {
    sessionService.getSession(sessionID) map {
      case Right(session: Session) =>
        logger.info(s"[SessionController][getById]: Successfully retrieved session: $session")
        Ok(Json.toJson(session))
      case Left(_) =>
        logger.error(s"[SessionController][getById]: Failed to retrieve session with id: $sessionID")
        InternalServerError(s"Failed to retrieve session with id: $sessionID")
    }
  }

  def create(): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson.getOrElse(Json.obj())
      .validate[Session] match {
        case err: JsError =>
          logger.error(s"[SessionController][create]: Json validation error while parsing request: $err")
          Future.successful(BadRequest(s"Json validation error while parsing request: $err"))
        case JsSuccess(validRequest, _) => sessionService.createSession(validRequest) map {
          case Right(_) =>
            logger.info(s"[SessionController][create]: Successfully created session")
            Ok(Json.toJson("Successfully created session"))
          case Left(_) =>
            logger.error(s"[SessionController][create]: Failed to create session")
            InternalServerError(Json.toJson("Failed to create session"))
        }
    }
  }
}