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

package mocks.services

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import uk.gov.hmrc.incometaxsessiondata.services.SessionService
import utils.TestSupport

import scala.concurrent.Future

trait MockSessionService extends TestSupport with BeforeAndAfterEach {

  val mockSessionService: SessionService = mock(classOf[SessionService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionService)
  }

  def setupMockGenericHandleValidRequest(): Unit =
    when(mockSessionService.handleValidRequest(any())).thenReturn(Future.successful(Ok("Generic OK message")))

  def setupMockHandleValidRequest(result: Result): Unit =
    when(mockSessionService.handleValidRequest(any())).thenReturn(Future.successful(result))

  def setupMockHandleValidRequestFutureFailed(): Unit =
    when(mockSessionService.handleValidRequest(any())).thenReturn(Future.failed(new Exception("Test future failed")))
}
