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
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.incometaxsessiondata.domain.{SessionData, SessionId}
import uk.gov.hmrc.incometaxsessiondata.predicates.AuthenticationPredicate
import uk.gov.hmrc.incometaxsessiondata.services.SessionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class SessionController @Inject()(cc: ControllerComponents,
                                  authentication: AuthenticationPredicate,
                                  sessionService: SessionService
                                 )(implicit ec: ExecutionContext)
    extends BackendController(cc) with Logging {

  def getById(sessionID: SessionId): Action[AnyContent] = authentication.async {  _ =>
    sessionService.get(sessionID) map {
      case Right(Some(session: SessionData)) =>
        logger.info(s"[SessionController][getById]: Successfully retrieved session data: $session")
        Ok(Json.toJson(session))
      case Right(None) =>
        logger.info(s"[SessionController][getById]: No live session")
        NotFound("No session data found")
      case Left(ex) =>
        logger.error(s"[SessionController][getById]: Failed to retrieve session with id: $sessionID - ${ex.getMessage}")
        InternalServerError(s"Failed to retrieve session with id: ${sessionID.value}")
    } recover {
      case ex =>
        logger.error(s"[SessionController][getById]: Unexpected error while getting session: $ex")
        InternalServerError(s"Unexpected error while getting session: ${ex.getMessage} - ${ex.getCause} - ${sessionID}")
    }
  }

  def set(): Action[AnyContent] = authentication.async { implicit request =>
    request.body.asJson.getOrElse(Json.obj())
      .validate[SessionData] match {
      case err: JsError =>
        logger.error(s"[SessionController][set]: Json validation error while parsing request: $err - ${request.body}")
        Future.successful(BadRequest(s"Json validation error while parsing request: $err"))
      case JsSuccess(validRequest, _) =>
        save(validRequest)
          .recover {
            case ex =>
              logger.error(s"[SessionController][set]: Unexpected error while setting session: $ex")
              InternalServerError(s"Unexpected error while setting session: $ex")
          }
    }
  }

  private def save(validRequest: SessionData): Future[Result] = {
    sessionService.set(validRequest) map {
      case true =>
        logger.info(s"[SessionController][save]: Successfully set session")
        Ok("Successfully set session")
      case false =>
        logger.error(s"[SessionController][save]: Failed to set session")
        InternalServerError("Failed to set session")
    }
  }
}
