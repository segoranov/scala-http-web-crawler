package homework3

import homework3.html.HtmlUtils
import homework3.http.HttpUtils
import homework3.math.Monoid.ops._
import homework3.math.Monoid._
import homework3.processors.{WordCount, WordCounter}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class Test extends FlatSpec with Matchers {

  def getFutureResultBlocking[R](f: Future[R]) = Await.result(f, Duration.Inf)

  val mockHttpClient = new MockHttpClient
  val testSpidey = new Spidey(mockHttpClient)

  "response from MockHttpClient" should "be html resource containing valid http link" in {
    val httpResponse = getFutureResultBlocking(mockHttpClient.get("https://www.test1.com/"))

    httpResponse.isSuccess shouldBe true

    httpResponse.isHTMLResource shouldBe true

    val links = HtmlUtils.linksOf(httpResponse.body, "https://www.test1.com/")

    links.size shouldBe 1
    links.foreach(HttpUtils.isValidHttp(_) shouldBe true)
    links.head shouldBe "https://www.test2.com/"
    WordCount.wordsOf(HtmlUtils.toText(httpResponse.body)) shouldBe
      List("text", "response", "2")
  }

  "wordCountMonoid" should "add two WordCount instances correctly" in {
    WordCount(Map("a" -> 2, "b" -> 3, "c" -> 4)) |+| WordCount(Map("a" -> 2, "b" -> 3, "d" -> 5)) shouldBe
      WordCount(Map("a" -> 4, "b" -> 6, "c" -> 4, "d" -> 5))

    WordCount(Map("a" -> 2, "b" -> 3, "c" -> 4)) |+| wordCountMonoid.identity shouldBe
      WordCount(Map("a" -> 2, "b" -> 3, "c" -> 4))
  }

  "processors should work correctly and crawl" should "go to only one link when depth is 0" in {
    getFutureResultBlocking(testSpidey.crawl("https://www.test1.com/", SpideyConfig(0))(WordCounter)).wordToCount shouldBe
      Map("text" -> 1, "response" -> 1, "2" -> 1)
  }

  it should "go to two links when depth is 1" in {
    getFutureResultBlocking(testSpidey.crawl("https://www.test1.com/", SpideyConfig(1))(WordCounter)).wordToCount shouldBe
      Map("text" -> 2, "response" -> 2, "2" -> 1, "3" -> 1)
  }

  it should "go to three links when depth is 2" in {
    getFutureResultBlocking(testSpidey.crawl("https://www.test1.com/", SpideyConfig(2))(WordCounter)).wordToCount shouldBe
      Map("text" -> 3, "response" -> 3, "2" -> 1, "3" -> 1, "1" -> 1)
  }

  it should "go to all the links when depth is a big number" in {
    getFutureResultBlocking(testSpidey.crawl("https://www.test1.com/", SpideyConfig(1000))(WordCounter)).wordToCount shouldBe
      Map("text" -> 3, "response" -> 3, "2" -> 1, "3" -> 1, "1" -> 1)
  }

}

object Test extends App {
  def getFutureResultBlocking[R](f: Future[R]) = Await.result(f, Duration.Inf)

  val mockHttpClient = new MockHttpClient

  import homework3.processors.WordCount

  val httpResponse = getFutureResultBlocking(mockHttpClient.get("https://www.test1.com/"))

  val links = HtmlUtils.linksOf(httpResponse.body, "https://www.test1.com/")

  println(WordCount.wordsOf(HtmlUtils.toText(httpResponse.body)))
}
