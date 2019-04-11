package models
import javax.inject.{ Inject, Singleton }
import play.api._
import scala.concurrent._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

case class User(_id: String, password: String, name: String, var _seq: Double = 0) {
  def getNextSeq = {
    val ret = _seq
    _seq += 1024
    ret
  }
}

object User {
  import play.api.libs.json._
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  import org.mongodb.scala.model._
  import org.mongodb.scala.model.Indexes._
  import org.mongodb.scala.bson._

  implicit val reads = Json.reads[User]
  val codecRegistry = fromRegistries(fromProviders(classOf[User]), DEFAULT_CODEC_REGISTRY)

  val ColName = "user"
  val defaultUsers = Seq(
    User("user", "abc123", "古煥文"),
    User("karateboy", "abc123", "karateboy"))

}

@Singleton
class UserDB @Inject() (mongoDB: MongoDB) {
  import User._
  import MongoDB._
  import ModelHelper._

  if (!mongoDB.colNames.contains(ColName)) {
    val f = mongoDB.database.createCollection(ColName).toFuture()
    f.onComplete(completeHandler)
    waitReadyResult(f)

    val f2 = collection.insertMany(defaultUsers).toFuture()
    f2.onComplete(completeHandler)
    waitReadyResult(f2)
  }

  lazy val collection = mongoDB.database.getCollection[User](ColName).withCodecRegistry(codecRegistry)

  import org.mongodb.scala.model._
  def get(_id: String) = {
    val f = collection.find(Filters.eq("_id", _id)).first().toFuture()
    f.onComplete(completeHandler)
    f
  }

  def upsert(user: User) = {
    val f = collection.replaceOne(Filters.eq("_id", user._id), user, UpdateOptions().upsert(true)).toFuture()
    f.onComplete(completeHandler)
    f
  }

  def delete(_id: String) = {
    val f = collection.deleteOne(Filters.eq("_id", _id)).toFuture()
    f.onComplete(completeHandler)
    f
  }
}