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
import kz.domain.library.utils._

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

  onCommand("/help") { implicit msg =>
    println(s"получил комманду ${msg.text}")
    reply(
      """
        |/start - starts the bot
        |/help - describes each command
        |/getGithubUser <login> - gets user's data
        |/getUserRepositories <login> - gets user's repositories with description
        |/currencies - gets the list of currencies
        |/rates <currency> - gets current currency rates
        |/convert <from> <to> <amount> - converts one currency to another
        |/news - gets latest news from profit.kz
        |/articles - get latest articles from profit.kz
        |""".stripMargin
    ).void
  }

  onCommand("/getGithubUser") { implicit msg =>
    Future(
      publisherActor ! SendGetUserRequest(
        msg.text.map(x => x.split(" ").last.trim).getOrElse("unknown"),
        msg
      )
    )
  }

  onCommand("/getUserRepositories") { implicit msg =>
    Future(
      publisherActor ! SendGetRepositoriesRequest(
        msg.text.map(x => x.split(" ").last.trim).getOrElse("unknown"),
        msg
      )
    )
  }

  onCommand("/currencies") { implicit msg =>
    println(s"получил комманду ${msg.text}")
    Future(publisherActor ! SendGetCurrenciesRequest(msg))
  }

  onCommand("/rates") { implicit msg =>
    Future(
      publisherActor ! SendGetRatesRequest(
        msg.text.map(x => x.split(" ").last.trim).getOrElse("unknown"),
        msg
      )
    )
  }

  onCommand("/convert") { implicit msg =>
    println(s"получил комманду ${msg.text}")
    val msgSplit = msg.text.getOrElse("unknown").split(" ").toList
    msgSplit match {
      case _ :: from :: to :: amount :: _ =>
        Future(publisherActor ? SendGetConvertRequest(from, to, amount, msg))
      case _ => reply("Incorrect command! Example: /convert RUB KZT 100").void
    }
  }

  onCommand("/articles") { implicit msg =>
    println(s"получил комманду ${msg.text}")
    Future(publisherActor ! SendGetArticlesRequest(msg))
  }

  onCommand("/news") { implicit msg =>
    println(s"получил комманду ${msg.text}")
    Future(publisherActor ! SendGetNewsRequest(msg))
  }

  onMessage { implicit msg =>
    if (!msg.text.getOrElse("").startsWith("/")) {
      println(s"получил ${msg.text}")
      reply(s"ECHO: ${msg.text.getOrElse("")}").void
    } else Future()
  }
}
