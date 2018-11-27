package cart

import java.net.URI

import akka.actor.Props
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import cart.Cart.Item
import cart.CartManager.{AddItem, CartState, CartTimerExpired, InCheckout, Initialized, ItemAdded, ItemRemoved, RemoveItem, _}
import checkout.CheckoutManager
import checkout.CheckoutManager.{CheckoutCancelled, CheckoutClosed, CheckoutStarted, StartCheckout}

import scala.concurrent.duration._
import scala.reflect.{ClassTag, classTag}

object CartManager {
  sealed trait CartState extends FSMState

  case object Initialized extends CartState {
    override def identifier: String = "Initialized"
  }
  case object InCheckout extends CartState {
    override def identifier: String = "InCheckout"
  }

  sealed trait CartCommand
  case class AddItem(item: Item) extends CartCommand
  case class RemoveItem(id: URI, count: Int) extends CartCommand

  sealed trait CartEvent
  case class ItemAdded(item: Item) extends CartEvent
  case class ItemRemoved(id: URI, count: Int) extends CartEvent
  case object CartEmptied extends CartEvent

  sealed trait Timer
  case object CartTimerExpired extends Timer
}


class CartManager extends PersistentFSM[CartState, Cart, CartEvent] {
  override def persistenceId = "cart-manager-fsm-id-1"
  override def domainEventClassTag: ClassTag[CartEvent] = classTag[CartEvent]
  startWith(Initialized, EmptyCart())

  when(Initialized) {
    case Event(AddItem(item), _) =>
      log.info("AddItem: " + item)
      stay applying ItemAdded(item) replying ItemAdded(item)

    case Event(RemoveItem(id, count), _) =>
      log.info("RemoveItem")
      stay applying ItemRemoved(id, count) replying ItemRemoved(id, count)

    case Event(StartCheckout, _) =>
      log.info("StartCheckout")
      val checkout = context.actorOf(Props[CheckoutManager], "checkout")
      goto(InCheckout) replying CheckoutStarted(checkout)
  }

  when(InCheckout) {
    case Event(CheckoutCancelled, _) =>
      log.info("Checkout cancelled CartEmptied")
      goto(Initialized) applying CartEmptied

    case Event(CheckoutClosed, _) =>
      log.info("Checkout closed CartEmptied")
      goto(Initialized) applying CartEmptied
  }

  onTransition {
    case _ -> Initialized => setTimer("cart-timer", CartTimerExpired, 30 seconds)
    case _ -> InCheckout => cancelTimer(self.toString)
  }

  whenUnhandled {
    case Event(CartTimerExpired, _) =>
      log.info("Timeout cart emptied")
      goto(Initialized) applying CartEmptied
    case Event(e, s) =>
      log.info("Received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  override def applyEvent(cartEvent: CartEvent, cartBeforeEvent: Cart): Cart = {
    cartEvent match {
      case ItemAdded(item) ⇒ cartBeforeEvent.addItem(item)
      case ItemRemoved(id, count) ⇒ cartBeforeEvent.removeItem(id, count)
      case CartEmptied ⇒ EmptyCart()
    }
  }

}


