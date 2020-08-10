package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.rabbitmq.client.{Channel, MessageProperties}
import kz.coders.chat.gateway.actors.AmqpPublisherActor.SendResponse
import org.json4s.jackson.Serialization.write
import kz.domain.library.messages.GatewayResponse
import kz.domain.library.utils.serializers.SenderSerializers
import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {

  def props(channel: Channel, exchangeName: String): Props = Props(new AmqpPublisherActor(channel, exchangeName))

  case class SendResponse(routingKey: String,
                          response: GatewayResponse)

}

class AmqpPublisherActor(channel: Channel, exchangeName: String)
  extends Actor
    with ActorLogging
    with SenderSerializers {

  override def receive: Receive = {
    case command: SendResponse =>
      val jsonMessage: String = write(command.response)
      Try(
        channel.basicPublish(
          exchangeName,
          command.routingKey,
          MessageProperties.TEXT_PLAIN,
          jsonMessage.getBytes
        )
      ) match {
        case Success(_) =>
          log.info(s"successfully sent message ${command.response.getClass}")
        case Failure(exception) =>
          log.warning(s"couldn't message ${exception.getMessage}")
      }
  }
}
