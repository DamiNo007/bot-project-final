package kz.coders.chat.gateway

package object actors {

  trait Response

  trait Request

  case class GetUser(login: String) extends Request

  case class GetRepositories(login: String) extends Request

  case class GetUserResponse(response: String) extends Response

  case class GetUserFailedResponse(error: String) extends Response

  case class GetRepositoriesResponse(response: String) extends Response

  case class GetRepositoriesFailedResponse(error: String) extends Response

}
