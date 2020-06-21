package io.musubu.fukuro.registry.storage

import io.musubu.fukuro.registry.{Location, LocationId}

trait Error
trait UserError extends Error
case class InternalError(err: String) extends Error

trait LocationStorage[M[_]] {
  def createLocation(location: Location): M[Option[Error]]
//  def locationById(id: LocationId): M[Either[Error, Location]]
}
