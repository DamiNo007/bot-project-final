package kz.coders.chat.gateway.actors

import akka.pattern.ask
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.stream.Materializer
import akka.util.Timeout
import kz.coders.chat.gateway.Boot.channel
import kz.coders.chat.gateway.actors.AmqpPublisherActor.ReceiveMessage
import kz.coders.chat.gateway.actors.ExchangeRequesterActor.{GetAllCurrencies, GetAllCurrenciesHttp, GetConvertResult, GetRatesAll, GetRatesAllHttp}
import org.json4s.jackson.Serialization.write
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import kz.domain.library.utils._

object ExchangeWorkerActor {
  def props()(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new ExchangeWorkerActor())
}

class ExchangeWorkerActor()(implicit val system: ActorSystem,
                            materializer: Materializer)
  extends Actor with ActorLogging with TelegramSerializers {

  implicit val ex: ExecutionContext = context.dispatcher
  implicit val timeout: Timeout = 100.seconds

  val requestActor: ActorRef =
    context.actorOf(Props(new ExchangeRequesterActor()))
  val amqpPublishActor = context.actorOf(AmqpPublisherActor.props(channel))

  override def receive: Receive = {
    case GetCurrencies(senderDetails, routingKey) =>
      (requestActor ? GetAllCurrencies("currencies")).onComplete {
        case Success(value) =>
          value match {
            case res: GetCurrenciesResponse =>
              amqpPublishActor ! ReceiveMessage(res.response, senderDetails, routingKey)
            case res: GetCurrenciesFailedResponse =>
              amqpPublishActor ! ReceiveMessage(res.error, senderDetails, routingKey)
          }
        case Failure(e) =>
          amqpPublishActor ! ReceiveMessage(e.getMessage, senderDetails, routingKey)
      }

    case GetRates(currency, senderDetails, routingKey) =>
      val sender = context.sender()
      (requestActor ? GetRatesAll(currency)).onComplete {
        case Success(value) =>
          value match {
            case res: GetRatesResponse =>
              amqpPublishActor ! ReceiveMessage(res.response, senderDetails, routingKey)
            case res: GetRatesFailedResponse =>
              amqpPublishActor ! ReceiveMessage(res.error, senderDetails, routingKey)
          }
        case Failure(e) =>
          amqpPublishActor ! ReceiveMessage(e.getMessage, senderDetails, routingKey)
      }

    case Convert(args, senderDetails, routingKey) =>
      val from = args(0)
      val to = args(1)
      val amount = args(2)
      (requestActor ? GetConvertResult(from, to, amount)).onComplete {
        case Success(value) =>
          value match {
            case res: ConvertResponse =>
              amqpPublishActor ! ReceiveMessage(res.response, senderDetails, routingKey)
            case res: ConvertFailedResponse =>
              amqpPublishActor ! ReceiveMessage(res.error, senderDetails, routingKey)
          }
        case Failure(e) =>
          amqpPublishActor ! ReceiveMessage(e.getMessage, senderDetails, routingKey)
      }

    case GetCurrenciesHttp(msg, senderDetails, routingKey) =>
      (requestActor ? GetAllCurrenciesHttp("currencies")).onComplete {
        case Success(value) =>
          value match {
            case res: GetCurrenciesResponseHttp =>
              val response = write(res.symbols)
              amqpPublishActor ! ReceiveMessage(response, senderDetails, routingKey)
            case res: GetCurrenciesFailedResponse =>
              amqpPublishActor ! ReceiveMessage(res.error, senderDetails, routingKey)
          }
        case Failure(e) =>
          sender ! GetCurrenciesFailedResponse(
            s"""Something went wrong! Try again later...
               |Details:
               |${e.getMessage}
               |""".stripMargin
          )
      }

    case GetRatesHttp(currency, senderDetails, routingKey) =>
      val sender = context.sender()
      (requestActor ? GetRatesAllHttp(currency)).onComplete {
        case Success(value) => sender ! value
        case Failure(e) =>
          sender ! GetRatesFailedResponse(
            s"""Something went wrong! Try again later...
               |Details:
               |${e.getMessage}
               |""".stripMargin
          )
      }
  }
}