package coders.telegram.actors

import akka.actor.{Actor, ActorLogging, Props}
import coders.telegram.actors.AmqpPublisherActor.SendMessage
import com.rabbitmq.client.{Channel, MessageProperties}
import com.typesafe.config.{Config, ConfigFactory}
import org.json4s.jackson.Serialization.write
import kz.domain.library.messages.{TelegramSenderDetails, UserMessage}
import kz.domain.library.utils.serializers.SenderSerializers

import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {
  def props(channel: Channel, routingKey: String): Props =
    Props(new AmqpPublisherActor(channel, routingKey))

  case class SendMessage(message: String, senderDetails: TelegramSenderDetails)

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

  override def receive: Receive = {
    case msg: SendMessage =>
      val replyTo = config.getString("rabbitMq.routingKey.telegramResponseRoutingKey")
      val userMessage = UserMessage(msg.senderDetails, msg.message, Some(replyTo))
      val jsonMessage = write(userMessage)
      publish(jsonMessage)
  }
}
