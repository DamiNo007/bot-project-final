package coders.telegram.actors

import akka.actor.{Actor, ActorLogging, Props}
import coders.telegram.Boot.config
import com.bot4s.telegram.models.Message
import com.rabbitmq.client.{Channel, MessageProperties}
import org.json4s.jackson.Serialization.write
import kz.domain.library.messages.{SenderDetails, UserMessage}
import kz.domain.library.utils._
import scala.util.{Failure, Success, Try}

object AmqpPublisherActor {
  def props(channel: Channel): Props = Props(new AmqpPublisherActor(channel))

  case class SendMessage(message: String)

}

class AmqpPublisherActor(channel: Channel) extends Actor with ActorLogging with TelegramSerializers {

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

  def toUserMessage(command: String, msgDetails: Message, args: List[String]): UserMessage = {
    UserMessage(
      "telegram",
      Some(
        SenderDetails(
          msgDetails.messageId,
          msgDetails.from,
          msgDetails.date,
          msgDetails.chat)
      ),
      Some(msgDetails.text.getOrElse("unknown")),
      command,
      Some(args))
  }

  override def receive: Receive = {
    case SendGetUserRequest(login, msgDetails) =>
      log.info(s"sending message to AMQP $login")
      val userMessage = toUserMessage("getGithubUser", msgDetails, List(login))
      val jsonMessage: String = write(userMessage)
      publish(jsonMessage)

    case SendGetRepositoriesRequest(login, msgDetails) =>
      log.info(s"sending message to AMQP $login")
      val userMessage = toUserMessage("getUserRepositories", msgDetails, List(login))
      val jsonMessage: String = write(userMessage)
      publish(jsonMessage)

    case SendGetCurrenciesRequest(msgDetails) =>
      log.info(s"sending message to AMQP as currencies request")
      val userMessage = toUserMessage("getCurrencies", msgDetails, List())
      val jsonMessage: String = write(userMessage)
      publish(jsonMessage)

    case SendGetRatesRequest(currency, msgDetails) =>
      log.info(s"sending message to AMQP $currency")
      val userMessage = toUserMessage("getRates", msgDetails, List(currency))
      val jsonMessage: String = write(userMessage)
      publish(jsonMessage)

    case SendGetConvertRequest(from, to, amount, msgDetails) =>
      log.info(s"sending message to AMQP $from, $to, $amount")
      val userMessage = toUserMessage("getConvert", msgDetails, List(from, to, amount))
      val jsonMessage: String = write(userMessage)
      publish(jsonMessage)

    case SendGetNewsRequest(msgDetails) =>
      log.info(s"sending message to AMQP as get news request")
      val userMessage = toUserMessage("getNews", msgDetails, List())
      val jsonMessage: String = write(userMessage)
      publish(jsonMessage)

    case SendGetArticlesRequest(msgDetails) =>
      log.info(s"sending message to AMQP as get articles request")
      val userMessage = toUserMessage("getArticles", msgDetails, List())
      val jsonMessage: String = write(userMessage)
      publish(jsonMessage)
  }
}
