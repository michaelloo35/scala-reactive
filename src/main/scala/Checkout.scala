import Checkout._
import OrderManager.{Event, OrderManagerCommand}
import Payment.{PaymentReceived, PaymentServiceStarted}
import akka.actor.{ActorRef, FSM, Props}

import scala.concurrent.duration._

object Checkout {
  sealed trait CheckoutState
  case object PaymentClosed extends CheckoutState
  case object PaymentCancelled extends CheckoutState
  case object SelectingDeliveryMethod extends CheckoutState
  case object SelectingPaymentMethod extends CheckoutState
  case object ProcessingPayment extends CheckoutState

  sealed trait CheckoutCommand
  case object StartCheckout extends CheckoutCommand with OrderManagerCommand
  case object ClosePayment extends CheckoutCommand with OrderManagerCommand
  case object CancelPayment extends CheckoutCommand with OrderManagerCommand
  case class SelectDeliveryMethod(deliveryMethod: String) extends CheckoutCommand with OrderManagerCommand
  case class SelectPaymentMethod(paymentMethod: String) extends CheckoutCommand with OrderManagerCommand

  sealed trait CheckoutEvent
  case class SelectedDeliveryMethod(deliveryMethod: String) extends CheckoutEvent
  case class SelectedPaymentMethod(paymentMethod: String) extends CheckoutEvent
  case class CheckoutStarted(checkoutRef: ActorRef) extends CheckoutEvent
  case object CheckoutCancelled extends CheckoutEvent with Event
  case object CheckoutClosed extends CheckoutEvent with Event

  sealed trait Timer
  case object DeliveryExpired extends Timer
  case object PaymentExpired extends Timer

  case class CheckoutData(deliveryMethod: String, paymentMethod: String, orderManager: ActorRef)
}
class Checkout extends FSM[CheckoutState, CheckoutData] {
  val paymentTimer: String = "payment" + self.toString
  val deliveryTimer: String = "delivery" + self.toString


  startWith(SelectingDeliveryMethod, CheckoutData(null, null, null))

  when(SelectingDeliveryMethod) {
    case Event(SelectDeliveryMethod(deliveryMethod), CheckoutData(_, _, _)) =>
      log.info("Selected delivery method: {}", deliveryMethod)
      sender ! SelectedDeliveryMethod(deliveryMethod)
      goto(SelectingPaymentMethod) using CheckoutData(deliveryMethod, null, sender)
  }

  when(SelectingPaymentMethod) {
    case Event(SelectPaymentMethod(paymentMethod), CheckoutData(deliveryMethod, _, _)) =>
      log.info("Selected payment method: {}", paymentMethod)
      sender ! SelectedPaymentMethod(paymentMethod)

      val payment = context.actorOf(Props[Payment])
      sender ! PaymentServiceStarted(payment)
      goto(ProcessingPayment) using CheckoutData(deliveryMethod, paymentMethod, sender)
  }

  when(ProcessingPayment) {
    case Event(PaymentReceived, CheckoutData(deliveryMethod, paymentMethod, orderManager)) =>
      log.info("Payment received. Delivery {}, payment {}", deliveryMethod, paymentMethod)
      cancelTimer(deliveryTimer)
      cancelTimer(paymentTimer)
      log.info("Checkout closed")
      orderManager ! CheckoutClosed
      context.parent ! CheckoutClosed
      stay
  }

  onTransition {
    case _ -> SelectingDeliveryMethod =>
      setTimer(deliveryTimer, DeliveryExpired, 3 seconds)
    case _ -> SelectingPaymentMethod =>
      cancelTimer(deliveryTimer)
      setTimer(paymentTimer, PaymentExpired, 3 seconds)
  }

  whenUnhandled {
    case Event(PaymentExpired, _) | Event(DeliveryExpired, _) =>
      log.info("Checkout canceled")
      context.parent ! CheckoutCancelled
      stop
      stay
  }
  initialize()

}
