package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.google.cloud.dialogflow.v2.{DetectIntentRequest, QueryInput, QueryResult, TextInput}
import kz.coders.chat.gateway.actors.AmqpPublisherActor.SendResponse
import kz.coders.chat.gateway.actors.DialogFlowActor.{ProcessUserMessage, getDialogflowResponse}
import kz.domain.library.messages.{GatewayResponse, Sender}
import kz.coders.chat.gateway.actors.exchange.ExchangeWorkerActor
import kz.coders.chat.gateway.actors.github.GithubWorkerActor
import kz.coders.chat.gateway.actors.profitkz.{ArticlesWorkerActor, NewsWorkerActor}
import kz.coders.chat.gateway.dialogflow.DialogflowConnection._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

object DialogFlowActor {

  def props(publisherActor: ActorRef)(implicit system: ActorSystem,
                                      materializer: Materializer): Props =
    Props(new DialogFlowActor(publisherActor))

  def getQueryInput(message: String): QueryInput = {
    QueryInput
      .newBuilder()
      .setText(
        TextInput
          .newBuilder()
          .setText(message)
          .setLanguageCode("EN-US")
          .build())
      .build()
  }

  def getDialogflowResponse(message: String): QueryResult = {
    sessionClient.detectIntent(
      DetectIntentRequest
        .newBuilder()
        .setQueryInput(
          getQueryInput(message)
        )
        .setSession(sessionName.toString)
        .build()
    )
      .getQueryResult
  }

  case class ProcessUserMessage(routingKey: String, message: String, sender: Sender)

}

class DialogFlowActor(publisherActor: ActorRef)(implicit val system: ActorSystem,
                                                materializer: Materializer)
  extends Actor with ActorLogging {

  implicit val ex: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = 20.seconds

  val githubWorkerActor = context.actorOf(GithubWorkerActor.props(config))
  val newsWorkerActor = context.actorOf(NewsWorkerActor.props(config))
  val articlesWorkerActor = context.actorOf(ArticlesWorkerActor.props(config))
  val exchangeWorkerActor = context.actorOf(ExchangeWorkerActor.props(config))

  def getParams(from: String, response: QueryResult): String = {
    response.getParameters.getFieldsMap
      .get(from)
      .getStringValue
  }

  def sendResponse(routingKey: String, sender: Sender, response: String): Unit = {
    publisherActor ! SendResponse(
      routingKey,
      GatewayResponse(
        s"""Вот что мне удалось найти:
           |
           |${response}""".stripMargin,
        sender
      )
    )
  }

  def extractResponse(response: Future[Any], routingKey: String, sender: Sender): Unit = {
    response.mapTo[Response].map {
      case res: ReceivedResponse =>
        sendResponse(routingKey, sender, res.response)
      case res: ReceivedFailureResponse =>
        sendResponse(routingKey, sender, res.error)
    }
  }

  override def receive: Receive = {
    case command: ProcessUserMessage =>
      val response = getDialogflowResponse(command.message)
      response.getIntent.getDisplayName match {
        case "get-github-account" =>
          val params = getParams("github-account", response)
          extractResponse(githubWorkerActor ? GetUser(params), command.routingKey, command.sender)
        case "get-github-repos" =>
          val params = getParams("github-repos", response)
          extractResponse(githubWorkerActor ? GetRepositories(params), command.routingKey, command.sender)
        case "get-news" =>
          val params = getParams("news", response)
          extractResponse(newsWorkerActor ? GetNews(params), command.routingKey, command.sender)
        case "get-articles" =>
          val params = getParams("articles", response)
          extractResponse(articlesWorkerActor ? GetArticles(params), command.routingKey, command.sender)
        case "get-currencies" =>
          val params = getParams("currencies", response)
          extractResponse(exchangeWorkerActor ? GetCurrencies(params), command.routingKey, command.sender)
        case "get-convert" =>
          val amount = getParams("amount", response)
          val from = getParams("from", response)
          val to = getParams("to", response)
          extractResponse(exchangeWorkerActor ? Convert(from, to, amount), command.routingKey, command.sender)
        case "get-rates" =>
          val params = getParams("rates", response)
          extractResponse(exchangeWorkerActor ? GetRates(params), command.routingKey, command.sender)
        case _ =>
          publisherActor ! SendResponse(command.routingKey, GatewayResponse(response.getFulfillmentText, command.sender))
      }
  }
}
