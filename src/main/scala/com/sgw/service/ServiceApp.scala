package com.sgw.service

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.sgw.service.config.Configuration
import com.sgw.service.rest.NotificationRestServiceActor
import spray.can.Http

/**
 * A web service that managers tweet-like "notifications" using Scala, Spray, Slick, and Akka.
 *
 * Run this app to start the http server that runs the notification service.
 *
 * Patterned after Oleg Yermolaiev's "Building REST Service with Scala" blog post at:
 *
 * http://sysgears.com/articles/building-rest-service-with-scala/
 * https://github.com/oermolaev/simple-scala-rest-example
 */
object ServiceApp extends App with Configuration {

  // create an actor system for application
  implicit val system = ActorSystem("notification-service")

  // create and start rest service actor
  val restService = system.actorOf(Props[NotificationRestServiceActor], "rest-endpoint")

  // start HTTP server with rest service actor as a handler
  IO(Http) ! Http.Bind(restService, serviceHost, servicePort)
}
