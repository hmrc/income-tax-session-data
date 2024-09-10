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

package uk.gov.hmrc.incometaxsessiondata.predicates

import play.api.Logging
import play.api.mvc.Results.Unauthorized
import play.api.mvc._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, confidenceLevel, internalId}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthorisationException, AuthorisedFunctions, ConfidenceLevel}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.incometaxsessiondata.auth.HeaderExtractor
import uk.gov.hmrc.incometaxsessiondata.config.AppConfig
import uk.gov.hmrc.incometaxsessiondata.connectors.MicroserviceAuthConnector
import uk.gov.hmrc.incometaxsessiondata.models.SessionDataRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticationPredicate @Inject() (
  val authConnector: MicroserviceAuthConnector,
  cc: ControllerComponents,
  val appConfig: AppConfig,
  val headerExtractor: HeaderExtractor
)(implicit val ec: ExecutionContext)
    extends ActionBuilder[SessionDataRequest, AnyContent]
    with ActionFunction[Request, SessionDataRequest]
    with AuthorisedFunctions
    with Logging {

  override val parser: BodyParser[AnyContent]     = cc.parsers.defaultBodyParser
  override val executionContext: ExecutionContext = cc.executionContext

  private val minimumConfidenceLevelOpt: Option[Int] = ConfidenceLevel
    .fromInt(appConfig.confidenceLevel)
    .map(_.level)
    .toOption

  private val requiredConfidenceLevel = (userConfidenceLevel: ConfidenceLevel) =>
    minimumConfidenceLevelOpt
      .exists(minimumConfidenceLevel => userConfidenceLevel.level >= minimumConfidenceLevel)

  override def invokeBlock[A](request: Request[A], f: SessionDataRequest[A] => Future[Result]): Future[Result] = {
    implicit val req: Request[A]   = request
    implicit val hc: HeaderCarrier = headerExtractor.extractHeader(request, request.session)

    authorised()
      .retrieve(affinityGroup and internalId and confidenceLevel) {
        case Some(AffinityGroup.Agent) ~ Some(id) ~ _ if hc.sessionId.isDefined =>
          val sessionId: String = hc.sessionId.map(_.value).get
          logger.info(s"[AuthenticationPredicate][authenticated] - authenticated as an agent")
          f(SessionDataRequest[A](internalId = id, sessionId = sessionId))

        case Some(AffinityGroup.Individual) ~ Some(id) ~ userConfidenceLevel
            if hc.sessionId.isDefined && requiredConfidenceLevel(userConfidenceLevel) =>
          val sessionId: String = hc.sessionId.map(_.value).get
          logger.info(s"[AuthenticationPredicate][authenticated] - authenticated as an individual")
          f(SessionDataRequest[A](internalId = id, sessionId = sessionId))
        case _ ~ _ =>
          logger.info(s"[AuthenticationPredicate][unauthorized]")
          Future(Unauthorized)
      }
      .recover { case ex: AuthorisationException =>
        logger.error(
          s"[AuthenticationPredicate][authenticated] Unauthorised Request to Backend. Propagating Unauthorised Response, ${ex.getMessage}"
        )
        Unauthorized
      }

  }

}
