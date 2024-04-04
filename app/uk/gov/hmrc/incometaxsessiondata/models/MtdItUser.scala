package uk.gov.hmrc.incometaxsessiondata.models

import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.retrieve.Name

case class MtdItUser(mtditid:   String,
                     nino:      String,
                     saUtr:     Option[String],
                     userName:  Option[Name],
                     userType:  Option[AffinityGroup])
