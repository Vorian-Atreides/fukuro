package io.musubu.fukuro.config.storage

sealed trait Error extends Exception
case class InternalError(reason: String, inner: Throwable) extends Error {
  override def getCause: Throwable = inner
  override def getMessage: String = reason
}

sealed trait UserError extends Error
case class ResourceNotFound(id: String) extends UserError {
  override def getMessage: String = s"resource $id not found"
}
