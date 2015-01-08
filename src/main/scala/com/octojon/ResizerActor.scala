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

  implicit val timeout: Timeout = Timeout(30.seconds)
  implicit val system = context.system

  def receive = {
    case rr: ResizeRequest => {
      println(s"ResizerActor got $rr")
      ensureOriginal(rr)
      resizeImage(rr)
      sender ! ResizerResponse(true, None)
      //sender ! ResizerResponse(false, Some("bad stuff happens sometimes"))*/
    }
  }

  def ensureOriginal(rr: ResizeRequest): Boolean = {
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

  def resizeImage(rr: ResizeRequest) = {
    val inStream = new java.io.FileInputStream(rr.originalFile)
    var baseImage = com.sksamuel.scrimage.Image(inStream)
    val options = rr.options

    if (options.contains("fit")) {
      println(s"Fitting to ${options.get("fit").get}")
      val d = options.get("fit").get
      baseImage = baseImage.fit(d.head, d.apply(1), rr.options.fillColor )
    } else if (options.contains("resize")) {
      val d = options.get("resize").get.head
      println(s"Resing to $d")
      baseImage = baseImage.fit(d, d, rr.options.fillColor).autocrop(rr.options.fillColor)
    } else {
      // TODO fail - must provide fit or resize
    }

    println(s"Saving to ${{rr.sizedImageName}}")
    baseImage
      .writer(com.sksamuel.scrimage.Format.JPEG)
      .withCompression( rr.options.map.getOrElse("quality", List(75)).head )
      .write(rr.sizedImageFile)
  }
}
