package controllers

import javax.inject._

import play.api.mvc._
import models._
import play._
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.json._
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() (recordOps: RecordOps, cc: ControllerComponents)(implicit assetsFinder: AssetsFinder)
  extends Authentication(cc) {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Redirect("/app/index.html")
  }

  import java.time._
  case class CellData(v: String, cellClassName: String)
  case class RowData(date: LocalDateTime, cellData: Seq[CellData])
  case class DataTab(columnNames: Seq[String], rows: Seq[RowData])

}
