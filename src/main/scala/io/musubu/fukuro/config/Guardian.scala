package io.musubu.fukuro.config

import akka.actor.typed.{Behavior, LogOptions, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import cats.effect.IO
import io.musubu.fukuro.config.locations.{LocationMasterManager, LocationSlave}
import io.musubu.fukuro.config.locations.LocationMasterManager.{CreateLocation, CreateLocationResponse}
import io.musubu.fukuro.config.locations.LocationSlave.{GetLocationById, GetLocationByIdResponse}
import io.musubu.fukuro.config.storage.LocationStorage
import io.musubu.fukuro.eow.LittleDeath

object Debug {
  def createLocation(): Behavior[GetLocationByIdResponse] =
    Behaviors.receiveMessage {
      case msg => println(msg)
        Behaviors.same
    }
}

object Guardian {
  def apply[M[_]](locationStorage: LocationStorage[M])(implicit littleDeath: LittleDeath[M]): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>

    val locationMasterManager = ctx.spawn(LocationMasterManager(locationStorage), "LocationManager")
    val locationSlave = ctx.spawn(LocationSlave(locationStorage), "LocationSlave")

    val debugger = ctx.spawn(Debug.createLocation(), "debugger")
    for (i <- 0 until 600) {
      locationSlave ! GetLocationById(LocationId("usea1"), debugger)
      Thread.sleep(1000)
    }
    Behaviors.empty
  }
}
