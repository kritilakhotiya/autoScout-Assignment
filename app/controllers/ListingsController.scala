package controllers

import javax.inject._
import play.api.mvc._
import java.io.File
import play.api.data.Form
import java.nio.file.Paths
import scala.io.Source
import scala.collection.mutable
import scala.collection.immutable.ListMap
import models._
import play.api.libs.json._

class ListingsController @Inject()(
                                cc: ControllerComponents
                              ) extends AbstractController(cc) {

  implicit val listingFormat = Json.format[Listing]
  val listingsList = scala.collection.mutable.ListBuffer[Listing]()

  //Inserts each of the listings data into Listing class.
  def processListing(fileSource: scala.io.BufferedSource): Unit = {
    //Assuming the analytics is done only one listing file at a time
    if (!listingsList.nonEmpty) {
      for (line <- fileSource.getLines()) {
        var listing = line.split(",").map(_.trim).map(_.replaceAll("\"", ""))
        listingsList += Listing(listing {0}.toInt,
                                listing {1},
                                listing {2}.toInt,
                                listing {3}.toInt,
                                listing {4})
      }
    }
  }

  //Formats price in the format €#,-
  def formatPrice(price: Int): String = {
    val locale = new java.util.Locale("de", "DE")
    val formatter = java.text.NumberFormat.getIntegerInstance(locale)
    "€" + formatter.format(price) + ",-"
  }

  //Returns aggregate lists of price for each seller type
  def aggregateListings(lists: scala.collection.mutable.ListBuffer[Listing]): List[(String, String)] = {
    var dealer_price, dealer_count, private_price, private_count, other_price, other_count, other_avg, dealer_avg, private_avg = 0

    lists.map(listing => {
      listing.seller_type match {
        case "dealer" => {
          dealer_price += listing.price
          dealer_count += 1
        }
        case "private" => {
          private_price += listing.price
          private_count += 1
        }
        case "other" => {
          other_price += listing.price
          other_count += 1
        }
        case _ =>
      }
    })
    try {
        other_avg = other_price / other_count
        dealer_avg = dealer_price / dealer_count
        private_avg = private_price / private_count
    }
    catch{
      case _: Throwable =>
    }
    val aggregateBySellerType = List(("dealer", dealer_avg),
      ("private", private_avg), ("other", other_avg))

    aggregateBySellerType.map { case (k, v) => k -> formatPrice(v) }
  }

  //Retirns percentage distribution of available cars by make
  def percentDistribution(lists: scala.collection.mutable.ListBuffer[Listing]): Seq[(String, String)] = {
    var countMake = collection.mutable.Map[String, Int]()

    lists.map(listing => {
      countMake.get(listing.make) match {
        case Some(value) => countMake(listing.make) = value + 1
        case None => countMake(listing.make) = 1
      }
    })
    countMake = countMake.map { case (k, v) => k -> ((v * 100) / lists.length) }
    countMake.toSeq.sortWith(_._2 > _._2).map(x => (x._1, s"${x._2}%"))
  }

  //Returns average price of 30% of the most contacted listing
  def averagePriceOfMostContacted(listOfTopContacts: List[Int]): String = {
    var totalPrice: Int = 0
    listingsList map (listing => {
      if (listOfTopContacts.contains(listing.id)) {
        totalPrice += listing.price
      }
    })
    formatPrice(totalPrice/listOfTopContacts.length)
  }

  //Returns details of most contacted listings per month
  def listMaxContactedByMonth(checkMaxContacted: collection.mutable.Map[Int, ListMap[Int, Int]]): collection.mutable.Map[Int, scala.collection.mutable.ListBuffer[MaxContact]] = {
    val monthlyListOfMaxContacts = collection.mutable.Map[Int, scala.collection.mutable.ListBuffer[MaxContact]]()

    checkMaxContacted.foreach({ case (k, v) => {
      v.foreach({ case (k2, v2) => {
        listingsList.find(x => x.id == k2) match {
          case Some(value) => {
            monthlyListOfMaxContacts.get(k) match {
              case Some(keyExisits) => monthlyListOfMaxContacts(k) += MaxContact(value.id, value.make, formatPrice(value.price), s"${value.mileage} KM", v2)
              case None => monthlyListOfMaxContacts(k) = scala.collection.mutable.ListBuffer[MaxContact]()
            }
          }
          case None =>
        }
      }
      })
    }
    })

    monthlyListOfMaxContacts
  }
}