package coders.telegram.actors

import akka.actor.{Actor, ActorLogging, Props}
import coders.telegram.actors.AmqpConsumerActor.ReceiveMessage
import coders.telegram.services.TelegramService
import com.rabbitmq.client.Channel
import org.json4s.DefaultFormats

import kz.domain.library.messages.GatewayResponse

object AmqpConsumerActor {
  def props(channel: Channel, service: TelegramService): Props = Props(new AmqpConsumerActor(channel, service))

  case class ReceiveMessage(message: GatewayResponse)

}

class AmqpConsumerActor(channel: Channel, service: TelegramService) extends Actor with ActorLogging {
  implicit val formats = DefaultFormats

  override def receive: Receive = {
    case ReceiveMessage(msg) =>
      service.answerToUser(msg)
  }
}