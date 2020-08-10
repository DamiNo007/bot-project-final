package coders.http.actors

import akka.actor.{Actor, ActorLogging, Props}
import com.rabbitmq.client.{Channel, MessageProperties}
import org.json4s.jackson.Serialization.write
import kz.domain.library.messages.{HttpSenderDetails, UserMessage}
import kz.domain.library.utils.serializers.SenderSerializers
import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {
  def props(channel: Channel, exchangeName: String, routingKey: String, replyTo: String): Props =
    Props(new AmqpPublisherActor(channel, exchangeName, routingKey, replyTo))

  case class SendMessage(message: String)

}

class AmqpPublisherActor(channel: Channel, exchangeName: String, routingKey: String, replyTo: String)
  extends Actor
    with ActorLogging
    with SenderSerializers {

  def publish(jsonMessage: String): Unit = {
    Try(
      channel.basicPublish(
        exchangeName,
        routingKey,
        MessageProperties.TEXT_PLAIN,
        jsonMessage.getBytes()
      )
    ) match {
      case Success(_) => log.info(s"successfully sent message $jsonMessage")
      case Failure(exception) =>
        log.warning(s"couldn't message ${
          exception.getMessage
        }")
    }
  }

  def getUserMessage(actorPath: String, command: String): UserMessage = {
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
      val message = s"${
        command.msg
      }"
      val userMessage = getUserMessage(actorPath, message)
      val jsonMessage: String = write(userMessage)
      publish(jsonMessage)
  }
}
