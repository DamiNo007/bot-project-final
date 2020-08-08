package coders.http

import com.bot4s.telegram.models.Message

package object actors {

  trait Request

  trait Response

  case class SendGetUserHttpRequest(login: String) extends Request

  case class SendGetRepositoriesHttpRequest(login: String) extends Request

  case class SendGetCurrenciesHttpRequest(msgDetails: Message) extends Request

  case class SendGetRatesHttpRequest(currency: String) extends Request

  case class SendGetConvertHttpRequest(from: String,
                                       to: String,
                                       amount: String,
                                       msgDetails: Message) extends Request

  case class SendGetNewsHttpRequest(msg: String) extends Request

  case class SendGetArticlesHttpRequest(msg: String) extends Request

}
