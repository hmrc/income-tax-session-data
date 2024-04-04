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

import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.incometaxsessiondata.domain.models.{Name, Session}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SessionService @Inject(){

  def createSession(session: Session): Future[Either[Throwable, Unit]] =
    Future.successful(Right(()))

  def getSession(sessionID: String): Future[Either[Throwable, Session]] =
    Future.successful(Right(dummySession))

  private lazy val dummySession: Session =
    Session(
      mtditid = "MTDITID",
      nino = "BB123456A",
      saUtr = None,
      userName = Some(Name(
        firstName = Some("John"),
        lastName = Some("Smith")
      )),
      userType = Some(Individual)
    )
}