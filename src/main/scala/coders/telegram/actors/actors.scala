package coders.telegram

import com.bot4s.telegram.models.Message

package object actors {

  trait Response

  trait Request

  case class GetUser(login: String, msgDetails: Message) extends Request

  case class GetRepositories(login: String, msgDetails: Message) extends Request

  case class GetUserResponse(response: String) extends Response

  case class GetUserFailedResponse(error: String) extends Response

  case class GetRepositoriesResponse(response: String) extends Response

  case class GetRepositoriesFailedResponse(error: String) extends Response

}
