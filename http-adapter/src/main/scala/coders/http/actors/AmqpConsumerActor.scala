package coders.http.actors

import akka.actor.{Actor, ActorLogging, Props}
import coders.http.Boot.materializer.system
import coders.http.actors.AmqpConsumerActor.ReceiveMessage
import com.rabbitmq.client.Channel
import kz.domain.library.messages.{GatewayResponse, HttpSenderDetails}
import kz.domain.library.utils.SenderSerializers

object AmqpConsumerActor {
  def props(channel: Channel): Props = Props(new AmqpConsumerActor(channel))

  case class ReceiveMessage(message: GatewayResponse)

}

class AmqpConsumerActor(channel: Channel) extends Actor with ActorLogging with SenderSerializers {

  override def receive: Receive = {
    case ReceiveMessage(msg) =>
      val sendTo = system.actorSelection(msg.senderDetails.asInstanceOf[HttpSenderDetails].actorPath)
      sendTo ! msg
  }
}