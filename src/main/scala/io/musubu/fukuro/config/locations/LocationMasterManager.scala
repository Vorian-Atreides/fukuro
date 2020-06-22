package io.musubu.fukuro.config.locations

import akka.actor.typed.{ActorRef, Behavior, PreRestart, SupervisorStrategy}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer}
import scala.collection.immutable.HashMap
import io.musubu.fukuro.config.Location
import io.musubu.fukuro.config.storage.LocationStorage
import io.musubu.fukuro.config.LocationId
import io.musubu.fukuro.config.locations.LocationMasterManager.Command
import io.musubu.fukuro.eow.LittleDeath

object LocationMasterManager {

  sealed trait Command

  sealed trait OperatingCommand extends Command
  final case class CreateLocation(location: Location, from: ActorRef[CreateLocationResponse]) extends OperatingCommand
  final case class CreateLocationResponse(result: Option[String])

  final case class UpdateLocation(location: Location, from: ActorRef[UpdateLocationResponse]) extends OperatingCommand
  final case class UpdateLocationResponse(result: Option[String])

  private sealed trait Initialization extends Command
  private final case class InitRequest() extends Initialization
  private final case class InitSuccess(locations: List[Location]) extends Initialization
  private final case class InitError(err: Throwable) extends Initialization

  def apply[M[_]](storage: LocationStorage[M])(implicit littleDeath: LittleDeath[M]): Behavior[Command] =
    Behaviors.withStash(100) { buffer =>
      Behaviors.setup { ctx =>
        val locationManager = new LocationMasterManager(ctx, storage)
        Behaviors.supervise(locationManager.start(buffer)).onFailure(SupervisorStrategy.restart)
      }
    }
}

private class LocationMasterManager[M[_]](ctx: ActorContext[Command], storage: LocationStorage[M])(implicit littleDeath: LittleDeath[M]) {
  import LocationMasterManager._

  private def spawnLocationActor(id: LocationId): ActorRef[Command] =
    ctx.spawn(LocationMaster(storage, id), id.value)

  private def requestToListLocations(): Unit =
    littleDeath.unsafeRunAsync(storage.list) {
      case Left(err) => ctx.self ! InitError(err)
      case Right(locations) => ctx.self ! InitSuccess(locations)
    }

  private def instantiateChildren(locations: List[Location]): HashMap[LocationId, ActorRef[Command]] =
    locations.foldLeft(new HashMap[LocationId, ActorRef[Command]]()) { (registry, location) =>
      val child = spawnLocationActor(location.id)
      registry + (location.id -> child)
    }

  def start(buffer: StashBuffer[Command]): Behavior[Command] = {
    ctx.self ! InitRequest()
    initialize(buffer)
  }

  private def initialize(buffer: StashBuffer[Command]): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case InitRequest() =>
        requestToListLocations()
        Behaviors.same
      case InitSuccess(locations) =>
        val children = instantiateChildren(locations)
        buffer.unstashAll(running(children))
      case InitError(err) =>
        throw err
      case msg =>
        initialize(buffer.stash(msg))
    } receiveSignal {
      case (ctx, PreRestart) =>
        ctx.self ! InitRequest()
        Behaviors.same
    }

  private def running(children: HashMap[LocationId, ActorRef[Command]]): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case CreateLocation(location, from) if children.contains(location.id) =>
        // TODO: return an error
        Behaviors.same
      case CreateLocation(location, from) =>
        val child = spawnLocationActor(location.id)
        val nextChildren = children + (location.id -> child)
        child ! CreateLocation(location, from)
        running(nextChildren)
      case UpdateLocation(location, from) =>
        children.get(location.id) match {
          case None =>
            // TODO: return an error
            Behaviors.same
          case Some(child) =>
            child ! UpdateLocation(location, from)
            Behaviors.same
        }
      case _ => Behaviors.unhandled
    }
}

