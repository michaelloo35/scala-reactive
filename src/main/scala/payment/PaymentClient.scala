package payment

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ErrorInfo, HttpRequest, HttpResponse, IllegalResponseException, _}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.ByteString
import payment.Payment.DoPayment
import spray.json.{DefaultJsonProtocol, _}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._


class PaymentClient extends Actor

  with ActorLogging
  with SprayJsonSupport
  with DefaultJsonProtocol
  with JsonSupport {

  import akka.pattern.pipe
  import context.dispatcher
  final implicit val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(context.system))

  val http = Http(context.system)


  private val serverAddress = "http://localhost:8080"
  def receive: PartialFunction[Any, Unit] = {


    // handle order manager request
    case DoPayment(method: String) if method == "PayPal" =>
      http.singleRequest(HttpRequest(uri = serverAddress + "/paypal"))
        .pipeTo(self)

    case DoPayment(method: String) if method == "Card" =>
      http.singleRequest(HttpRequest(uri = serverAddress + "/card"))
        .pipeTo(self)

    // HANDLE SERVER RESPONSES

    case HttpResponse(code, _, _, _) if code != StatusCodes.OK =>
      throw new IllegalResponseException(ErrorInfo(s"Status code: $code"))

    case resp@HttpResponse(StatusCodes.OK, _, entity, _) =>
      entity.dataBytes.runFold(ByteString(""))(_ ++ _).foreach { body =>
        val status = body.utf8String.parseJson.convertTo[PaymentStatus]
        println("response, body: " + status)
        context.parent ! status
        resp.discardEntityBytes()
        close()
      }
    case resp@HttpResponse(code, _, _, _) =>
      println("Request failed, response code: " + code)
      resp.discardEntityBytes()
      close()

    case unknown => println(s"unknown message. Oops. $unknown"); Stop

  }

  def close(): Future[Terminated] = {
    Await.result(http.shutdownAllConnectionPools(), Duration.Inf)
    context.system.terminate()
  }


}

