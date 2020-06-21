package io.musubu.fukuro.config

import akka.actor.typed.{Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import cats.effect.IO
import io.musubu.fukuro.config.locations.LocationManager
import io.musubu.fukuro.config.storage.LocationStorage

object Guardian {
  def apply(locationStorage: LocationStorage[IO]): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
//    Behaviors.supervise(LocationManager(locationStorage)).onFailure(SupervisorStrategy.restart)
    val locationManager = ctx.spawn(LocationManager(locationStorage), "LocationManager")
    Behaviors.empty
  }
}
