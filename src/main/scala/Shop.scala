import java.net.URI

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import cart.Cart.Item
import cart.CartManager.AddItem
import checkout.CheckoutManager.{SelectDeliveryMethod, SelectPaymentMethod, StartCheckout}
import order.OrderManager
import payment.PaymentManager.DoPayment

import scala.concurrent.Await
import scala.concurrent.duration._

object Shop extends App {
  implicit val timeout: Timeout = 30 seconds

  val system = ActorSystem("Shop")
  val orderActor: ActorRef = system.actorOf(Props[OrderManager], "OrderManagerActor")

  Await.result(orderActor ? AddItem(Item(URI.create("/egg"), "egg", BigDecimal.valueOf(12.34), 2)), timeout.duration)
  Await.result(orderActor ? StartCheckout, timeout.duration)

  Await.result(orderActor ? SelectDeliveryMethod("DHL"), timeout.duration)
  Await.result(orderActor ? SelectPaymentMethod("PAYPAL"), timeout.duration)

  Await.result(orderActor ? DoPayment, timeout.duration)


}