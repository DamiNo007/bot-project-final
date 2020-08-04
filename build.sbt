import sbt.Keys.version

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.6.7",
  "com.typesafe.akka" %% "akka-slf4j" % "2.6.7",
  "com.typesafe.akka" %% "akka-stream" % "2.6.7",
  "de.heikoseeberger" %% "akka-http-json4s" % "1.31.0",
  "com.typesafe.akka" %% "akka-http" % "10.1.12",
)

val amqpDependencies = Seq("com.rabbitmq" % "amqp-client" % "5.9.0")

val commonDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.json4s" %% "json4s-native" % "3.6.9",
  "org.json4s" %% "json4s-jackson" % "3.6.9",
  "com.lucidchart" %% "xtract" % "2.0.0",
  "com.lucidchart" %% "xtract-testing" % "2.0.0" % "test",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0"
)

lazy val `domain-library` = project
  .in(file("domain-library"))
  .settings(
    organization := "kz.coders",
    name := "domain-library",
    version := "0.1",
    scalaVersion := "2.12.12",
    libraryDependencies ++= commonDependencies ++ Seq(
      "com.bot4s" %% "telegram-core" % "4.4.0-RC2",
      "com.softwaremill.sttp" %% "core" % "1.6.4"
    )
  )

lazy val `telegram-adapter` = project
  .in(file("telegram-adapter"))
  .settings(
    name := "project-telegram-adapter",
    version := "0.1",
    scalaVersion := "2.12.12",
    libraryDependencies ++= Seq(
      "com.bot4s" %% "telegram-core" % "4.4.0-RC2",
      "com.softwaremill.sttp" %% "core" % "1.6.4"
    ) ++ amqpDependencies ++ akkaDependencies ++ commonDependencies
  )
  .dependsOn(`domain-library`)

lazy val `bot-gateway` = project
  .in(file("bot-gateway"))
  .settings(
    name := "project-bot-gateway",
    version := "0.1",
    scalaVersion := "2.12.12",
    libraryDependencies ++= amqpDependencies ++ akkaDependencies ++ commonDependencies
  )
  .dependsOn(`domain-library`)
