package io.musubu.fukuro.eow

import cats.effect.IO

trait LittleDeath[M[_]] {
  def unsafeRunAsync[T](a: M[T])(cb: Either[Throwable, T] => Unit): Unit
}

object implicits {
  implicit val ioLittleDeath: LittleDeath[IO] = new LittleDeath[IO] {
    override def unsafeRunAsync[T](a: IO[T])(cb: Either[Throwable, T] => Unit): Unit =
      a.unsafeRunAsync(cb)
  }
}
