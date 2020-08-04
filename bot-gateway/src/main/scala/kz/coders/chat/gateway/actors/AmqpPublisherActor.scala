package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.rabbitmq.client.{Channel, MessageProperties}
import kz.coders.chat.gateway.Boot.config
import kz.coders.chat.gateway.actors.AmqpPublisherActor.ReceiveMessage
import org.json4s.jackson.Serialization.write
import kz.domain.library.messages.{GatewayResponse, SenderDetails}
import kz.domain.library.utils.TelegramSerializers
import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {

  def props(channel: Channel): Props = Props(new AmqpPublisherActor(channel))

  case class ReceiveMessage(response: String,
                            msgDetails: Option[SenderDetails],
                            routingKey: String)

}

class AmqpPublisherActor(channel: Channel)
    extends Actor
    with ActorLogging
    with TelegramSerializers {

  override def receive: Receive = {
    case ReceiveMessage(response, msgDetails, replyTo) =>
      val gatewayResponse = GatewayResponse(response, msgDetails)
      val jsonMessage: String = write(gatewayResponse)
      Try(
        channel.basicPublish(
          config.getString("rabbitMq.exchange.responseExchangeName"),
          replyTo,
          MessageProperties.TEXT_PLAIN,
          jsonMessage.getBytes
        )
      ) match {
        case Success(_) =>
          log.info(s"successfully sent message ${response.getClass}")
        case Failure(exception) =>
          log.warning(s"couldn't message ${exception.getMessage}")
      }
  }
}
