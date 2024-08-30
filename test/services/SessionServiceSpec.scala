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

package services

import com.mongodb.client.result.UpdateResult
import mocks.repositories.MockSessionDataRepository
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import testConstants.BaseTestConstants.{testRequest, testSession, testSessionAllA, testSessionData, testSessionDifferentInternalId, testValidRequest}
import uk.gov.hmrc.incometaxsessiondata.models.{FullDuplicate, NonDuplicate, PartialDuplicate, Session, SessionData}
import uk.gov.hmrc.incometaxsessiondata.services.SessionService
import utils.TestSupport

import scala.concurrent.Future

class SessionServiceSpec extends TestSupport with MockSessionDataRepository {

  object testSessionService
    extends SessionService(
      mockRepository
    )(ec)

  "SessionService.get" should {
    "return session data" when {
      "data returned from the repository" in {
        when(mockRepository.get(any())).thenReturn(Future(Some(testSession)))
        val result = testSessionService.get(testRequest)
        result.futureValue shouldBe Some(testSessionData)
      }
    }
    "return None" when {
      "no data returned from repository" in {
        when(mockRepository.get(any())).thenReturn(Future(None))
        val result = testSessionService.get(testRequest)
        result.futureValue shouldBe None
      }
    }
  }

  "SessionService.set" should {
    "return true" when {
      "repository returns an acknowledged update" in {
        when(mockRepository.set(any())).thenReturn(Future(UpdateResult.acknowledged(1, null, null)))
        val result = testSessionService.set(testSession)
        result.futureValue shouldBe Right(true)
      }
    }
    "return false" when {
      "repository returns an unacknowledged update" in {
        when(mockRepository.set(any())).thenReturn(Future(UpdateResult.unacknowledged()))

        val result = testSessionService.set(testSession)
        result.futureValue shouldBe Right(false)
      }
    }
    "return a throwable" when {
      "repository returns an exception" in {
        when(mockRepository.set(any())).thenThrow(new RuntimeException("testException"))

        val result = testSessionService.set(testSession)
        result.futureValue.toString shouldBe Left(new RuntimeException("testException")).toString
      }
    }
  }

  "SessionService.getDuplicationStatus" should {
    "return that a request is not a duplicate of any record in the database" when {
      "there are no records in the database" in {
        mockGetBySessionId(Seq.empty[Session])

        val result = testSessionService.getDuplicationStatus(testValidRequest)
        result.futureValue shouldBe NonDuplicate
      }
    }

    "return that a request is a partial duplicate to a record in the database" when {
      "there is a record in the database with the same session id but a different internal id" in {
        mockGetBySessionId(Seq(testSessionDifferentInternalId))

        val result = testSessionService.getDuplicationStatus(testValidRequest)
        result.futureValue shouldBe PartialDuplicate
      }
    }

    "return that a request is a full duplicate of a record in the database" when {
      "there is a record in the database with the same session id and internal id" in {
        mockGetBySessionId(Seq(testSession))

        val result = testSessionService.getDuplicationStatus(testValidRequest)
        result.futureValue shouldBe FullDuplicate
      }
      "there is a record in the database with the same session id and internal id, and a second partial duplicate" in {
        mockGetBySessionId(Seq(testSessionDifferentInternalId, testSession))

        val result = testSessionService.getDuplicationStatus(testValidRequest)
        result.futureValue shouldBe FullDuplicate
      }
    }

  }

  }
