package coders.http.routes

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import akka.util.Timeout
import scala.concurrent.{ExecutionContext, Future, Promise}
import akka.http.scaladsl.server.Directives._
import coders.http.Boot.channel
import coders.http.actors.PerRequest.PerRequestActor
import coders.http.actors.{AmqpPublisherActor, Request, SendGetRepositoriesHttpRequest, SendGetUserHttpRequest}
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, Formats, Serialization}

import scala.concurrent.duration.DurationInt

class Routes()(implicit ex: ExecutionContext, system: ActorSystem) extends Json4sSupport {
  implicit val formats: Formats = DefaultFormats
  implicit val serialization: Serialization = Serialization
  implicit val timeout: Timeout = 5.seconds

  val routingKey = "rabbitMq.routingKey.telegramResponseRoutingKey"

  val handlers: Route = pathPrefix("api") {
    pathPrefix("github") {
      pathPrefix(Segment) { username =>
        path("repos") { ctx =>
          val body = SendGetRepositoriesHttpRequest(username)
          completeRequest(body, ctx, AmqpPublisherActor.props(channel, routingKey))
        } ~ get { ctx =>
          val body = SendGetUserHttpRequest(username)
          completeRequest(body, ctx, AmqpPublisherActor.props(channel, routingKey))
        }
      }
    }
  }

  def completeRequest(body: Request,
                      ctx: RequestContext,
                      props: Props): Future[RouteResult] = {
    val promise = Promise[RouteResult]
    system.actorOf(Props(new PerRequestActor(body, props, promise, ctx)))
    promise.future
  }
}
