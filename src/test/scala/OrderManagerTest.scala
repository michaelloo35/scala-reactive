import Cart.{AddItem, ItemAdded}
import Checkout.{CheckoutStarted, SelectDeliveryMethod, SelectPaymentMethod, StartCheckout}
import OrderManager._
import Payment.DoPayment
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{TestFSMRef, TestKit}
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class OrderManagerTest
  extends TestKit(ActorSystem("OrderManagerTest"))
    with WordSpecLike
    with BeforeAndAfterAll
    with ScalaFutures
    with Matchers {

  implicit val timeout: Timeout = 3.second

  "An order manager" must {
    "supervise whole order process" in {

      def sendMessageAndValidateState(
                                       orderManager: TestFSMRef[OrderManagerState, OrderManagerData, OrderManager],
                                       message: OrderManagerCommand,
                                       expectedState: OrderManagerState,
                                     ): Unit = {
        (orderManager ? message).mapTo[Event].futureValue shouldBe Done
        orderManager.stateName shouldBe expectedState
      }

      val orderManager = TestFSMRef[OrderManagerState, OrderManagerData, OrderManager](new OrderManager(),"orderManager")
      orderManager.stateName shouldBe Open

      sendMessageAndValidateState(orderManager, AddItem("rollerblades"), Open)

      sendMessageAndValidateState(orderManager, StartCheckout, InCheckout)

      sendMessageAndValidateState(orderManager, SelectDeliveryMethod("DHL"), InCheckout)

      sendMessageAndValidateState(orderManager, SelectPaymentMethod("PAYPAL"), InPayment)

      sendMessageAndValidateState(orderManager, DoPayment, Finished)
    }
  }
}