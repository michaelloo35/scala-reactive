package cart

import java.net.URI

import cart.Cart.Item

object Cart {
  case class Item(id: URI, name: String, price: BigDecimal, count: Int)
}

sealed trait Cart {
  def addItem(item: Item): Cart
  def removeItem(id: URI, count: Int): Cart
  def empty(): Cart
}

case class NonEmptyCart(items: Map[URI, Item]) extends Cart {

  def addItem(item: Item): NonEmptyCart = {
    val currentCount = getCurrentCount(item)
    val updatedItem = item.copy(count = currentCount + item.count)

    println("ITEMS BEFORE ADDITION: " + items)
    val cart = this.copy(items + ((updatedItem.id, updatedItem)))
    println("ITEMS AFTER ADDITION: " + cart.items)

    cart
  }

  def removeItem(id: URI, count: Int): NonEmptyCart = {
    val currentCount = getCurrentCount(id)

    if (currentCount <= 0 + count) this.copy(items - id) else {
      val updatedItem = items(id).copy(count = currentCount - count)
      this.copy(items + ((id, updatedItem)))
    }
  }

  override def empty(): Cart = EmptyCart()

  private def getCurrentCount(item: Item): Int = {
    if (items contains item.id) items(item.id).count else 0
  }

  private def getCurrentCount(id: URI): Int = {
    if (items contains id) items(id).count else 0
  }
}
case class EmptyCart() extends Cart {
  override def addItem(item: Item): Cart = NonEmptyCart(Map.empty + ((item.id, item)))
  override def removeItem(id: URI, count: Int): Cart = this
  override def empty(): Cart = this
}

