package com.byteforscher

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

// See // See http://stackoverflow.com/questions/21151234/how-do-i-read-port-configuration-for-my-spray-can
// See http://stackoverflow.com/questions/21151234/how-do-i-read-port-configuration-for-my-spray-can
import com.typesafe.config._

object Boot extends App {

  // we need an ActorSystem to host our application in
  implicit val system = ActorSystem("on-spray-can")

  implicit val conf = ConfigFactory.load()

  // create and start our service actor
  val service = system.actorOf(Props[MyServiceActor], "demo-service")

  implicit val timeout = Timeout(5.seconds)
  // start a new HTTP server on port 8080 with our service actor as the handler
  IO(Http) ? Http.Bind(service, interface = "localhost", port = 8080)
}
