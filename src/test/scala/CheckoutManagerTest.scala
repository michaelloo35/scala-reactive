//import akka.actor.{ActorSystem, Props}
//import akka.testkit.{ImplicitSender, TestKit, TestProbe}
//import checkout.CheckoutManager
//import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
//
//class CheckoutManagerTest extends TestKit(ActorSystem())
//  with WordSpecLike
//  with BeforeAndAfterAll
//  with ImplicitSender {
//
//  override def afterAll(): Unit = {
//    system.terminate
//  }
//
//  "checkout.Checkout actor" must {
//    "send CheckoutClosed message to Cart" in {
//      val cart = TestProbe()
//      val checkout = cart.childActorOf(Props[CheckoutManager])
//
//      checkout ! StartCheckout
//      checkout ! SelectDeliveryMethod("DHL")
//      expectMsg(SelectedDeliveryMethod("DHL"))
//
//      checkout ! SelectPaymentMethod("PAYPAL")
//      expectMsg(SelectedPaymentMethod("PAYPAL"))
//      expectMsgType[PaymentServiceStarted]
//
//      checkout ! PaymentReceived
//
//      cart.expectMsg(CheckoutClosed)
//    }
//
//    "checkout expired" in {
//      val cartParent = TestProbe()
//      val checkoutChild = cartParent.childActorOf(Props[CheckoutManager])
//
//      checkoutChild ! StartCheckout
//      Thread sleep 4000
//
//      cartParent.expectMsg(CheckoutCancelled)
//    }
//
//    "payment expired" in {
//      val cartParent = TestProbe()
//      val checkoutChild = cartParent.childActorOf(Props[CheckoutManager])
//
//      checkoutChild ! StartCheckout
//      checkoutChild ! SelectDeliveryMethod("DHL")
//      Thread sleep 4000
//
//      cartParent.expectMsg(CheckoutCancelled)
//    }
//  }
//}