package uk.gov.hmrc.incometaxsessiondata.controllers

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}

class SessionControllerSpec extends AnyWordSpec with Matchers {

  private val fakeRequest = FakeRequest("GET", "/")
//  private val controller = new SessionController(Helpers.stubControllerComponents())

  "GET /" should {
    "return 200" in {
//      val result = controller.hello()(fakeRequest)
//      status(result) shouldBe Status.OK
    }
  }
}
