package dev

import controllers.Assets.BadRequest
import play.api.mvc.BodyParser

import scala.concurrent.Future

package object playmonad {
  def header(name: String): HeaderReader.Aux[String] = HeaderReader { request =>
    request.headers.get(name) match {
      case Some(value) => Future.successful(Right(value))
      case None        => Future.successful(Left(BadRequest(s"Header $name must be provided")))
    }
  }

  def body[A](bodyParser: BodyParser[A]): BodyReader.Aux[A, A] = BodyReader(bodyParser)
}
