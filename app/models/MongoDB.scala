package models
import play.api._
import javax.inject.{ Inject, Singleton }
import com.typesafe.config._
import play.api.inject.ApplicationLifecycle
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
object MongoDB {
  import play.api.libs.json._
  import org.mongodb.scala.bson._
  implicit val objWrites = new Writes[ObjectId] {
    def writes(id: ObjectId) = JsString(id.toHexString())
  }

  implicit val objReads: Reads[ObjectId] = new Reads[ObjectId] {
    def reads(json: JsValue): JsResult[ObjectId] = {
      val ret = json.validate[String]
      ret.fold(
        err => {
          JsError(err)
        },
        hexStr => {
          JsSuccess(new ObjectId(hexStr))
        })
    }
  }

  implicit val docWrites: Writes[Document] = new Writes[Document] {
    def writes(v: Document): JsValue = Json.parse(v.toJson())
  }

}

@Singleton
class MongoDB @Inject() (config: Configuration, appLifecycle: ApplicationLifecycle) {
  import org.mongodb.scala._

  val url = config.get[String]("my.mongodb.url")
  val dbName = config.get[String]("my.mongodb.db")

  Logger.info(s"MongoDB url= $url")
  Logger.info(s"DB name=$dbName")

  val mongoClient: MongoClient = MongoClient(url)
  val database: MongoDatabase = mongoClient.getDatabase(dbName);
  val colNames = {
    val f = database.listCollectionNames().toFuture()
    import ModelHelper._
    waitReadyResult(f)
  }

  appLifecycle.addStopHook { () =>
    Logger.info("Stopping mongoDB Client")
    mongoClient.close()
    Future.successful(())
  }
}