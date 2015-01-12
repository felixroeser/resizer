package com.octojon

import java.io._
import org.apache.commons.io.FileUtils

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.routing._
import akka.util.Timeout
import akka.io.IO
import spray.routing.{HttpService, RequestContext}
import spray.routing.directives.CachingDirectives
import spray.can.server.Stats
import spray.can.Http
import spray.http._
import spray.util._
import StatusCodes._
import MediaTypes._
import CachingDirectives._
import HttpMethods._

import com.typesafe.config._

import com.octojon._

class ResizerAPIActor extends Actor with ResizerAPI {
  implicit def actorRefFactory = context
  def receive = runRoute(resizerRoute)
}

trait ResizerAPI extends HttpService { this: ResizerAPIActor =>

  implicit def executionContext = actorRefFactory.dispatcher
  implicit val timeout: Timeout = Timeout(30.seconds)
  implicit val system = context.system
  lazy val resizerActor = system.actorOf(Props[ResizerActor].withRouter(RoundRobinPool(nrOfInstances = 3)))

  val resizerRoute = {
    get {
      pathSingleSlash {
        complete("welcome stanger.")
      } ~
      path("ping") {
        complete("PONG!")
      } ~
      path(Segment / "i" / Segment / Segment) { (realm, options, url) =>
        val rr = ResizeRequest(realm, options, url)
        proxyResizedImage(rr)
      }
    } ~
    delete {
      path(Segment / "i" / Segment) { (realm, url) =>
        val pr = PurgeRequest(realm, url)
        purgeImage(pr)
      }
    }
  }

  def proxyResizedImage(rr: ResizeRequest)(ctx: RequestContext): Future[Unit] = {
    if(!rr.dir.exists) { rr.dir.mkdirs }

    if (rr.sizedImageFile.exists) {
      println(s"Cache HIT: $rr")
      Future.successful {
        respondWithMediaType(`image/jpeg`) { getFromFile(rr.sizedImageFile) }.apply(ctx)
      }
    } else {
      println(s"Cache MISS: $rr")
      val future = resizerActor ? rr
      future.map { response =>

        println(response)
        val forClient = response match {
          case ResizerResponse(true, None) => getFromFile(rr.sizedImageFile)
          case ResizerResponse(false, Some(error)) => complete(InternalServerError, error)
          case _ => complete(BadRequest, "dont know what to do")
        }

        respondWithMediaType(`image/jpeg`) { forClient }.apply(ctx)
      }
    }
  }

  def purgeImage(pr: PurgeRequest)(ctx: RequestContext): Future[Boolean] = {
    Future {
      val dirExists = pr.dir.exists
      if (pr.dir.exists) { FileUtils.forceDelete(pr.dir) }
      dirExists
    }
  }
}
