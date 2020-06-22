package io.musubu.fukuro.config.locations

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import io.musubu.fukuro.config.locations.LocationSlave.Command
import io.musubu.fukuro.config.{Location, LocationId}
import io.musubu.fukuro.config.storage.{LocationStorage, UserError}
import io.musubu.fukuro.eow.LittleDeath

object LocationSlave {
  sealed trait Command
  final case class GetLocationById(id: LocationId, from: ActorRef[GetLocationByIdResponse]) extends Command
  final case class GetLocationByIdResponse(location: Location)

  sealed trait Internal extends Command
  private final case class StorageGetLocationByIdSuccess(location: Location, from: ActorRef[GetLocationByIdResponse]) extends Internal
  private final case class StorageGetLocationByIdFailure(err: Throwable, from: ActorRef[GetLocationByIdResponse]) extends Internal

  def apply[M[_]](storage: LocationStorage[M])(implicit littleDeath: LittleDeath[M]) : Behavior[Command] =
    Behaviors.setup { ctx =>
      val locationSlaveActor = new LocationSlave(ctx, storage)
      Behaviors.supervise(locationSlaveActor.receive()).onFailure(SupervisorStrategy.restart)
    }
}

private class LocationSlave[M[_]](ctx: ActorContext[Command], storage: LocationStorage[M])(implicit littleDeath: LittleDeath[M])  {
  import LocationSlave._

  private def requestGetById(id: LocationId, from: ActorRef[GetLocationByIdResponse]): Unit =
    littleDeath.unsafeRunAsync(storage.getById(id)) { cb =>
      cb.flatMap(a => a) match {
        case Left(err) => ctx.self ! StorageGetLocationByIdFailure(err, from)
        case Right(location) => ctx.self ! StorageGetLocationByIdSuccess(location, from)
      }
    }

  def receive(): Behavior[Command] = Behaviors.receiveMessage {
    case GetLocationById(id, from) =>
      requestGetById(id, from)
      Behaviors.same
    case StorageGetLocationByIdSuccess(location, from) =>
      from ! GetLocationByIdResponse(location)
      Behaviors.same
    case StorageGetLocationByIdFailure(err: UserError, from) =>
      // TODO: return an error
      Behaviors.same
    case StorageGetLocationByIdFailure(err, from) =>
      Behaviors.same
  }

}