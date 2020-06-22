package io.musubu.fukuro.config.locations

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import io.musubu.fukuro.config.LocationId
import io.musubu.fukuro.config.locations.LocationMasterManager.Command
import io.musubu.fukuro.config.storage.LocationStorage
import io.musubu.fukuro.eow.LittleDeath

object LocationMaster {

  def apply[M[_]](storage: LocationStorage[M], id: LocationId)(implicit littleDeath: LittleDeath[M]) : Behavior[Command] =
    Behaviors.setup { ctx =>
      val locationMaster = new LocationMaster(storage, id)
      locationMaster.start()
    }
}

private class LocationMaster[M[_]](storage: LocationStorage[M], id: LocationId)(implicit littleDeath: LittleDeath[M])  {

  def start(): Behavior[Command] =
    Behaviors.receiveMessage {
      case msg =>
        println(msg)
        Behaviors.same
    }

}