import java.net.URI

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import cart.Cart.Item
import cart.CartManager.AddItem
import checkout.CheckoutManager.{SelectDeliveryMethod, SelectPaymentMethod, StartCheckout}
import com.typesafe.config.ConfigFactory
import order.OrderManager
import payment.Payment.DoPayment
import product.ProductCatalog.GetItems

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Shop extends App {
  implicit val timeout: Timeout = 30 seconds

  implicit val system: ActorSystem = ActorSystem("Product-system")

  val catalog = system.actorSelection("akka.tcp://Reactive5@127.0.0.1:2557/user/catalog")

  private val future: Future[Any] = catalog ? GetItems("", List("", ""))

  private val value: Any = Await.result(future, 10 seconds)
  println(value.toString)

  private def shopDemo = {
    val config = ConfigFactory.load()
    implicit val system: ActorSystem = ActorSystem("ClientSystem", config.getConfig("clientapp").withFallback(config))

    val orderActor: ActorRef = system.actorOf(Props[OrderManager], "OrderManagerActor")

    Await.result(orderActor ? AddItem(Item(URI.create("/mayo"), "helmanz", "mayo", BigDecimal.valueOf(1.34), 2)), timeout.duration)
    Await.result(orderActor ? StartCheckout, timeout.duration)

    Await.result(orderActor ? SelectDeliveryMethod("DHL"), timeout.duration)
    Await.result(orderActor ? SelectPaymentMethod("PAYPAL"), timeout.duration)

    Await.result(orderActor ? DoPayment("PayPal"), timeout.duration)
  }

}