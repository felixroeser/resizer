package com.octojon

import java.io._

import spray.http._
import HttpMethods._
import spray.can.Http
import akka.util.Timeout
import akka.io.IO
import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.actor._

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._

import com.octojon._

case class ResizerResponse(success: Boolean, error: Option[String])

class ResizerActor extends Actor {

  implicit val timeout: Timeout = Timeout(60.seconds)
  implicit val system = context.system

  def receive = {
    case rr: ResizeRequest => {
      println(s"ResizerActor received $rr")
      sender ! processResizeRequest(rr)
    }
  }

  private def processResizeRequest(rr: ResizeRequest): ResizerResponse = {
    try {
      ensureOriginal(rr)
      resizeImage(rr)
      ResizerResponse(true, None)
    } catch {
      case e: Throwable => {
        println(e)
        ResizerResponse(false, Some("bad stuff happens sometimes"))
      }
    }
  }

  private def ensureOriginal(rr: ResizeRequest): Boolean = {
    if (rr.originalFile.exists) {
      println(s"Original HIT: $rr ${rr.dirName}")
    } else {
      println(s"Original MISS: $rr")
      val future: Future[HttpResponse] = (IO(Http) ? HttpRequest(GET, Uri(rr.expandedUrl))).mapTo[HttpResponse]
      // blocking io for now
      val response = Await.result(future, timeout.duration)
      println(s"GET ${rr.expandedUrl} returned ${response.status}")

      val out = new BufferedOutputStream(new java.io.FileOutputStream(rr.originalFile))
      out.write(response.entity.data.toByteArray)
      out.close
    }
    true
  }

  private def resizeImage(rr: ResizeRequest) = {
    val inStream = new java.io.FileInputStream(rr.originalFile)
    var baseImage = com.sksamuel.scrimage.Image(inStream)
    val options = rr.options

    if (options.contains("fit")) {
      val d = options.get("fit").get.map { List(_, 3).max } // 3x3 is the min size
      println(s"Fitting to ${d}")
      baseImage = baseImage.fit(d.head, d.apply(1), options.fillColor )
    } else if (options.contains("resize")) {
      val d = List(options.get("resize").get.head, 3).max
      println(s"Resizing to $d")
      baseImage = baseImage.fit(d, d, options.fillColor).autocrop(options.fillColor)
    }

    println(s"Saving to ${{rr.sizedImagePath}}")
    baseImage
      .writer(com.sksamuel.scrimage.Format.JPEG)
      .withCompression( rr.options.map.getOrElse("quality", List(75)).head )
      .write(rr.sizedImageFile)
  }
}
