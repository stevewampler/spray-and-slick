package com.sgw.service.dao

import java.sql._

import com.sgw.service.config.Configuration
import com.sgw.service.domain._

import scala.slick.driver.MySQLDriver.simple.Database.threadLocalSession
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable

/**
 * Provides a DAO for Notification entities.
 */
class NotificationDAO extends Configuration {

  // init Database instance
  private val db = Database.forURL(url = s"jdbc:mysql://$dbHost:$dbPort/$dbName",
    user = dbUser, password = dbPassword, driver = "com.mysql.jdbc.Driver")

  // create tables if not exist
  db.withSession {
    if (MTable.getTables("notifications").list().isEmpty) {
      Notifications.ddl.create
    }
  }

  /**
   * Returns the number of notifications sent by the specified user id.
   *
   * @param userId the user id
   */
  def countByUserId(userId: Long): Either[Failure, Int] = {
    try {
      db.withSession {
        Right(Notifications.where(_.user_id === userId).length.run)
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Saves a notification entity into the database.
   *
   * @param notification notification entity to
   * @return saved notification entity
   */
  def create(notification: Notification): Either[Failure, Notification] = {
    try {
      val id = db.withSession {
        Notifications returning Notifications.id insert notification
      }
      Right(notification.copy(id = Some(id)))
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Updates a notification entity.
   *
   * @param id       id of the notification to update.
   * @param notification updated notification entity
   * @return updated notification entity
   */
  def update(id: Long, notification: Notification): Either[Failure, Notification] = {
    try
      db.withSession {
        Notifications.where(_.id === id) update notification.copy(id = Some(id)) match {
          case 0 => Left(notFoundError(id))
          case _ => Right(notification.copy(id = Some(id)))
        }
      }
    catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Deletes a notification from database.
   *
   * @param id id of the customer to delete
   * @return deleted customer entity
   */
  def delete(id: Long): Either[Failure, Notification] = {
    try {
      db.withTransaction {
        val query = Notifications.where(_.id === id)
        val notifications = query.run.asInstanceOf[Seq[Notification]]
        notifications.size match {
          case 0 =>
            Left(notFoundError(id))
          case _ => {
            query.delete
            Right(notifications.head)
          }
        }
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Deletes all of the notifications with the specified user id.
   *
   * @param userId the user id of the notifications to delete
   */
  def deleteByUserId(userId: Long): Either[Failure, Boolean] = {
    try {
      db.withSession {
        // build a query matching all of the user's notifications
        val query = for {
          notification <- Notifications if notification.user_id is userId
        } yield notification

        query.delete
        Right(true)
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Retrieves a specific notification from the database.
   *
   * @param id id of the notification to retrieve
   * @return notification entity with specified id
   */
  def get(id: Long): Either[Failure, Notification] = {
    try {
      db.withSession {
        Notifications.findById(id).firstOption match {
          case Some(notification: Notification) =>
            Right(notification)
          case _ =>
            Left(notFoundError(id))
        }
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Returns a list of Notification entities that match the specified user id.
   *
   * @param userId the user id of the notifications
   */
  def getByUserid(userId: Long, offset: Int, limit: Int): Either[Failure, List[Notification]] = {
    try {
      db.withSession {
        // build a query matching all of the user's notifications
        val query = for {
          notification <- Notifications if notification.user_id is userId
        } yield notification

        // add sort by time descending
        val sortedQuery = query.sortBy(_.timestamp.desc).drop(offset).take(limit)

        // run the query
        Right(sortedQuery.run.toList)
      }
    } catch {
      case e: SQLException =>
        Left(databaseError(e))
    }
  }

  /**
   * Produce a database error.
   *
   * @param e SQL Exception
   * @return database error description
   */
  protected def databaseError(e: SQLException) =
    Failure("%d: %s".format(e.getErrorCode, e.getMessage), FailureType.DatabaseFailure)

  /**
   * Produces a notification not found error.
   *
   * @param notificationId id of the notification
   * @return not found error description
   */
  protected def notFoundError(notificationId: Long) =
    Failure("Notification with id=%d does not exist".format(notificationId), FailureType.NotFound)
}
