package kz.coders.chat.gateway.actors.profitkz

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.Materializer
import akka.pattern.ask
import akka.util.Timeout
import kz.coders.chat.gateway.actors.{GetNews, ReceivedFailureResponse}
import kz.coders.chat.gateway.actors.profitkz.NewsRequesterActor.GetNewsAll
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object NewsWorkerActor {
  def props()(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new NewsWorkerActor())
}

class NewsWorkerActor()(implicit val system: ActorSystem,
                        materializer: Materializer)
  extends Actor with ActorLogging {

  implicit val timeout: Timeout = 100.seconds
  implicit val ex: ExecutionContext = context.dispatcher
  val requestActor = context.actorOf(Props(new NewsRequesterActor))

  override def receive: Receive = {
    case GetNews(msg) =>
      val sender = context.sender()
      (requestActor ? GetNewsAll(msg)).onComplete {
        case Success(value) =>
          sender ! value
        case Failure(e) =>
          sender ! ReceivedFailureResponse(e.getMessage)
      }
  }
}