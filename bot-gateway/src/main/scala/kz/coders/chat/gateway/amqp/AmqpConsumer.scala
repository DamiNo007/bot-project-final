package kz.coders.chat.gateway.amqp

import akka.actor.ActorRef
import akka.util.Timeout
import com.rabbitmq.client.{AMQP, Consumer, Envelope, ShutdownSignalException}
import kz.coders.chat.gateway.Boot.materializer.system
import kz.domain.library.messages.UserMessage
import kz.domain.library.utils._
import org.json4s.jackson.JsonMethods.parse
import scala.concurrent.duration.DurationInt

object AmqpConsumer {
  def apply(githubActor: ActorRef, exchangeActor: ActorRef, newsActor: ActorRef, articlesActor: ActorRef): AmqpConsumer = new AmqpConsumer(githubActor, exchangeActor, newsActor, articlesActor)
}

class AmqpConsumer(githubActor: ActorRef, exchangeActor: ActorRef, newsActor: ActorRef, articlesActor: ActorRef) extends Consumer with TelegramSerializers {

  implicit val timeout: Timeout = 20.seconds

  override def handleConsumeOk(consumerTag: String): Unit = ()

  override def handleCancelOk(consumerTag: String): Unit = ()

  override def handleCancel(consumerTag: String): Unit = ()

  override def handleShutdownSignal(consumerTag: String,
                                    sig: ShutdownSignalException): Unit = ()

  override def handleRecoverOk(consumerTag: String): Unit = ()

  override def handleDelivery(consumerTag: String,
                              envelope: Envelope,
                              properties: AMQP.BasicProperties,
                              body: Array[Byte]): Unit = {
    val message = parse(new String(body)).extract[UserMessage]
    val senderDetails = message.senderDetails

    message.senderPlatform match {
      case "telegram" =>
        val routingKey = "rabbitMq.routingKey.telegramResponseRoutingKey"
        message.args match {
          case Some(List()) =>
            message.command match {
              case "getCurrencies" => exchangeActor ! GetCurrencies(senderDetails, routingKey)
              case "getNews" => newsActor ! GetNews(senderDetails, routingKey)
              case "getArticles" => articlesActor ! GetArticles(senderDetails, routingKey)
              case _ =>
                system.log.warning(s"No such command ${message.command}!")
            }
          case Some(List(arg)) =>
            message.command match {
              case "getGithubUser" => githubActor ! GetUser(arg, senderDetails, routingKey)
              case "getUserRepositories" => githubActor ! GetRepositories(arg, senderDetails, routingKey)
              case "getRates" => exchangeActor ! GetRates(arg, senderDetails, routingKey)
              case _ =>
                system.log.warning(s"No such command ${message.command}!")
            }
          case Some(List(arg1, arg2, arg3)) =>
            message.command match {
              case "getConvert" => exchangeActor ! Convert(List(arg1, arg2, arg3), senderDetails, routingKey)
              case _ =>
                system.log.warning(s"No such command ${message.command}!")
            }
        }
      case "http" =>
        val routingKey = "rabbitMq.routingKey.httpResponseRoutingKey"
        message.args match {
          case Some(List(arg)) =>
            message.command match {
              case "getGithubUser" => githubActor ! GetUserHttp(arg, routingKey)
              case "getUserRepositories" => githubActor ! GetRepositoriesHttp(arg, routingKey)
            }
        }
    }
  }
}