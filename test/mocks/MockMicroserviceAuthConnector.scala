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

package mocks

import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{doReturn, mock}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.incometaxsessiondata.connectors.MicroserviceAuthConnector
import utils.TestSupport

import scala.concurrent.Future

trait MockMicroserviceAuthConnector extends TestSupport with BeforeAndAfterEach {

  // TODO: clarify if we really need Individual auth handling
//  val individualEnrollments: Enrolments = Enrolments(Set(
//    Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "testMtditid")), "activated")
//  ))
  val arnEnrolment: Enrolment = Enrolment(
    "HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "testArn")), "activated")

  val agentsEnrollments: Enrolments =  Enrolments(Set(arnEnrolment))

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////

  val mockMicroserviceAuthConnector: MicroserviceAuthConnector = mock(classOf[MicroserviceAuthConnector])

  //val individualAuthResponseWithCL250: Some[AffinityGroup.Individual.type] ~ ConfidenceLevel.L250.type ~ Some[String] ~ Enrolments = Some(AffinityGroup.Individual) and ConfidenceLevel.L250 and Some("internalId") and  individualEnrollments

  //val individualAuthResponseWithCL50: Some[AffinityGroup.Individual.type] ~ ConfidenceLevel.L50.type ~ Some[String] ~ Enrolments = Some(AffinityGroup.Individual) and ConfidenceLevel.L50 and Some("internalId") and individualEnrollments

  val agentResponseWithCL50: Some[AffinityGroup.Agent.type] ~ ConfidenceLevel.L50.type ~ Some[String] ~ Enrolments = Some(AffinityGroup.Agent) and ConfidenceLevel.L50 and Some("internalId") and agentsEnrollments

  def mockAuth(response: Future[Any] = Future.successful( agentResponseWithCL50 ) ): Future[Nothing] = {
    doReturn(response, Nil: _*).when(mockMicroserviceAuthConnector)
      .authorise(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
  }

}