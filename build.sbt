name := "project-telegram-adapter"

version := "0.1"

scalaVersion := "2.12.12"

libraryDependencies ++= Seq(
  "com.bot4s" %% "telegram-core" % "4.4.0-RC2",
  "com.softwaremill.sttp" %% "core" % "1.6.4",
  "com.typesafe.akka" %% "akka-actor" % "2.6.7",
  "com.rabbitmq" % "amqp-client" % "5.9.0",
  "kz.coders" %% "domain-library" % "0.1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.6.7",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.json4s" %% "json4s-native" % "3.6.9",
  "org.json4s" %% "json4s-jackson" % "3.6.9",
  "com.lucidchart" %% "xtract" % "2.0.0",
  "com.lucidchart" %% "xtract-testing" % "2.0.0" % "test",
  "com.typesafe.play" %% "play-json" % "2.8.1",
  "org.scala-lang.modules" %% "scala-xml" % "1.1.0"
)

