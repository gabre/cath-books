import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.Element
import cats.data.EitherT
import cats.instances.list._
import PpekBook.toBookPage
import PpekBook.getPpekBooks
import PpekBook.toBook

object Scraper {
  def main(): Unit =
    main(Array())

  def main(args: Array[String]): Unit = {
    val books = getPpekBooks().value
    println(books.last.flatMap(toBook))
  }

  // def getPpekBooks2(): List[PpekBook] = {

  // }
}
