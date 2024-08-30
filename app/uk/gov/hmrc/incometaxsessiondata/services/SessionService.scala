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

import uk.gov.hmrc.incometaxsessiondata.models.{FullDuplicate, PartialDuplicate, NonDuplicate, Session, SessionData, SessionDataRequest, SessionDuplicationType}
import uk.gov.hmrc.incometaxsessiondata.repositories.SessionDataRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class SessionService @Inject() (
  val repository: SessionDataRepository
)(implicit ec: ExecutionContext) {

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

}
