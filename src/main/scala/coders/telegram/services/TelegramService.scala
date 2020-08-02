package coders.telegram.services

import java.time.LocalDateTime
import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import cats.instances.future._
import cats.syntax.functor._
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.FutureSttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.models.Message
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import scala.concurrent.Future
import scala.concurrent.duration._
import kz.domain.library.messages.GatewayResponse
import kz.domain.library.utils.{
  GetRepositoriesFailedResponse,
  GetRepositoriesResponse,
  GetUserFailedResponse,
  GetUserResponse,
  Response,
  SendGetRepositoriesRequest,
  SendGetUserRequest
}

class TelegramService(token: String, publisherActor: ActorRef)
  extends TelegramBot
    with Polling
    with Commands[Future] {

  implicit val timeout: Timeout = 5.seconds
  implicit val backend: SttpBackend[Future, Nothing] = OkHttpFutureBackend()
  override val client: RequestHandler[Future] = new FutureSttpClient(token)

  def getCurrentDate(date: LocalDateTime): Int = {
    val dateAsInt = date.getYear * 10000 + date.getMonthValue * 100 + date.getDayOfMonth
    dateAsInt
  }

  def answerToUser(response: GatewayResponse): Unit = {
    reply(response.response) {
      response.senderDetails match {
        case Some(value) =>
          Message(
            messageId = value.messageId.toInt,
            from = value.from,
            date = getCurrentDate(LocalDateTime.now()),
            chat = value.chat
          )
      }
    }.void
  }


  onCommand("/start") { implicit msg =>
    println(s"получил комманду ${msg.text}")
    reply("Привет").void
  }

  onCommand("/getGithubUser") { implicit msg =>
    (publisherActor ? SendGetUserRequest(
      msg.text.map(x => x.split(" ").last.trim).getOrElse("unknown"),
      msg
    )).mapTo[Response]
      .map {
        case res: GetUserResponse => reply(res.response)
        case res: GetUserFailedResponse => reply(res.error)
      }
      .void
  }

  onCommand("/getUserRepositories") { implicit msg =>
    (publisherActor ? SendGetRepositoriesRequest(
      msg.text.map(x => x.split(" ").last.trim).getOrElse("unknown"),
      msg
    )).mapTo[Response]
      .map {
        case res: GetRepositoriesResponse => reply(res.response)
        case res: GetRepositoriesFailedResponse => reply(res.error)
      }
      .void
  }

  onMessage { implicit msg =>
    if (!msg.text.getOrElse("").startsWith("/")) {
      println(s"получил ${msg.text}")
      reply(s"ECHO: ${msg.text.getOrElse("")}").void
    } else Future()
  }
}