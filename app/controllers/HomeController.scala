package controllers

import models._
import javax.inject._
import play.api.mvc._
import java.io.File
import play.api.data.Form
import java.nio.file.Paths
import scala.io.Source
import scala.collection.mutable
import scala.collection.immutable.ListMap
import play.api.libs.json._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents,
                               val listing : ListingsController,
                               val contact : ContactsController)
                              (implicit assetsFinder: AssetsFinder)
  extends AbstractController(cc) {

  implicit val maxContactFormat = Json.format[MaxContact]
  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def upload = Action(parse.multipartFormData) { request =>
    request.body
      .file("csvUpload")
      .map { csvUpload =>
        // only get the last part of the filename
        // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
        val filename = Paths.get(csvUpload.filename).getFileName
        val fileSize = csvUpload.fileSize
        val contentType = csvUpload.contentType
        csvUpload.ref.copyTo(Paths.get(s"/tmp/files/$filename"), replace = true)

        val fileSource = Source.fromFile(s"/tmp/files/$filename")
        val firstLine = fileSource.getLines().take(1).toList

        val headerLine = firstLine(0).split(",").map(_.trim)
        headerLine.length match {
          case 5 => {
            listing.processListing(fileSource)
          }
          case 2 => {
            contact.processContacts(fileSource)
          }
          case _ =>
        }
        fileSource.close()
        Ok(views.html.index("Your new application is ready."))
      }
      .getOrElse {
        Redirect(routes.HomeController.index()).flashing("error" -> "Missing file")
      }
  }

  def getAggregateListing : Action[AnyContent] = Action {
    Ok("Aggregate Listing Based On Seller Type : " + Json.toJson(listing.aggregateListings(listing.listingsList)))
  }

  def getPercentageDistribution : Action[AnyContent] = Action {
    Ok("Percentage Distribution of Available Cars by Make : " + Json.toJson(listing.percentDistribution(listing.listingsList)))
  }

  def getAveragePriceMostContacted : Action[AnyContent] = Action {
    val listOfTopContacts : List[Int] = contact.averagePriceMostContacted()
    Ok("Average Price of 30% of most contacted listings : " + Json.toJson(listing.averagePriceOfMostContacted(listOfTopContacts)))
  }

  def getMaxContactedMonthly : Action[AnyContent] = Action {
    val checkMaxContacted : collection.mutable.Map[Int, ListMap[Int,Int]] = contact.checkMaxContactedByDate()
    Ok("Most contacted seller monthly : " + Json.toJson(listing.listMaxContactedByMonth(checkMaxContacted)))
  }

}