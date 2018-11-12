package order

import akka.actor.Props
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import cart.CartManager
import cart.CartManager._
import checkout.CheckoutManager.{DeliveryMethodSelected, _}
import order.OrderManager.{Finished, InCheckout, InPayment, Open, OrderState, _}
import payment.PaymentManager.{DoPayment, PaymentConfirmed, PaymentReceived, PaymentServiceStarted}

import scala.reflect.{ClassTag, classTag}

object OrderManager {

  sealed trait OrderState extends FSMState
  case object Open extends OrderState {
    override def identifier: String = "Open"
  }
  case object InCheckout extends OrderState {
    override def identifier: String = "InCheckout"
  }
  case object InPayment extends OrderState {
    override def identifier: String = "InPayment"
  }
  case object Finished extends OrderState {
    override def identifier: String = "Finished"
  }

  trait OrderCommand

  trait OrderEvent
  case object Done extends OrderEvent
}

class OrderManager extends PersistentFSM[OrderState, Order, OrderEvent] {
  override def persistenceId: String = "order-manager-fsm-id-1"
  override def domainEventClassTag: ClassTag[OrderEvent] = classTag[OrderEvent]
  startWith(Open, CartManagerRef(context.actorOf(Props[CartManager], "cart"), null))

  when(Open) {
    case Event(AddItem(item), _) =>
      log.info("AddItem: " + item)
      stay applying AddItem(item)

    case Event(ItemAdded(item), _) =>
      log.info("ItemAdded: " + item)
      stay applying ItemAdded(item)

    case Event(RemoveItem(id, count), _) =>
      stay applying RemoveItem(id, count)

    case Event(ItemRemoved(id, count), _) =>
      stay applying ItemRemoved(id, count)

    case Event(StartCheckout, _) =>
      log.info("StartCheckout")
      stay applying StartCheckout

    case Event(CheckoutStarted(checkout), _) =>
      log.info("CheckoutStarted")
      goto(InCheckout) applying CheckoutStarted(checkout)
  }


  when(InCheckout) {
    case Event(SelectDeliveryMethod(deliveryMethod), _) =>
      log.info("SelectDeliveryMethod: " + deliveryMethod)
      stay applying SelectDeliveryMethod(deliveryMethod)

    case Event(DeliveryMethodSelected(deliveryMethod), _) =>
      log.info("DeliveryMethodSelected: " + deliveryMethod)
      stay applying DeliveryMethodSelected(deliveryMethod)

    case Event(SelectPaymentMethod(paymentMethod), _) =>
      log.info("SelectPaymentMethod: " + paymentMethod)
      stay applying SelectPaymentMethod(paymentMethod)

    case Event(PaymentMethodSelected(paymentMethod), _) =>
      log.info("PaymentMethodSelected " + paymentMethod)
      stay applying PaymentMethodSelected(paymentMethod)

    case Event(PaymentServiceStarted(payment), _) =>
      log.info("PaymentServiceStarted")
      goto(InPayment) applying PaymentServiceStarted(payment)
  }

  when(InPayment) {
    case Event(DoPayment, _) =>
      log.info("DoPayment")
      stay applying DoPayment

    case Event(PaymentConfirmed, _) =>
      log.info("PaymentConfirmed")
      goto(Finished)
  }

  when(Finished) {
    case Event(CartEmptied, _) =>
      log.info("Order finished")
      stop
  }

  whenUnhandled {
    case Event(e, s) =>
      log.info("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  override def applyEvent(orderEvent: OrderEvent, orderBeforeEvent: Order): Order = {
    orderEvent match {
      case ItemAdded(_) | ItemRemoved(_, _) | DeliveryMethodSelected(_) | PaymentMethodSelected(_) | PaymentReceived ⇒
        orderBeforeEvent.getOwnerRef ! Done
        orderBeforeEvent

      case AddItem(item) ⇒
        orderBeforeEvent.getCartManagerRef ! AddItem(item)
        CartManagerRef(orderBeforeEvent.getCartManagerRef, sender) // update sender

      case RemoveItem(id, count) ⇒
        orderBeforeEvent.getCartManagerRef ! RemoveItem(id, count)
        CartManagerRef(orderBeforeEvent.getCartManagerRef, sender) // update sender

      case StartCheckout ⇒
        orderBeforeEvent.getCartManagerRef ! StartCheckout
        CheckoutManagerRef(null, sender) // update sender

      case CheckoutStarted(checkoutManager) ⇒
        orderBeforeEvent.getOwnerRef ! Done
        CheckoutManagerRef(checkoutManager, orderBeforeEvent.getOwnerRef)

      case SelectDeliveryMethod(deliveryMethod) ⇒
        orderBeforeEvent.getCheckoutManagerRef ! SelectDeliveryMethod(deliveryMethod)
        CheckoutManagerRef(orderBeforeEvent.getCheckoutManagerRef, sender) // update sender

      case SelectPaymentMethod(paymentMethod) ⇒
        orderBeforeEvent.getCheckoutManagerRef ! SelectPaymentMethod(paymentMethod)
        CheckoutManagerRef(orderBeforeEvent.getCheckoutManagerRef, sender) // update sender

      case PaymentServiceStarted(payment) ⇒
        orderBeforeEvent.getOwnerRef ! Done
        PaymentManagerRef(payment, orderBeforeEvent.getOwnerRef)

      case DoPayment ⇒
        orderBeforeEvent.getPaymentManagerRef ! DoPayment
        orderBeforeEvent
    }
  }

}
