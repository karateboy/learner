package models
import play.api._
import play.api.libs.json._
import models.ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import javax.inject.{ Inject, Singleton }
import org.mongodb.scala.model._
import org.mongodb.scala.bson._
import MongoDB._

case class PhotoCase(_id: ObjectId, image: Array[Byte])
object Photo {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  import org.mongodb.scala.bson.conversions._

  val ColName = "photo"
  val noPhotoID = "000000000000000000000000"

  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }

  val codecRegistry = fromRegistries(fromProviders(classOf[PhotoCase]), DEFAULT_CODEC_REGISTRY)

}

@Singleton
class Photo @Inject() (mongoDB: MongoDB) {
  import Photo._
  lazy private val collection = mongoDB.database.getCollection[PhotoCase](ColName).withCodecRegistry(codecRegistry)

  if (!mongoDB.colNames.contains(ColName)) {
    val f = mongoDB.database.createCollection(ColName).toFuture()
    f.onComplete(completeHandler)
    waitReadyResult(f)
  }

  def getPhoto(objID: ObjectId) = {
    val f = collection.find(Filters.eq("_id", objID)).first().toFuture()
    f.onComplete(completeHandler)
    f
  }

  def updatePhoto(photo: PhotoCase) = {
    val f = collection.updateOne(Filters.eq("_id", photo._id), Updates.set("image", photo.image)).toFuture()
    f.onComplete(completeHandler)
    f
  }

  def insert(photo: PhotoCase) = {
    val f = collection.insertOne(photo).toFuture()
    f.onComplete(completeHandler)
    f
  }

}