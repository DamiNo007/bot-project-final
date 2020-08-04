package kz.domain.library

import cats.implicits.catsSyntaxTuple3Semigroupal
import com.bot4s.telegram.models.Message
import com.lucidchart.open.xtract.XmlReader.seq
import com.lucidchart.open.xtract.{XmlReader, __}
import kz.domain.library.messages.SenderDetails

package object utils {

  trait Response

  trait Request

  case class GithubUser(login: String,
                        name: String,
                        avatarUrl: Option[String],
                        publicRepos: Option[String])

  case class GithubRepository(name: String,
                              size: Int,
                              fork: Boolean,
                              pushedAt: String,
                              stargazersCount: Int)

  case class SendGetUserRequest(login: String, msgDetails: Message)
      extends Request

  case class SendGetRepositoriesRequest(login: String, msgDetails: Message)
      extends Request

  case class SendGetCurrenciesRequest(msgDetails: Message) extends Request

  case class SendGetRatesRequest(currency: String, msgDetails: Message)
      extends Request

  case class SendGetConvertRequest(from: String,
                                   to: String,
                                   amount: String,
                                   msgDetails: Message)
      extends Request

  case class SendGetNewsRequest(msgDetails: Message) extends Request

  case class SendGetArticlesRequest(msgDetails: Message) extends Request

  case class GetUser(login: String,
                     senderDetails: Option[SenderDetails],
                     routingKey: String)
      extends Request

  case class GetRepositories(login: String,
                             senderDetails: Option[SenderDetails],
                             routingKey: String)
      extends Request

  case class GetUserHttp(login: String, routingKey: String) extends Request

  case class GetRepositoriesHttp(login: String, routingKey: String)
      extends Request

  case class GetUserResponse(response: String) extends Response

  case class GetUserFailedResponse(error: String) extends Response

  case class GetRepositoriesResponse(response: String) extends Response

  case class GetRepositoriesFailedResponse(error: String) extends Response

  case class GetUserHttpResponse(user: GithubUser) extends Response

  case class GetRepositoriesHttpResponse(list: List[GithubRepository])
      extends Response

  case class Rates(sell: String,
                   buy: String,
                   amount: String,
                   bankId: String,
                   bankTitle: String,
                   lastReceivedRatesTime: String,
                   bankLogoUrl: String)

  case class GetCurrencies(senderDetails: Option[SenderDetails],
                           routingKey: String)
      extends Request

  case class GetCurrenciesHttp(msg: String,
                               senderDetails: Option[SenderDetails],
                               routingKey: String)
      extends Request

  case class GetRates(currency: String,
                      senderDetails: Option[SenderDetails],
                      routingKey: String)
      extends Request

  case class GetRatesHttp(currency: String,
                          senderDetails: Option[SenderDetails],
                          routingKey: String)
      extends Request

  case class Convert(args: List[String],
                     senderDetails: Option[SenderDetails],
                     routingKey: String)
      extends Request

  case class GetCurrenciesResponse(response: String) extends Response

  case class GetCurrenciesResponseHttp(symbols: Map[String, String])
      extends Response

  case class GetRatesResponse(response: String) extends Response

  case class GetRatesHttpResponse(rates: List[Rates]) extends Response

  case class GetRatesFailedResponse(error: String) extends Response

  case class GetCurrenciesFailedResponse(error: String) extends Response

  case class ConvertResponse(response: String) extends Response

  case class ConvertFailedResponse(error: String) extends Response

  case class News(items: Seq[NewsItem])

  case class NewsItem(title: String, description: String, link: String)

  object News {
    implicit val reader: XmlReader[News] = (
      (__ \ "channel" \ "item").read(seq[NewsItem])
    ).map(apply _)
  }

  object NewsItem {
    implicit val reader: XmlReader[NewsItem] = (
      (__ \ "title").read[String],
      (__ \ "description").read[String],
      (__ \ "link").read[String]
    ).mapN(apply _)
  }

  case class Articles(items: Seq[ArticleItem])

  case class ArticleItem(title: String, description: String, link: String)

  object ArticleItem {
    implicit val reader: XmlReader[ArticleItem] = (
      (__ \ "title").read[String],
      (__ \ "description").read[String],
      (__ \ "link").read[String]
    ).mapN(apply _)
  }

  object Articles {
    implicit val reader: XmlReader[Articles] = (
      (__ \ "channel" \ "item").read(seq[ArticleItem])
    ).map(apply _)
  }

  case class GetNews(senderDetails: Option[SenderDetails], routingKey: String)
      extends Request

  case class GetNewsHttp(senderDetails: Option[SenderDetails],
                         routingKey: String)
      extends Request

  case class GetNewsResponse(response: String) extends Response

  case class GetNewsHttpResponse(news: List[NewsItem]) extends Response

  case class GetNewsFailedResponse(error: String) extends Response

  case class GetArticles(senderDetails: Option[SenderDetails],
                         routingKey: String)
      extends Request

  case class GetArticlesHttp(senderDetails: Option[SenderDetails],
                             routingKey: String)
      extends Request

  case class GetArticlesResponse(response: String) extends Response

  case class GetArticlesHttpResponse(news: List[ArticleItem]) extends Response

  case class GetArticlesFailedResponse(error: String) extends Response

}
