import org.mockito.IdiomaticMockito
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import PpekBook.toBookPage
import cats.syntax.all._
import net.ruippeixotog.scalascraper.model.Element

class PpekBookSpec extends AnyFlatSpec with Matchers with IdiomaticMockito {

  "PpekBook" should "parse author, series title and title correctly" in {
    val link = mockElement(
      "Prohászka gyermekkora; Prohászka a szívekben; Barlay Ö. Szabolcs",
    )
    val result = PpekBook.parseAuthorAndTitle(link)
    result shouldBe
      Right(
        BookIdentifier(
          "Prohászka gyermekkora",
          "Barlay Ö. Szabolcs",
          "Prohászka a szívekben".some,
        ),
      )
  }

  "PpekBook" should "parse author and title correctly" in {
    val link = mockElement(
      "1940. november 24-re elrendelt engesztelés imádságai; XII. Pius pápa",
    )
    val result = PpekBook.parseAuthorAndTitle(link)
    result shouldBe
      Right(
        BookIdentifier(
          "1940. november 24-re elrendelt engesztelés imádságai",
          "XII. Pius pápa",
        ),
      )
  }

  it should "return an error if the link text has more than two semicolons" in {
    val link = mockElement("a;b;c;d")
    val result = PpekBook.parseAuthorAndTitle(link)
    result shouldBe a[Left[_, _]]
  }

  it should "return not an error if the link text has no semicolon" in {
    val link = mockElement("no semicolon")
    val result = PpekBook.parseAuthorAndTitle(link)
    result shouldBe Right(BookIdentifier("no semicolon"))
  }

  it should "return an error if the link text is empty" in {
    val link = mockElement("")
    val result = PpekBook.parseAuthorAndTitle(link)
    result shouldBe a[Left[_, _]]
  }

  it should "create a new PpekBookPage instance correctly" in {
    val link = mockElement("title; author", Map("href" -> "asdf"))
    val result = toBookPage(link)
    result shouldBe
      Right(
        PpekBookPage(BookIdentifier("title", "author"), "http://ppek.hu/asdf"),
      )
  }

  it should "return an error if the link does not have an href attribute" in {
    val link = mockElement("author; title", Map())
    val result = toBookPage(link)
    result shouldBe a[Left[_, _]]
  }

  def mockElement(
      text: String,
      attrs: Map[String, String] = Map("href" -> "some-href"),
    ) = {
    val element = mock[Element]
    element.text.returns(text)
    element.attrs.returns(attrs)
    element
  }
}
