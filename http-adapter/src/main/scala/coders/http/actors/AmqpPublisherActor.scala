package coders.http.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.rabbitmq.client.{Channel, MessageProperties}
import com.typesafe.config.{Config, ConfigFactory}
import org.json4s.jackson.Serialization.write
import kz.domain.library.messages.{HttpSenderDetails, UserMessage}
import kz.domain.library.utils.serializers.SenderSerializers

import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {
  def props(channel: Channel, routingKey: String): Props =
    Props(new AmqpPublisherActor(channel, routingKey))

  case class SendMessage(message: String)

}

class AmqpPublisherActor(channel: Channel, routingKey: String)
  extends Actor
    with ActorLogging
    with SenderSerializers {

  val config: Config = ConfigFactory.load()

  def publish(jsonMessage: String): Unit = {
    Try(
      channel.basicPublish(
        config.getString("rabbitMq.exchange.requestExchangeName"),
        routingKey,
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
    val replyTo = config.getString("rabbitMq.routingKey.httpResponseRoutingKey")
    UserMessage(
      HttpSenderDetails(
        actorPath
      ),
      command,
      Some(replyTo)
    )
  }

  override def receive: Receive = {
    case command: SendRequest =>
      val sender = context.sender()
      val actorPath = sender.path.toStringWithoutAddress
      val message = s"${command.msg}"
      val userMessage = getUserMessage(actorPath, message)
      val jsonMessage: String = write(userMessage)
      publish(jsonMessage)
  }
}
