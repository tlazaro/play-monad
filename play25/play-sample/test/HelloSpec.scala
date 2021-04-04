import akka.stream.Materializer
import controllers.HelloController
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import play.api.test._

import scala.concurrent.Future
class HelloSpec extends PlaySpec with Results with GuiceOneAppPerSuite {
  implicit lazy val materializer: Materializer = app.materializer

  val UserAgent        = "scala-test"
  val User             = "insomnia"
  val UnauthorizedUser = "user"

  "Example Controller" should {
    "should work in index" in {
      val request = FakeRequest().withHeaders("User-Agent" -> UserAgent)

      val controller             = new HelloController()
      val result: Future[Result] = controller.index().apply(request).run()
      val bodyText: String       = contentAsString(result)
      bodyText mustBe s"It works for $UserAgent"
    }

    "should work in index with Helpers.call" in {
      val request = FakeRequest().withHeaders("User-Agent" -> UserAgent)

      val controller             = new HelloController()
      val result: Future[Result] = Helpers.call(controller.index(), request)
      val bodyText: String       = contentAsString(result)
      bodyText mustBe s"It works for $UserAgent"
    }

    "should work in hello with Helpers.call" in {
      val request = FakeRequest().withHeaders("User-Agent" -> UserAgent)

      val controller             = new HelloController()
      val result: Future[Result] = Helpers.call(controller.hello(User), request)
      val bodyText: String       = contentAsString(result)
      bodyText mustBe s"Hello $User, from $UserAgent"
    }

    "should work in save with Helpers.call" in {
      val body = Json.parse("""
                              |{
                              |	"some": 5,
                              |	"json": ["a", "b", "c"]
                              |}
                              |""".stripMargin)
      val request =
        FakeRequest()
          .withHeaders("User-Agent" -> UserAgent, "Expect" -> "100-continue", "Content-Type" -> "application/json")
          .withJsonBody(body)

      val controller             = new HelloController()
      val result: Future[Result] = Helpers.call(controller.save(User), request)
      val bodyText: String       = contentAsString(result)
      bodyText mustBe s"Hello $User, from $UserAgent and $body"
    }

    "should fail the headers without sending the body" in {
      val request =
        FakeRequest("POST", "/").withHeaders("User-Agent"   -> UserAgent,
                                             "Expect"       -> "100-continue",
                                             "Content-Type" -> "application/json")

      val controller             = new HelloController()
      val result: Future[Result] = Helpers.call(controller.save(UnauthorizedUser), request)
      val bodyText: String       = contentAsString(result)

      status(result) mustBe Forbidden.header.status
      bodyText mustBe s"User $UnauthorizedUser is not allowed to save files"
    }
  }
}
