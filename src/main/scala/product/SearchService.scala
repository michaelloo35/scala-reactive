package product

import java.net.URI
import java.util.zip.GZIPInputStream

import cart.Cart.Item

import scala.io.Source
import scala.util.Random

class SearchService() {
  private val gz = new GZIPInputStream(
    getClass.getResourceAsStream("/query_result.gz")
  )
  val warehouse: Map[String, List[Item]] = loadAndParseItemsInGzip(gz)

  def searchBy(company: String, keyWords: List[String]): List[Item] = {
    val keyWordsLowerCase = keyWords.map(_.toLowerCase)
    warehouse
      .getOrElse(company.toLowerCase, Nil)
      .map(item => (keyWordsLowerCase.count(item.name.toLowerCase.contains), item))
      .sortBy(-_._1)
      .take(10)
      .map(_._2)
  }
  private def loadAndParseItemsInGzip(gzip: GZIPInputStream): Map[String, List[Item]] = {
    Source
      .fromInputStream(gzip)
      .getLines()
      .drop(1)
      .filter(_.split(",").length >= 3)
      .map { line =>
        val values = line.split(",")
        Item(
          new URI("http://catalog.com/product/" + values(0).replaceAll("\"", "")),
          values(1).replaceAll("\"", ""),
          values(2).replaceAll("\"", ""),
          Random.nextInt(1000).toDouble,
          Random.nextInt(100)
        )
      }
      .toList
      .groupBy(_.company.toLowerCase)
  }
}
