package kz.domain.library.utils

import com.bot4s.telegram.models.ChatType
import com.bot4s.telegram.models.ChatType.ChatType
import org.json4s.JsonAST.JString
import org.json4s.{CustomSerializer, DefaultFormats}
import scala.util.{Failure, Success, Try}

trait TelegramSerializers {

  class ChatTypeSerializers extends CustomSerializer[ChatType](
    implicit formats =>
      ( {
        case JString(str) =>
          Try(ChatType.withName(str)) match {
            case Success(value) => value
            case Failure(e) => throw new Exception("")
          }
      }, {
        case s: ChatType =>
          JString(s.toString)
      })
  )

  implicit val formats = DefaultFormats ++ List(new ChatTypeSerializers)

}
