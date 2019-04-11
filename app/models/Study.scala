package models
import play.api._
import javax.inject.{ Inject, Singleton }
import org.mongodb.scala.bson._
import play.api.libs.json._

case class KeyWord(key: String, url: Option[String])
case class Study(_id: ObjectId, owner: String, seq: Double, content: String, keywords: Seq[KeyWord])

object Study {
  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  import org.mongodb.scala.model._
  import org.mongodb.scala.model.Indexes._
  import org.mongodb.scala.bson._
  import MongoDB._

  implicit val keyWrites = Json.writes[KeyWord]
  implicit val keyReads = Json.reads[KeyWord]
  implicit val studyWrites = Json.writes[Study]
  implicit val studyReads = Json.reads[Study]

  def emptyStudy(userId: String, seq: Double) = Study(
    new ObjectId(),
    owner = userId,
    seq = seq,
    content = "",
    keywords = Seq.empty[KeyWord])

}