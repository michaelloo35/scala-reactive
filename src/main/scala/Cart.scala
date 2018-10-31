import Cart.{NonEmpty, _}
import Checkout.{CheckoutCancelled, CheckoutClosed, CheckoutStarted, StartCheckout}
import OrderManager.OrderManagerCommand
import akka.actor.{FSM, Props}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

object Cart {
  sealed trait CartState
  case object Empty extends CartState
  case object NonEmpty extends CartState
  case object InCheckout extends CartState

  sealed trait CartCommand
  case class AddItem(item: String) extends CartCommand with OrderManagerCommand
  case class RemoveItem(item: String) extends CartCommand with OrderManagerCommand

  sealed trait CartEvent
  case class ItemAdded(item: String) extends CartEvent
  case class ItemRemoved(item: String) extends CartEvent
  case object CartEmptied extends CartEvent

  sealed trait Timer
  case object CartTimerExpired extends Timer

  case class CartContent(content: ListBuffer[String])
}


class Cart extends FSM[CartState, CartContent] {

  startWith(Empty, CartContent(ListBuffer()))

  when(Empty) {
    case Event(AddItem(item), CartContent(items)) =>
      log.info("Added new item to empty cart: " + item)
      sender ! ItemAdded(item)
      goto(NonEmpty) using CartContent(items :+ item)
  }

  when(NonEmpty) {

    case Event(AddItem(item), CartContent(items)) =>
      log.info("Added {} to cart: ", item)
      sender ! ItemAdded(item)
      stay using CartContent(items += item)

    case Event(RemoveItem(item), CartContent(items)) =>
      log.info("Removed {} from cart", item)
      items -= item
      sender ! ItemRemoved(item)
      if (items isEmpty)
        goto(Empty) using CartContent(items)
      else
        stay using CartContent(items)

    case Event(StartCheckout, CartContent(items)) =>
      log.info("Starting checkout")
      val checkout = context.actorOf(Props[Checkout])
      sender ! CheckoutStarted(checkout)
      goto(InCheckout) using CartContent(items)
  }

  when(InCheckout) {
    case Event(CheckoutCancelled, CartContent(_)) =>
      log.info("Checkout canceled")
      goto(Empty) using CartContent(ListBuffer())

    case Event(CheckoutClosed, CartContent(ListBuffer())) =>
      log.info("Checkout closed")
      goto(Empty) using CartContent(ListBuffer())
  }

  onTransition {
    case _ -> NonEmpty => setTimer(self.toString, CartTimerExpired, 3 seconds)
    case _ -> Empty =>
      cancelTimer(self.toString)
      sender ! CartEmptied
    case _ -> InCheckout => cancelTimer(self.toString)
  }

  whenUnhandled {
    case Event(CartTimerExpired, _) =>
      goto(Empty) using CartContent(ListBuffer())
    case Event(e, s) =>
      log.warning("Received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  initialize()
}



