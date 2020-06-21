package io.musubu.fukuro.config.locations

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import cats.effect.IO
import io.musubu.fukuro.config.LocationId
import io.musubu.fukuro.config.locations.LocationManager.Command
import io.musubu.fukuro.config.storage.LocationStorage

object LocationActor {

  def apply(storage: LocationStorage[IO], id: LocationId): Behavior[Command] = Behaviors.setup { ctx =>
    val state = new LocationActor(storage, id)
    receive(ctx, state)
  }

  def receive(ctx: ActorContext[Command], state: LocationActor[IO]): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case _ =>
        Behaviors.same
    }
}

class LocationActor[M[_]](storage: LocationStorage[M], id: LocationId) {

  //  def create(location: Location): M[Option[Error]] =
  //    storage.create(location)
  //
  //  def update(location: Location): M[Option[Error]] =
  //    storage.update(location)
}