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
import uk.gov.hmrc.incometaxsessiondata.models.{MtdItUser, Name, SuccessResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class UserDetailsService @Inject(){

  def createUserDetails(mtdItUser: MtdItUser): Future[Either[Any, Any]] =
    Future.successful(Right(SuccessResponse))

  def getUserDetails(mtditid: String): Future[Either[Any, MtdItUser]] =
    Future.successful(Right(dummyUser))

  private lazy val dummyUser: MtdItUser =
    MtdItUser(
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
