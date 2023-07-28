import Dependencies._

ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.encoway"
ThisBuild / organizationName := "encoway"

lazy val root = (project in file("."))
  .settings(
    name := "encoflix",
    libraryDependencies ++= Seq(
      typedActor,
      typedPersistentActor,
      typedConfig,
      mongoDbDriver,
      akkaPersistenceMongo,
      akkaSerialJackson,
      akkaStream,
      akkaHttp,
      akkaHttpJackson,
      akkaJacksonSupport,
      scalaTest,
      akkaTestKit,
      akkaTypedTestKit,
      akkaHttpTestKit,
      akkaPersistenceTestKit,
      slf4j,
      logback
    )
  )
