package kz.coders.chat.gateway

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.config.{Config, ConfigFactory}
import kz.coders.chat.gateway.actors.{ArticlesWorkerActor, ExchangeWorkerActor, GithubWorkerActor, NewsWorkerActor}
import kz.coders.chat.gateway.amqp.{AmqpConsumer, RabbitMqConnection}
import scala.util.{Failure, Success}

object Boot extends App {
  implicit val system = ActorSystem("chat-gateway")
  implicit val materializer: Materializer =
    Materializer.createMaterializer(system)

  val config: Config = ConfigFactory.load()

  val connection = RabbitMqConnection.getRabbitMqConnection(
    config.getString("rabbitMq.username"),
    config.getString("rabbitMq.password"),
    config.getString("rabbitMq.host"),
    config.getInt("rabbitMq.port"),
    config.getString("rabbitMq.virtualHost")
  )

  val channel = connection.createChannel()
  val githubActor = system.actorOf(GithubWorkerActor.props())
  val exchangeActor = system.actorOf(ExchangeWorkerActor.props())
  val newsActor = system.actorOf(NewsWorkerActor.props())
  val articlesActor = system.actorOf(ArticlesWorkerActor.props())

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

  channel.basicConsume(config.getString("rabbitMq.queue.requestQueueName"), AmqpConsumer(githubActor, exchangeActor, newsActor, articlesActor))
  channel.basicConsume(config.getString("rabbitMq.queue.httpRequestQueueName"), AmqpConsumer(githubActor, exchangeActor, newsActor, articlesActor))
}