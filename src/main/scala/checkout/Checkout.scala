package checkout

import akka.actor.ActorRef

sealed trait Checkout {
  def getDeliveryMethod: String
  def getPaymentMethod: String
  def getOrderManager: ActorRef
}
case class CheckoutWithDelivery(deliveryMethod: String, orderManager: ActorRef) extends Checkout {
  override def getDeliveryMethod: String = deliveryMethod
  override def getPaymentMethod: String = null
  override def getOrderManager: ActorRef = orderManager
}
case class CheckoutWithDeliveryAndPayment(deliveryMethod: String, paymentMethod: String, orderManager: ActorRef) extends Checkout {
  override def getDeliveryMethod: String = deliveryMethod
  override def getPaymentMethod: String = paymentMethod
  override def getOrderManager: ActorRef = orderManager
}
