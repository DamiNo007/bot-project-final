package coders.telegram.actors

import akka.actor.{Actor, ActorLogging, Props}
import coders.telegram.Boot.config
import com.rabbitmq.client.{Channel, MessageProperties}
import org.json4s.jackson.Serialization.write

import kz.domain.library.messages.{SenderDetails, UserMessage}
import scala.kz.domain.library.utils.TelegramSerializers
import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {
  def props(channel: Channel): Props = Props(new AmqpPublisherActor(channel))

  case class SendMessage(message: String)

}

class AmqpPublisherActor(channel: Channel) extends Actor with ActorLogging with TelegramSerializers {

  override def receive: Receive = {
    case GetUser(login, msgDetails) =>
      log.info(s"sending message to AMQP $login")
      val userMessage =
        UserMessage("telegram", SenderDetails(
          msgDetails.messageId,
          msgDetails.from,
          msgDetails.date,
          msgDetails.chat),
          Some(login),
          "getGithubUser",
          login)

      val jsonMessage: String = write(userMessage)

      Try(
        channel.basicPublish(
          config.getString("rabbitMq.exchange.requestExchangeName"),
          config.getString("rabbitMq.routingKey.telegramRequestRoutingKey"),
          MessageProperties.TEXT_PLAIN,
          jsonMessage.getBytes()
        )
      ) match {
        case Success(_) => log.info(s"successfully sent message $login")
        case Failure(exception) =>
          log.warning(s"couldn't message ${exception.getMessage}")
      }

    case GetRepositories(login, msgDetails) =>
      log.info(s"sending message to AMQP $login")
      val userMessage =
        UserMessage("telegram", SenderDetails(
          msgDetails.messageId,
          msgDetails.from,
          msgDetails.date,
          msgDetails.chat),
          Some(login),
          "getUserRepositories",
          login)

      val jsonMessage: String = write(userMessage)

      Try(
        channel.basicPublish(
          config.getString("rabbitMq.exchange.requestExchangeName"),
          config.getString("rabbitMq.routingKey.telegramRequestRoutingKey"),
          MessageProperties.TEXT_PLAIN,
          jsonMessage.getBytes()
        )
      ) match {
        case Success(_) => log.info(s"successfully sent message $login")
        case Failure(exception) =>
          log.warning(s"couldn't message ${exception.getMessage}")
      }
  }
}
