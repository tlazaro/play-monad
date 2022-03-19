package dev.playmonad

import cats.{CoflatMap, Monad, MonadError}
import cats.data.{EitherT, IndexedStateT}
import play.api.libs.iteratee.{Done, Input, Iteratee}
import play.api.mvc.{BodyParser, EssentialAction, RequestHeader, Result}

import scala.concurrent.{ExecutionContext, Future, Promise}

sealed trait RequestReader
case class HeaderReader(requestHeader: RequestHeader)                           extends RequestReader
case class BodyReader[A](accumulator: Iteratee[Array[Byte], Either[Result, A]]) extends RequestReader

trait MonadicActionImplicits {
  implicit val ec: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
  implicit val futureInstances: MonadError[Future, Throwable] with CoflatMap[Future] with Monad[Future] =
    cats.implicits.catsStdInstancesForFuture
}

object HeaderReader extends MonadicActionImplicits {
  type Aux[A] = IndexedStateT[EitherT[Future, Result, *], HeaderReader, HeaderReader, A]

  def withHeadersM[A](f: RequestHeader => Future[Either[Result, A]]): HeaderReader.Aux[A] =
    IndexedStateT[EitherT[Future, Result, *], HeaderReader, HeaderReader, A] { state =>
      EitherT[Future, Result, (HeaderReader, A)](f(state.requestHeader).map(_.right.map(value => (state, value))))
    }

  def withHeaders[A](f: RequestHeader => Either[Result, A]): HeaderReader.Aux[A] =
    IndexedStateT[EitherT[Future, Result, *], HeaderReader, HeaderReader, A] { state =>
      EitherT[Future, Result, (HeaderReader, A)](
        Future.successful(f(state.requestHeader).right.map(v => (state, v)))
      )
    }

  def withValue[A](value: Future[A]): HeaderReader.Aux[A] =
    IndexedStateT[EitherT[Future, Result, *], HeaderReader, HeaderReader, A] { state =>
      EitherT[Future, Result, (HeaderReader, A)](value.map(v => Right((state, v))))
    }

  def withValue[A](value: Either[Result, A]): HeaderReader.Aux[A] =
    IndexedStateT[EitherT[Future, Result, *], HeaderReader, HeaderReader, A] { state =>
      EitherT[Future, Result, (HeaderReader, A)](Future.successful(value.right.map(v => (state, v))))
    }

  def withValue[A](value: A): HeaderReader.Aux[A] =
    IndexedStateT[EitherT[Future, Result, *], HeaderReader, HeaderReader, A] { state =>
      EitherT[Future, Result, (HeaderReader, A)](Future.successful(Right((state, value))))
    }

  def withResult[A](value: Result): HeaderReader.Aux[A] =
    IndexedStateT[EitherT[Future, Result, *], HeaderReader, HeaderReader, A] { _ =>
      EitherT[Future, Result, (HeaderReader, A)](Future.successful(Left(value)))
    }
}

object BodyReader extends MonadicActionImplicits {
  type Aux[Body, A] = IndexedStateT[EitherT[Future, Result, *], HeaderReader, BodyReader[Body], Future[A]]

  def withBody[A](bodyParser: BodyParser[A]): BodyReader.Aux[A, A] =
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

object MonadicAction extends MonadicActionImplicits {
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
