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

package uk.gov.hmrc.incometaxsessiondata.models

import play.api.http.Status.{CONFLICT, OK, FORBIDDEN}

sealed trait SessionDuplicationType

case object FullDuplicate extends SessionDuplicationType {
  val statusCode = CONFLICT
}

case object MtditidDuplicate extends SessionDuplicationType {
  val statusCode = FORBIDDEN
}

case object NonDuplicate extends SessionDuplicationType {
  val statusCode = OK
}
