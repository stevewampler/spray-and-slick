package com.sgw.service.domain

import scala.slick.driver.MySQLDriver.simple._

/**
 * Notification entity.
 *
 * @param id        unique id
 * @param user_id   the user id of the user who sent the notification
 * @param timestamp the time, in millis since the epoch, that the notification was sent
 * @param message   the notification's message
 */
case class Notification(id: Option[Long], user_id: Long, timestamp: Long, message: String)

/**
 * Mapped notification table object.
 */
object Notifications extends Table[Notification]("notifications") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def user_id = column[Long]("user_id")

  def timestamp = column[Long]("timestamp")

  def message = column[String]("message")

  def * = id.? ~ user_id ~ timestamp ~ message <>(Notification, Notification.unapply _)

  def idx = index("idx_user_id", (user_id))

  val findById = for {
    id <- Parameters[Long]
    notification <- this if notification.id is id
  } yield notification

  val findByUserId = for {
    user_id <- Parameters[Long]
    notification <- this if notification.user_id is user_id
  } yield notification
}
