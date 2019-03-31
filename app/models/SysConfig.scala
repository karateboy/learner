package models
import javax.inject.{ Inject, Singleton }
import play.api.libs.json._
import models.ModelHelper._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import org.mongodb.scala.model._
import org.mongodb.scala.bson._

@Singleton
class SysConfig @Inject() (mongoDB: MongoDB) {
  val ColName = "sysConfig"
  val collection = mongoDB.database.getCollection(ColName)

  val valueKey = "value"
  val ImportBuildCase = 'ImportBuildCase
  val Test = 'Test
  val defaultConfig = Map(
    ImportBuildCase -> Document(valueKey -> true),
    Test->Document(valueKey->true))

  def init() {
    if (!mongoDB.colNames.contains(ColName)) {
      val f = mongoDB.database.createCollection(ColName).toFuture()
      f.onComplete(completeHandler)
    }

    val idSet = defaultConfig.keys map { _.toString() }
    //Clean up unused
    val f1 = collection.deleteMany(Filters.not(Filters.in("_id", idSet.toList: _*))).toFuture()
    f1.onComplete(completeHandler)
    val updateModels =
      for ((k, defaultDoc) <- defaultConfig) yield {
        UpdateOneModel(
          Filters.eq("_id", k.toString()),
          Updates.setOnInsert(valueKey, defaultDoc(valueKey)), UpdateOptions().upsert(true))
      }

    val f2 = collection.bulkWrite(updateModels.toList, BulkWriteOptions().ordered(false)).toFuture()

    import scala.concurrent._
    val f = Future.sequence(List(f1, f2))
    waitReadyResult(f)
  }

  def upsert(_id: Symbol, doc: Document) = {
    val uo = new UpdateOptions().upsert(true)
    val f = collection.replaceOne(Filters.equal("_id", _id.toString()), doc, uo).toFuture()
    f.onComplete(completeHandler)
    f
  }

  def get(_id: Symbol) = {
    val f = collection.find(Filters.eq("_id", _id.toString())).headOption()
    f.onComplete(completeHandler)
    for (ret <- f) yield {
      val doc = ret.getOrElse(defaultConfig(_id))
      doc("value")
    }
  }

  def set(_id: Symbol, v: BsonValue) = upsert(_id, Document(valueKey -> v))
}