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
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.incometaxsessiondata.models.{Session, SessionData}
import uk.gov.hmrc.incometaxsessiondata.predicates.AuthenticationPredicate
import uk.gov.hmrc.incometaxsessiondata.services.SessionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton()
class SessionController @Inject() (
  cc: ControllerComponents,
  authentication: AuthenticationPredicate,
  sessionService: SessionService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def get(): Action[AnyContent] = authentication.async { request =>
    // Here is required internalID => request.internalId and request.sessionId
    sessionService.get(request) map {
      case Some(session: Session) =>
        logger.info(s"[SessionController][get]: Successfully retrieved session data. SessionId: ${session.sessionId}")
        Ok(Json.toJson(SessionData.fromSession(session)))
      case None                   =>
        logger.info(s"[SessionController][get]: No live session")
        NotFound("No session data found")
    } recover { case ex =>
      logger.error(s"[SessionController][get]: Unexpected error while getting session: $ex")
      InternalServerError(s"Unexpected error while getting session: $ex")
    }
  }

  def set(): Action[AnyContent] = authentication.async { implicit request =>
    // Here is required internalID => request.internalId and request.sessionId
    request.body.asJson
      .getOrElse(Json.obj())
      .validate(Session.readsWithRequest(request)) match {
      case err: JsError               =>
        logger.error(s"[SessionController][set]: Json validation error while parsing request: $err")
        Future.successful(BadRequest(s"Json validation error while parsing request: $err"))
      case JsSuccess(validRequest, _) =>
        sessionService
          .handleValidRequest(validRequest)
          .recover { case ex =>
            logger.error(s"[SessionController][set]: Unexpected error while setting session: $ex")
            InternalServerError(s"Unexpected error while setting session: $ex")
          }
    }
  }

}
