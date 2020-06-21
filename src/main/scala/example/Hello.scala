package example

import cats.effect.IO
import io.musubu.fukuro.config._
import io.musubu.fukuro.config.storage.etcd.LocationEtcd
import org.etcd4s
import org.etcd4s.{Etcd4sClient, Etcd4sClientConfig}

object Hello extends Greeting with App {
  println(greeting)

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  implicit val cs = IO.contextShift(ec)

  val app = for {
    _ <- IO (Etcd4sClient.newClient(etcd4s.Etcd4sClientConfig("127.0.0.1", 32769))).bracket { client =>
      val locationStorage = LocationEtcd[IO](client)
//      val environmentStorage =
      for {
        result <- locationStorage.create(Location(LocationId("usea1")))
        _ <-  IO { println(result) }
        notFound <- locationStorage.getById( LocationId("euwe1"))
        _ <- IO { println(notFound) }
        found <- locationStorage.getById( LocationId("usea1"))
        _ <- IO { println(found) }
        result <- locationStorage.create(Location(LocationId("euwe1")))
        _ <-  IO { println(result) }
        list <- locationStorage.list
        _ <-  IO { println(list) }
      } yield()
    } { client =>
      IO { client.shutdown() }
    }
  } yield ()
//  app.unsafeRunAsync()
  app.unsafeRunSync()
//  val storage = new LocationEtcd[IO]()
}

trait Greeting {
  lazy val greeting: String = "hello"
}
