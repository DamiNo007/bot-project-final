package kz.coders.chat.gateway.actors.profitkz

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.Materializer
import akka.pattern.ask
import akka.util.Timeout
import kz.coders.chat.gateway.actors.{GetArticles, ReceivedFailureResponse}
import kz.coders.chat.gateway.actors.profitkz.ArticlesRequesterActor._
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
          sender ! ReceivedFailureResponse(e.getMessage)
      }
  }
}