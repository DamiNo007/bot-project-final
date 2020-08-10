package coders.telegram

import akka.actor.{ActorRef, ActorSystem}
import coders.telegram.actors.{AmqpConsumerActor, AmqpPublisherActor}
import coders.telegram.amqp.AmqpConsumer
import coders.telegram.services.TelegramService
import com.typesafe.config.ConfigFactory
import kz.amqp.library.utils.connection.RabbitMqConnection
import scala.util.{Failure, Success}

object Boot extends App {

  val system = ActorSystem("telegram-test")
  val config = ConfigFactory.load()
  val token = config.getString("telegram-token")

  val connection = RabbitMqConnection.getRabbitMqConnection(
    config.getString("rabbitMq.username"),
    config.getString("rabbitMq.password"),
    config.getString("rabbitMq.host"),
    config.getInt("rabbitMq.port"),
    config.getString("rabbitMq.virtualHost")
  )
  val channel = connection.createChannel()

  val ref: ActorRef = system.actorOf(
    AmqpPublisherActor.props(
      channel,
      config.getString("rabbitMq.exchange.requestExchangeName"),
      config.getString("rabbitMq.routingKey.telegramRequestRoutingKey"),
      config.getString("rabbitMq.routingKey.telegramResponseRoutingKey")
    )
  )
  val telegramService = new TelegramService(token, ref, system)
  val amqpConsumer = system.actorOf(AmqpConsumerActor.props(channel, telegramService))

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
    config.getString("rabbitMq.queue.responseQueueName"),
    config.getString("rabbitMq.exchange.responseExchangeName"),
    config.getString("rabbitMq.routingKey.telegramResponseRoutingKey")
  )

  telegramService.run()

  channel.basicConsume(config.getString("rabbitMq.queue.responseQueueName"), AmqpConsumer(amqpConsumer))
}
