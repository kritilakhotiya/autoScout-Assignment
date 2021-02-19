package controllers

import javax.inject._
import play.api.mvc._
import models._
import play.api.libs.json._
import java.io.File
import play.api.data.Form
import java.nio.file.Paths
import scala.io.Source
import scala.collection.mutable
import scala.collection.immutable.ListMap
import java.util.Calendar

class ContactsController @Inject()(val listing : ListingsController,
                                       cc: ControllerComponents
                                     ) extends AbstractController(cc) {

  implicit val contactFormat = Json.format[Contact]
  val contactsList = scala.collection.mutable.ListBuffer[Contact]()

  //Inserts each of the contacts data into Contact class.
  def processContacts (fileSource: scala.io.BufferedSource) : Unit = {
    //Assuming the analytics is done only one contact file at a time
    if (!contactsList.nonEmpty) {
      for (line <- fileSource.getLines()) {
        var contact = line.split(",").map(_.trim).map(_.replaceAll("\"", ""))
        contactsList += Contact(contact {0}.toInt, contact {1}.toLong)
      }
    }
  }

  //Returns 30% of most contacted listing
  def averagePriceMostContacted () : List[Int]= {

    var maxContacted = collection.mutable.Map[Int,Int]()
    contactsList.map( contact => {
      maxContacted.get(contact.listing_id) match {
        case Some(value) => maxContacted(contact.listing_id) = maxContacted(contact.listing_id) + 1
        case None =>
          maxContacted(contact.listing_id) = 1
      }
    })
    val totalContactSort : ListMap[Int,Int]= ListMap(maxContacted.toSeq.sortWith(_._2 > _._2):_*)
    totalContactSort.take((30*maxContacted.size)/100).keys.toList
  }

  //Returns most contacted listing monthly
  def checkMaxContactedByDate (): collection.mutable.Map[Int, ListMap[Int,Int]] = {
    var maxContactedPerMonth =  collection.mutable.Map[Int, collection.mutable.Map[Int,Int]]()
    for (contact <- contactsList) {
      val cal = Calendar.getInstance()
      cal.setTimeInMillis(contact.contact_date)
      val month = cal.get(Calendar.MONTH)

      maxContactedPerMonth.get(month) match {
        case Some(value) => {
          value.get(contact.listing_id) match {
            case Some(valueInner) => value(contact.listing_id) += 1
            case None => value(contact.listing_id) = 1
          }}
        case None => maxContactedPerMonth(month)=collection.mutable.Map[Int,Int]()

    }}
    maxContactedPerMonth.map { case (k,v) => {
        k -> ListMap(v.toSeq.sortWith(_._2 > _._2):_*).take(5)
      }}
  }



}