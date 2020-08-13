package coders.http.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.util.Timeout
import coders.http.actors.AmqpConsumerActor.ReceiveMessage
import com.rabbitmq.client.Channel
import kz.domain.library.messages.{GatewayResponse, HttpSenderDetails}
import kz.domain.library.utils.serializers.SenderSerializers
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

object AmqpConsumerActor {
  def props(channel: Channel, system: ActorSystem): Props = Props(new AmqpConsumerActor(channel, system))

  case class ReceiveMessage(message: GatewayResponse)

}

class AmqpConsumerActor(channel: Channel, system: ActorSystem) extends Actor with ActorLogging with SenderSerializers {

  implicit val ex: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = 20.seconds

  override def receive: Receive = {
    case ReceiveMessage(msg) =>
      val sender = msg.senderDetails
      sender match {
        case HttpSenderDetails(actorPath) =>
          system.actorSelection(actorPath).resolveOne.onComplete {
            case Success(ref) => ref ! msg
            case Failure(error) => log.warning(s"actor by path $actorPath not fount: ${error.getMessage}")
          }
        case unhandledSender => log.warning("wrong type {}", unhandledSender.getClass.getName)
      }
  }
}