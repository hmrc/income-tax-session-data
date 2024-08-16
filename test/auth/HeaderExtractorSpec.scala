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

package auth

import uk.gov.hmrc.http.Authorization
import uk.gov.hmrc.incometaxsessiondata.auth.BackEndHeaderExtractor
import utils.TestSupport

class HeaderExtractorSpec extends TestSupport {
  val underTest = new BackEndHeaderExtractor()

  "HeaderExtractor" should {
    "copy Authorization header from request" in {
      val actual = underTest.extractHeader(fakeRequestWithActiveSession,
        fakeRequestWithActiveSession.session)
      actual.authorization shouldBe Some(Authorization("Bearer Token"))
    }

    "copy empty Authorization header from request" in {
      val actual = underTest.extractHeader(fakeRequestWithActiveSessionAndEmptySessionId,
        fakeRequestWithActiveSessionAndEmptySessionId.session)
      actual.authorization shouldBe None
    }
  }
}
