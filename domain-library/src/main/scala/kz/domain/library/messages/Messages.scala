package kz.domain.library.messages

import com.bot4s.telegram.models.{Chat, User}

case class UserMessage(senderPlatform: String,
                       senderDetails: Option[SenderDetails],
                       messages: Option[String],
                       command: String,
                       args: Option[List[String]])

//replyToUser(msg.response)
// {
//    "response": msg.response
// }

// 100 долларов в тенге будет 421.89 ТГ
case class GatewayResponse(response: String,
                           senderDetails: Option[SenderDetails])

case class SenderDetails(messageId: Long,
                         from: Option[User],
                         date: Int,
                         chat: Chat)
