package com.octojon

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import spray.can.Http

import com.typesafe.config._

object Boot extends App {

  implicit val system = ActorSystem("resizer")
  implicit val timeout = Timeout(5.seconds)
  val conf = ConfigFactory.load()

  val resizerApi = system.actorOf(Props[ResizerAPIActor], "resizer-api")

  // FIXME panic if configuration is missing
  println(s"Caching to ${conf.getString("cachePath")}")
  println(s"Routing images for ${conf.getConfig("realmMapper")}")

  IO(Http) ? Http.Bind(resizerApi, interface = "localhost", port = 8080)
}
