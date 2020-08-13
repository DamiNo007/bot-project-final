package coders.http

package object actors {

  trait Request

  case class SendRequest(msg: String) extends Request

}
