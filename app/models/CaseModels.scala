package models

case class Listing(id: Int, make: String, price: Int, mileage:Int, seller_type:String)
case class Contact(listing_id: Int, contact_date: Long)
case class MaxContact(listing_id: Int, make: String, price : String, mileage : String, count : Int)
