package io.musubu.fukuro

//import org.apache.avro.reflect.AvroSchema

package object config {
  case class EnvironmentId(value: String)
  case class Environment(id: EnvironmentId, name: String)

  case class ServiceId(value: String)
  case class Service(id: ServiceId)

  case class LocationId(value: String)
  case class Location(id: LocationId)

  case class VersionId(value: String)
  case class Version(
                      id: VersionId,
                      serviceId: ServiceId,
//                      schema: AvroSchema,
//                      config: Option[Array[Byte]]
                    )

  case class InstanceId(value: String)
  case class Instance(
                       id: InstanceId,
                       serviceId: ServiceId,
                       versionId: VersionId,
                       envId: EnvironmentId,
                       locationId: LocationId
                     )

//  case class ConfigurationId(value: String)
//  case class Configuration(
//                            id: ConfigurationId,
//                            schema: AvroSchema
//                          )
}
