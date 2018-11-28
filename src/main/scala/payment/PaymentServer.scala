package payment

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.{Directives, StandardRoute}
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

final case class PaymentStatus(status: Boolean, currentBalance: Long)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val paymentStatus: RootJsonFormat[PaymentStatus] = jsonFormat2(PaymentStatus)
}

object PaymentServer extends Directives with JsonSupport {

  val paypalBalance = new AtomicInteger(10)
  val cardBalance = new AtomicInteger(10)

  def main(args: Array[String]) {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem = ActorSystem("PaymentSystem", config.getConfig("serverapp").withFallback(config))

    implicit val materializer: ActorMaterializer = ActorMaterializer()

    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    def decrementAndComplete(balance: AtomicInteger): StandardRoute = {
      balance.decrementAndGet()
      println("processing Payment")
      Thread.sleep(5000)
      complete(PaymentStatus(status = true, balance.intValue()))
    }

    val routePayPal = path("paypal") {
      get {
        decrementAndComplete(paypalBalance)
      }
    }

    val routeCard = path("card") {
      get {
        decrementAndComplete(paypalBalance)
      }
    }


    val routes = routePayPal ~ routeCard
    val localhost = "localhost"
    val portBinding = Http().bindAndHandle(routes, localhost, 8080)

    println(s"Server online at http://$localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return


    // gracefull shutdown
    portBinding
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())

  }

}


