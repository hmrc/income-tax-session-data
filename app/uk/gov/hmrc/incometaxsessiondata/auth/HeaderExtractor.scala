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

package uk.gov.hmrc.incometaxsessiondata.auth

import com.google.inject.ImplementedBy
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Singleton

@ImplementedBy(classOf[BackEndHeaderExtractor])
trait HeaderExtractor {

  def extractHeader(request: play.api.mvc.Request[_], session: play.api.mvc.Session): HeaderCarrier = {
    val authHeader = request.headers.get(HeaderNames.authorisation)
    HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)
      .copy(authorization = authHeader.map(Authorization))
  }

}

@Singleton
class BackEndHeaderExtractor extends HeaderExtractor
