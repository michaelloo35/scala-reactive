package order

import akka.actor.ActorRef

sealed trait Order {
  def getOwnerRef: ActorRef
  def getCartManagerRef: ActorRef
  def getCheckoutManagerRef: ActorRef
  def getPaymentManagerRef: ActorRef
}

case class CartManagerRef(cartManager: ActorRef, owner: ActorRef) extends Order {
  override def getOwnerRef: ActorRef = owner
  override def getCartManagerRef: ActorRef = cartManager
  override def getCheckoutManagerRef: ActorRef = null
  override def getPaymentManagerRef: ActorRef = null
}
case class CheckoutManagerRef(checkoutManager: ActorRef, owner: ActorRef) extends Order {
  override def getOwnerRef: ActorRef = owner
  override def getCartManagerRef: ActorRef = null
  override def getCheckoutManagerRef: ActorRef = checkoutManager
  override def getPaymentManagerRef: ActorRef = null
}
case class PaymentManagerRef(paymentManager: ActorRef, owner: ActorRef) extends Order {
  override def getOwnerRef: ActorRef = owner
  override def getCartManagerRef: ActorRef = null
  override def getCheckoutManagerRef: ActorRef = null
  override def getPaymentManagerRef: ActorRef = paymentManager
}
