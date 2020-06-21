package io.musubu.fukuro.registry.storage.etcd

import cats.Monad
import cats.data.OptionT
import cats.effect.{Async, ContextShift, IO, LiftIO}
import cats.effect.implicits._
import cats.implicits._
import org.etcd4s.formats._
import org.etcd4s.{Etcd4sClient, Etcd4sClientConfig}
import org.etcd4s.pb.etcdserverpb.PutRequest

import scala.concurrent.ExecutionContext
import io.musubu.fukuro.registry.{Location, LocationId}
import io.musubu.fukuro.registry.storage.{Error, InternalError}
import org.etcd4s.services.KVService

import scala.util.{Failure, Success}

class LocationEtcd[M[_]: Async](kv: KVService)(implicit ec: ExecutionContext, cs: ContextShift[M]) {
  private val path = "/registry/locations"

  def createLocation(location: Location): M[Option[Error]] = Async[M].async[Option[Error]] { cb =>
    println("test")
    kv.setKey(s"$path/${location.id}", "test").onComplete {
      case Success(_) => cb(Right(None))
      case Failure(err) => cb(Right(Some(InternalError(err.getMessage))))
    }
  }
//
//  def locationById(id: LocationId): M[Either[Error, Location]] = {
//
//  }
}
