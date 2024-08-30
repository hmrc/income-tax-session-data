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

package testConstants

import play.api.test.FakeRequest
import uk.gov.hmrc.incometaxsessiondata.models.SessionDataRequest
import utils.TestSupport

object BaseTestConstants extends TestSupport {

  val testRequest: SessionDataRequest[_] = SessionDataRequest(
    internalId = "testInternalId",
    sessionId = "test1123",
    mtditid = "testMtditid"
  )(FakeRequest())

}
