package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import akka.util.Timeout
import com.google.cloud.dialogflow.v2.{DetectIntentRequest, QueryInput, QueryResult, TextInput}
import kz.coders.chat.gateway.actors.AmqpPublisherActor.SendResponse
import kz.coders.chat.gateway.actors.DialogFlowActor.{ProcessUserMessage, getDialogflowResponse}
import kz.domain.library.messages.{GatewayResponse, Sender}
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
      publisherActor ! SendResponse(
        command.routingKey,
        GatewayResponse(
          response.getFulfillmentMessages(0).getText.getText(0),
          command.sender
        )
      )
  }
}
