import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.model.Element
import cats.data.EitherT
import cats.instances.list._
import net.ruippeixotog.scalascraper.model.TextNode

object PpekBook {
  def parseAuthorAndTitle(link: Element): Either[Throwable, (String, String)] =
    link.text.split(";").toList match {
      case title :: author :: Nil if author.trim.nonEmpty && title.trim.nonEmpty =>
        Right((author.trim, title.trim))
      case _ =>
        Left(new RuntimeException(s"Failed to parse ${link.text} from $link"))
    }

  def toBookPage(link: Element): Either[Throwable, PpekBookPage] =
    for {
      href <- link.attrs.get("href").toRight(new RuntimeException(s"No href for $link"))
      authorAndTitle <- parseAuthorAndTitle(link)
    } yield new PpekBookPage(
      authorAndTitle._1,
      authorAndTitle._2,
      s"${Conf.ppekUrl}/$href"
    )

  def toBook(bookPage: PpekBookPage): Either[Throwable, PpekBook] = {
      val doc = JsoupBrowser().get(s"${bookPage.url}")
      val body = doc >> element("body")
      val description = body.childNodes.collectFirst {
        case t: TextNode => t.content
      }
      val tableRows = body >> elementList("tr")
      val fileUrls = tableRows.flatMap { row =>
        (row >> elementList("a"))
          .map(_.attrs.get("href"))
          .map(url => s"${Conf.ppekUrl}/${url}")
      }

      Right(PpekBook(bookPage.author, bookPage.title, description.get, fileUrls))
    }

  def getPpekBooks(): EitherT[List, Throwable, PpekBookPage] =
    EitherT {
      val doc = JsoupBrowser().get(s"${Conf.ppekUrl}/ppekcim.htm")
      for {
        row <- doc >> elementList("tr")
        a <- row >> elementList("a")
      } yield toBookPage(a)
    }
}

case class PpekBookPage(author: String, title: String, url: String)

case class PpekBook(author: String, title: String, description: String, fileUrls: List[String])
