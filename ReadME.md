## AutoScout24 Assignment

To run : sbt run \
Host : http://localhost:9000

### Api Calls

1. /api/getAggregateListing            :: Returns average price of each seller type
2. /api/getPercentageDistribution      :: Returns sorted list of percentage distribution of available cars by Make
3. /api/getAveragePriceMostContacted   :: Returns average price of 30% of most contacted listings
4. /api/getMaxContactedMonthly         :: Returns details about most contacted listing monthly 

### Controllers
1. HomeController : Contains main methods which makes all the api calls
2. ListingsController : Processes listings.csv 
3. ContactsController : Processes contacts.csv


### View 
1. Main.scala.html : Contains upload button to upload the files.


### Models
CaseModels : Contains case classes of different class

