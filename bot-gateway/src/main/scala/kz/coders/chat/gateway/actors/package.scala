package kz.coders.chat.gateway

import kz.coders.chat.gateway.actors.profitkz.ArticlesRequesterActor.ArticleItem
import kz.coders.chat.gateway.actors.profitkz.NewsRequesterActor.NewsItem

package object actors {

  trait Response

  trait Request

  case class GetUser(login: String) extends Request

  case class GetRepositories(login: String) extends Request

  case class GetUserResponse(response: String) extends Response

  case class GetUserFailedResponse(error: String) extends Response

  case class GetRepositoriesResponse(response: String) extends Response

  case class GetRepositoriesFailedResponse(error: String) extends Response

  case class GetNews(msg: String) extends Request

  case class GetNewsHttp(msg: String) extends Request

  case class GetNewsResponse(response: String) extends Response

  case class GetNewsHttpResponse(news: List[NewsItem]) extends Response

  case class GetNewsFailedResponse(error: String) extends Response

  case class GetArticles(msg: String) extends Request

  case class GetArticlesHttp(msg: String) extends Request

  case class GetArticlesResponse(response: String) extends Response

  case class GetArticlesHttpResponse(articles: List[ArticleItem]) extends Response

  case class GetArticlesFailedResponse(error: String) extends Response

}
