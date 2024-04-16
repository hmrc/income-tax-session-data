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

package models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.incometaxsessiondata.domain.{SessionData, SessionId}
import uk.gov.hmrc.incometaxsessiondata.models.Session

class SessionDataSpec extends AnyWordSpec with Matchers{


  val testSessionData: SessionData = SessionData(
    sessionID = SessionId("session-123"),
    mtditid = "id-123",
    nino = "nino-123",
    saUtr = "utr-123",
    clientFirstName = Some("David"),
    clientLastName = None,
    userType = "Individual")

  "SessionData" should{
    "implicitly convert a Session to the relevant SessionData" in {
      val session = Session(
        sessionID = "session-123",
        mtditid = "id-123",
        nino = "nino-123",
        saUtr = "utr-123",
        clientFirstName = Some("David"),
        clientLastName = None,
        userType = "Individual"
      )
      val data: SessionData = session
      data shouldBe testSessionData
    }
  }

}
