package coders.telegram.actors

import akka.actor.{Actor, ActorLogging, Props}
import coders.telegram.Boot.config
import coders.telegram.actors.AmqpPublisherActor.SendMessage
import com.rabbitmq.client.{Channel, MessageProperties}
import org.json4s.jackson.Serialization.write
import kz.domain.library.messages.{TelegramSenderDetails, UserMessage}
import kz.domain.library.utils.SenderSerializers
import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {
  def props(channel: Channel): Props = Props(new AmqpPublisherActor(channel))

  case class SendMessage(message: String, senderDetails: TelegramSenderDetails)

}

class AmqpPublisherActor(channel: Channel)
  extends Actor
    with ActorLogging
    with SenderSerializers {

  def publish(jsonMessage: String): Unit = {
    Try(
      channel.basicPublish(
        config.getString("rabbitMq.exchange.requestExchangeName"),
        config.getString("rabbitMq.routingKey.telegramRequestRoutingKey"),
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
      val replyTo = "rabbitMq.routingKey.telegramResponseRoutingKey"
      val userMessage = UserMessage(msg.senderDetails, msg.message, Some(replyTo))
      val jsonMessage = write(userMessage)
      publish(jsonMessage)
  }
}
