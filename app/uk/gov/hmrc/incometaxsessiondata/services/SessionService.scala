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

import uk.gov.hmrc.incometaxsessiondata.models.{FullDuplicate, MtditidDuplicate, NonDuplicate, Session, SessionData, SessionDataRequest, SessionDuplicationType}
import uk.gov.hmrc.incometaxsessiondata.repositories.SessionDataRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SessionService @Inject() (
  val repository: SessionDataRepository
)(implicit ec: ExecutionContext) {

  def get(request: SessionDataRequest[_]): Future[Either[Throwable, Option[SessionData]]] = {
    repository.get(request) map {
      case Some(data: Session) =>
        Right(Some(SessionData.fromSession(data)))
      case None => Right(None)
    }
  }

  def getByMtditid(mtditid: String): Future[Seq[Session]] =
    repository.getByMtditid(mtditid)

  def set(session: Session): Future[Either[Throwable, Boolean]] = {
    try
      repository.set(session).flatMap(res => Future.successful(Right(res.wasAcknowledged)))
    catch {
      case ex: Throwable => Future.successful(Left(ex))
    }
  }

  def getDuplicationStatus(sessionListForMtditid: Seq[Session], validRequest: Session): SessionDuplicationType = {
    sessionListForMtditid match {
      case Nil => NonDuplicate
      case _ =>
        val requestIndex: IndexFields = IndexFields(validRequest.sessionId, validRequest.internalId, validRequest.mtditid)
        val condition: Boolean = sessionListForMtditid.map(item => IndexFields(item.sessionId, item.internalId, item.mtditid)).contains(requestIndex)

        if (condition) FullDuplicate else MtditidDuplicate
    }
  }

  private case class IndexFields(sessionId: String, internalId: String, mtditid: String)

}
