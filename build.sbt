import Dependencies._

ThisBuild / scalaVersion     := "2.13.2"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "fukuro",
    libraryDependencies ++= {
      lazy val catsVersion = "2.0.0"
      lazy val circeVersion = "0.12.3"
      lazy val akkaVersion = "2.6.6"

      Seq(
        "io.circe" %% "circe-core" % circeVersion,
        "io.circe" %% "circe-generic" % circeVersion,
        "io.circe" %% "circe-parser" % circeVersion,

        "org.typelevel" %% "cats-effect" % catsVersion,

        "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,

        "com.sksamuel.avro4s" %% "avro4s-core" % "3.1.1",
        "com.github.mingchuno" %% "etcd4s-core" % "0.3.0",

        scalaTest % Test
      )
    }
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
