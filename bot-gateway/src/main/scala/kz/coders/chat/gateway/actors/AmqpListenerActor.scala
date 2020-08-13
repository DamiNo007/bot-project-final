package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import kz.coders.chat.gateway.actors.DialogFlowActor.ProcessUserMessage
import kz.domain.library.messages.UserMessage
import kz.domain.library.utils.serializers.SenderSerializers
import org.json4s.jackson.JsonMethods.parse

object AmqpListenerActor {
  def props(dialogFlowActor: ActorRef): Props =
    Props(new AmqpListenerActor(dialogFlowActor))
}

class AmqpListenerActor(dialogFlowActor: ActorRef) extends Actor with ActorLogging with SenderSerializers {
  override def receive: Receive = {
    case msg: String =>
      val userMessage = parse(msg).extract[UserMessage]
      log.info(s"received message $userMessage")

      userMessage.replyTo match {
        case Some(replyTo) =>
          dialogFlowActor ! ProcessUserMessage(
            replyTo,
            userMessage.message,
            userMessage.senderDetails
          )
        case None =>
          log.info("No replyTo provided")
      }
  }
}
