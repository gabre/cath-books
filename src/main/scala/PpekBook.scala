import scala.util.Try

import cats.data.EitherT
import cats.syntax.all._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.model.TextNode

object PpekBook {
  def parseAuthorAndTitle(
      link: Element,
    ): Either[CathBooksScraperError, TitleParserResult] =
    link.text.split(";").toList match {
      case title :: series :: author :: Nil
          if author.trim.nonEmpty && title.trim.nonEmpty &&
            series.trim.nonEmpty =>
        Right(TitleParserResult(title.trim, author.trim, series.trim.some))
      case title :: author :: Nil
          if author.trim.nonEmpty && title.trim.nonEmpty =>
        Right(TitleParserResult(title.trim, author.trim))
      case title :: Nil if title.trim.nonEmpty =>
        Right(TitleParserResult(title.trim))
      case _ =>
        Left(LinkParseError(link))
    }

  def toBookPage(
      link: Element,
    ): Either[CathBooksScraperError, PpekBookPage] =
    for {
      href <- link.attrs.get("href").toRight(MissingAttribute(link, "href"))
      parsed <- parseAuthorAndTitle(link)
    } yield new PpekBookPage(
      parsed.author,
      parsed.title,
      s"${Conf.ppekUrl}/$href",
    )

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
    } yield new PpekBook(bookPage.author, bookPage.title, description, fileUrls)

  def getDescription(
      body: Element,
    ): Either[CathBooksScraperError, String] =
    body
      .childNodes
      .collectFirst { case t: TextNode =>
        t.content
      }
      .toRight(ElementNotFound("Book description"))

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

case class TitleParserResult(
    title: String,
    author: String = "Unknown author",
    series: Option[String] = None)

case class PpekBookPage(
    author: String,
    title: String,
    url: String)

case class PpekBook(
    author: String,
    title: String,
    description: String,
    fileUrls: List[String])
