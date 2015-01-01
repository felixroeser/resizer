package com.byteforscher

import java.io.{BufferedOutputStream, FileOutputStream}
import java.io.{File, BufferedInputStream, ByteArrayInputStream, InputStream, FileInputStream}
import org.apache.commons.io.FileUtils

import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import spray.routing.{HttpService, RequestContext}
import spray.routing.directives.CachingDirectives
import spray.can.server.Stats
import spray.can.Http
import spray.httpx.marshalling.Marshaller
import spray.httpx.encoding.Gzip
import spray.util._
import spray.http._
import StatusCodes._
import MediaTypes._
import CachingDirectives._

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.util.Timeout
import akka.pattern.ask
import akka.io.IO

import spray.can.Http
import spray.http._
import HttpMethods._
import com.typesafe.config._

import com.sksamuel._
import com.sksamuel.scrimage.Format._

// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class MyServiceActor extends Actor with MyService {

  // the HttpService trait defines only one abstract member, which
  // connects the services environment to the enclosing actor or test
  implicit def actorRefFactory = context

  // this actor only runs our route, but you could add
  // other things here, like request stream processing
  // or timeout handling
  def receive = runRoute(myRoute)
}

// this trait defines our service behavior independently from the service actor
trait MyService extends HttpService { this: MyServiceActor =>

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our Futures and Scheduler
  implicit def executionContext = actorRefFactory.dispatcher

  //implicit val system: ActorSystem = ActorSystem()
  implicit val timeout: Timeout = Timeout(30.seconds)
  implicit val system = context.system
  implicit val conf = ConfigFactory.load()

  val myRoute = {
    get {
      pathSingleSlash {
        complete("welcome stanger.")
      } ~
      path("ping") {
        complete("PONG!")
      } ~
      path(Segment / "i" / Segment / Segment) { (realm, options, url) =>
        val realmTemplate = realmMapper.getString(realm)
        proxyResizedImage(options, url, realm, realmTemplate)
      }
    } ~
    delete {
      path(Segment / "i" / Segment) { (realm, url) =>
        val realmTemplate = realmMapper.getString(realm)
        purgeImage(url, realm, realmTemplate)
      }
    }
  }

  private object Regexs {
    val wah  = """(\d+)x(\d+)""".r
    val max  = """(\d+)""".r
    val qual = """q(\d{1,2})""".r
    val fill = """c(\d{1,3})-(\d{1,3})-(\d{1,3})""".r
  }

  lazy val realmMapper = ConfigFactory.load().getConfig("realmMapper")

  def getBaseImageFromCacheOrRequest(url: String, dir: java.io.File): Future[Array[Byte]] = {
    val file = new java.io.File(dir + "/original")
    if (file.exists) {
        println(s"Baseimage Cache HIT: $url")
        Future.successful {
          val bis = new BufferedInputStream(new FileInputStream(file))
          Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
        }
    } else {
      println(s"Baseimage Cache MISS: $url")
      val responseFuture: Future[HttpResponse] = (IO(Http) ? HttpRequest(GET, Uri(url))).mapTo[HttpResponse]
      responseFuture.map { response =>
        println(s"GET $url returned ${response.status}")
        // FIXME write non-blocking using akka.io
        val out = new BufferedOutputStream(new java.io.FileOutputStream(file))
        out.write(response.entity.data.toByteArray)
        out.close
        response.entity.data.toByteArray
      }
    }
  }

  def proxyResizedImage(options: String, url: String, realm: String, realmTemplate: String)(ctx: RequestContext): Future[Unit] = {
    val cacheDir = cacheDirName(url, realm)
    val dir = new java.io.File(cacheDir)
    if (!dir.exists) { dir.mkdirs }

    val sizedImageName = java.security.MessageDigest.getInstance("SHA-1").digest( options.getBytes("UTF-8") ).map("%02x".format(_)).mkString
    val sizedImageFile = new java.io.File(cacheDir + "/" + sizedImageName)
    if (sizedImageFile.exists) {
      println(s"Cache HIT: $url")
      Future.successful {
        respondWithMediaType(`image/jpeg`) {
          getFromFile(sizedImageFile)
        }.apply(ctx)
      }
    } else {
      val expandedUrl = realmTemplate.replace(" ", "").replace("{{url}}", url)
      getBaseImageFromCacheOrRequest(expandedUrl, dir).map { inputData =>
        // FIXME replace with a proper function call chain - or fold
        var baseImage = com.sksamuel.scrimage.Image(inputData)

        val parsedOptions = options.split(",").foldLeft(Map.empty[String, List[Int]]) { (map, s) =>
          s match {
            case Regexs.wah(w, h)   => map + ("fit" -> List(w.toInt, h.toInt) )
            case Regexs.max(d)      => map + ("resize" -> List(d.toInt) )
            case Regexs.qual(q)     => map + ("quality" -> List(q.toInt) )
            case Regexs.fill(r,g,b) => map + ("fill" -> List(r.toInt, g.toInt, b.toInt)  )
            case _ => println(s"no match for $s") ; map
          }
        }

        println(s"Options: $parsedOptions")

        val fillValues = parsedOptions.getOrElse("fill", List(255, 255, 255))
        val fillColor = new com.sksamuel.scrimage.RGBColor(fillValues.head, fillValues.apply(1), fillValues.apply(2))

        if (parsedOptions.contains("fit")) {
          println(s"Fitting to ${parsedOptions.get("fit").get}")
          val d = parsedOptions.get("fit").get
          baseImage = baseImage.fit(d.head, d.apply(1), fillColor )
        } else if (parsedOptions.contains("resize")) {
          val d = parsedOptions.get("resize").get.head
          println(s"Resing to $d")
          baseImage = baseImage.fit(d, d, fillColor).autocrop(fillColor)
        } else {
          // TODO raise exception
        }

        baseImage.writer(com.sksamuel.scrimage.Format.JPEG).withCompression( parsedOptions.getOrElse("quality", List(75)).head ).write(sizedImageFile)

        respondWithMediaType(`image/jpeg`) {
          getFromFile(sizedImageFile)
        }.apply(ctx)
      }
    }

    // http://stackoverflow.com/questions/26735649/iohttp-is-cause-the-error-could-not-find-implicit-value-for-parameter-system
    // http://stackoverflow.com/questions/24284641/spray-akka-missing-implicit
    // https://github.com/eigengo/activator-spray-twitter/blob/master/src/main/scala/core/tweetstream.scala
    // http://filip-andersson.blogspot.de/2014/03/a-very-simple-example-of-spray-client.html
    // http://localhost:8080/ol/i/300x300,q70/carts%2F48727acb-7122-418a-9ab8-48ad4c5dda7a%2Fimages%2Fimage_0_snow2.png
  } // proxyResizedImage

  def purgeImage(url: String, realm: String, realmTemplate: String)(ctx: RequestContext): Future[Boolean] = {
    Future {
      val dir = new java.io.File(cacheDirName(url, realm))
      println(s"Pruging $dir")
      val dirExists = dir.exists
      if (dir.exists) { FileUtils.forceDelete(dir); }
      dirExists
    }
  }

  def cacheDirName(url: String, realm: String): String = {
    val baseImageName = java.security.MessageDigest.getInstance("SHA-1").digest( url.getBytes("UTF-8") ).map("%02x".format(_)).mkString
    s"/tmp/resizer_cache/$realm/$baseImageName"
  }

}
