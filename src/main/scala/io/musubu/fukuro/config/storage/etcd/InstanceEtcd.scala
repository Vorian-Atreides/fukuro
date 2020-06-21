package io.musubu.fukuro.config.storage.etcd

import cats.effect.{Async, ContextShift}
import com.google.protobuf.ByteString
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.auto._
import io.musubu.fukuro.config.{EnvironmentId, Instance, InstanceId, Location, LocationId, ServiceId, VersionId}
import io.musubu.fukuro.config.storage.{Error, InstanceStorage, InternalError}
import org.etcd4s.formats.{Read, Write}
import org.etcd4s.services.KVService

import scala.concurrent.ExecutionContext

object InstanceEtcd {
  def apply[M[_]: Async](service: KVService)(implicit ec: ExecutionContext, cs: ContextShift[M]): InstanceStorage[M] =
    new InstanceEtcd[M](service)
}

private class InstanceEtcd[M[_]: Async](service: KVService)(implicit ec: ExecutionContext, cs: ContextShift[M]) extends InstanceStorage[M] {
  private val rootPrefix = "/fukuro/config/services"

  private def key(id: InstanceId, serviceId: ServiceId, versionId: VersionId, envId: EnvironmentId, locationId: LocationId): String =
    s"$rootPrefix/${serviceId.value}/versions/${versionId.value}/environments/${envId.value}/locations/${locationId.value}/instances/${id.value}"

  private implicit val instanceKey: Key[Instance] = new Key[Instance] {
    def path(value: Instance): String = key(value.id, value.serviceId, value.versionId, value.envId, value.locationId)
  }

  private implicit val instanceWriter: Write[Instance] =  new Write[Instance] {
    def write(value: Instance): ByteString =
      ByteString.copyFromUtf8(value.asJson.noSpaces)
  }

  private implicit val instanceReader: Read[Either[Error, Instance]] = new Read[Either[Error, Instance]] {
    def read(value: ByteString): Either[Error, Instance] =
      decode[Instance](value.toStringUtf8).left.map { err =>
        InternalError("unable to JSON decode an Instance", err)
      }
  }

  private val etcd = new Etcd[M, Instance](service)

  def create(instance: Instance): M[Option[Error]] =
    etcd.create(instance)

  def update(instance: Instance): M[Option[Error]] =
    etcd.update(instance)

  def getById(id: InstanceId, serviceId: ServiceId, versionId: VersionId, envId: EnvironmentId, locationId: LocationId): M[Either[Error, Instance]] =
    etcd.getById(key(id, serviceId, versionId, envId, locationId))

  def getByLocationId(serviceId: ServiceId, versionId: VersionId, envId: EnvironmentId, locationId: LocationId): M[Either[Error, List[Instance]]] =
    etcd.list(s"$rootPrefix/${serviceId.value}/versions/${versionId.value}/environments/${envId.value}/locations/${locationId.value}/instances/")
}
