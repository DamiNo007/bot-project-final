package kz.coders.chat.gateway.actors

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.Materializer
import kz.coders.chat.gateway.Boot.config
import kz.coders.chat.gateway.actors.ExchangeRequesterActor.{Converted, CurrencyAll, GetAllCurrencies, GetAllCurrenciesHttp, GetConvertResult, GetRatesAll, GetRatesAllHttp}
import kz.domain.library.utils._
import org.json4s.{DefaultFormats, Formats, MappingException}
import org.json4s.jackson.JsonMethods.parse
import kz.coders.chat.gateway.utils.RestClientImpl._
import scala.collection.immutable.ListMap
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ExchangeRequesterActor {

  case class CurrencyAll(symbols: Map[String, String])

  case class Converted(result: Double)

  case class GetAllCurrencies(msg: String)

  case class GetRatesAll(currency: String)

  case class GetConvertResult(from: String, to: String, amount: String)

  case class GetAllCurrenciesHttp(msg: String)

  case class GetRatesAllHttp(currency: String)

}

class ExchangeRequesterActor()(implicit val system: ActorSystem,
                               materializer: Materializer)
  extends Actor with ActorLogging {

  implicit val ex: ExecutionContext = context.dispatcher
  implicit val formats: Formats = DefaultFormats

  val baseUrl = config.getString("exchange.base-url")
  val ratesBaseUrl = config.getString("exchange.rates-base-url")
  val apiHost = config.getString("exchange.api-host")
  val apiKey = config.getString("exchange.api-key")

  def getCurrencies(url: String): Future[CurrencyAll] = {
    get(url, List(RawHeader("x-rapidapi-host", apiHost), RawHeader("x-rapidapi-key", apiKey)))
      .map { body =>
        parse(body).extract[CurrencyAll]
      }
  }

  def getRates(url: String): Future[List[Rates]] = {
    get(url, Nil)
      .map {
        body => parse(body).camelizeKeys.extract[List[Rates]]
      }
  }

  def convert(url: String): Future[Converted] = {
    get(url, List(RawHeader("x-rapidapi-host", apiHost), RawHeader("x-rapidapi-key", apiKey)))
      .map { body =>
        parse(body).camelizeKeys.extract[Converted]
      }
  }

  def mkListOfString(list: List[Rates]): List[String] = {
    list.zipWithIndex.map {
      case (Rates(sell, buy, amount, bankId, title, ratesTime, imageUrl), id) =>
        s"${id + 1}. $title: id = $bankId, sell = $sell, buy = $buy, amount = $amount; last date = $ratesTime, logo-url = $imageUrl"
    }
  }

  override def receive: Receive = {
    case GetAllCurrencies(msg) =>
      log.info(s"got message $msg")
      val sender = context.sender()
      val url = s"${baseUrl}/symbols"
      getCurrencies(url).onComplete {
        case Success(response) =>
          val res = ListMap(response.symbols.toSeq.sortBy(_._1): _*)
          sender ! GetCurrenciesResponse(
            res.map(_.productIterator.mkString(": ")).mkString("\n")
          )
        case Failure(e) =>
          sender ! GetCurrenciesFailedResponse(
            s"""Something went wrong! Try again later...
               |Details:
               |${e.getMessage}
               |""".stripMargin
          )
      }

    case GetRatesAll(currency) =>
      val sender = context.sender()
      val url = s"${ratesBaseUrl}/${currency.toUpperCase}"

      getRates(url).onComplete {
        case Success(response) =>
          val listString = mkListOfString(response)
          val result =
            if (listString.isEmpty)
              "Sorry, empty response! No data about this currency."
            else listString.mkString("\n")
          sender ! GetRatesResponse(result)
        case Failure(e: MappingException) =>
          sender ! GetRatesFailedResponse(
            "Incorrect Command! Example: /rates USD"
          )
        case Failure(e) =>
          sender ! GetRatesFailedResponse(
            "Connection problems! Try again later!"
          )
      }

    case GetConvertResult(from, to, amount) =>
      val sender = context.sender()
      val url =
        s"${baseUrl}/convert?from=${from}&to=${to}&amount=${amount}"
      convert(url).onComplete {
        case Success(response) =>
          sender ! ConvertResponse(s"${response.result} ${to.toUpperCase()}")
        case Failure(e: MappingException) =>
          sender ! ConvertFailedResponse(
            "Some problems occurred! May be you wrote an incorrect currency name!"
          )
        case Failure(e) =>
          sender ! ConvertFailedResponse(
            "Connection problems! Try again later!"
          )
      }
    case GetAllCurrenciesHttp(msg) =>
      val sender = context.sender()
      val url = s"${baseUrl}/symbols"
      getCurrencies(url).onComplete {
        case Success(response) =>
          val res = ListMap(response.symbols.toSeq.sortBy(_._1): _*)
          sender ! GetCurrenciesResponseHttp(
            res
          )
        case Failure(e) =>
          sender ! GetCurrenciesFailedResponse(
            s"""Something went wrong! Try again later...
               |Details:
               |${e.getMessage}
               |""".stripMargin
          )
      }
    case GetRatesAllHttp(currency) =>
      val sender = context.sender()
      val url = s"${ratesBaseUrl}/${currency.toUpperCase}"
      getRates(url).onComplete {
        case Success(response) =>
          sender ! GetRatesHttpResponse(response)
        case Failure(e: MappingException) =>
          sender ! GetRatesFailedResponse(
            "Incorrect Command! Example: /rates USD"
          )
        case Failure(e) =>
          sender ! GetRatesFailedResponse(
            "Connection problems! Try again later!"
          )
      }
  }
}

