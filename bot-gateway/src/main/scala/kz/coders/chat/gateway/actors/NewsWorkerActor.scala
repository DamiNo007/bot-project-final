package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.Materializer
import akka.pattern.ask
import akka.util.Timeout
import kz.coders.chat.gateway.Boot.channel
import kz.coders.chat.gateway.actors.AmqpPublisherActor.ReceiveMessage
import kz.coders.chat.gateway.actors.NewsRequesterActor.{GetNewsAll, GetNewsAllHttp}
import kz.domain.library.utils._
import org.json4s.jackson.Serialization.write
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object NewsWorkerActor {
  def props()(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new NewsWorkerActor())
}

class NewsWorkerActor()(implicit val system: ActorSystem,
                        materializer: Materializer)
  extends Actor with ActorLogging with TelegramSerializers {

  implicit val timeout: Timeout = 100.seconds
  implicit val ex: ExecutionContext = context.dispatcher
  val requestActor = context.actorOf(Props(new NewsRequesterActor))
  val amqpPublishActor = context.actorOf(AmqpPublisherActor.props(channel))

  override def receive: Receive = {
    case GetNews(senderDetails, routingKey) =>
      (requestActor ? GetNewsAll("news")).onComplete {
        case Success(value) =>
          value match {
            case res: GetNewsResponse =>
              amqpPublishActor ! ReceiveMessage(res.response, senderDetails, routingKey)
            case res: GetNewsFailedResponse =>
              amqpPublishActor ! ReceiveMessage(res.error, senderDetails, routingKey)
          }
        case Failure(e) =>
          amqpPublishActor ! ReceiveMessage(e.getMessage, senderDetails, routingKey)
      }
    case GetNewsHttp(senderDetails, routingKey) =>
      (requestActor ? GetNewsAllHttp("news")).onComplete {
        case Success(value) =>
          value match {
            case res: GetNewsHttpResponse =>
              val response = write(res)
              amqpPublishActor ! ReceiveMessage(response, senderDetails, routingKey)
            case res: GetNewsFailedResponse =>
              amqpPublishActor ! ReceiveMessage(res.error, senderDetails, routingKey)
          }
        case Failure(e) =>
          amqpPublishActor ! ReceiveMessage(e.getMessage, senderDetails, routingKey)
      }
  }
}