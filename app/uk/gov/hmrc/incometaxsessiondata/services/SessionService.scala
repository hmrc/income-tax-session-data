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
import play.api.mvc.Results.{Conflict, InternalServerError, Ok}
import uk.gov.hmrc.incometaxsessiondata.models._
import uk.gov.hmrc.incometaxsessiondata.repositories.SessionDataRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class SessionService @Inject() (
  val repository: SessionDataRepository
)(implicit ec: ExecutionContext) extends Logging {

  def get(request: SessionDataRequest[_]): Future[Option[SessionData]] = {
    repository.get(request.sessionId, request.internalId) map {
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

  private def getDuplicationStatus(validRequest: Session): Future[SessionDuplicationType] = {
    repository.get(validRequest.sessionId, validRequest.internalId) map {
      case Some(_) => FullDuplicate
      case _ => NonDuplicate
    }
  }

  def handleValidRequest(validRequest: Session): Future[Result] = {
    getDuplicationStatus(validRequest) flatMap {
      case result@FullDuplicate =>
        logger.info(
          s"[SessionService][handleValidRequest] A session in the database matched the current session request, list of sessions: $result"
        )
        handleConflictScenario(validRequest)
      case NonDuplicate =>
        logger.info(s"[SessionService][handleValidRequest]: No live sessions matching mtditid: ${validRequest.mtditid}")
        handleOkScenario(validRequest)
    }
  }

  private def handleConflictScenario(validRequest: Session): Future[Result] = {
    set(validRequest) map {
      case Right(true) =>
        logger.info(
          s"[SessionService][handleConflictScenario]: Successfully set session despite matching documenting existing in the database"
        )
        Conflict("Successfully set session despite matching documenting existing in the database")
      case Right(false) => logAndReturnInternalServerError("Write operation was not acknowledged", None)
      case Left(ex: Throwable) => logAndReturnInternalServerError("Unknown exception", Some(ex))
    }
  }

  private def handleOkScenario(validRequest: Session): Future[Result] = {
    set(validRequest) map {
      case Right(true) =>
        logger.info(s"[SessionService][handleOkScenario]: Successfully set session")
        Ok("Successfully set session")
      case Right(false) => logAndReturnInternalServerError("Write operation was not acknowledged", None)
      case Left(ex: Throwable) => logAndReturnInternalServerError("Unknown exception", Some(ex))
    }
  }

  private def logAndReturnInternalServerError(message: String, ex: Option[Throwable]): Result = {
    ex match {
      case Some(value) => logger.error(
        s"[SessionService][logAndReturnInternalServerError] $message < Message: ${value.getMessage}, Cause: ${value.getCause} >"
      )
      case None => logger.error(
        s"[SessionService][logAndReturnInternalServerError] $message"
      )
    }

    InternalServerError(message)
  }

}
