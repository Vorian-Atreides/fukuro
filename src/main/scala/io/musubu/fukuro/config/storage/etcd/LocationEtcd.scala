package io.musubu.fukuro.config.storage.etcd

import cats.effect.{Async, ContextShift}
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.auto._
import scala.concurrent.ExecutionContext
import org.etcd4s.formats.{Read, Write}
import org.etcd4s.services.KVService
import com.google.protobuf.ByteString
import io.musubu.fukuro.config.{Location, LocationId}
import io.musubu.fukuro.config.storage.{Error, InternalError, LocationStorage}

object LocationEtcd {
  def apply[M[_]: Async](kv: KVService)(implicit ec: ExecutionContext, cs: ContextShift[M]): LocationStorage[M] =
    new LocationEtcd[M](kv)
}

private class LocationEtcd[M[_]: Async](service: KVService)(implicit ec: ExecutionContext, cs: ContextShift[M]) extends LocationStorage[M] {
  private val rootPrefix = "/fukuro/config/locations"

  private def key(id: LocationId): String =
    s"$rootPrefix/${id.value}"

  private implicit val locationKey: Key[Location] = new Key[Location] {
    def path(value: Location): String = key(value.id)
  }

  private implicit val locationWriter: Write[Location] =  new Write[Location] {
    def write(value: Location): ByteString =
      ByteString.copyFromUtf8(value.asJson.noSpaces)
  }

  private implicit val locationReader: Read[Either[Error, Location]] = new Read[Either[Error, Location]] {
    def read(value: ByteString): Either[Error, Location] =
      decode[Location](value.toStringUtf8).left.map { err =>
        InternalError("unable to JSON decode a Location", err)
      }
  }

  private val etcd = new Etcd[M, Location](service)

  def create(location: Location): M[Option[Error]] =
    etcd.create(location)

  def update(location: Location): M[Option[Error]] =
    etcd.update(location)

  def getById(id: LocationId): M[Either[Error, Location]] =
    etcd.getById(key(id))

  def list: M[List[Location]] =
    etcd.list(rootPrefix)

}
