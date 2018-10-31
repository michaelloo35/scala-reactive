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

  trait OrderManagerCommand

  sealed trait OrderManagerData
  case class CartRef(cart: ActorRef, owner: ActorRef) extends OrderManagerData
  case class CheckoutRef(checkout: ActorRef, owner: ActorRef) extends OrderManagerData
  case class PaymentRef(payment: ActorRef, owner: ActorRef) extends OrderManagerData

  trait Event
  case object Done extends Event
}

class OrderManager extends FSM[OrderManagerState, OrderManagerData] {

  startWith(Open, CartRef(context.actorOf(Props[Cart],"cart"), null))

  when(Open) {
    case Event(AddItem(item), CartRef(cart, _)) =>
      cart ! AddItem(item)
      stay using CartRef(cart, sender)
    case Event(ItemAdded(item), CartRef(cart, owner)) =>
      log.info("Item added : {}", item)
      owner ! Done
      stay using CartRef(cart, owner)

    case Event(RemoveItem(item), CartRef(cart, _)) =>
      cart ! RemoveItem(item)
      stay using CartRef(cart, sender)
    case Event(ItemRemoved(item), CartRef(cart, owner)) =>
      log.info("Item removed : {}", item)
      owner ! Done
      stay using CartRef(cart, owner)

    case Event(StartCheckout, CartRef(cart, _)) =>
      log.info("Starting checkout")
      cart ! StartCheckout
      stay using CartRef(cart, sender)
    case Event(CheckoutStarted(checkout), CartRef(_, owner)) =>
      log.info("Started checkout")
      owner ! Done
      goto(InCheckout) using CheckoutRef(checkout, owner)
  }


  when(InCheckout) {
    case Event(SelectDeliveryMethod(deliveryMethod), CheckoutRef(checkout, _)) =>
      checkout ! SelectDeliveryMethod(deliveryMethod)
      stay using CheckoutRef(checkout, sender)
    case Event(SelectedDeliveryMethod(method), CheckoutRef(checkoutRef, owner)) =>
      log.info("Delivery method registered {} " + method)
      owner ! Done
      stay using CheckoutRef(checkoutRef, owner)

    case Event(SelectPaymentMethod(paymentMethod), CheckoutRef(checkout, _)) =>
      checkout ! SelectPaymentMethod(paymentMethod)
      stay using CheckoutRef(checkout, sender)
    case Event(SelectedPaymentMethod(method), CheckoutRef(checkoutRef, owner)) =>
      log.info("Payment method registered {} " + method)
      owner ! Done
      stay using CheckoutRef(checkoutRef, owner)

    case Event(PaymentServiceStarted(payment), CheckoutRef(_, owner)) =>
      log.info("Payment service started")
      owner ! Done
      goto(InPayment) using PaymentRef(payment, owner)
  }

  when(InPayment) {
    case Event(DoPayment, PaymentRef(payment, _)) =>
      payment ! DoPayment
      stay using PaymentRef(payment, sender)

    case Event(PaymentConfirmed, PaymentRef(_, owner)) =>
      log.info("Finished payment")
      owner ! Done
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
