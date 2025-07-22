import PpekBook.getPpekBooks
import PpekBook.toBook
import cats.syntax.all._

object Scraper {
  def main(): Unit = main(Array())

  def main(
      args: Array[String],
    ): Unit =
    process() match {
      case Left(error) =>
        println(s"Error: $error")
      case Right(value) =>
        println(s"Success: ${value.mkString("\n")}")
    }

  def process(): Either[CathBooksScraperError, List[PpekBook]] =
    for {
      books <- getPpekBooks()
      limitedBooks = books.take(10)
      ppekBooks <- limitedBooks.map(toBook).sequence
    } yield ppekBooks
}
