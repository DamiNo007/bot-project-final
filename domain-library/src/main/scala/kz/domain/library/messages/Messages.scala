package kz.domain.library.messages

trait Sender

case class UserMessage(senderDetails: Sender,
                       message: String,
                       replyTo: Option[String])

case class GatewayResponse(response: String,
                           senderDetails: Sender)

case class TelegramSenderDetails(chatId: Long,
                                 userId: Option[Int],
                                 username: Option[String],
                                 lastName: Option[String],
                                 firstName: Option[String],
                                ) extends Sender

case class HttpSenderDetails(actorPath: String) extends Sender
