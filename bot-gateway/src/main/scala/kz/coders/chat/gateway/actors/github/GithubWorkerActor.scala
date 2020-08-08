package kz.coders.chat.gateway.actors.github

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import kz.coders.chat.gateway.actors.github.GithubRequesterActor.{GetUserAccount, GetUserRepositories}
import kz.coders.chat.gateway.actors.{GetRepositories, GetRepositoriesFailedResponse, GetUser, GetUserFailedResponse}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object GithubWorkerActor {
  def props()(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new GithubWorkerActor())
}

class GithubWorkerActor()(implicit val system: ActorSystem,
                          materializer: Materializer)
  extends Actor with ActorLogging {

  implicit val timeout: Timeout = 100.seconds
  implicit val ex: ExecutionContext = context.dispatcher
  val requestActor = context.actorOf(Props(new GithubRequesterActor))

  override def receive: Receive = {
    case GetUser(login) =>
      val sender = context.sender()
      (requestActor ? GetUserAccount(login)).onComplete {
        case Success(value) =>
          sender ! value
        case Failure(e) =>
          sender ! GetUserFailedResponse(e.getMessage)
      }
    case GetRepositories(login) =>
      val sender = context.sender()
      (requestActor ? GetUserRepositories(login)).onComplete {
        case Success(value) => sender ! value
        case Failure(e) =>
          sender ! GetRepositoriesFailedResponse(e.getMessage)
      }
  }
}