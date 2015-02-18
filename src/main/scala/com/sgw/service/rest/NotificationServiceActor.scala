package com.sgw.service.rest

import akka.actor.Actor
import akka.event.slf4j.SLF4JLogging
import com.sgw.service.dao.NotificationDAO
import com.sgw.service.domain._
import net.liftweb.json.Serialization._
import net.liftweb.json.DefaultFormats
import spray.http._
import spray.httpx.unmarshalling.Unmarshaller
import spray.routing._

/**
 * Notification REST Service actor.
 */
class NotificationRestServiceActor extends Actor with NotificationRestService {

  implicit def actorRefFactory = context

  def receive = runRoute(rest)
}

/**
 * REST Service
 */
trait NotificationRestService extends HttpService with SLF4JLogging {

  val notificationDAO = new NotificationDAO

  implicit val executionContext = actorRefFactory.dispatcher

  implicit val liftJsonFormats = DefaultFormats

  implicit val notificationRejectionHandler = RejectionHandler {
    case rejections => mapHttpResponse {
      response =>
        response.withEntity(HttpEntity(ContentType(MediaTypes.`application/json`),
          write(Map("error" -> response.entity.asString))))
    } {
      RejectionHandler.Default(rejections)
    }
  }

  val rest = respondWithMediaType(MediaTypes.`application/json`) {
    path("notifications" / "by_user" / LongNumber) {
      case userId =>
        get {
          ctx: RequestContext => handleRequest(ctx) {
            log.debug(s"Searching for notifications with user id $userId.")
            notificationDAO.getByUserid(userId, 0, 100)
          }
        } ~
        delete {
          ctx: RequestContext => handleRequest(ctx) {
            log.debug(s"Deleting notification with user id $userId.")
            notificationDAO.deleteByUserId(userId)
          }
        }
    } ~
    path("notifications") {
      post {
        entity(Unmarshaller(MediaTypes.`application/json`) {
          case httpEntity: HttpEntity =>
            read[Notification](httpEntity.asString(HttpCharsets.`UTF-8`))
        }) {
          notification: Notification =>
            ctx: RequestContext =>
              handleRequest(ctx, StatusCodes.Created) {
                log.debug("Creating notification: %s".format(notification))
                notificationDAO.create(notification)
              }
        }
      }
    } ~
    path("notifications" / LongNumber) {
      case notificationId =>
        put {
          entity(Unmarshaller(MediaTypes.`application/json`) {
            case httpEntity: HttpEntity =>
              read[Notification](httpEntity.asString(HttpCharsets.`UTF-8`))
          }) {
            notification: Notification =>
              ctx: RequestContext =>
                handleRequest(ctx) {
                  log.debug("Updating notification with id %d: %s".format(notificationId, notification))
                  notificationDAO.update(notificationId, notification)
                }
          }
        } ~
        delete {
          ctx: RequestContext =>
            handleRequest(ctx) {
              log.debug("Deleting notification with id %d".format(notificationId))
              notificationDAO.delete(notificationId)
            }
        } ~
        get {
          ctx: RequestContext =>
            handleRequest(ctx) {
              log.debug("Retrieving notification with id %d".format(notificationId))
              notificationDAO.get(notificationId)
            }
        }
    }
  }

  /**
   * Handles an incoming request and create valid response for it.
   *
   * @param ctx         request context
   * @param successCode HTTP Status code for success
   * @param action      action to perform
   */
  protected def handleRequest(ctx: RequestContext, successCode: StatusCode = StatusCodes.OK)(action: => Either[Failure, _]) {
    action match {
      case Right(result: Object) =>
        ctx.complete(successCode, write(result))
      case Left(error: Failure) =>
        ctx.complete(error.getStatusCode, net.liftweb.json.Serialization.write(Map("error" -> error.message)))
      case _ =>
        ctx.complete(StatusCodes.InternalServerError)
    }
  }
}
