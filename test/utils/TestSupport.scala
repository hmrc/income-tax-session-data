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

package utils

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.{AnyWordSpec, AnyWordSpecLike}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.incometaxsessiondata.config.AppConfig

import scala.concurrent.ExecutionContext

trait TestSupport extends AnyWordSpec with AnyWordSpecLike with Matchers with OptionValues
  with GuiceOneServerPerSuite  with ScalaFutures {
  this: Suite =>

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  def fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")

  val fakePostRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withMethod("POST")
  val appConfig: AppConfig = new AppConfig( app.injector.instanceOf[Configuration] ){}

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(3, Seconds), interval = Span(5, Millis))

  val fakeRequestWithActiveSession: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(
      SessionKeys.lastRequestTimestamp -> "1498236506662")
    .withHeaders(
      HeaderNames.REFERER -> "/test/url",
      "X-Session-ID" -> "test1123",
      HeaderNames.AUTHORIZATION -> "Bearer Token"
    )

  val fakeRequestWithActiveSessionAndEmptySessionId: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withSession(
      SessionKeys.lastRequestTimestamp -> "1498236506662")
    .withHeaders(
      HeaderNames.REFERER -> "/test/url"
    )
}