package controllers

import cats.implicits._
import controllers.Assets.{Forbidden, Ok}
import io.playmonad._
import play.api.Play._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.BodyParsers
import play.api.mvc.Action

import scala.concurrent.Future

class HelloController {

  def index = MonadicAction {
    for {
      agent <- header("User-Agent")
    } yield Ok(s"It works for $agent")
  }

  def hello(name: String) = MonadicAction {
    for {
      agent <- header("User-Agent")
      _     <- auth(name)
    } yield Ok(s"Hello $name, from $agent")
  }

  def auth(user: String): HeaderReader.Aux[String] = HeaderReader { _ =>
    if (user == "insomnia") Future.successful(Right(user))
    else Future.successful(Left(Forbidden(s"User $user is not allowed to save files")))
  }

  def save(name: String) = MonadicAction {
    for {
      agent    <- header("User-Agent")
      _        <- auth(name)
      jsonBody <- body(BodyParsers.parse.tolerantJson)
    } yield {
      jsonBody.map { json =>
        Ok(s"Hello $name, from $agent and ${json.toString()}")
      }
    }
  }
}
