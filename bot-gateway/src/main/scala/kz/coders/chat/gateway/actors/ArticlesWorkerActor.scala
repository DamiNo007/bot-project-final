package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.Materializer
import akka.pattern.ask
import akka.util.Timeout
import kz.coders.chat.gateway.Boot.channel
import kz.coders.chat.gateway.actors.AmqpPublisherActor.ReceiveMessage
import kz.coders.chat.gateway.actors.ArticlesRequesterActor.{GetArticlesAll, GetArticlesAllHttp}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}
import kz.domain.library.utils._
import org.json4s.native.Serialization.write

object ArticlesWorkerActor {
  def props()(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new ArticlesWorkerActor())
}

class ArticlesWorkerActor()(implicit val system: ActorSystem,
                            materializer: Materializer)
  extends Actor with ActorLogging with TelegramSerializers {

  implicit val timeout: Timeout = 100.seconds
  implicit val ex: ExecutionContext = context.dispatcher
  val requestActor = context.actorOf(Props(new ArticlesRequesterActor))
  val amqpPublishActor = context.actorOf(Props(new AmqpPublisherActor(channel)))

  override def receive: Receive = {
    case GetArticles(senderDetails, routingKey) =>
      (requestActor ? GetArticlesAll("articles")).onComplete {
        case Success(value) =>
          value match {
            case res: GetArticlesResponse =>
              amqpPublishActor ! ReceiveMessage(res.response, senderDetails, routingKey)
            case res: GetArticlesFailedResponse =>
              amqpPublishActor ! ReceiveMessage(res.error, senderDetails, routingKey)
          }
        case Failure(e) =>
          amqpPublishActor ! ReceiveMessage(e.getMessage, senderDetails, routingKey)
      }
    case GetArticlesHttp(senderDetails, routingKey) =>
      (requestActor ? GetArticlesAllHttp("articles")).onComplete {
        case Success(value) =>
          value match {
            case res: GetArticlesHttpResponse =>
              val response = write(res)
              amqpPublishActor ! ReceiveMessage(response, senderDetails, routingKey)
            case res: GetArticlesFailedResponse =>
              amqpPublishActor ! ReceiveMessage(res.error, senderDetails, routingKey)
          }
        case Failure(e) =>
          amqpPublishActor ! ReceiveMessage(e.getMessage, senderDetails, routingKey)
      }
  }
}