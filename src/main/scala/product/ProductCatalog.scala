package product

import akka.actor.{Actor, ActorSystem, Props}
import cart.Cart.Item
import com.typesafe.config.ConfigFactory
import product.ProductCatalog.{GetItems, Items}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object ProductCatalog {

  case class GetItems(brand: String, productKeyWords: List[String])

  trait Ack

  case class Items(items: List[Item]) extends Ack

  def props(searchService: SearchService): Props =
    Props(new ProductCatalog(searchService))
}

class ProductCatalog(searchService: SearchService) extends Actor {

  override def receive: Receive = {
    case GetItems(brand, productKeyWords) =>
      sender() ! Items(searchService.searchBy(brand, productKeyWords))
  }
}

object ProductCatalogApp extends App {
  private val config = ConfigFactory.load()
  private val productCatalogSystem = ActorSystem("Reactive5", config.getConfig("productcatalog").withFallback(config))
  productCatalogSystem.actorOf(ProductCatalog.props(new SearchService()), "catalog")
  // wait forever
  Await.result(productCatalogSystem.whenTerminated, Duration.Inf)


}