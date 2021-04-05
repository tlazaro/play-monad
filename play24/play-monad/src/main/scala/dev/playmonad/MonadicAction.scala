package dev.playmonad

import cats.data.{EitherT, IndexedStateT}
import cats.instances.future._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Done, Input, Iteratee}
import play.api.mvc.{BodyParser, EssentialAction, RequestHeader, Result}

import scala.concurrent.{Future, Promise}

sealed trait RequestReader
case class HeaderReader(requestHeader: RequestHeader)                           extends RequestReader
case class HeaderReaderHeaderReader(requestHeader: RequestHeader)               extends RequestReader
case class BodyReader[A](accumulator: Iteratee[Array[Byte], Either[Result, A]]) extends RequestReader

object HeaderReader {
  type Aux[A] = IndexedStateT[EitherT[Future, Result, *], HeaderReader, HeaderReader, A]

  def apply[A](f: RequestHeader => Future[Either[Result, A]]): HeaderReader.Aux[A] =
    IndexedStateT[EitherT[Future, Result, *], HeaderReader, HeaderReader, A] { state =>
      EitherT[Future, Result, A](f(state.requestHeader)).map(value => (state, value))
    }
}

object BodyReader {
  type Aux[Body, A] = IndexedStateT[EitherT[Future, Result, *], HeaderReader, BodyReader[Body], Future[A]]

  def apply[A](bodyParser: BodyParser[A]): BodyReader.Aux[A, A] =
    IndexedStateT[EitherT[Future, Result, *], HeaderReader, BodyReader[A], Future[A]] { state =>
      val promise = Promise[A]()

      val bd = BodyReader(bodyParser.apply(state.requestHeader).map {
        case Left(errorResult) => Left(errorResult)
        case Right(a) =>
          promise.success(a) // unblocks `result` for completion
          Right(a)
      })

      EitherT[Future, Result, (BodyReader[A], Future[A])](Future.successful(Right((bd, promise.future))))
    }
}

object MonadicAction {
  def apply[R <: RequestReader, A](
      reader: IndexedStateT[EitherT[Future, Result, *], HeaderReader, R, A]
  )(implicit solver: RequestReaderSolver[R, A]): EssentialAction =
    EssentialAction { request =>
      Iteratee.flatten(reader.run(HeaderReader(request)).value.map {
        case Right((r, a)) => solver.makeResult(r, a)
        case Left(result)  => Done[Array[Byte], Result](result, Input.EOF)
      })
    }
}
