/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.incometaxsessiondata.predicates

import play.api.Logging
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthorisationException, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.incometaxsessiondata.config.AppConfig
import uk.gov.hmrc.incometaxsessiondata.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticationPredicate @Inject()(val authConnector: MicroserviceAuthConnector, cc: ControllerComponents,
                                        val appConfig: AppConfig,
                                       )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthorisedFunctions with Logging {

  private val minimumConfidenceLevelOpt: Option[Int] = ConfidenceLevel.fromInt(appConfig.confidenceLevel).map(_.level).toOption

  def async(action: Request[AnyContent] => Future[Result]): Action[AnyContent] =
    Action.async { implicit request =>
      authorised().retrieve(affinityGroup and confidenceLevel) {
        case Some(AffinityGroup.Agent) ~ _ =>
          action(request)
        case _ ~ userConfidence if minimumConfidenceLevelOpt.exists(minimumConfidenceLevel => userConfidence.level >= minimumConfidenceLevel) =>
          action(request)
        case _ ~ _ =>
          logger.info(s"[AuthenticationPredicate][authenticated] User has confidence level below ${minimumConfidenceLevelOpt}")
          Future(Unauthorized)
      } recover {
        case ex: AuthorisationException =>
          logger.error(s"[AuthenticationPredicate][authenticated] Unauthorised Request to Backend. Propagating Unauthorised Response, ${ex.getMessage}")
          Unauthorized
      }
    }
}
