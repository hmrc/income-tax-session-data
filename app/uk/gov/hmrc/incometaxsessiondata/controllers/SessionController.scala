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
import uk.gov.hmrc.incometaxsessiondata.models.{FullDuplicate, MtditidDuplicate, NonDuplicate, Session, SessionData}
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
      case Right(Some(session: SessionData)) =>
        logger.info(s"[SessionController][getById]: Successfully retrieved session data: $session")
        Ok(Json.toJson(session))
      case Right(None)                       =>
        logger.info(s"[SessionController][getById]: No live session")
        NotFound("No session data found")
      case Left(ex)                          =>
        logger.error(
          s"[SessionController][getById]: Failed to retrieve session with mtditid: ${request.mtditid} - ${ex.getMessage}"
        )
        InternalServerError(s"Failed to retrieve session with mtditid: ${request.mtditid}")
    } recover { case ex =>
      logger.error(s"[SessionController][getById]: Unexpected error while getting session: $ex")
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
        handleValidRequest(validRequest)
          .recover { case ex =>
            logger.error(s"[SessionController][set]: Unexpected error while setting session: $ex")
            InternalServerError(s"Unexpected error while setting session: $ex")
          }
    }
  }

  private def handleValidRequest(validRequest: Session): Future[Result] = {
    for {
      sessions <- sessionService.getByMtditid(validRequest.mtditid)
      result <- sessionService.getDuplicationStatus(sessions, validRequest) match {
        case FullDuplicate =>
          logger.info(
            s"[SessionController][handleValidRequest]" +
              s" A session in the database matched the current session request, list of sessions: $sessions"
          )
          handleConflictScenario(validRequest)
        case MtditidDuplicate =>
          logger.info(
            s"[SessionController][handleValidRequest]" +
              s" Another document matching mtditid: ${validRequest.mtditid} but different sessionId: ${validRequest.sessionId} and internalId ${validRequest.internalId}"
          )
          Future.successful(
            Forbidden(
              s"Another document matching mtditid: ${validRequest.mtditid} but different sessionId: ${validRequest.sessionId} and internalId ${validRequest.internalId}"
            )
          )
        case NonDuplicate =>
          logger.info(s"[SessionController][handleValidRequest]: No live sessions matching mtditid: ${validRequest.mtditid}")
          handleOkScenario(validRequest)
      }
    } yield result
  }

  private def handleConflictScenario(validRequest: Session): Future[Result] = {
    sessionService.set(validRequest) map {
      case Right(true) =>
        logger.info(
          s"[SessionController][handleConflictScenario]: Successfully set session despite matching documenting existing in the database"
        )
        Conflict("Successfully set session despite matching documenting existing in the database")
      case Right(false) =>
        logger.info(s"[SessionController][handleConflictScenario]: Write operation was not acknowledged")
        InternalServerError("Write operation was not acknowledged")
      case Left(ex: Throwable) =>
        logger.error(
          s"[SessionController][handleConflictScenario]: Unknown exception < Message: ${ex.getMessage}, Cause: ${ex.getCause} >"
        )
        InternalServerError("Unknown exception")
    }
  }

  private def handleOkScenario(validRequest: Session): Future[Result] = {
    sessionService.set(validRequest) map {
      case Right(true) =>
        logger.info(s"[SessionController][handleOkScenario]: Successfully set session")
        Ok("Successfully set session")
      case Right(false) =>
        logger.info(s"[SessionController][handleOkScenario]: Write operation was not acknowledged")
        InternalServerError("Write operation was not acknowledged")
      case Left(ex: Throwable) =>
        logger.error(
          s"[SessionController][handleOkScenario]: Unknown exception < Message: ${ex.getMessage}, Cause: ${ex.getCause} >"
        )
        InternalServerError("Unknown exception")
    }
  }

}
