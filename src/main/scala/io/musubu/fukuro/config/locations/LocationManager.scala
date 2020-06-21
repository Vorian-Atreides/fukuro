package io.musubu.fukuro.config.locations

import akka.actor.typed.{ActorRef, Behavior, PreRestart, SupervisorStrategy}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer}
import cats.effect.IO
import io.musubu.fukuro.config.Location
import io.musubu.fukuro.config.storage.LocationStorage
import io.musubu.fukuro.config.LocationId

import scala.collection.immutable.HashMap
import scala.concurrent.duration.FiniteDuration
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

object LocationManager {

  sealed trait Command
  final case class CreateLocation(location: Location, rom: ActorRef[CreateLocationResponse]) extends Command
  final case class CreateLocationResponse(result: Option[String])

  final case class UpdateLocation(location: Location, from: ActorRef[UpdateLocationResponse]) extends Command
  final case class UpdateLocationResponse(result: Option[String])

  private final case class InitRequest() extends Command
  private final case class InitSuccess(locations: List[Location]) extends Command
  private final case class InitError(err: Throwable) extends Command

  def apply(storage: LocationStorage[IO]): Behavior[Command] =
    Behaviors.withStash(100) { buffer =>
      Behaviors.setup { ctx =>
        ctx.self ! InitRequest()
        Behaviors.supervise(loading(ctx, storage, buffer)).onFailure(SupervisorStrategy.restart)
      }
    }

  def loading(ctx: ActorContext[Command], storage: LocationStorage[IO], buffer: StashBuffer[Command]): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case InitRequest() =>
        ctx.pipeToSelf(storage.list.unsafeToFuture){
          case Success(Right(locations)) => InitSuccess(locations)
          case Success(Left(err)) => InitError(err)
          case Failure(err) => InitError(err)
        }
        Behaviors.same
      case InitSuccess(locations) =>
        println(locations)
        val children = locations.foldLeft(new HashMap[LocationId, ActorRef[Command]]()) { (registry, location) =>
          val child = ctx.spawn(LocationActor(storage, location.id), location.id.value)
          registry + (location.id -> child)
        }
        buffer.unstashAll(running(ctx, storage, children))
      case InitError(err) =>
        println(err)
        throw err
      case msg =>
        loading(ctx, storage, buffer.stash(msg))
    } receiveSignal {
      case (ctx, PreRestart) =>
        ctx.self ! InitRequest()
        Behaviors.same
    }

  def running(ctx: ActorContext[Command], storage: LocationStorage[IO], children: HashMap[LocationId, ActorRef[Command]]): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case CreateLocation(location, from) if children.contains(location.id) =>
        // TODO: return an error
        Behaviors.same
      case CreateLocation(location, from) =>
        val child = ctx.spawn(LocationActor(storage, location.id), location.id.value)
        val nextChildren = children + (location.id -> child)
        child ! CreateLocation(location, from)
        running(ctx, storage, nextChildren)
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

class LocationManager {
  import LocationManager._


}
