package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import kz.coders.chat.gateway.Boot.channel
import kz.coders.chat.gateway.actors.AmqpPublisherActor.ReceiveMessage
import kz.coders.chat.gateway.actors.GithubRequesterActor.{GetUserAccount, GetUserAccountHttp, GetUserRepositories, GetUserRepositoriesHttp}
import org.json4s.jackson.Serialization.write
import kz.domain.library.utils._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import org.json4s.jackson.JsonMethods.parse

object GithubWorkerActor {
  def props()(implicit system: ActorSystem, materializer: Materializer): Props =
    Props(new GithubWorkerActor())
}

class GithubWorkerActor()(implicit val system: ActorSystem,
                          materializer: Materializer)
  extends Actor with ActorLogging with TelegramSerializers {

  implicit val timeout: Timeout = 100.seconds
  implicit val ex: ExecutionContext = context.dispatcher
  val requestActor = context.actorOf(Props(new GithubRequesterActor))
  val amqpPublishActor = context.actorOf(AmqpPublisherActor.props(channel))

  override def receive: Receive = {
    case GetUser(login, senderDetails, routingKey) =>
      (requestActor ? GetUserAccount(login)).onComplete {
        case Success(value) =>
          value match {
            case res: GetUserResponse =>
              amqpPublishActor ! ReceiveMessage(res.response, senderDetails, routingKey)
            case res: GetUserFailedResponse =>
              amqpPublishActor ! ReceiveMessage(res.error, senderDetails, routingKey)
          }
        case Failure(e) =>
          amqpPublishActor ! ReceiveMessage(e.getMessage, senderDetails, routingKey)
      }
    case GetRepositories(login, senderDetails, routingKey) =>
      (requestActor ? GetUserRepositories(login)).onComplete {
        case Success(value) =>
          value match {
            case res: GetRepositoriesResponse =>
              amqpPublishActor ! ReceiveMessage(res.response, senderDetails, routingKey)
            case res: GetRepositoriesFailedResponse =>
              amqpPublishActor ! ReceiveMessage(res.error, senderDetails, routingKey)
          }
        case Failure(e) =>
          amqpPublishActor ! ReceiveMessage(e.getMessage, senderDetails, routingKey)
      }

    case GetUserHttp(login, routingKey) =>
      (requestActor ? GetUserAccountHttp(login)).onComplete {
        case Success(value) =>
          value match {
            case res: GetUserHttpResponse =>
              val response = write(res.user)
              println(response)
              val back = parse(response).extract[GithubUser]
              amqpPublishActor ! ReceiveMessage(response, None, routingKey)
            case res: GetUserFailedResponse =>
              amqpPublishActor ! ReceiveMessage(res.error, None, routingKey)
          }
        case Failure(e) =>
          amqpPublishActor ! ReceiveMessage(e.getMessage, None, routingKey)
      }

    case GetRepositoriesHttp(login, routingKey) =>
      (requestActor ? GetUserRepositoriesHttp(login)).onComplete {
        case Success(value) =>
          value match {
            case res: GetRepositoriesHttpResponse =>
              val response = write(res.list)
              println(response)
              amqpPublishActor ! ReceiveMessage(response, None, routingKey)
            case res: GetRepositoriesFailedResponse =>
              amqpPublishActor ! ReceiveMessage(res.error, None, routingKey)
          }
        case Failure(e) =>
          amqpPublishActor ! ReceiveMessage(e.getMessage, None, routingKey)
      }
  }
}

