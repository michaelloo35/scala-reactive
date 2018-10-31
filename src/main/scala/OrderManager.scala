import Cart.{InCheckout => _, _}
import Checkout._
import OrderManager._
import Payment.{DoPayment, PaymentConfirmed, PaymentServiceStarted}
import akka.actor.{ActorRef, FSM, Props}

object OrderManager {

  sealed trait OrderManagerState
  case object Open extends OrderManagerState
  case object InCheckout extends OrderManagerState
  case object InPayment extends OrderManagerState
  case object Finished extends OrderManagerState

  sealed trait OrderManagerData
  case class CartRef(cart: ActorRef) extends OrderManagerData
  case class CheckoutRef(checkout: ActorRef) extends OrderManagerData
  case class PaymentRef(payment: ActorRef) extends OrderManagerData

  sealed trait Event
  case object Done extends Event
}

class OrderManager extends FSM[OrderManagerState, OrderManagerData] {

  startWith(Open, CartRef(context.actorOf(Props[Cart])))

  when(Open) {
    case Event(AddItem(item), CartRef(cart)) =>
      cart ! AddItem(item)
      stay using CartRef(cart)
    case Event(ItemAdded(item), CartRef(cart)) =>
      log.info("Item added : {}", item)
      sender ! Done
      stay using CartRef(cart)

    case Event(RemoveItem(item), CartRef(cart)) =>
      cart ! RemoveItem(item)
      stay using CartRef(cart)
    case Event(ItemRemoved(item), CartRef(cart)) =>
      log.info("Item removed : {}", item)
      sender ! Done
      stay using CartRef(cart)

    case Event(StartCheckout, CartRef(cart)) =>
      log.info("Starting checkout")
      cart ! StartCheckout
      stay using CartRef(cart)
    case Event(CheckoutStarted(checkout), CartRef(_)) =>
      log.info("Started checkout")
      context.parent ! Done
      goto(InCheckout) using CheckoutRef(checkout)
  }


  when(InCheckout) {
    case Event(SelectDeliveryMethod(deliveryMethod), CheckoutRef(checkout)) =>
      checkout ! SelectDeliveryMethod(deliveryMethod)
      stay using CheckoutRef(checkout)
    case Event(SelectedDeliveryMethod(method), CheckoutRef(checkoutRef)) =>
      log.info("Delivery method registered {} " + method)
      context.parent ! Done
      stay using CheckoutRef(checkoutRef)

    case Event(SelectPaymentMethod(paymentMethod), CheckoutRef(checkout)) =>
      checkout ! SelectPaymentMethod(paymentMethod)
      stay using CheckoutRef(checkout)
    case Event(SelectedPaymentMethod(method), CheckoutRef(checkoutRef)) =>
      log.info("Payment method registered {} " + method)
      context.parent ! Done
      stay using CheckoutRef(checkoutRef)

    case Event(PaymentServiceStarted(payment), CheckoutRef(_)) =>
      log.info("Payment service started")
      context.parent ! Done
      goto(InPayment) using PaymentRef(payment)
  }

  when(InPayment) {
    case Event(DoPayment, PaymentRef(payment)) =>
      payment ! DoPayment
      stay using PaymentRef(payment)

    case Event(PaymentConfirmed, PaymentRef(_)) =>
      log.info("Finished payment")
      context.parent ! Done
      goto(Finished)
  }

  when(Finished) {
    case Event(CartEmptied, _) =>
      log.info("Order finished Cart Emptied")
      stay
  }

  whenUnhandled {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  initialize()

}
