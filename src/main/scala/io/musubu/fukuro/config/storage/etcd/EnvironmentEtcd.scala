package io.musubu.fukuro.config.storage.etcd

import cats.effect.{Async, ContextShift}
import com.google.protobuf.ByteString
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.auto._
import io.musubu.fukuro.config.{Environment, EnvironmentId}
import io.musubu.fukuro.config.storage.{EnvironmentStorage, Error, InternalError}
import org.etcd4s.formats.{Read, Write}
import org.etcd4s.services.KVService

import scala.concurrent.ExecutionContext

object EnvironmentEtcd {
  def apply[M[_]: Async](kv: KVService)(implicit ec: ExecutionContext, cs: ContextShift[M]): EnvironmentStorage[M] =
    new EnvironmentEtcd[M](kv)
}

private class EnvironmentEtcd[M[_]: Async](service: KVService)(implicit ec: ExecutionContext, cs: ContextShift[M]) extends EnvironmentStorage[M] {
  private val rootPrefix = "/fukuro/config/environments"

  private def key(id: EnvironmentId): String =
    s"$rootPrefix/${id.value}"

  private implicit val environmentKey: Key[Environment] = new Key[Environment] {
    def path(value: Environment): String = key(value.id)
  }

  private implicit val environmentWriter: Write[Environment] =  new Write[Environment] {
    def write(value: Environment): ByteString =
      ByteString.copyFromUtf8(value.asJson.noSpaces)
  }

  private implicit val environmentReader: Read[Either[Error, Environment]] = new Read[Either[Error, Environment]] {
    def read(value: ByteString): Either[Error, Environment] =
      decode[Environment](value.toStringUtf8).left.map { err =>
        InternalError("unable to JSON decode an Environment", err)
      }
  }

  private val etcd = new Etcd[M, Environment](service)

  def create(environment: Environment): M[Option[Error]] =
    etcd.create(environment)

  def update(environment: Environment): M[Option[Error]] =
    etcd.update(environment)

  def getById(id: EnvironmentId): M[Either[Error, Environment]] =
    etcd.getById(key(id))

  def list: M[List[Environment]]=
    etcd.list(rootPrefix)
}
