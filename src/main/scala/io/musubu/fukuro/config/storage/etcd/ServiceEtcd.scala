package io.musubu.fukuro.config.storage.etcd

import cats.effect.{Async, ContextShift}
import com.google.protobuf.ByteString
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.auto._
import io.musubu.fukuro.config.{Service, ServiceId}
import io.musubu.fukuro.config.storage.{Error, InternalError, ServiceStorage}
import org.etcd4s.formats.{Read, Write}
import org.etcd4s.services.KVService

import scala.concurrent.ExecutionContext

object ServiceEtcd {
  def apply[M[_]: Async](kv: KVService)(implicit ec: ExecutionContext, cs: ContextShift[M]): ServiceStorage[M] =
    new ServiceEtcd[M](kv)
}

private class ServiceEtcd[M[_]: Async](service: KVService)(implicit ec: ExecutionContext, cs: ContextShift[M]) extends ServiceStorage[M] {
  private val rootPrefix = "/fukuro/config/services"

  private def key(id: ServiceId): String =
    s"$rootPrefix/${id.value}"

  private implicit val serviceKey: Key[Service] = new Key[Service] {
    def path(value: Service): String = key(value.id)
  }

  private implicit val serviceWriter: Write[Service] =  new Write[Service] {
    def write(value: Service): ByteString =
      ByteString.copyFromUtf8(value.asJson.noSpaces)
  }

  private implicit val serviceReader: Read[Either[Error, Service]] = new Read[Either[Error, Service]] {
    def read(value: ByteString): Either[Error, Service] =
      decode[Service](value.toStringUtf8).left.map { err =>
        InternalError("unable to JSON decode a Service", err)
      }
  }

  private val etcd = new Etcd[M, Service](service)

  def create(service: Service): M[Option[Error]] =
    etcd.create(service)

  def update(service: Service): M[Option[Error]] =
    etcd.update(service)

  def getById(id: ServiceId): M[Either[Error, Service]] =
    etcd.getById(key(id))

  def list: M[Either[Error, List[Service]]] =
    etcd.list(rootPrefix)
}
