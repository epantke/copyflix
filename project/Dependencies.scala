import sbt.*

object Dependencies {
  val akkaVersion = "2.8.1"
  val akkaHttpVersion = "10.5.0"
  val scalaTestVersion = "3.2.15"

  // Core dependencies
  lazy val typedActor = "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
  lazy val typedPersistentActor = "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion
  lazy val typedConfig = "com.typesafe" % "config" % "1.4.2"

  // DB dependencies
  lazy val mongoDbDriver = "org.mongodb.scala" %% "mongo-scala-driver" % "4.9.0"
  lazy val akkaPersistenceMongo = "com.github.scullxbones" %% "akka-persistence-mongo-scala" % "3.0.8"

  // Logging
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.4.7"
  lazy val slf4j = "org.slf4j" % "slf4j-api" % "2.0.5"

  // HTTP dependencies
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % akkaHttpVersion
  lazy val akkaStream = "com.typesafe.akka" %% "akka-stream" % akkaVersion
  lazy val akkaHttpJackson = "com.typesafe.akka" %% "akka-http-jackson" % akkaHttpVersion
  lazy val akkaSerialJackson = "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion
  lazy val akkaJacksonSupport = "de.heikoseeberger" %% "akka-http-jackson" % "1.40.0-RC3"

  // Test dependencies
  lazy val scalaTest = "org.scalatest" %% "scalatest" % scalaTestVersion % Test
  lazy val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
  lazy val akkaTypedTestKit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
  lazy val akkaHttpTestKit = "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
  lazy val akkaPersistenceTestKit = "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test
}
