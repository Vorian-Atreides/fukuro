package io.musubu.fukuro.config.storage

import io.musubu.fukuro.config._

trait LocationStorage[M[_]] {
  def create(location: Location): M[Option[Error]]
  def update(location: Location): M[Option[Error]]
  def getById(id: LocationId): M[Either[Error, Location]]
  def list: M[List[Location]]
}

trait EnvironmentStorage[M[_]] {
  def create(environment: Environment): M[Option[Error]]
  def update(environment: Environment): M[Option[Error]]
  def getById(id: EnvironmentId): M[Either[Error, Environment]]
  def list: M[List[Environment]]
}

trait ServiceStorage[M[_]] {
  def create(service: Service): M[Option[Error]]
  def update(environment: Service): M[Option[Error]]
  def getById(id: ServiceId): M[Either[Error, Service]]
  def list: M[List[Service]]
}

trait VersionStorage[M[_]] {
  def create(version: Version): M[Option[Error]]
  def update(version: Version): M[Option[Error]]
  def getById(id: VersionId, serviceId: ServiceId): M[Either[Error, Version]]
  def getByServiceId(serviceId: ServiceId): M[List[Version]]
}

trait InstanceStorage[M[_]] {
  def create(version: Instance): M[Option[Error]]
  def update(version: Instance): M[Option[Error]]
  def getById(id: InstanceId, serviceId: ServiceId, versionId: VersionId, envId: EnvironmentId, locationId: LocationId): M[Either[Error, Instance]]
  def getByLocationId(serviceId: ServiceId, versionId: VersionId, envId: EnvironmentId, locationId: LocationId): M[List[Instance]]
}
