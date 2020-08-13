package kz.coders.chat.gateway.amqp

import akka.actor.ActorRef
import akka.util.Timeout
import com.rabbitmq.client.{AMQP, Consumer, Envelope, ShutdownSignalException}
import scala.concurrent.duration.DurationInt

object AmqpConsumer {
  def apply(amqpListenerActor: ActorRef): AmqpConsumer = new AmqpConsumer(amqpListenerActor)
}

class AmqpConsumer(amqpListenerActor: ActorRef) extends Consumer {

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
    amqpListenerActor ! new String(body)
  }
}