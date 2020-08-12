import sbt.Keys.version

val sVersion = "2.12.12"
val akkaVersion = "2.6.7"
val jsonVersion = "3.6.9"
val xtractVersion = "2.0.0"

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "de.heikoseeberger" %% "akka-http-json4s" % "1.31.0",
  "com.typesafe.akka" %% "akka-http" % "10.1.12",
)

val amqpDependencies = Seq("com.rabbitmq" % "amqp-client" % "5.9.0")

val commonDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.json4s" %% "json4s-native" % jsonVersion,
  "org.json4s" %% "json4s-jackson" % jsonVersion,
  "com.lucidchart" %% "xtract" % xtractVersion,
  "com.lucidchart" %% "xtract-testing" % xtractVersion % "test",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0"
)

lazy val `domain-library` = project
  .in(file("domain-library"))
  .settings(
    organization := "kz.coders",
    name := "domain-library",
    version := "0.1",
    scalaVersion := sVersion,
    libraryDependencies ++= Seq(
      "com.bot4s" %% "telegram-core" % "4.4.0-RC2",
      "com.softwaremill.sttp" %% "core" % "1.6.4"
    ) ++ commonDependencies ++ amqpDependencies
  )

lazy val `amqp-library` = project
  .in(file("amqp-library"))
  .settings(
    organization := "kz.coders",
    name := "amqp-library",
    version := "0.1",
    scalaVersion := sVersion,
    libraryDependencies ++= commonDependencies ++ amqpDependencies
  )

lazy val `telegram-adapter` = project
  .in(file("telegram-adapter"))
  .settings(
    name := "project-telegram-adapter",
    version := "0.1",
    scalaVersion := sVersion,
    libraryDependencies ++= Seq(
      "com.bot4s" %% "telegram-core" % "4.4.0-RC2",
      "com.softwaremill.sttp" %% "core" % "1.6.4"
    ) ++ amqpDependencies ++ akkaDependencies ++ commonDependencies
  )
  .dependsOn(`domain-library`, `amqp-library`)

lazy val `bot-gateway` = project
  .in(file("bot-gateway"))
  .settings(
    name := "project-bot-gateway",
    version := "0.1",
    scalaVersion := sVersion,
    libraryDependencies ++= Seq(
      "com.google.cloud" % "google-cloud-dialogflow" % "2.1.0"
    ) ++ amqpDependencies ++ akkaDependencies ++ commonDependencies
  )
  .dependsOn(`domain-library`, `amqp-library`)

lazy val `http-adapter` = project
  .in(file("http-adapter"))
  .settings(
    name := "project-http-adapter",
    version := "0.1",
    scalaVersion := sVersion,
    libraryDependencies ++=
      amqpDependencies ++ akkaDependencies ++ commonDependencies
  )
  .dependsOn(`domain-library`, `amqp-library`)
