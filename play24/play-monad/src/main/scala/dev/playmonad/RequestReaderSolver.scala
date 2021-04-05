package dev.playmonad

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Done, Input, Iteratee}
import play.api.mvc.Result

import scala.annotation.implicitNotFound
import scala.concurrent.Future

/** Implicit proof that from RequestReader and a value of type A, an Action result can be built */
@implicitNotFound(
  "\n\nA RequestReaderSolver implicit for state ${R} and value ${A} could not be found." +
    " This is needed so the MonadicAction can convert this expression to a Play Action." +
    " When a MonadicAction ends up with a Result or a Future[Result] in either HeaderReader or BodyReader states, the built in" +
    " RequestReaderSolvers will be found automatically, no import is needed." +
    "\n - The R type is ${R}:" +
    "\n   - It should be dev.playmonad.HeaderReader or dev.playmonad.BodyReader[x]." +
    "\n   - Otherwise it's possible this was built using unsupported states." +
    "\n - The A type is ${A}:" +
    "\n   - It should be play.api.mvc.Result or scala.concurrent.Future[play.api.mvc.Result]." +
    "\n   - Otherwise you are using a custom extension and should import a RequestReaderSolver for this type." +
    "\n\n"
)
trait RequestReaderSolver[R <: RequestReader, A] {
  def makeResult(reader: R, result: A): Iteratee[Array[Byte], Result]
}

object RequestReaderSolver {

  /** A HeaderReader of Result can be turned into a Play Action */
  implicit object HeaderResultSolver extends RequestReaderSolver[HeaderReader, Result] {
    override def makeResult(reader: HeaderReader, result: Result): Iteratee[Array[Byte], Result] =
      Done(result, Input.EOF)
  }

  /** A BodyReader of Result can be turned into a Play Action */
  implicit def BodyResultSolver[A]: RequestReaderSolver[BodyReader[A], Result] =
    new RequestReaderSolver[BodyReader[A], Result] {
      override def makeResult(reader: BodyReader[A], result: Result): Iteratee[Array[Byte], Result] =
        reader.accumulator.mapM {
          case Left(errorResult) => Future.successful(errorResult)
          case Right(_)          => Future.successful(result)
        }
    }

  /** A HeaderReader of Future[Result] can be turned into a Play Action */
  implicit object HeaderFutureResultSolver extends RequestReaderSolver[HeaderReader, Future[Result]] {
    override def makeResult(reader: HeaderReader, result: Future[Result]): Iteratee[Array[Byte], Result] =
      Iteratee.flatten(result.map(Done(_, Input.EOF)))
  }

  /** A BodyReader of Future[Result] can be turned into a Play Action */
  implicit def BodyFutureResultSolver[A]: RequestReaderSolver[BodyReader[A], Future[Result]] =
    new RequestReaderSolver[BodyReader[A], Future[Result]] {
      override def makeResult(reader: BodyReader[A], result: Future[Result]): Iteratee[Array[Byte], Result] =
        reader.accumulator.mapM {
          case Left(errorResult) => Future.successful(errorResult)
          case Right(_)          => result
        }
    }
}
