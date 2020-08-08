package kz.coders.chat.gateway.actors.profitkz

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.Materializer
import akka.pattern.ask
import akka.util.Timeout
import kz.coders.chat.gateway.actors.{GetArticles, GetArticlesFailedResponse, GetArticlesHttp}
import kz.coders.chat.gateway.actors.profitkz.ArticlesRequesterActor.{GetArticlesAll, GetArticlesAllHttp}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object ArticlesWorkerActor {
  def props()(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new ArticlesWorkerActor())
}

class ArticlesWorkerActor()(implicit val system: ActorSystem,
                            materializer: Materializer)
  extends Actor with ActorLogging {

  implicit val timeout: Timeout = 100.seconds
  implicit val ex: ExecutionContext = context.dispatcher
  val requestActor = context.actorOf(Props(new ArticlesRequesterActor))

  override def receive: Receive = {
    case GetArticles(msg) =>
      val sender = context.sender()
      (requestActor ? GetArticlesAll(msg)).onComplete {
        case Success(value) =>
          sender ! value
        case Failure(e) =>
          sender ! GetArticlesFailedResponse(e.getMessage)
      }
    case GetArticlesHttp(msg) =>
      val sender = context.sender()
      (requestActor ? GetArticlesAllHttp(msg)).onComplete {
        case Success(value) =>
          log.info(s"received response $value")
          sender ! value
        case Failure(e) =>
          log.info(s"received error response ${e.getMessage}")
          sender ! GetArticlesFailedResponse(e.getMessage)
      }
  }
}