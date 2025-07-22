import scala.util.Try

import cats.data.EitherT
import cats.syntax.all._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.model.ElementNode
import net.ruippeixotog.scalascraper.model.TextNode

object PpekBook {
  def parseAuthorAndTitle(
      link: Element,
    ): Either[CathBooksScraperError, BookIdentifier] =
    link.text.split(";").toList match {
      case title :: series :: author :: Nil
          if author.trim.nonEmpty && title.trim.nonEmpty &&
            series.trim.nonEmpty =>
        Right(BookIdentifier(title.trim, author.trim, series.trim.some))
      case title :: author :: Nil
          if author.trim.nonEmpty && title.trim.nonEmpty =>
        Right(BookIdentifier(title.trim, author.trim))
      case title :: Nil if title.trim.nonEmpty =>
        Right(BookIdentifier(title.trim))
      case _ =>
        Left(LinkParseError(link))
    }

  def toBookPage(
      link: Element,
    ): Either[CathBooksScraperError, PpekBookPage] =
    for {
      href <- link.attrs.get("href").toRight(MissingAttribute(link, "href"))
      id <- parseAuthorAndTitle(link)
    } yield new PpekBookPage(id, s"${Conf.ppekUrl}/$href")

  def toBook(
      bookPage: PpekBookPage,
    ): Either[CathBooksScraperError, PpekBook] =
    for {
      doc <- Try(JsoupBrowser().get(s"${bookPage.url}"))
        .toOption
        .toRight(NoSuchPage(bookPage.url))
      body <- (doc >?> element("body")).toRight(ElementNotFound("Book page body"))
      tableRows <- Either.right(body >> elementList("tr"))
      fileUrls <- getBookLinks(tableRows)
      description <- getDescription(body)
    } yield new PpekBook(bookPage.id, description, fileUrls)

  def getDescription(
      body: Element,
    ): Either[CathBooksScraperError, String] = {
    val nodes = body.childNodes.toSeq
    val firstHrNodeIndex = nodes.indexWhere {
      case ElementNode(JsoupBrowser.JsoupElement(e)) if e.tagName == "hr" =>
        true
      case _ =>
        false
    }
    val downloadSmallTextIndex = nodes.indexWhere {
      case ElementNode(JsoupBrowser.JsoupElement(e))
          if e.text.startsWith("Kattintson") =>
        true
      case _ =>
        false
    }
    if (firstHrNodeIndex == -1 || downloadSmallTextIndex == -1)
      Left(ElementNotFound("Book description"))
    else
      nodes
        .slice(firstHrNodeIndex + 1, downloadSmallTextIndex)
        .collect {
          case t: TextNode if t.content.trim.nonEmpty =>
            t.content.trim
          case ElementNode(e) =>
            e.outerHtml
        }
        .mkString
        .asRight
  }

  def getBookLinks(
      tableRows: List[Element],
    ): Either[CathBooksScraperError, List[String]] =
    (
      for {
        row <- tableRows
        a <- row >> elementList("a")
      } yield a
        .attrs
        .get("href")
        .map(url => s"${Conf.ppekUrl}/$url")
        .toRight(MissingAttribute(a, "href"))
    ).sequence

  def getPpekBooks(): Either[CathBooksScraperError, List[PpekBookPage]] =
    EitherT {
      val doc = JsoupBrowser().get(s"${Conf.ppekUrl}/ppekcim.htm")
      for {
        row <- doc >> elementList("tr")
        a <- row >> elementList("a")
        if a.attrs.get("href").isDefined
      } yield toBookPage(a)
    }.value.sequence
}

case class BookIdentifier(
    title: String,
    author: String = "Unknown author",
    series: Option[String] = None)

case class PpekBookPage(
    id: BookIdentifier,
    url: String)

case class PpekBook(
    id: BookIdentifier,
    description: String,
    fileUrls: List[String])
