package io.musubu.fukuro.config.locations

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import cats.data.EitherT
import cats.effect.IO
import cats.implicits._
import io.musubu.fukuro.config.{Location, LocationId}
import io.musubu.fukuro.config.storage.{Error, LocationStorage}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object LocationSlaveActor {
  sealed trait Command
  final case class GetLocationById(id:  LocationId, from: ActorRef[GetLocationByIdResponse]) extends Command
  final case class GetLocationByIdResponse(location: Location)
//  final case class GetLocationByIdFailure(err: Error)

  //  final case class ListLocations() extends Command

  sealed trait Internal extends Command
  private final case class StorageGetLocationByIdResult(result: Either[Throwable, Location], from: ActorRef[GetLocationByIdResponse]) extends Internal


  def apply(storage: LocationStorage[IO]): Behavior[Command] = Behaviors.setup { ctx =>
    receive(ctx, storage)
  }

  def receive(ctx: ActorContext[Command], storage: LocationStorage[IO]): Behavior[Command] = Behaviors.receiveMessage {
    case GetLocationById(id, from) =>
      ctx.pipeToSelf(storage.getById(id).unsafeToFuture) {
        case Success(result) => StorageGetLocationByIdResult(result, from)
        case Failure(err) => StorageGetLocationByIdResult(Either.left(err), from)
      }

      Behaviors.same
    case StorageGetLocationByIdResult(result, from) =>
      result match {
        case Left(err) => throw err
        case Right(result) =>
          from ! GetLocationByIdResponse(result)
          Behaviors.same
      }
  }
}
