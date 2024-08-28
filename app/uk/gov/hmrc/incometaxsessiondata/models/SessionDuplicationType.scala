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
