CREATE DATABASE  IF NOT EXISTS `jj1233_search_engine` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `jj1233_search_engine`;
-- MySQL dump 10.13  Distrib 5.5.31, for debian-linux-gnu (i686)
--
-- Host: warehouse.cims.nyu.edu    Database: jj1233_search_engine
-- ------------------------------------------------------
-- Server version	5.1.34

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `ads_info`
--

DROP TABLE IF EXISTS `ads_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ads_info` (
  `id` int(5) NOT NULL AUTO_INCREMENT,
  `company` varchar(255) DEFAULT NULL,
  `url` varchar(1024) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `content` text,
  `cost` int(20) DEFAULT NULL,
  `num_view` int(11) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ads_info`
--

LOCK TABLES `ads_info` WRITE;
/*!40000 ALTER TABLE `ads_info` DISABLE KEYS */;
INSERT INTO `ads_info` VALUES (1,'AVIS','https://www.avis.com/car-rental/avisHome/home.ac?kw=rent+car&sipubid=22525664820&mpch=ads','Avis Car Rental - Avis.com‎','Official Site: Rent Your Next Car On Avis.com & Save Big. Rent Now!‎',3213,0),(2,'KAYAK.com','http://www.kayak.com/cars?k_adgroup=3794&kw=best+car+rental+deals&k_clickid=_kenshoo_clickid_&gclid=CMiq4uzdk7cCFQFx4AoddRAAyQ&k_affcode=356699&ci=803-527-2209&k_prof=5&k_camp=187','Best Car Rental Deals - Compare 100s Of Cars For No Fee‎','Save On Car Rentals In One Search.‎',123,0),(3,'RentalCars.com','http://www.rentalcars.com/?affiliateCode=google_bob&cor=us&label=us-domestic-EbXXUmFmQOlv*v8O9s66rAS14152335898&ws=&gclid=CJeTvYjlk7cCFQTd4AodWzQADA','Car Rental From $12/Day - Rentalcars.com‎','Best Rates on All Car Guaranteed. All the Best Names at Low Prices!‎',3213,0),(4,'Budget.com','http://www.budget.com/budgetWeb/home/home.ex?kw=rent+car&sipubid=22888381045&mpch=ads','Budget Rent A Car‎','Budget Offers Huge Savings Everyday To Customers Like You. Rent Today!‎',32,0),(5,'Orbitz.com','http://www.orbitz.com/shop/carsearch?type=car&car.dropoffType=AIRPORT&car.pickupType=AIRPORT&WT.srch=1&ap=1s2&crid=3337571715&dv=c&dvm=&gcid=s11287x431&gclid=CMqgk_nlk7cCFYHe4Aodc1kA8g&keyword=rental+cars&ksaid=1128479&kscid=92960&kskid=970461&kspid=74&kw','Compare Rental Car Prices‎','Sort Rental Cars by Price, Type, Company. Compare on ORBITZ!‎',2000,0),(6,'Priceline.com','http://www.priceline.com/l/rental/cars.htm?refid=PLGOOGLECPC&refclickid=High|Volume|Generic|31_NEW&gclid=CILJqZPmk7cCFUWK4AodN20Afg','Rental Cars From $10.95‎','Book Cheap Car Rentals @ Priceline. Book A Deal Fast & Easily Today.‎',100,0),(7,'HotWire','http://www.hotwire.com/car/index.jsp','Hotwire® Car Rentals‎','Find Rental Cars from $11.95/Day. See Our Hot Rates. Search & Save!‎',50,0),(8,'LowFares.com','http://www.lowfares.com/rentalcars/?t=gDEF6&_=ylbWUmFuZG9tSVYgICAgICAgINPmupF93LBh6JR7LyalvMAXiM%2FqElBKhvDDowBRQKd4&lp=1&asid=1125&gclid=CMzGkdLmk7cCFccx4AodLFAARQ','Cars From Only $7 A Day‎','Find Car Rental Deals from $7 A Day Compare Deals from Top Companies!‎',52,0),(9,'LocalBuzz','http://www.localbuzz.us/Car-Rental?_dst=info&_cb=2_b&network=g&cbcc=14808319629&creative=14808319629&hd=114&gclid=CNDS1PTmk7cCFcuj4AodbQgACQ&nocache=13684729','$8/Day Cheap Car Rental‎','No Hidden Fee,No Tax On Car Rental. Cheapest Rental Prices Guaranteed!‎',51,0),(10,'BookingBuddy.com','http://www.bookingbuddy.com/car.php?&billboard=a&suppress_dart_ads=1&taparam=EBBGoogleUSSP_K130626231_A3477343036_NS&supai=19706179636&supsn=g&supmob=&suppos=1s7&supap1=&supap2=&supmbl=&supdev=c&suprnd=115896734176988405','60% Off Car Rental Cheap‎','$5.95-10.95 Car Rental Lowest Rate. Compare & Save. Up To 60% Off Now!‎',54,0),(11,'Expedia.com','http://www.expedia.com/Cars?semcid=13172-1&kword=car_rentals.ZzZz.560002121104.0.20137943004.car%20rentals.car_rentals&gclid=CICVhqLnk7cCFek7Ogod2xYAbA','Car Rentals‎','Expedia Guarantees the Best Price! Book Car Rentals.‎',90,0),(12,'ORBITZ.com','http://www.orbitz.com/shop/airsearch?type=air&ar.type=roundTrip&ar.rt.leaveSlice.time=Anytime&ar.rt.returnSlice.time=Anytime&WT.srch=1&ap=1t1&crid=12394408086&dv=c&dvm=&gcid=2872&gclid=CPmiz8Gal7cCFcJo4Aodt3cASQ&keyword=flight+tickets&ksaid=1224092&kscid=','Flight Tickets - ORBITZ.com','Find Cheap Flights at Orbitz That Are Not Available on Airline Sites!',11,0),(13,'CheapOair®','http://www.cheapoair.com/?FpAffiliate=Google&Fpsub=130108-PremiumGeneric-FlightTickets-Ad3&utm_source=Google&utm_medium=cpc&utm_campaign=PremiumGeneric&utm_term=%7BKeyword%7D&utm_content=Ad4&gclid=COLz2NGcl7cCFUJN4AodlhoAZg','Cheap Flights Tickets','Cheap Flights + Reliable Support. Fast & Easy Booking on CheapOair®',15,0),(14,'OneTravel','http://www.onetravel.com/?FPaffiliate=Google&FPSub=GOGenT1exact-fltktshome&gclid=CKiIqeWcl7cCFQFx4AodSE0AAw','$99+ Cheap Flight Tickets','Round Trip Tickets Starting $124*. Cheap Flight to All Destinations!',17,0),(15,'Cheap Tickets','http://www.cheaptickets.com/shop/airsearch?type=air&ar.type=roundTrip&ar.rt.leaveSlice.time=Anytime&ar.rt.returnSlice.time=Anytime&WT.srch=1&ap=1s2&crid=1397383291&dv=c&dvm=&gcid=s16036x129&gclid=CL6gmsSdl7cCFe4DOgoddmsAFg&keyword=flight+tickets&ksaid=237','Flight - Cheap Tickets','When You Save Money On Flights With Cheaptickets®, You Win.',11,0),(16,'Travelocity','http://www.travelocity.com/Flights?WA1=01010&WA2=gl&WA3=flight%20ticket&WA4=E&WA5=NT&WA6=air&WA7=Air_Flights_TL_Other_Exact|Flights_Tickets&WA8=0cd9d38e-5a31-f268-dbfe-00003ac57e3a&gclid=CMyxgcSel7cCFQFp4AodHTQA2w','Cheap Airline Tickets','Travelocity® Best Price Guarantee. The Smart & Simple Way to Book!',10,0);
/*!40000 ALTER TABLE `ads_info` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2013-05-15  7:41:27
