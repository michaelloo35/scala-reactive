package checkout

import akka.actor.{ActorRef, Props}
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import checkout.CheckoutManager.{CheckoutState, DeliveryExpired, PaymentExpired, ProcessingPayment, SelectDeliveryMethod, SelectPaymentMethod, DeliveryMethodSelected, PaymentMethodSelected, SelectingDeliveryMethod, SelectingPaymentMethod, _}
import order.OrderManager.{OrderEvent, OrderCommand}
import payment.PaymentManager
import payment.PaymentManager.{PaymentReceived, PaymentServiceStarted}

import scala.concurrent.duration._
import scala.reflect.{ClassTag, classTag}

object CheckoutManager {
  sealed trait CheckoutState extends FSMState
  case object PaymentClosed extends CheckoutState {
    override def identifier: String = "PaymentClosed"
  }
  case object PaymentCancelled extends CheckoutState {
    override def identifier: String = "PaymentCancelled"
  }
  case object SelectingDeliveryMethod extends CheckoutState {
    override def identifier: String = "SelectingDeliveryMethod"
  }
  case object SelectingPaymentMethod extends CheckoutState {
    override def identifier: String = "SelectingPaymentMethod"
  }
  case object ProcessingPayment extends CheckoutState {
    override def identifier: String = "ProcessingPayment"
  }

  sealed trait CheckoutCommand
  case object StartCheckout extends CheckoutCommand with OrderCommand with OrderEvent
  case object ClosePayment extends CheckoutCommand with OrderCommand
  case object CancelPayment extends CheckoutCommand with OrderCommand
  case class SelectDeliveryMethod(deliveryMethod: String) extends CheckoutCommand with OrderCommand with OrderEvent
  case class SelectPaymentMethod(paymentMethod: String) extends CheckoutCommand with OrderCommand with OrderEvent

  sealed trait CheckoutEvent
  case class DeliveryMethodSelected(deliveryMethod: String) extends CheckoutEvent with OrderEvent
  case class PaymentMethodSelected(paymentMethod: String) extends CheckoutEvent with OrderEvent
  case class CheckoutStarted(checkoutManager: ActorRef) extends CheckoutEvent with OrderEvent
  case object CheckoutCancelled extends CheckoutEvent with OrderEvent
  case object CheckoutClosed extends CheckoutEvent with OrderEvent

  sealed trait Timer
  case object DeliveryTimer extends Timer
  case object PaymentTimer extends Timer

  sealed trait TimerExpired
  case object DeliveryExpired extends TimerExpired
  case object PaymentExpired extends TimerExpired
}
class CheckoutManager extends PersistentFSM[CheckoutState, Checkout, CheckoutEvent] {
  override def persistenceId = "checkout-manager-fsm-id-1"
  override def domainEventClassTag: ClassTag[CheckoutEvent] = classTag[CheckoutEvent]
  startWith(SelectingDeliveryMethod, null)

  when(SelectingDeliveryMethod) {
    case Event(SelectDeliveryMethod(deliveryMethod), _) =>
      log.info("Selected delivery method: {}", deliveryMethod)

      goto(SelectingPaymentMethod) applying DeliveryMethodSelected(deliveryMethod) replying DeliveryMethodSelected(deliveryMethod)
  }

  when(SelectingPaymentMethod) {
    case Event(SelectPaymentMethod(paymentMethod), _) =>
      log.info("Selected payment method: {}", paymentMethod)

      val payment = context.actorOf(Props[PaymentManager], "payment")
      goto(ProcessingPayment) applying PaymentMethodSelected(paymentMethod) replying PaymentServiceStarted(payment)
  }

  when(ProcessingPayment) {
    case Event(PaymentReceived, checkout) =>
      log.info("Payment received. Delivery {}, payment {}", checkout.getDeliveryMethod, checkout.getPaymentMethod)
      cancelTimer(PaymentTimer.toString)
      log.info("Checkout Finished")
      checkout.getOrderManager ! CheckoutClosed
      context.parent ! CheckoutClosed // reply to cart
      stop
  }

  onTransition {
    case _ -> SelectingDeliveryMethod =>
      setTimer(DeliveryTimer.toString, DeliveryExpired, 3 seconds)
    case _ -> SelectingPaymentMethod =>
      cancelTimer(DeliveryTimer.toString)
      setTimer(PaymentTimer.toString, PaymentExpired, 3 seconds)
  }

  whenUnhandled {
    case Event(DeliveryExpired | PaymentExpired, _) =>
      log.info("Timeout checkout cancelled")
      context.parent ! CheckoutCancelled
      stop
    case Event(e, s) =>
      log.info("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  override def applyEvent(checkoutEvent: CheckoutEvent, checkoutBeforeEvent: Checkout): Checkout = {
    checkoutEvent match {
      case DeliveryMethodSelected(deliveryMethod) => CheckoutWithDelivery(deliveryMethod, sender)
      case PaymentMethodSelected(paymentMethod) => CheckoutWithDeliveryAndPayment(checkoutBeforeEvent.getDeliveryMethod, paymentMethod, sender)
      case CheckoutCancelled ⇒ checkoutBeforeEvent
      case CheckoutClosed ⇒ checkoutBeforeEvent
      case CheckoutStarted(_) ⇒ checkoutBeforeEvent

    }
  }
}
