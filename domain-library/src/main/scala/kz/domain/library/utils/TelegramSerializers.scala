package kz.domain.library.utils

import kz.domain.library.messages.{HttpSenderDetails, Sender, TelegramSenderDetails}
import org.json4s.JsonAST.{JInt, JObject, JString}
import org.json4s.{CustomSerializer, DefaultFormats}

trait SenderSerializers {

  class SenderTypeSerializers extends CustomSerializer[Sender](
    implicit formats =>
      ( {
        case JObject(
        List(
        ("chatId", JInt(chatId)),
        ("userId", JInt(userId)),
        ("username", JString(username)),
        ("lastname", JString(lastname)),
        ("firstname", JString(firstname))
        )
        ) =>
          TelegramSenderDetails(
            chatId.toLong,
            Some(userId.toInt),
            Some(username),
            Some(lastname),
            Some(firstname))
        case JObject(
        List(
        ("actorPath", JString(actorPath))
        )
        ) =>
          HttpSenderDetails(actorPath)
      }
        , {
        case obj: TelegramSenderDetails =>
          JObject(
            List(
              ("chatId", JInt(obj.chatId)),
              ("userId", JInt(BigInt(obj.userId.getOrElse(-1)))),
              ("username", JString(obj.username.getOrElse(""))),
              ("lastname", JString(obj.lastName.getOrElse(""))),
              ("firstname", JString(obj.firstName.getOrElse("")))
            )
          )
        case obj: HttpSenderDetails =>
          JObject(
            List(
              ("actorPath", JString(obj.actorPath))
            )
          )
      })
  )

  implicit val formats = DefaultFormats ++ List(new SenderTypeSerializers)
}
