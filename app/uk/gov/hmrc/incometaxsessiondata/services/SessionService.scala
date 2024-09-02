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

package uk.gov.hmrc.incometaxsessiondata.services

import play.api.Logging
import play.api.mvc.Result
import play.api.mvc.Results.{Conflict, Forbidden, InternalServerError, Ok}
import uk.gov.hmrc.incometaxsessiondata.models.{FullDuplicate, NonDuplicate, PartialDuplicate, Session, SessionData, SessionDataRequest, SessionDuplicationType}
import uk.gov.hmrc.incometaxsessiondata.repositories.SessionDataRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class SessionService @Inject() (
  val repository: SessionDataRepository
)(implicit ec: ExecutionContext) extends Logging {

  def get(request: SessionDataRequest[_]): Future[Option[SessionData]] = {
    repository.get(request) map {
      case Some(data: Session) =>
        Some(SessionData.fromSession(data))
      case None => None
    }
  }

  def set(session: Session): Future[Either[Throwable, Boolean]] = {
    Try {
      repository.set(session)
    } match {
      case Failure(exception) => Future.successful(Left(exception))
      case Success(value) => value.flatMap(res => Future.successful(Right(res.wasAcknowledged)))
    }
  }

  def getDuplicationStatus(validRequest: Session): Future[SessionDuplicationType] = {
    repository.getBySessionId(validRequest.sessionId) map {
      case Nil => NonDuplicate
      case sessionList =>
        val requestIndex: IndexFields = IndexFields(validRequest.sessionId, validRequest.internalId)
        val condition: Boolean = sessionList.map(item => IndexFields(item.sessionId, item.internalId)).contains(requestIndex)

        if (condition) FullDuplicate else PartialDuplicate
    }
  }

  private case class IndexFields(sessionId: String, internalId: String)

  def handleValidRequest(validRequest: Session): Future[Result] = {
    getDuplicationStatus(validRequest) flatMap {
      case result@FullDuplicate =>
        logger.info(
          s"[SessionController][handleValidRequest]" +
            s" A session in the database matched the current session request, list of sessions: $result"
        )
        handleConflictScenario(validRequest)
      case PartialDuplicate =>
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
  }

  private def handleConflictScenario(validRequest: Session): Future[Result] = {
    set(validRequest) map {
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
    set(validRequest) map {
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
