package coders.http.routes

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import akka.util.Timeout
import scala.concurrent.{ExecutionContext, Future, Promise}
import akka.http.scaladsl.server.Directives._
import coders.http.actors.PerRequest.PerRequestActor
import coders.http.actors._
import com.rabbitmq.client.Channel
import com.typesafe.config.Config
import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.jackson.Serialization
import org.json4s.{DefaultFormats, Formats, Serialization}
import scala.concurrent.duration.DurationInt

class Routes(channel: Channel, config: Config)(implicit ex: ExecutionContext, system: ActorSystem) extends Json4sSupport {
  implicit val formats: Formats = DefaultFormats
  implicit val serialization: Serialization = Serialization
  implicit val timeout: Timeout = 5.seconds

  val handlers: Route = pathPrefix("api") {
    pathPrefix("bot") {
      pathPrefix("get") {
        post {
          entity(as[SendRequest]) { body =>
            ctx =>
              completeRequest(
                body,
                ctx,
                AmqpPublisherActor.props(
                  channel,
                  config.getString("rabbitMq.exchange.requestExchangeName"),
                  config.getString("rabbitMq.routingKey.httpRequestRoutingKey"),
                  config.getString("rabbitMq.routingKey.httpResponseRoutingKey")
                )
              )
          }
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
