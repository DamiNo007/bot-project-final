package coders.http.actors

import akka.actor.{Actor, ActorLogging, Props}
import coders.http.Boot.config
import com.rabbitmq.client.{Channel, MessageProperties}
import org.json4s.jackson.Serialization.write
import kz.domain.library.messages.{HttpSenderDetails, UserMessage}
import kz.domain.library.utils.SenderSerializers

import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {
  def props(channel: Channel, replyTo: String): Props = Props(new AmqpPublisherActor(channel, replyTo))

  case class SendMessage(message: String)

}

class AmqpPublisherActor(channel: Channel, replyTo: String) extends Actor with ActorLogging with SenderSerializers {

  def publish(jsonMessage: String): Unit = {
    Try(
      channel.basicPublish(
        config.getString("rabbitMq.exchange.requestExchangeName"),
        config.getString("rabbitMq.routingKey.httpRequestRoutingKey"),
        MessageProperties.TEXT_PLAIN,
        jsonMessage.getBytes()
      )
    ) match {
      case Success(_) => log.info(s"successfully sent message $jsonMessage")
      case Failure(exception) =>
        log.warning(s"couldn't message ${exception.getMessage}")
    }
  }

  def getUserMessage(actorPath: String, command: String): UserMessage = {
    val replyTo = "rabbitMq.routingKey.httpResponseRoutingKey"
    UserMessage(
      HttpSenderDetails(
        actorPath
      ),
      command,
      Some(replyTo)
    )
  }

  override def receive: Receive = {
    case command: SendGetUserHttpRequest =>
      log.info(s"sending message to AMQP ${command.login}")
      val sender = context.sender()
      val actorPath = sender.path.toStringWithoutAddress
      val userMessage = getUserMessage(actorPath, s"get github account ${command.login}")
      val jsonMessage: String = write(userMessage)
      publish(jsonMessage)

    case command: SendGetRepositoriesHttpRequest =>
      log.info(s"sending message to AMQP ${command.login}")
      val sender = context.sender()
      val actorPath = sender.path.toStringWithoutAddress
      val userMessage = getUserMessage(actorPath, s"get repos ${command.login}")
      val jsonMessage: String = write(userMessage)
      publish(jsonMessage)
  }
}
