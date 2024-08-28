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

  def get(mtditid: String): Action[AnyContent] = authentication.async { request =>
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
          s"[SessionController][getById]: Failed to retrieve session with mtditid: $mtditid - ${ex.getMessage}"
        )
        InternalServerError(s"Failed to retrieve session with mtditid: $mtditid")
    } recover { case ex =>
      logger.error(s"[SessionController][getById]: Unexpected error while getting session: $ex")
      InternalServerError(s"Unexpected error while getting session: $ex")
    }
  }

  def getByMtditid(mtditid: String): Action[AnyContent] = authentication.async { _ =>
    sessionService.getByMtditid(mtditid) map {
      case sessionList: List[SessionData] if sessionList.nonEmpty =>
        logger.info(
          s"[SessionController][getByMtditid]" +
            s" Successfully retrieved a list of session data that matches the given mtditid: $mtditid, list of sessions: $sessionList"
        )
        Ok(Json.toJson(sessionList))
      case sessionList: List[SessionData] if sessionList.isEmpty  =>
        logger.info(s"[SessionController][getByMtditid]: No live sessions matching mtditid: $mtditid")
        NotFound("No session data found for this mtditid")
    } recover { case ex =>
      logger.error(
        s"[SessionController][getByMtditid]: Unexpected error while getting sessions matching mtditid: $mtditid. Exception: $ex"
      )
      InternalServerError(s"Unexpected error while getting session using mtditid: $ex")
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
        save(validRequest)
          .recover { case ex =>
            logger.error(s"[SessionController][set]: Unexpected error while setting session: $ex")
            InternalServerError(s"Unexpected error while setting session: $ex")
          }
    }
  }

  private def save(validRequest: Session) =
    sessionService.getByMtditid(validRequest.mtditid) flatMap {
      case sessionList: List[Session] if sessionList.nonEmpty && !isFullDuplicate(sessionList, validRequest) =>
        logger.info(
          s"[SessionController][save]" +
            s" Another document matching mtditid: ${validRequest.mtditid} but different sessionId: ${validRequest.sessionId} and internalId ${validRequest.internalId}"
        )
        Future.successful(
          Forbidden(
            s"Another document matching mtditid: ${validRequest.mtditid} but different sessionId: ${validRequest.sessionId} and internalId ${validRequest.internalId}"
          )
        )
      case sessionList: List[Session] if sessionList.nonEmpty && isFullDuplicate(sessionList, validRequest)  =>
        logger.info(
          s"[SessionController][save]" +
            s" A session in the database matched the current session request, list of sessions: $sessionList"
        )
        handleConflictScenario(validRequest)
      case sessionList: List[Session] if sessionList.isEmpty                                                 =>
        logger.info(s"[SessionController][save]: No live sessions matching mtditid: ${validRequest.mtditid}")
        handleOkScenario(validRequest)
    }

  private def handleConflictScenario(validRequest: Session) =
    sessionService.set(validRequest) map {
      case Right(true)         =>
        logger.info(
          s"[SessionController][handleConflictScenario]: Successfully set session despite matching documenting existing in the database"
        )
        Conflict("Successfully set session despite matching documenting existing in the database")
      case Right(false)        =>
        logger.info(s"[SessionController][handleConflictScenario]: Write operation was not acknowledged")
        InternalServerError("Write operation was not acknowledged")
      case Left(ex: Throwable) =>
        logger.error(
          s"[SessionController][handleConflictScenario]: Unknown exception < Message: ${ex.getMessage}, Cause: ${ex.getCause} >"
        )
        InternalServerError("Unknown exception")
    }

  private def handleOkScenario(validRequest: Session) =
    sessionService.set(validRequest) map {
      case Right(true)         =>
        logger.info(s"[SessionController][handleOkScenario]: Successfully set session")
        Ok("Successfully set session")
      case Right(false)        =>
        logger.info(s"[SessionController][handleOkScenario]: Write operation was not acknowledged")
        InternalServerError("Write operation was not acknowledged")
      case Left(ex: Throwable) =>
        logger.error(
          s"[SessionController][handleOkScenario]: Unknown exception < Message: ${ex.getMessage}, Cause: ${ex.getCause} >"
        )
        InternalServerError("Unknown exception")
    }

  private def isFullDuplicate(sessionList: List[Session], validRequest: Session): Boolean = {
    val requestIndex: IndexFields = IndexFields(validRequest.sessionId, validRequest.internalId, validRequest.mtditid)
    sessionList.map(item => IndexFields(item.sessionId, item.internalId, item.mtditid)).contains(requestIndex)
  }

  private case class IndexFields(sessionId: String, internalId: String, mtditid: String)

}
