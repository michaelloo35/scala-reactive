package payment

import akka.actor.{ActorRef, FSM}
import checkout.CheckoutManager.CheckoutEvent
import order.OrderManager.OrderManagerCommand
import payment.Payment._

object Payment {
  sealed trait PaymentState
  case object Open extends PaymentState

  sealed trait PaymentCommand
  case object DoPayment extends PaymentCommand with OrderManagerCommand

  sealed trait PaymentEvent
  case object PaymentReceived extends PaymentEvent with CheckoutEvent
  case object PaymentConfirmed extends PaymentEvent
  case class PaymentServiceStarted(paymentRef: ActorRef) extends PaymentEvent

  sealed trait PaymentData
}

class Payment extends FSM[PaymentState, PaymentData] {

  startWith(Open, null)

  when(Open) {
    case Event(DoPayment, _) =>
      log.info("Received Payment")
      sender ! PaymentConfirmed
      context.parent ! PaymentReceived
      stop
      stay
  }

  initialize()
}
