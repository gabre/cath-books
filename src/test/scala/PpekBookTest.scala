import org.mockito.IdiomaticMockito
import org.mockito.Mockito._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import PpekBook.toBookPage
import net.ruippeixotog.scalascraper.model.Element

class PpekBookSpec extends AnyFlatSpec with Matchers with IdiomaticMockito {

  "PpekBook" should "parse author and title correctly" in {
    val link = mockElement(
      "1940. november 24-re elrendelt engesztelés imádságai; XII. Pius pápa",
    )
    val result = PpekBook.parseAuthorAndTitle(link)
    result shouldBe Right(
      ("XII. Pius pápa", "1940. november 24-re elrendelt engesztelés imádságai"),
    )
  }

  it should "return an error if the link text has multiple semicolons" in {
    val link = mockElement("a;b;c")
    val result = PpekBook.parseAuthorAndTitle(link)
    result shouldBe a[Left[_, _]]
  }

  it should "return an error if the link text has no semicolon" in {
    val link = mockElement("no semicolon")
    val result = PpekBook.parseAuthorAndTitle(link)
    result shouldBe a[Left[_, _]]
  }

  it should "return an error if the link text is empty" in {
    val link = mockElement("")
    val result = PpekBook.parseAuthorAndTitle(link)
    result shouldBe a[Left[_, _]]
  }

  it should "create a new PpekBookPage instance correctly" in {
    val link = mockElement("title; author", Map("href" -> "asdf"))
    val result = toBookPage(link)
    result shouldBe Right(PpekBookPage("author", "title", "http://ppek.hu/asdf"))
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
