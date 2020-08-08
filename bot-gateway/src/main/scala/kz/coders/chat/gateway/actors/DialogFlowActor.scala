package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.google.cloud.dialogflow.v2.QueryResult
import kz.coders.chat.gateway.actors.AmqpPublisherActor.SendResponse
import kz.coders.chat.gateway.actors.DialogFlowActor.ProcessUserMessage
import kz.domain.library.messages.{GatewayResponse, Sender}
import kz.coders.chat.gateway.Boot.{executionContext, materializer, system}
import kz.coders.chat.gateway.actors.github.GithubWorkerActor
import kz.coders.chat.gateway.actors.profitkz.{ArticlesWorkerActor, NewsWorkerActor}
import kz.coders.chat.gateway.dialogflow.DialogflowConf
import scala.concurrent.duration.DurationInt

object DialogFlowActor {
  def props(publisherActor: ActorRef): Props =
    Props(new DialogFlowActor(publisherActor))

  case class ProcessUserMessage(routingKey: String, message: String, sender: Sender)

}

class DialogFlowActor(publisherActor: ActorRef)
  extends Actor with ActorLogging
    with DialogflowConf {

  implicit val timeout: Timeout = 20.seconds

  val githubWorkerActor = context.actorOf(GithubWorkerActor.props())
  val newsWorkerActor = context.actorOf(NewsWorkerActor.props())
  val articlesWorkerActor = context.actorOf(ArticlesWorkerActor.props())

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

  override def receive: Receive = {
    case command: ProcessUserMessage =>
      val response = getDialogflowResponse(command.message)
      response.getIntent.getDisplayName match {
        case "get-github-account" =>
          val params = getParams("github-account", response)
          (githubWorkerActor ? GetUser(params))
            .mapTo[Response]
            .map {
              case res: GetUserResponse =>
                sendResponse(command.routingKey, command.sender, res.response)

              case res: GetUserFailedResponse =>
                sendResponse(command.routingKey, command.sender, res.error)
            }
        case "get-github-repos" =>
          val params = getParams("github-repos", response)
          (githubWorkerActor ? GetRepositories(params))
            .mapTo[Response]
            .map {
              case res: GetRepositoriesResponse =>
                sendResponse(command.routingKey, command.sender, res.response)

              case res: GetRepositoriesFailedResponse =>
                sendResponse(command.routingKey, command.sender, res.error)
            }
        case "get-news" =>
          val params = getParams("news", response)
          (newsWorkerActor ? GetNews(params))
            .mapTo[Response]
            .map {
              case res: GetNewsResponse =>
                sendResponse(command.routingKey, command.sender, res.response)

              case res: GetNewsFailedResponse =>
                sendResponse(command.routingKey, command.sender, res.error)
            }
        case "get-articles" =>
          val params = getParams("articles", response)
          (articlesWorkerActor ? GetArticles(params))
            .mapTo[Response]
            .map {
              case res: GetArticlesResponse =>
                sendResponse(command.routingKey, command.sender, res.response)

              case res: GetArticlesFailedResponse =>
                sendResponse(command.routingKey, command.sender, res.error)
            }
        case _ =>
          publisherActor ! SendResponse(command.routingKey, GatewayResponse(response.getFulfillmentText, command.sender))
      }
  }
}
