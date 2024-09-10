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
import org.mockito.Mockito
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.http.Status.{CONFLICT, OK}
import play.api.mvc.Result
import play.api.mvc.Results.InternalServerError
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import testConstants.BaseTestConstants.{testEncryptedSession, testRequest, testSession, testValidRequest}
import uk.gov.hmrc.incometaxsessiondata.services.SessionService
import utils.TestSupport

import scala.concurrent.Future

class SessionServiceSpec extends TestSupport with MockSessionDataRepository {

  object testSessionService
    extends SessionService(
      mockRepository,
      app.injector.instanceOf[Configuration]
    )(ec)

  override def beforeEach(): Unit = {
    Mockito.reset(mockRepository)
  }

  "SessionService.get" should {
    "return session data" when {
      "data returned from the repository" in {
        when(mockRepository.get(any(), any())).thenReturn(Future(Some(testEncryptedSession)))
        val result = testSessionService.get(testRequest)
        result.futureValue shouldBe Some(testSession)
      }
    }
    "return None" when {
      "no data returned from repository" in {
        when(mockRepository.get(any(), any())).thenReturn(Future(None))
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

  "SessionService.handleValidRequest" should {
    "record is not a duplicate" when {
      "the database is empty the service adds the record to the database successfully" should {
        "return an Ok response" in {
          when(mockRepository.set(any())).thenReturn(Future(UpdateResult.acknowledged(1, null, null)))
          when(mockRepository.get(any(), any())).thenReturn(Future(None))

          val result: Future[Result] = testSessionService.handleValidRequest(testValidRequest)
          status(result) shouldBe OK
        }
      }
      "the repository does not acknowledge the database operation" should {
        "return an error" in {
          when(mockRepository.set(any())).thenReturn(Future(UpdateResult.unacknowledged()))
          when(mockRepository.get(any(), any())).thenReturn(Future(None))

          val result: Future[Result] = testSessionService.handleValidRequest(testValidRequest)
          result.futureValue shouldBe InternalServerError("Write operation was not acknowledged")
        }
      }
      "the service returns an exception" should {
        "return an error" in {
          when(mockRepository.set(any())).thenThrow(new RuntimeException("Test error"))
          when(mockRepository.get(any(), any())).thenReturn(Future(None))

          val result: Future[Result] = testSessionService.handleValidRequest(testValidRequest)
          result.futureValue shouldBe InternalServerError("Unknown exception")
        }
      }
    }

    "record is a full duplicate" when {
      "the service adds the record to the database successfully" should {
        "return a Conflict response" in {
          when(mockRepository.get(any(), any())).thenReturn(Future(Some(testEncryptedSession)))
          when(mockRepository.set(any())).thenReturn(Future(UpdateResult.acknowledged(1, null, null)))

          val result: Future[Result] = testSessionService.handleValidRequest(testValidRequest)
          status(result) shouldBe CONFLICT
        }
      }
      "the repository does not acknowledge the database operation" should {
        "return an error" in {
          when(mockRepository.set(any())).thenReturn(Future(UpdateResult.unacknowledged()))
          when(mockRepository.get(any(), any())).thenReturn(Future(Some(testEncryptedSession)))

          val result: Result = testSessionService.handleValidRequest(testValidRequest).futureValue
          result shouldBe InternalServerError("Write operation was not acknowledged")
        }
      }
      "the service returns an exception" should {
        "return an error" in {
          when(mockRepository.get(any(), any())).thenReturn(Future(Some(testEncryptedSession)))
          when(mockRepository.set(any())).thenThrow(new RuntimeException("Test error"))

          val result: Future[Result] = testSessionService.handleValidRequest(testValidRequest)
          result.futureValue shouldBe InternalServerError("Unknown exception")
        }
      }
    }
  }

}
