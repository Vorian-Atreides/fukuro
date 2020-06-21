package io.musubu.fukuro.config.storage.etcd

import cats.effect.{Async, ContextShift}
import com.google.protobuf.ByteString
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.auto._
import io.musubu.fukuro.config.{ServiceId, Version, VersionId}
import io.musubu.fukuro.config.storage.{Error, InternalError, VersionStorage}
import org.etcd4s.formats.{Read, Write}
import org.etcd4s.services.KVService

import scala.concurrent.ExecutionContext

object VersionEtcd {
  def apply[M[_]: Async](kv: KVService)(implicit ec: ExecutionContext, cs: ContextShift[M]): VersionStorage[M] =
    new VersionEtcd[M](kv)
}

private class VersionEtcd[M[_]: Async](service: KVService)(implicit ec: ExecutionContext, cs: ContextShift[M]) extends VersionStorage[M] {
  private val rootPrefix = "/fukuro/config/services"

  private def key(id: VersionId, serviceId: ServiceId): String =
    s"$rootPrefix/${serviceId.value}/versions/${id.value}"

  private implicit val serviceKey: Key[Version] = new Key[Version] {
    def path(value: Version): String = key(value.id, value.serviceId)
  }

  private implicit val versionWriter: Write[Version] =  new Write[Version] {
    def write(value: Version): ByteString =
      ByteString.copyFromUtf8(value.asJson.noSpaces)
  }

  private implicit val versionReader: Read[Either[Error, Version]] = new Read[Either[Error, Version]] {
    def read(value: ByteString): Either[Error, Version] =
      decode[Version](value.toStringUtf8).left.map { err =>
        InternalError("unable to JSON decode a Version", err)
      }
  }

  private val etcd = new Etcd[M, Version](service)

  def create(version: Version): M[Option[Error]] =
    etcd.create(version)

  def update(version: Version): M[Option[Error]] =
    etcd.update(version)

  def getById(id: VersionId, serviceId: ServiceId): M[Either[Error, Version]] =
    etcd.getById(key(id, serviceId))

  def getByServiceId(serviceId: ServiceId): M[Either[Error, List[Version]]] =
    etcd.list(s"$rootPrefix/${serviceId.value}/versions")
}
