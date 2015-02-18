package com.sgw.service.dao

import com.sgw.service.domain.Notification
import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}

/**
 * Unit tests for the NotificationDAO.
 */
class NotificationDAOSpec extends FlatSpec with Matchers with BeforeAndAfter {
  private val TestUserId = -1

  private val dao = new NotificationDAO

  private def cleanUpDB(): Unit = {
    dao.deleteByUserId(TestUserId)
    dao.countByUserId(TestUserId) should be (Right(0))
  }

  before {
    cleanUpDB()
  }

  after {
    cleanUpDB()
  }

  "The NotificationDAO" should "be able to create, query, and update a set of notifications" in {
    val now = System.currentTimeMillis()

    // create a few test notifications
    val inputNotifications = (0 until 5).map(i => Notification(None, TestUserId, now + i, "Message" + i))

    // create the notifications from the test notifications and sort them decending by time
    val expectedNotifications = inputNotifications.map(dao.create).map {
      case Right(actualNotification) => actualNotification
      case Left(failure) => fail(failure.message)
    } sortWith((n1, n2) => n1.timestamp > n2.timestamp)

    // should get back a set of actual notifications that are the same size as the input notifications
    expectedNotifications.size should be (inputNotifications.size)

    // also do a count
    dao.countByUserId(TestUserId) should be (Right(5))

    // get all of the notifications for the test user id
    dao.getByUserid(TestUserId, 0, 100) match {
      case Right(actualNotifications) => {
        actualNotifications.size should be (expectedNotifications.size)

        actualNotifications.zip(expectedNotifications).foreach {
          case (actualNotification, expectedNotification) => actualNotification should be (expectedNotification)
        }
      }
      case Left(failure) => fail(failure.message)
    }

    // get a few of the notifications for the test user id
    dao.getByUserid(TestUserId, 1, 3) match {
      case Right(actualNotifications) => {
        actualNotifications.size should be (3)

        actualNotifications(0) should be (expectedNotifications(1))
        actualNotifications(1) should be (expectedNotifications(2))
        actualNotifications(2) should be (expectedNotifications(3))
      }
      case Left(failure) => fail(failure.message)
    }

    // get one of the notifications
    val oldNotification = expectedNotifications.head

    // update its message
    dao.update(oldNotification.id.get, oldNotification.copy(message = "Changed!")) match {
      case Right(newNotification) => newNotification.message should be ("Changed!")
      case Left(failure) => fail(failure.message)
    }
  }
}
