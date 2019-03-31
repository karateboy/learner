package models
import scala.language.implicitConversions
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api._
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author user
 */

object ModelHelper {
  import org.mongodb.scala.bson.BsonDateTime

  def logException(ex: Throwable) = {
    Logger.error(ex.getMessage, ex)
  }

  def errorHandler: PartialFunction[Throwable, Any] = {
    case ex: Throwable =>
      Logger.error("Error=>", ex)
      throw ex
  }

  def errorHandler(prompt: String = "Error=>"): PartialFunction[Throwable, Any] = {
    case ex: org.mongodb.scala.MongoException =>
      Logger.error(ex.getMessage)

    case ex: Throwable =>
      Logger.error(prompt, ex)
      throw ex
  }

  import scala.concurrent._
  import scala.util._
  def completeHandler[T](t: Try[T]) = {
    t match {
      case Success(ret)=> //Ignore...
      case Failure(ex) => Logger.error("Error=>", ex)
    }
  }

  def waitReadyResult[T](f: Future[T], ignoreError: Boolean = false) = {
    import scala.concurrent.duration._
    import scala.util._

    val ret = Await.ready(f, Duration.Inf).value.get

    ret match {
      case Success(t) =>
        t
      case Failure(ex) =>
        if (!ignoreError) {
          Logger.error(ex.getMessage, ex)
        }
        throw ex
    }
  }

  import org.mongodb.scala.bson._
  def getOptionTime(key: String)(implicit doc: Document) = {
    if (doc.get(key).isEmpty || doc(key).isNull())
      None
    else
      Some(doc(key).asInt64().getValue)
  }

  def getOptionStr(key: String)(implicit doc: Document) = {
    if (doc.get(key).isEmpty || doc(key).isNull())
      None
    else
      Some(doc.getString(key))
  }

  def getOptionDouble(key: String)(implicit doc: Document) = {
    if (doc.get(key).isEmpty || doc(key).isNull())
      None
    else
      Some(doc(key).asDouble().getValue)
  }

  def getOptionInt(key: String)(implicit doc: Document) = {
    if (doc.get(key).isEmpty || doc(key).isNull())
      None
    else
      Some(doc(key).asInt32().getValue)
  }

  def getOptionDoc(key: String)(implicit doc: Document) = {
    if (doc.get(key).isEmpty || doc(key).isNull())
      None
    else
      Some(doc(key).asDocument())
  }

  def getArray[T](key: String, mapper: (BsonValue) => T)(implicit doc: Document) = {

    val array = doc(key).asArray().getValues

    val result = array.asScala map {
      v => mapper(v)
    }
    result.toSeq
  }

  def getOptionArray[T](key: String, mapper: (BsonValue) => T)(implicit doc: Document) = {
    if (doc.get(key).isEmpty || doc(key).isNull())
      None
    else
      Some(getArray(key, mapper))
  }

  def isVaildPhone(phone: String) =
    phone.forall { x => x.isDigit || x == '-' || x == '(' || x == ')' || x.isSpaceChar } && !phone.isEmpty()
}

object EnumUtils {
  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(enum.withName(s))
        } catch {
          case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
        }
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }
}

import org.mongodb.scala.bson.ObjectId
object ObjectIdUtil {
  //  def objectIdReads: Reads[ObjectId] = new Reads[ObjectId] {
  //    def reads(json: JsValue): JsResult[ObjectId] = json match {
  //      case JsString(s) => {
  //        try {
  //          JsSuccess(new ObjectId(s))
  //        } catch {
  //          case _: NoSuchElementException => JsError(s"unexpected ObjectId")
  //        }
  //      }
  //      case _ => JsError("String value expected")
  //    }
  //  }
  //
  //  implicit def objectIdOptReads: Reads[Option[ObjectId]] = new Reads[Option[ObjectId]] {
  //    def reads(json: JsValue): JsResult[Option[ObjectId]] = json match {
  //      case JsString(s) => {
  //        try {
  //          JsSuccess(Some(new ObjectId(s)))
  //        } catch {
  //          case _: Throwable =>
  //            JsSuccess(None)
  //        }
  //      }
  //      case _ => JsSuccess(None)
  //    }
  //  }
  implicit def objectWrites: Writes[ObjectId] = new Writes[ObjectId] {
    def writes(v: ObjectId): JsValue = JsString(v.toHexString)
  }
}
