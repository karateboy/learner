package controllers

import javax.inject._

import akka.actor.ActorSystem
import play.api.mvc._
import play.api._
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future, Promise }
import models._
import play.api.libs.json._

/**
 * This controller creates an `Action` that demonstrates how to write
 * simple asynchronous code in a controller. It uses a timer to
 * asynchronously delay sending a response for 1 second.
 *
 * @param cc standard controller components
 * @param actorSystem We need the `ActorSystem`'s `Scheduler` to
 * run code after a delay.
 * @param exec We need an `ExecutionContext` to execute our
 * asynchronous code.  When rendering content, you should use Play's
 * default execution context, which is dependency injected.  If you are
 * using blocking operations, such as database or network access, then you should
 * use a different custom execution context that has a thread pool configured for
 * a blocking API.
 */
@Singleton
class AsyncController @Inject() (cc: ControllerComponents, actorSystem: ActorSystem, landLaw: LandLaw, photo: Photo, userOp: UserDB)(implicit exec: ExecutionContext) extends Authentication(cc) {

  /**
   * Creates an Action that returns a plain text message after a delay
   * of 1 second.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/message`.
   */
  def message = Action.async {
    getFutureMessage(1.second).map { msg => Ok(msg) }
  }

  private def getFutureMessage(delayTime: FiniteDuration): Future[String] = {
    val promise: Promise[String] = Promise[String]()
    actorSystem.scheduler.scheduleOnce(delayTime) {
      promise.success("Hi!")
    }(actorSystem.dispatcher) // run scheduled tasks using the actor system's dispatcher
    promise.future
  }

  def getLandLaw(offset: Int) = Authenticated.async {
    implicit request =>
      val userInfo = request.user
      import LandLaw._
      val studyF = landLaw.query(QueryParam(userInfo.id))(offset, 1)
      val userF = userOp.get(userInfo.id)
      for {
        studies <- studyF
        user <- userF
      } yield {
        import Study._
        if (studies.isEmpty){
          val seq = user.getNextSeq
          userOp.upsert(user)
          Ok(Json.toJson(Study.emptyStudy(userInfo.id, user.getNextSeq)))
        }else
          Ok(Json.toJson(studies(0)))
      }
  }

  def getLandLawCount = Authenticated.async {
    implicit request =>
      val userInfo = request.user
      import LandLaw._
      val countF = landLaw.count(QueryParam(userInfo.id))
      for (count <- countF) yield {
        Ok(Json.obj("count" -> count))
      }
  }

  def upsertLandLaw = Authenticated.async(parse.json) {
    implicit request =>
      import Study._
      val ret = request.body.validate[Study]
      ret.fold(
        error => {
          Logger.error(JsError.toJson(error).toString())
          Future { BadRequest(Json.obj("ok" -> false, "msg" -> JsError.toJson(error).toString())) }
        },
        studyCase => {
          val f = landLaw.upsert(studyCase)
          for (ret <- f) yield {
            Ok(Json.obj("ok" -> ret.getModifiedCount))
          }
        })
  }
  def delLandLaw(id: String) = Authenticated.async {
    import LandLaw._
    import org.mongodb.scala.bson._
    val _id = new ObjectId(id)
    val f = landLaw.delete(_id)
    for (ret <- f) yield {
      Ok(Json.obj("count" -> ret.getDeletedCount))
    }
  }

  import scala.concurrent._
  import java.nio.file._
  def uploadPhoto = Authenticated.async(parse.multipartFormData) {
    implicit request =>
      import org.mongodb.scala.model._
      import org.mongodb.scala.bson._

      val seqF =
        request.body.files map { upload =>
          val filename = upload.filename
          val contentType = upload.contentType
          val path = upload.ref.path
          val imageData = Files.readAllBytes(path);

          val photoCase = PhotoCase(new ObjectId(), imageData)
          for (ret <- photo.insert(photoCase)) yield photoCase._id
        }
      val f = Future.sequence(seqF)
      for (objIds <- f) yield {
        import ObjectIdUtil._
        Ok(Json.toJson(objIds))
      }
  }

  def getPhoto(id: String) = Authenticated.async {
    import org.mongodb.scala.bson._
    val noPhotoId = new ObjectId(Photo.noPhotoID)
    val objId = new ObjectId(id)
    if (objId == noPhotoId) {
      Future {
        NoContent
      }
    } else {
      val f = photo.getPhoto(new ObjectId(id))
      for (ret <- f) yield {
        Ok(ret.image).as("image/jpeg")
      }
    }
  }
}
