package com.sgw.service

import java.util.concurrent.TimeUnit

import akka.event.slf4j.SLF4JLogging
import com.sgw.service.config.Configuration
import com.sgw.service.domain.Notification
import dispatch.Defaults._
import dispatch._

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.parsing.json.JSON

/**
 * A test for the notification service. The service must be running for this test to succeed.
 */
object ServiceTest extends Configuration with SLF4JLogging {
  private val TestUserId = 1

  private def getByUserId(userId: Long) = {
    val req = url(s"http://$serviceHost:$servicePort/notifications/by_user/$userId")
    Http(req OK as.String)
  }

  private def create(notification: Notification) = {
    val notificationJSON =
      s"""{"user_id": ${notification.user_id},
         |"timestamp": ${notification.timestamp},
         |"message": "${notification.message}"}""".stripMargin

    log.info(notificationJSON)

    val req = url(s"http://localhost:8080/notifications")
    val post1 = req.POST
    val post2 = post1.setContentType("application/json", "UTF-8")
    val post3 = post2 << notificationJSON

    Http(post3 OK as.String)
  }

  private def deleteByUserId(userId: Long) = {
    val req = url(s"http://localhost:8080/notifications/by_user/$userId")
    val delete = req.DELETE

    Http(delete OK as.String)
  }

  private def error(msg: String) = throw new RuntimeException(msg)

  private def jsonToNotification(noteMap: Map[String, Any]): Notification = {
    val id = noteMap.getOrElse("id", error("no id")).asInstanceOf[Double].toLong
    val user_id = noteMap.getOrElse("user_id", error("no user_id")).asInstanceOf[Double].toLong
    val timestamp = noteMap.getOrElse("timestamp", error("no timestamp")).asInstanceOf[Double].toLong
    val message = noteMap.getOrElse("message", error("no message")).asInstanceOf[String]

    Notification(Some(id), user_id, timestamp, message)
  }

  def main(args: Array[String]): Unit = try {
    val now = System.currentTimeMillis()

    // delete all of the test records (from any previous runs)
    deleteByUserId(1)

    // create a few test notifications
    val inputNotifications = (0 until 5).map(i => Notification(None, TestUserId, now + i, "Message" + i))

    // create the notifications through the service
    val createdNotifications = inputNotifications.map(create)
      .map(fut => Await.result(fut, Duration(30, TimeUnit.SECONDS)))
      .flatMap(json => JSON.parseFull(json))
      .map(_.asInstanceOf[Map[String, Any]])
      .map(jsonToNotification)
      .sortWith((n1, n2) => n1.timestamp > n2.timestamp)

    // the created notification should be the same size as the input notifications
    if (createdNotifications.size != inputNotifications.size) {
      error("the size of the created notifications list (" +
        createdNotifications.size +
        ") differs from the size of the input notifications list (" +
        inputNotifications.size
      )
    }

    // get the notifications by user id
    val fetchedNotifications = JSON.parseFull(
      Await.result(getByUserId(TestUserId), Duration(30, TimeUnit.SECONDS))
    ).map(_.asInstanceOf[Seq[Map[String, Any]]])
      .map(arr => arr.map(jsonToNotification))
      .getOrElse(error(s"Failed to get notifications for user id $TestUserId"))

    // the list of fetched notifications should be the same size as the list of created notifications
    if (fetchedNotifications.size != createdNotifications.size) {
      error("the size of the fetched notifications list (" +
        fetchedNotifications.size +
        ") differs from the size of the created notifications list (" +
        createdNotifications.size
      )
    }

    fetchedNotifications.zip(createdNotifications).foreach {
      case (actualNotification, expectedNotification) => if (actualNotification != expectedNotification) {
        error("The actual and expected notifications don't match. actual=" +
          actualNotification + ", expected=" + expectedNotification)
      }
    }

    Thread.sleep(2000)

    log.info("Success!")
  } finally {
    System.exit(0)
  }
}
