package io.musubu.fukuro.config.storage.etcd

import cats.data.EitherT
import cats.implicits._
import cats.effect.{Async, ContextShift}
import io.musubu.fukuro.config.storage.{Error, InternalError, ResourceNotFound}
import org.etcd4s.formats.{Read, Write}
import org.etcd4s.services.KVService

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

private trait Key[T] {
  def path(value: T): String
}

private class Etcd[M[_]: Async, T](service: KVService)(implicit ec: ExecutionContext, cs: ContextShift[M]) {

  def create(value: T)(implicit id: Key[T], writer: Write[T]): M[Option[Error]] =
    Async[M].async[Option[Error]] { cb =>
      service.setKey(id.path(value), value) onComplete {
        case Failure(err) =>
          val internalError = InternalError("unable to save in etcd", err)
          cb(Either.left(internalError))
        case Success(_) => cb(Either.right(None))
      }
    }

  def update(value: T)(implicit id: Key[T], writer: Write[T]): M[Option[Error]] =
    create(value)

  def getById(key: String)(implicit reader: Read[Either[Error, T]]): M[Either[Error, T]] =
    Async[M].async[Either[Error, T]] { cb =>
      service.getKey[String, Either[Error, T]](key).
        map(opt => opt.getOrElse(Either.left(ResourceNotFound(key)))).
        onComplete {
          case Failure(err) =>
            val internalError = InternalError(s"unable to get the resource: $key", err)
            cb(Either.left(internalError))
          case Success(Left(err : InternalError)) => cb(Either.left(err))
          case Success(result) => cb(Either.right(result))
        }
    }

  def list(prefix: String)(implicit reader: Read[Either[Error, T]]): M[List[T]] =
    Async[M].async[List[T]] { cb =>
      service.getRange(prefix).
        map(_.kvs.toList.map(kv => reader.read(kv.value))).
        map(_.sequence).
        onComplete {
          case Failure(err) =>
            val internalError = InternalError("unable to list the resources", err)
            cb(Either.left(internalError))
          case Success(Left(err)) => cb(Either.left(err))
          case Success(Right(locations)) => cb(Either.right(locations))
        }
    }
}
