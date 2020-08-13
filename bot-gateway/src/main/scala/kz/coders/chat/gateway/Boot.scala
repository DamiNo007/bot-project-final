package kz.coders.chat.gateway

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.{Config, ConfigFactory}
import kz.coders.chat.gateway.actors.{AmqpListenerActor, AmqpPublisherActor, DialogFlowActor}
import kz.coders.chat.gateway.amqp.AmqpConsumer
import kz.amqp.library.utils.connection.RabbitMqConnection
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

object Boot extends App {
  implicit val system = ActorSystem("chat-gateway")
  implicit val materializer: Materializer =
    Materializer.createMaterializer(system)
  implicit val executionContext: ExecutionContextExecutor =
    ExecutionContext.global

  val config: Config = ConfigFactory.load()

  val connection = RabbitMqConnection.getRabbitMqConnection(
    config.getString("rabbitMq.username"),
    config.getString("rabbitMq.password"),
    config.getString("rabbitMq.host"),
    config.getInt("rabbitMq.port"),
    config.getString("rabbitMq.virtualHost")
  )

  val channel = connection.createChannel()

  val amqpPublisherActor = system.actorOf(AmqpPublisherActor.props(
    channel,
    config.getString("rabbitMq.exchange.responseExchangeName")
  ))
  val dialogFlowActor = system.actorOf(DialogFlowActor.props(amqpPublisherActor))
  val amqpListenerActor = system.actorOf(AmqpListenerActor.props(dialogFlowActor))

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
    config.getString("rabbitMq.queue.requestQueueName"),
    config.getString("rabbitMq.exchange.requestExchangeName"),
    config.getString("rabbitMq.routingKey.telegramRequestRoutingKey")
  )

  RabbitMqConnection.declareAndBindQueue(
    channel,
    config.getString("rabbitMq.queue.httpRequestQueueName"),
    config.getString("rabbitMq.exchange.requestExchangeName"),
    config.getString("rabbitMq.routingKey.httpRequestRoutingKey")
  )

  channel.basicConsume(config.getString("rabbitMq.queue.requestQueueName"), AmqpConsumer(amqpListenerActor))
  channel.basicConsume(config.getString("rabbitMq.queue.httpRequestQueueName"), AmqpConsumer(amqpListenerActor))
}