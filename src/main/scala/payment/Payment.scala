package payment

import java.util.UUID

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorRef, OneForOneStrategy, Props, SupervisorStrategy}
import akka.event.LoggingReceive
import akka.http.scaladsl.model.{IllegalResponseException, IllegalUriException}
import checkout.CheckoutManager.CheckoutEvent

import scala.concurrent.duration._

object Payment {

  sealed trait PaymentCommand
  case class DoPayment(paymentMethod: String) extends PaymentCommand

  sealed trait PaymentEvent
  case class PaymentServiceStarted(actorRef: ActorRef) extends PaymentEvent
  case object PaymentSuccessful extends PaymentEvent with CheckoutEvent
  case object PaymentUnsuccessful extends PaymentEvent

}

class Payment(val paymentMethod: String) extends Actor {

  import Payment._

  var orderManager: ActorRef = _

  override def supervisorStrategy: SupervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 5, withinTimeRange = 10 seconds) {
      case IllegalResponseException(errorInfo) => print(errorInfo); Restart

      case IllegalUriException(errorInfo) => println(s" Oops, $errorInfo"); Stop

      case spray.json.DeserializationException(msg, cause, _) => println(msg); println(cause); Restart
      case unknown => println(s"unknown exception. Oops. $unknown"); Stop
    }


  override def receive: Receive = LoggingReceive {
    case DoPayment(method) =>
      orderManager = sender()
      val paymentClient = context.actorOf(Props(new PaymentClient()), UUID.randomUUID().toString)
      paymentClient ! DoPayment(method)

    case PaymentStatus(status, balance) =>
      println(s"payment status = $status and you still have $balance credits on $paymentMethod")
      orderManager ! PaymentSuccessful
      context.parent ! PaymentSuccessful

  }


}
