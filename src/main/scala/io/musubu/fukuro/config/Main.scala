package io.musubu.fukuro.config

import akka.actor.typed.ActorSystem
import cats.effect.IO
import io.musubu.fukuro.config.storage.etcd.LocationEtcd
import io.musubu.fukuro.eow.implicits._
import org.etcd4s
import org.etcd4s.Etcd4sClient
import org.etcd4s.services.KVService

import scala.concurrent.Await
import scala.io.StdIn

object Main {
  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs = IO.contextShift(ec)

  def runSystem(client: KVService): IO[ActorSystem[Nothing]] = IO {
    val locationStorage = LocationEtcd[IO](client)
    ActorSystem[Nothing](Guardian(locationStorage), "Fukuro")
  }

  def main(args: Array[String]): Unit = {
    val app = for {
      _ <- IO (Etcd4sClient.newClient(etcd4s.Etcd4sClientConfig("127.0.0.1", 2379))).bracket { client =>
        for {
          system <- runSystem(client)
          _ <- IO{ StdIn.readLine() }
          _ <- IO{ system.terminate() }
        } yield()
      }{ client =>
        IO { client.shutdown() }
      }
    } yield()
    app.unsafeRunSync()
  }
}
