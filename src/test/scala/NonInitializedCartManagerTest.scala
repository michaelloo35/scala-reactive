//import Checkout.StartCheckout
//import akka.actor.ActorSystem
//import akka.testkit.{TestFSMRef, TestKit}
//import cart.CartManager
//import org.scalatest.concurrent.ScalaFutures
//import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}
//
//import scala.collection.mutable.ListBuffer
//
//class NonInitializedCartManagerTest extends TestKit(ActorSystem("CartTest"))
//  with WordSpecLike
//  with BeforeAndAfterAll
//  with ScalaFutures
//  with Matchers {
//
//  override def afterAll(): Unit = {
//    system.terminate()
//  }
//
//  "CartActor" must {
//    "contains item after addition" in {
//      val cartActor = TestFSMRef(new CartManager)
//
//      cartActor ! AddItem("pencil")
//
//      cartActor.stateName shouldBe NonEmpty
//      cartActor.stateData shouldBe CartContent(ListBuffer("pencil"))
//    }
//
//    "contains 1 item after removal" in {
//      val cartActor = TestFSMRef(new CartManager)
//
//      cartActor ! AddItem("pencil")
//      cartActor ! AddItem("notebook")
//      cartActor ! RemoveItem("pencil")
//
//      cartActor.stateName shouldBe NonEmpty
//      cartActor.stateData shouldBe CartContent(ListBuffer("notebook"))
//    }
//
//    "contains no item after timer expiration" in {
//      val cartActor = TestFSMRef(new CartManager)
//
//      cartActor ! AddItem("pencil")
//      cartActor ! AddItem("notebook")
//
//      Thread sleep 3500
//
//      cartActor.stateName shouldBe Empty
//      cartActor.stateData shouldBe CartContent(ListBuffer())
//    }
//
//    "cart should be in checkout" in {
//      val cartActor = TestFSMRef(new CartManager)
//
//      cartActor ! AddItem("pencil")
//      cartActor ! StartCheckout
//
//      cartActor.stateName shouldBe InCheckout
//      cartActor.stateData shouldBe CartContent(ListBuffer("pencil"))
//    }
//  }
//
//}