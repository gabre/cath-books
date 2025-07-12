import PpekBook.getPpekBooks
import PpekBook.toBook

object Scraper {
  def main(): Unit = main(Array())

  def main(args: Array[String]): Unit = {
    val books = getPpekBooks().value
    println(books.last.flatMap(toBook))
  }

  // def getPpekBooks2(): List[PpekBook] = {

  // }
}
