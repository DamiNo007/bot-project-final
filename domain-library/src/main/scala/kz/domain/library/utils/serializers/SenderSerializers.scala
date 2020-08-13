package kz.domain.library.utils.serializers

import kz.domain.library.messages.{HttpSenderDetails, TelegramSenderDetails}
import org.json4s.jackson.Serialization
import org.json4s.ShortTypeHints

trait SenderSerializers {

  implicit val formats =
    Serialization.formats(ShortTypeHints(List(classOf[TelegramSenderDetails], classOf[HttpSenderDetails])))

}
