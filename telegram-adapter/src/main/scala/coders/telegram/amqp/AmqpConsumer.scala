package coders.telegram.amqp

import akka.actor.ActorRef
import akka.util.Timeout
import coders.telegram.actors.AmqpConsumerActor.ReceiveMessage
import com.rabbitmq.client.{AMQP, Consumer, Envelope, ShutdownSignalException}
import org.json4s.jackson.JsonMethods.parse
import scala.concurrent.duration.DurationInt
import kz.domain.library.messages.GatewayResponse
import kz.domain.library.utils.SenderSerializers

object AmqpConsumer {
  def apply(consumerActor: ActorRef): AmqpConsumer = new AmqpConsumer(consumerActor)
}

class AmqpConsumer(consumerActor: ActorRef) extends Consumer with SenderSerializers {

  implicit val timeout: Timeout = 20.seconds

  override def handleConsumeOk(consumerTag: String): Unit = ()

  override def handleCancelOk(consumerTag: String): Unit = ()

  override def handleCancel(consumerTag: String): Unit = ()

  override def handleShutdownSignal(consumerTag: String,
                                    sig: ShutdownSignalException): Unit = ()

  override def handleRecoverOk(consumerTag: String): Unit = ()

  override def handleDelivery(consumerTag: String,
                              envelope: Envelope,
                              properties: AMQP.BasicProperties,
                              body: Array[Byte]): Unit = {
    val response = parse(new String(body)).extract[GatewayResponse]
    consumerActor ! ReceiveMessage(response)
  }
}