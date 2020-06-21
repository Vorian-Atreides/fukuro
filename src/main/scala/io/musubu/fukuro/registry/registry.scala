package io.musubu.fukuro

package object registry {

  type LocationId = String
  case class Location(
                       id: String,
                       description: Option[String]
                     )

  type ServiceId = String
  case class Service(
                      id: ServiceId,
                      name: String,
                      description: Option[String],
                    )

  type ReleaseId = String
  case class Release(
                    id: ReleaseId,
                    serviceId: ServiceId,
                    releasedAt: Long,
                    )

  type InstanceId = String
  case class Instance(
                       id: InstanceId,
                       releaseId: ReleaseId,
                       locationId: LocationId,
                       endpoints: List[Endpoint]
                     )

  sealed trait HealthStatus
  final case object Healthy extends HealthStatus
  final case object Unhealthy extends HealthStatus

  case class HealthCheck(status: HealthStatus, lastCheckedAt: Long)

  case class Endpoint(
                     address: Address,
                     protocol: Protocol,
                     healthCheck: Option[HealthCheck]
                     )

  sealed trait Address
  case class CName(value: String) extends Address
  case class IpV4(value: Int) extends Address
  case class IpV6(value: Int) extends Address

  sealed trait Protocol
  case class HTTPS(address: CName, port: Int) extends Protocol
  case class HTTP(address: Address, port: Int) extends Protocol
  case class GRPCS(address: CName, port: Int) extends Protocol
  case class GRPC(address: Address, port: Int) extends Protocol
  case class TCP(address: Address, port: Int) extends Protocol
}

