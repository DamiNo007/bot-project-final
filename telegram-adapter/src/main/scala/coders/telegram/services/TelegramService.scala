package coders.telegram.services

import java.time.LocalDateTime
import akka.actor.ActorRef
import akka.util.Timeout
import cats.instances.future._
import cats.syntax.functor._
import coders.telegram.actors.AmqpPublisherActor.SendMessage
import com.bot4s.telegram.api.RequestHandler
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.clients.FutureSttpClient
import com.bot4s.telegram.future.{Polling, TelegramBot}
import com.bot4s.telegram.models.{Chat, ChatType, Message}
import com.softwaremill.sttp.SttpBackend
import com.softwaremill.sttp.okhttp.OkHttpFutureBackend
import scala.concurrent.Future
import scala.concurrent.duration._
import kz.domain.library.messages.{GatewayResponse, TelegramSenderDetails}

object TelegramService {
  def getSenderDetails(msg: Message): TelegramSenderDetails = {
    val chatId = msg.chat.id
    val userId: Option[Int] = msg.from.map(_.id)
    val firstName = msg.from.map(_.firstName)
    val lastName = msg.from.flatMap(_.lastName)
    val username = msg.from.flatMap(_.username)
    TelegramSenderDetails(
      chatId = chatId,
      userId = userId,
      username = username,
      lastName = lastName,
      firstName = firstName)
  }
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
    val senderDetails = response.senderDetails.asInstanceOf[TelegramSenderDetails]
    reply(response.response) {
      Message(
        1,
        date = getCurrentDate(LocalDateTime.now()),
        chat = Chat(senderDetails.chatId, ChatType.Private)
      )
    }.void
  }

  onCommand("/start") { implicit msg =>
    println(s"получил комманду ${msg.text}")
    reply("Привет, я DamiNoBot! Чем могу помочь?").void
  }

  onMessage { implicit msg =>
    if (!msg.text.getOrElse("").startsWith("/")) {
      val senderDetails = TelegramService.getSenderDetails(msg)
      publisherActor ! SendMessage(msg.text.getOrElse(""), senderDetails)
    }
    Future()
  }
}
