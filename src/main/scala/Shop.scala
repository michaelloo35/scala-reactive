import Cart.AddItem
import Checkout.{SelectDeliveryMethod, SelectPaymentMethod, StartCheckout}
import Payment.DoPayment
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

object Shop extends App {
  implicit val timeout: Timeout = 30 seconds

  val system = ActorSystem("Shop")
  val orderActor: ActorRef = system.actorOf(Props[OrderManager], "managerActor")

  Await.result(orderActor ? AddItem("notebook"), timeout.duration)
  Await.result(orderActor ? StartCheckout, timeout.duration)

  Await.result(orderActor ? SelectDeliveryMethod("DHL"), timeout.duration)
  Await.result(orderActor ? SelectPaymentMethod("PAYPAL"), timeout.duration)

  Await.result(orderActor ? DoPayment, timeout.duration)
}