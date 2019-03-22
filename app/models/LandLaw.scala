package models
import play.api._
import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.implicitConversions
import scala.collection.JavaConverters._
import ModelHelper._
import play.api.libs.json._
case class Law(_id: String, module: String, chapter: String, no: String, content: String, terms: Seq[String])
case class QueryParam(sortBy: String = "no+")

@Singleton
class LandLaw @Inject() (mongoDB: MongoDB) {

  

  import org.mongodb.scala.bson.codecs.Macros._
  import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
  import org.bson.codecs.configuration.CodecRegistries.{ fromRegistries, fromProviders }
  import org.mongodb.scala.model._
  import org.mongodb.scala.model.Indexes._
  import org.mongodb.scala.bson._

  val codecRegistry = fromRegistries(fromProviders(classOf[Law]), DEFAULT_CODEC_REGISTRY)

  val ColName = "landLaw"
  val collection = mongoDB.database.getCollection[Law](ColName).withCodecRegistry(codecRegistry)

  def init() {
    if (!mongoDB.colNames.contains(ColName)) {
      val f = mongoDB.database.createCollection(ColName).toFuture()
      waitReadyResult(f)      
      //val cif = collection.createIndex(Indexes.ascending("no")).toFuture()
      //waitReadyResult(cif)
      importLaw
    }
  }
  
  init
  
  def importLaw() = {
    import org.jsoup._

    val doc = Jsoup.connect("https://law.moj.gov.tw/LawClass/LawAll.aspx?pcode=D0060001").get();
    val tags = doc.select("div#pnLawFla").select(".law-reg-content").asScala
    var module = ""
    var chapter = ""
    var laws = Seq.empty[Law]
    for (div <- tags) {
      for (ele <- div.children().asScala) {
        ele.className() match {
          case "h3 char-1" =>
            module = ele.text()
          case "h3 char-2" =>
            chapter = ele.text()
          case "row" =>
            val col_no = ele.select(".col-no").select("a").text()
            val content = ele.select(".col-data").text()
            val law = Law(col_no, module, chapter, col_no, content, Seq.empty[String])
            laws = laws :+ (law)
        }
      }
    }
    Logger.info(s"total ${laws.size} laws")
    val f = collection.insertMany(laws).toFuture()
    waitReadyResult(f)
  }

  import org.mongodb.scala.model._
  import scala.concurrent._
  def getSortBy(param: QueryParam) = {
    import org.mongodb.scala.model.Sorts.ascending
    val sortByField = param.sortBy.takeWhile { x => !(x == '+' || x == '-') }
    val dir = param.sortBy.contains("+")

    val firstSort =
      if (dir)
        Sorts.ascending(sortByField)
      else
        Sorts.descending(sortByField)

    val secondSort = Sorts.descending("siteInfo.area")
    if (sortByField != "siteInfo.area")
      Sorts.orderBy(firstSort, secondSort)
    else
      firstSort
  }

  def getFilter(param: QueryParam) = {
    import org.mongodb.scala.model.Filters._

    /*
     * case class QueryBuildCase2Param(
  			county: Option[String],
  			builder: Option[String],
  			architect: Option[String],
  			areaGT: Option[Double], areaLT: Option[Double],
  			addr: Option[String],
  			tag: Option[Seq[String]],
  			state: Option[String],
  			sales: Option[String],
  			sortBy: Option[String])
    */
    /*
    import org.mongodb.scala.bson.conversions._
    val keywordFilter: Option[Bson] = param.keyword map {
      keyword =>
        val countyFilter = regex("_id.county", "(?i)" + keyword)
        val permitIdFilter = regex("_id.permitID", "(?i)" + keyword)
        val builderFilter = regex("builder", "(?i)" + keyword)
        val architectFilter = regex("architect", "(?i)" + keyword)
        val addrFilter = regex("siteInfo.addr", "(?i)" + keyword)
        or(countyFilter, permitIdFilter, builderFilter, architectFilter, addrFilter)
    }

    val areaGtFilter = param.areaGT map { v => Filters.gt("siteInfo.area", v) }
    val areaLtFilter = param.areaLT map { v => Filters.lt("siteInfo.area", v) }
    val stateFilter = param.state map { v => Filters.eq("state", v) }
    val ownerFilter = param.owner map { sales => regex("owner", "(?i)" + sales) }
    val hasFormFilter = param.hasForm map { has =>
      if (has)
        Filters.ne("form", null)
      else
        Filters.eq("form", null)
    }

    val filterList = List(areaGtFilter, areaLtFilter, stateFilter, ownerFilter, keywordFilter, hasFormFilter).flatMap { f => f }

    val filter = if (!filterList.isEmpty)
      and(filterList: _*)
    else
      Filters.exists("_id")
	  */

    Filters.exists("_id")
  }

  def query(param: QueryParam)(skip: Int, limit: Int): Future[Seq[Law]] = {
    val filter = getFilter(param)
    val sortBy = getSortBy(param)
    query(filter)(sortBy)(skip, limit)
  }

  import org.mongodb.scala.bson.conversions.Bson
  def query(filter: Bson)(sortBy: Bson = Sorts.descending("siteInfo.area"))(skip: Int, limit: Int) = {
    val f = collection.find((filter)).skip(skip).limit(limit).toFuture()
    f.onFailure(errorHandler)
    f
  }

  def count(param: QueryParam): Future[Long] = count(getFilter(param))
  def count(filter: Bson) = {
    val f = collection.count((filter)).toFuture()
    f.onFailure(errorHandler)
    f
  }
}