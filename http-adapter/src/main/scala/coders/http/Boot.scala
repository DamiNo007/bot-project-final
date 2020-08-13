package coders.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.Materializer
import coders.http.actors.AmqpConsumerActor
import coders.http.amqp.AmqpConsumer
import coders.http.routes.Routes
import com.typesafe.config.ConfigFactory
import kz.amqp.library.utils.connection.RabbitMqConnection
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

object Boot extends App {
  implicit val system = ActorSystem("http-adapter")
  implicit val materializer: Materializer =
    Materializer.createMaterializer(system)
  implicit val executionContext: ExecutionContextExecutor =
    ExecutionContext.global

  val config = ConfigFactory.load()

  val connection = RabbitMqConnection.getRabbitMqConnection(
    config.getString("rabbitMq.username"),
    config.getString("rabbitMq.password"),
    config.getString("rabbitMq.host"),
    config.getInt("rabbitMq.port"),
    config.getString("rabbitMq.virtualHost")
  )

  val channel = connection.createChannel()
  println(channel.getClass)
  val routes = new Routes(channel, config)
  val host = config.getString("application.host")
  val port = config.getInt("application.port")
  val amqpConsumer = system.actorOf(AmqpConsumerActor.props(channel, system))

  RabbitMqConnection.declareExchange(
    channel,
    config.getString("rabbitMq.exchange.requestExchangeName"),
    config.getString("rabbitMq.exchange.requestExchangeType")) match {
    case Success(_) => system.log.info("successfully declared request exchange")
    case Failure(exception) =>
      system.log.warning(s"couldn't declare exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareExchange(
    channel,
    config.getString("rabbitMq.exchange.responseExchangeName"),
    config.getString("rabbitMq.exchange.responseExchangeType")) match {
    case Success(_) => system.log.info("successfully declared response exchange")
    case Failure(exception) =>
      system.log.warning(s"couldn't declare exchange ${exception.getMessage}")
  }

  RabbitMqConnection.declareAndBindQueue(
    channel,
    config.getString("rabbitMq.queue.httpResponseQueueName"),
    config.getString("rabbitMq.exchange.responseExchangeName"),
    config.getString("rabbitMq.routingKey.httpResponseRoutingKey")
  )

  Http().bindAndHandle(routes.handlers, host, port)

  system.log.info(s"running on $host:$port")

  channel.basicConsume(config.getString("rabbitMq.queue.httpResponseQueueName"), AmqpConsumer(amqpConsumer))
}
