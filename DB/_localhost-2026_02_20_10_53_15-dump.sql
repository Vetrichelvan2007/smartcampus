-- MariaDB dump 10.19  Distrib 10.4.32-MariaDB, for Win64 (AMD64)
--
-- Host: 127.0.0.1    Database: smartcampus
-- ------------------------------------------------------
-- Server version	10.4.32-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `course`
--

DROP TABLE IF EXISTS `course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `course` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `course_code` varchar(30) NOT NULL,
  `course_name` varchar(100) NOT NULL,
  `credit` int(11) DEFAULT NULL,
  `course_type` enum('THEORY','LAB','LOT') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `course_code` (`course_code`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course`
--

LOCK TABLES `course` WRITE;
/*!40000 ALTER TABLE `course` DISABLE KEYS */;
INSERT INTO `course` VALUES (1,'HS23111','Technical Communication I',2,'THEORY'),(2,'MA23111','Linear Algebra and Calculus',4,'THEORY'),(3,'MA23116','Mathematical Foundations for AI',4,'THEORY'),(4,'MA23213','Discrete Mathematical Structures',4,'THEORY'),(5,'MA23214','Probability and Inferential Statistics',4,'THEORY'),(6,'MA23312','Fourier Series and Number Theory',4,'THEORY'),(7,'MA23313','Discrete Mathematics for AI',4,'THEORY'),(8,'GE23117','Heritage of Tamils',1,'THEORY'),(9,'GE23217','Tamils and Technology',1,'THEORY'),(10,'GE23311','Fundamentals of Management for Engineers',3,'THEORY'),(11,'CS23511','Theory of Computation',4,'THEORY'),(12,'CS23512','Fundamentals of Mobile Computing',3,'THEORY'),(13,'AI23512','Data Engineering',3,'THEORY'),(14,'AI23611','Secure Systems Engineering',3,'THEORY'),(15,'AI23711','Social and Ethical Issues in AI',1,'THEORY'),(16,'AI23712','Reinforcement Learning',3,'THEORY'),(17,'GE23131','Programming using C',4,'LOT'),(18,'GE23111','Engineering Graphics',4,'LOT'),(19,'PH23132','Physics for Information Science',4,'LOT'),(20,'EE23133','Basic Electrical and Electronics Engineering',4,'LOT'),(21,'EE23233','Basic Electrical and Electronics Engineering',4,'LOT'),(22,'EC23232','Digital Logic and Microprocessor',4,'LOT'),(23,'EC23331','Microprocessors and Microcontroller',4,'LOT'),(24,'EC23314','Analog and Digital Communication',3,'LOT'),(25,'IT23331','Digital Logic and Computer Architecture',4,'LOT'),(26,'IT23231','Digital Principles and Computer Architecture',4,'LOT'),(27,'CS23231','Data Structures',5,'LOT'),(28,'CS23331','Design and Analysis of Algorithms',4,'LOT'),(29,'CS23332','Database Management Systems',5,'LOT'),(30,'CS23333','Object Oriented Programming using Java',4,'LOT'),(31,'CS23334','Fundamentals of Data Science',4,'LOT'),(32,'CS23431','Operating Systems',5,'LOT'),(33,'CS23432','Software Construction',4,'LOT'),(34,'MA23434','Optimization Techniques for AI',4,'LOT'),(35,'MA23435','Probability, Statistics and Simulation',4,'LOT'),(36,'AI23231','Principles of Artificial Intelligence',4,'LOT'),(37,'AI23331','Fundamentals of Machine Learning',4,'LOT'),(38,'AI23431','Web Technology and Mobile Application',3,'LOT'),(39,'AI23531','Deep Learning',4,'LOT'),(40,'AI23631','Predictive and Prescriptive Analytics',4,'LOT'),(41,'AI23632','Natural Language Processing',4,'LOT'),(42,'CS23531','Web Programming',4,'LOT'),(43,'CS23532','Computer Networks',5,'LOT'),(44,'CS23631','Compiler Design',4,'LOT'),(45,'CS23632','Cryptography and Network Security',3,'LOT'),(46,'CS23633','Cloud Computing',3,'LOT'),(47,'CS23634','Fundamentals of Generative AI and Prompt Engineering',3,'LOT'),(48,'IT23431','MongoDB Essentials',3,'LOT'),(49,'IT23531','Computer Vision',4,'LOT'),(50,'IT23731','Cloud and Big Data Architecture',4,'LOT'),(51,'AD23632','Framework for Data and Visual Analytics',4,'LOT'),(52,'GE23121','Engineering Practices - Civil and Mechanical',1,'LAB'),(53,'GE23122','Engineering Practices - Electrical and Electronics',1,'LAB'),(54,'CS23221','Python Programming Lab',2,'LAB'),(55,'HS23221','Technical Communication II',1,'LAB'),(56,'HS23222','English for Professional Competence',1,'LAB'),(57,'GE23421','Soft Skills - I',1,'LAB'),(58,'GE23521','Soft Skills - II',1,'LAB'),(59,'GE23621','Problem Solving Techniques',1,'LAB'),(60,'GE23627','Design Thinking and Innovation',2,'LAB'),(61,'CS23621','Mobile Application Development Laboratory',2,'LAB'),(62,'IT23721','Data Science using R',2,'LAB'),(63,'AI23521','Build and Deploy Machine Learning Applications',1,'LAB'),(64,'AI23721','Project Phase I',4,'LAB'),(67,'AI23821','Project Phase II',6,'LAB'),(70,'MC23111','Indian Constitution and Freedom Movement',0,'THEORY'),(71,'MC23112','Environmental Science and Engineering',0,'THEORY'),(72,'AD23431','Statistical Analysis and Computing',4,'LOT'),(73,'AD23421','Internship',2,'LAB'),(74,'OE23401','Open Elective',3,'THEORY'),(75,'AD23531','Big Data Architecture',4,'LOT'),(76,'AD23532','Principles of Data Science',4,'LOT'),(79,'AD23631','Data Privacy and Security',4,'LOT'),(95,'PE00000','Professional Elective',3,'THEORY'),(96,'CS23311','Computer Architecture',3,'THEORY'),(97,'CS23421','Internship (2 weeks)',1,'LAB'),(98,'CS23533','Foundations of Artificial Intelligence',4,'LOT'),(99,'CS23721','Project Phase I',3,'LAB'),(100,'CS23821','Project Phase II',6,'LAB');
/*!40000 ALTER TABLE `course` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_for_depts`
--

DROP TABLE IF EXISTS `course_for_depts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `course_for_depts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `course_id` bigint(20) NOT NULL,
  `dept_id` bigint(20) NOT NULL,
  `sem` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_course_dept_sem` (`course_id`,`dept_id`,`sem`),
  KEY `fk_cfd_dept` (`dept_id`),
  CONSTRAINT `fk_cfd_course` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cfd_dept` FOREIGN KEY (`dept_id`) REFERENCES `department` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=210 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_for_depts`
--

LOCK TABLES `course_for_depts` WRITE;
/*!40000 ALTER TABLE `course_for_depts` DISABLE KEYS */;
INSERT INTO `course_for_depts` VALUES (5,1,1,1),(75,1,2,1),(143,1,4,1),(144,2,4,1),(6,3,1,1),(76,3,2,1),(162,4,4,2),(24,5,1,2),(94,5,2,2),(174,6,4,3),(29,7,1,3),(105,7,2,3),(2,8,1,1),(72,8,2,1),(140,8,4,1),(21,9,1,2),(91,9,2,2),(159,9,4,2),(191,11,4,5),(192,12,4,5),(116,13,2,5),(122,14,2,6),(129,15,2,7),(61,16,1,7),(130,16,2,7),(4,17,1,1),(74,17,2,1),(142,17,4,1),(19,18,1,2),(89,18,2,2),(157,18,4,2),(8,19,1,1),(78,19,2,1),(146,19,4,1),(1,20,1,1),(139,20,4,1),(71,21,2,1),(156,22,4,2),(23,26,1,2),(93,26,2,2),(18,27,1,2),(88,27,2,2),(155,27,4,2),(26,28,1,3),(102,28,2,3),(170,28,4,3),(27,29,1,3),(103,29,2,3),(171,29,4,3),(28,30,1,3),(104,30,2,3),(172,30,4,3),(173,31,4,3),(35,32,1,4),(110,32,2,4),(177,32,4,4),(36,33,1,4),(111,33,2,4),(178,33,4,4),(38,34,1,4),(113,34,2,4),(181,35,4,4),(16,36,1,2),(86,36,2,2),(25,37,1,3),(101,37,2,3),(198,37,4,6),(34,38,1,4),(109,38,2,4),(49,39,1,5),(118,39,2,5),(123,40,2,6),(124,41,2,6),(193,42,4,5),(50,43,1,5),(119,43,2,5),(194,43,4,5),(200,44,4,6),(201,45,4,6),(63,46,1,7),(202,46,4,6),(56,47,1,6),(125,47,2,6),(203,47,4,6),(132,50,2,7),(55,51,1,6),(115,51,2,5),(20,52,1,2),(90,52,2,2),(141,52,4,1),(3,53,1,1),(73,53,2,1),(158,53,4,2),(17,54,1,2),(87,54,2,2),(154,54,4,2),(22,55,1,2),(92,55,2,2),(160,55,4,2),(161,56,4,2),(37,57,1,4),(112,57,2,4),(179,57,4,4),(51,58,1,5),(120,58,2,5),(196,58,4,5),(57,59,1,6),(126,59,2,6),(204,59,4,6),(58,60,1,6),(127,60,2,6),(180,60,4,4),(199,61,4,6),(117,63,2,5),(62,64,1,7),(131,64,2,7),(68,67,1,8),(136,67,2,8),(7,70,1,1),(77,70,2,1),(145,70,4,1),(30,71,1,3),(106,71,2,3),(163,71,4,2),(33,72,1,4),(32,73,1,4),(108,73,2,4),(39,74,1,4),(59,74,1,6),(114,74,2,4),(182,74,4,4),(206,74,4,7),(47,75,1,5),(48,76,1,5),(54,79,1,6),(52,95,1,5),(60,95,1,6),(64,95,1,7),(69,95,1,8),(121,95,2,5),(128,95,2,6),(133,95,2,7),(137,95,2,8),(183,95,4,4),(197,95,4,5),(207,95,4,7),(209,95,4,8),(169,96,4,3),(176,97,4,4),(195,98,4,5),(205,99,4,7),(208,100,4,8);
/*!40000 ALTER TABLE `course_for_depts` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_for_depts_backup`
--

DROP TABLE IF EXISTS `course_for_depts_backup`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `course_for_depts_backup` (
  `id` bigint(20) NOT NULL DEFAULT 0,
  `course_id` bigint(20) NOT NULL,
  `dept_id` bigint(20) NOT NULL,
  `sem` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_for_depts_backup`
--

LOCK TABLES `course_for_depts_backup` WRITE;
/*!40000 ALTER TABLE `course_for_depts_backup` DISABLE KEYS */;
INSERT INTO `course_for_depts_backup` VALUES (1,1,1,1),(54,1,2,1),(2,3,1,1),(55,3,2,1),(9,5,1,2),(62,5,2,2),(19,7,1,3),(72,7,2,3),(3,8,1,1),(56,8,2,1),(10,9,1,2),(63,9,2,2),(87,13,2,5),(94,14,2,6),(102,15,2,7),(49,16,1,7),(103,16,2,7),(5,17,1,1),(58,17,2,1),(11,18,1,2),(64,18,2,2),(4,19,1,1),(57,19,2,1),(6,20,1,1),(59,21,2,1),(12,26,1,2),(65,26,2,2),(14,27,1,2),(67,27,2,2),(21,28,1,3),(74,28,2,3),(22,29,1,3),(75,29,2,3),(23,30,1,3),(76,30,2,3),(29,32,1,4),(81,32,2,4),(30,33,1,4),(82,33,2,4),(26,34,1,4),(79,34,2,4),(13,36,1,2),(66,36,2,2),(20,37,1,3),(73,37,2,3),(27,38,1,4),(80,38,2,4),(37,39,1,5),(88,39,2,5),(95,40,2,6),(96,41,2,6),(38,43,1,5),(90,43,2,5),(50,46,1,7),(44,47,1,6),(97,47,2,6),(104,50,2,7),(43,51,1,6),(89,51,2,5),(17,52,1,2),(70,52,2,2),(7,53,1,1),(60,53,2,1),(18,54,1,2),(71,54,2,2),(15,55,1,2),(68,55,2,2),(16,56,1,2),(69,56,2,2),(31,57,1,4),(83,57,2,4),(39,58,1,5),(92,58,2,5),(46,59,1,6),(99,59,2,6),(45,60,1,6),(98,60,2,6),(91,63,2,5),(105,64,2,7),(108,67,2,8),(8,70,1,1),(61,70,2,1),(24,71,1,3),(77,71,2,3),(28,72,1,4),(32,73,1,4),(25,74,1,4),(78,74,2,4),(35,75,1,5),(36,76,1,5),(33,77,1,5),(34,78,1,5),(42,79,1,6),(40,80,1,6),(41,81,1,6),(51,82,1,7),(47,83,1,7),(48,84,1,7),(53,85,1,8),(52,86,1,8),(84,87,2,4),(85,88,2,5),(86,89,2,5),(93,90,2,6),(100,91,2,7),(101,92,2,7),(106,93,2,8),(107,94,2,8);
/*!40000 ALTER TABLE `course_for_depts_backup` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `course_teacher_allocation`
--

DROP TABLE IF EXISTS `course_teacher_allocation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `course_teacher_allocation` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `course_id` bigint(20) NOT NULL,
  `teacher_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=138 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `course_teacher_allocation`
--

LOCK TABLES `course_teacher_allocation` WRITE;
/*!40000 ALTER TABLE `course_teacher_allocation` DISABLE KEYS */;
INSERT INTO `course_teacher_allocation` VALUES (1,1,15),(2,55,15),(3,56,15),(4,2,13),(5,2,1),(6,3,13),(7,3,1),(8,4,13),(9,4,1),(10,5,13),(11,5,1),(12,6,13),(13,6,1),(14,7,13),(15,7,1),(16,34,13),(17,34,1),(18,35,13),(19,35,1),(20,19,14),(21,19,9),(22,13,1),(23,13,2),(24,14,1),(25,14,2),(26,15,2),(27,15,3),(28,16,1),(29,16,2),(30,36,1),(31,36,2),(32,37,1),(33,37,2),(34,38,1),(35,38,2),(36,39,1),(37,39,2),(38,40,1),(39,40,2),(40,41,1),(41,41,2),(42,63,2),(43,63,3),(44,64,1),(45,64,2),(46,67,1),(47,67,2),(48,11,9),(49,11,11),(50,12,10),(51,12,12),(52,29,9),(53,29,11),(54,30,10),(55,30,12),(56,31,9),(57,31,10),(58,32,9),(59,32,11),(60,33,10),(61,33,12),(62,43,11),(63,43,9),(64,44,11),(65,44,12),(66,45,11),(67,45,9),(68,46,12),(69,46,10),(70,47,12),(71,47,11),(72,96,9),(73,96,10),(74,98,9),(75,98,11),(76,27,9),(77,27,10),(78,28,9),(79,28,11),(80,42,10),(81,42,12),(82,54,9),(83,54,10),(84,61,10),(85,61,12),(86,97,9),(87,97,11),(88,99,9),(89,99,11),(90,100,11),(91,100,12),(92,48,12),(93,48,3),(94,49,12),(95,49,3),(96,50,12),(97,50,3),(98,51,3),(99,51,1),(100,72,3),(101,72,1),(102,75,3),(103,75,1),(104,76,3),(105,76,1),(106,79,3),(107,79,1),(108,17,9),(109,17,10),(110,18,9),(111,18,10),(112,20,9),(113,20,10),(114,21,9),(115,21,10),(116,22,9),(117,22,11),(118,23,9),(119,23,11),(120,24,9),(121,24,11),(122,25,9),(123,25,11),(124,26,9),(125,26,11),(126,8,15),(127,9,15),(128,10,15),(129,57,15),(130,58,15),(131,59,15),(132,60,15),(133,70,15),(134,71,15),(135,74,15),(136,95,15),(137,73,1);
/*!40000 ALTER TABLE `course_teacher_allocation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `department` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `dept_name` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `dept_name` (`dept_name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `department`
--

LOCK TABLES `department` WRITE;
/*!40000 ALTER TABLE `department` DISABLE KEYS */;
INSERT INTO `department` VALUES (1,'AIDS'),(2,'AIML'),(4,'CSE'),(3,'IT');
/*!40000 ALTER TABLE `department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `father_details`
--

DROP TABLE IF EXISTS `father_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `father_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `dob` date DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `address` text DEFAULT NULL,
  `occupation` varchar(100) DEFAULT NULL,
  `annual_income` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `student_id` (`student_id`),
  CONSTRAINT `father_details_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `father_details`
--

LOCK TABLES `father_details` WRITE;
/*!40000 ALTER TABLE `father_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `father_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `identity_details`
--

DROP TABLE IF EXISTS `identity_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `identity_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(20) NOT NULL,
  `aadhar_number` varchar(12) DEFAULT NULL,
  `pan_number` varchar(10) DEFAULT NULL,
  `passport_number` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `student_id` (`student_id`),
  UNIQUE KEY `aadhar_number` (`aadhar_number`),
  UNIQUE KEY `pan_number` (`pan_number`),
  UNIQUE KEY `passport_number` (`passport_number`),
  CONSTRAINT `identity_details_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `identity_details`
--

LOCK TABLES `identity_details` WRITE;
/*!40000 ALTER TABLE `identity_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `identity_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `mother_details`
--

DROP TABLE IF EXISTS `mother_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mother_details` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `dob` date DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `address` text DEFAULT NULL,
  `occupation` varchar(100) DEFAULT NULL,
  `annual_income` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `student_id` (`student_id`),
  CONSTRAINT `mother_details_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `mother_details`
--

LOCK TABLES `mother_details` WRITE;
/*!40000 ALTER TABLE `mother_details` DISABLE KEYS */;
/*!40000 ALTER TABLE `mother_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student`
--

DROP TABLE IF EXISTS `student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `student` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `roll_number` varchar(30) NOT NULL,
  `name` varchar(100) NOT NULL,
  `dob` date DEFAULT NULL,
  `gender` varchar(10) DEFAULT NULL,
  `blood_group` varchar(5) DEFAULT NULL,
  `mother_tongue` varchar(50) DEFAULT NULL,
  `nationality` varchar(50) DEFAULT NULL,
  `address` text DEFAULT NULL,
  `dept_id` bigint(20) NOT NULL,
  `current_year` int(11) NOT NULL,
  `current_semester` int(11) NOT NULL,
  `email` varchar(100) NOT NULL,
  `phone` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `roll_number` (`roll_number`),
  UNIQUE KEY `email` (`email`),
  KEY `dept_id` (`dept_id`),
  CONSTRAINT `student_ibfk_1` FOREIGN KEY (`dept_id`) REFERENCES `department` (`id`) ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student`
--

LOCK TABLES `student` WRITE;
/*!40000 ALTER TABLE `student` DISABLE KEYS */;
INSERT INTO `student` VALUES (1,'AIDS24001','Arun Kumar','2006-05-14','Male','O+','Tamil','Indian','Chennai',1,1,1,'arun.aids1@gmail.com','9000000001'),(2,'AIDS24002','Divya R','2006-07-21','Female','A+','Tamil','Indian','Madurai',1,1,2,'divya.aids2@gmail.com','9000000002'),(3,'AIDS23003','Karthik S','2005-03-11','Male','B+','Tamil','Indian','Salem',1,2,3,'karthik.aids3@gmail.com','9000000003'),(4,'AIDS23004','Meena V','2005-09-19','Female','O-','Tamil','Indian','Trichy',1,2,4,'meena.aids4@gmail.com','9000000004'),(5,'AIDS22005','Rahul P','2004-01-30','Male','AB+','Tamil','Indian','Coimbatore',1,3,5,'rahul.aids5@gmail.com','9000000005'),(6,'AIDS22006','Sneha K','2004-12-02','Female','B-','Tamil','Indian','Erode',1,3,6,'sneha.aids6@gmail.com','9000000006'),(7,'AIDS21007','Vikram D','2003-06-25','Male','O+','Tamil','Indian','Chennai',1,4,7,'vikram.aids7@gmail.com','9000000007'),(8,'AIML24001','Naveen M','2006-02-17','Male','A+','Tamil','Indian','Chennai',2,1,1,'naveen.aiml1@gmail.com','9000000008'),(9,'AIML24002','Priya S','2006-10-08','Female','O+','Tamil','Indian','Vellore',2,1,2,'priya.aiml2@gmail.com','9000000009'),(10,'AIML23003','Harish R','2005-04-14','Male','B+','Tamil','Indian','Tirunelveli',2,2,3,'harish.aiml3@gmail.com','9000000010'),(11,'AIML23004','Keerthi L','2005-11-29','Female','A-','Tamil','Indian','Madurai',2,2,4,'keerthi.aiml4@gmail.com','9000000011'),(12,'AIML22005','Aravind G','2004-07-03','Male','O-','Tamil','Indian','Salem',2,3,5,'aravind.aiml5@gmail.com','9000000012'),(13,'AIML22006','Pavithra N','2004-08-18','Female','B+','Tamil','Indian','Coimbatore',2,3,6,'pavithra.aiml6@gmail.com','9000000013'),(14,'AIML21007','Sanjay T','2003-03-22','Male','AB+','Tamil','Indian','Chennai',2,4,7,'sanjay.aiml7@gmail.com','9000000014'),(15,'CSE24001','Ajay K','2006-06-09','Male','O+','Tamil','Indian','Chennai',3,1,1,'ajay.cse1@gmail.com','9000000015'),(16,'CSE24002','Anitha P','2006-01-12','Female','A+','Tamil','Indian','Erode',3,1,2,'anitha.cse2@gmail.com','9000000016'),(17,'CSE23003','Surya V','2005-05-20','Male','B-','Tamil','Indian','Trichy',3,2,3,'surya.cse3@gmail.com','9000000017'),(18,'CSE23004','Lavanya R','2005-10-05','Female','O-','Tamil','Indian','Madurai',3,2,4,'lavanya.cse4@gmail.com','9000000018'),(19,'CSE22005','Dinesh M','2004-02-27','Male','AB+','Tamil','Indian','Salem',3,3,5,'dinesh.cse5@gmail.com','9000000019'),(20,'CSE21006','Monika S','2003-12-14','Female','A-','Tamil','Indian','Coimbatore',3,4,8,'monika.cse8@gmail.com','9000000020');
/*!40000 ALTER TABLE `student` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `student_course_teacher`
--

DROP TABLE IF EXISTS `student_course_teacher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `student_course_teacher` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(20) NOT NULL,
  `course_id` bigint(20) NOT NULL,
  `teacher_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_student_course` (`student_id`,`course_id`),
  KEY `course_id` (`course_id`),
  KEY `teacher_id` (`teacher_id`),
  CONSTRAINT `student_course_teacher_ibfk_1` FOREIGN KEY (`student_id`) REFERENCES `student` (`id`) ON DELETE CASCADE,
  CONSTRAINT `student_course_teacher_ibfk_2` FOREIGN KEY (`course_id`) REFERENCES `course` (`id`) ON DELETE CASCADE,
  CONSTRAINT `student_course_teacher_ibfk_3` FOREIGN KEY (`teacher_id`) REFERENCES `teacher` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=43 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `student_course_teacher`
--

LOCK TABLES `student_course_teacher` WRITE;
/*!40000 ALTER TABLE `student_course_teacher` DISABLE KEYS */;
INSERT INTO `student_course_teacher` VALUES (1,1,1,15),(2,1,2,13),(3,1,3,13),(4,2,4,13),(5,2,5,13),(6,2,27,9),(7,2,54,3),(8,3,6,13),(9,3,7,13),(10,3,29,11),(11,3,30,10),(12,3,36,1),(13,4,32,12),(14,4,43,10),(15,4,37,2),(16,5,39,2),(17,5,41,1),(18,6,63,2),(19,7,64,1),(20,8,1,4),(21,8,2,13),(22,8,3,5),(23,9,5,7),(24,9,27,6),(25,9,54,6),(26,10,7,5),(27,10,29,3),(28,10,36,5),(29,11,37,7),(30,12,39,7),(31,12,41,6),(32,13,63,7),(33,15,1,10),(34,15,2,13),(35,16,4,9),(36,16,27,11),(37,17,6,11),(38,17,29,9),(39,17,30,12),(40,18,32,9),(41,18,43,11),(42,20,100,11);
/*!40000 ALTER TABLE `student_course_teacher` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `teacher`
--

DROP TABLE IF EXISTS `teacher`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `teacher` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `teacher_clg_id` varchar(30) NOT NULL,
  `name` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `teacher_clg_id` (`teacher_clg_id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `teacher`
--

LOCK TABLES `teacher` WRITE;
/*!40000 ALTER TABLE `teacher` DISABLE KEYS */;
INSERT INTO `teacher` VALUES (1,'TCH-AIDS-01','Dr. Ram Kumar','ram.aids@college.edu'),(2,'TCH-AIDS-02','Dr. Lakshmi Priya','lakshmi.aids@college.edu'),(3,'TCH-AIDS-03','Mr. Suresh B','suresh.aids@college.edu'),(4,'TCH-AIDS-04','Ms. Kavya R','kavya.aids@college.edu'),(5,'TCH-AIML-01','Dr. Manoj K','manoj.aiml@college.edu'),(6,'TCH-AIML-02','Ms. Revathi S','revathi.aiml@college.edu'),(7,'TCH-AIML-03','Dr. Pradeep N','pradeep.aiml@college.edu'),(8,'TCH-AIML-04','Mr. Arun V','arun.aiml@college.edu'),(9,'TCH-CSE-01','Dr. Raghavan P','raghavan.cse@college.edu'),(10,'TCH-CSE-02','Ms. Divya K','divya.cse@college.edu'),(11,'TCH-CSE-03','Dr. Hari Shankar','hari.cse@college.edu'),(12,'TCH-CSE-04','Ms. Swetha M','swetha.cse@college.edu'),(13,'TCH-MATH-01','Dr. Meenakshi Iyer','meenakshi.math@college.edu'),(14,'TCH-PHY-01','Dr. Srinivasan R','srinivasan.physics@college.edu'),(15,'TCH-ENG-01','Ms. Anjali Thomas','anjali.english@college.edu');
/*!40000 ALTER TABLE `teacher` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('STUDENT','TEACHER','ADMIN') NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=37 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'anjali.english@college.edu','pass123','TEACHER'),(2,'arun.aiml@college.edu','pass123','TEACHER'),(3,'divya.cse@college.edu','pass123','TEACHER'),(4,'hari.cse@college.edu','pass123','TEACHER'),(5,'kavya.aids@college.edu','pass123','TEACHER'),(6,'lakshmi.aids@college.edu','pass123','TEACHER'),(7,'manoj.aiml@college.edu','pass123','TEACHER'),(8,'meenakshi.math@college.edu','pass123','TEACHER'),(9,'pradeep.aiml@college.edu','pass123','TEACHER'),(10,'raghavan.cse@college.edu','pass123','TEACHER'),(11,'ram.aids@college.edu','pass123','TEACHER'),(12,'revathi.aiml@college.edu','pass123','TEACHER'),(13,'srinivasan.physics@college.edu','pass123','TEACHER'),(14,'suresh.aids@college.edu','pass123','TEACHER'),(15,'swetha.cse@college.edu','pass123','TEACHER'),(16,'ajay.cse1@gmail.com','pass123','STUDENT'),(17,'anitha.cse2@gmail.com','pass123','STUDENT'),(18,'aravind.aiml5@gmail.com','pass123','STUDENT'),(19,'arun.aids1@gmail.com','pass123','STUDENT'),(20,'dinesh.cse5@gmail.com','pass123','STUDENT'),(21,'divya.aids2@gmail.com','pass123','STUDENT'),(22,'harish.aiml3@gmail.com','pass123','STUDENT'),(23,'karthik.aids3@gmail.com','pass123','STUDENT'),(24,'keerthi.aiml4@gmail.com','pass123','STUDENT'),(25,'lavanya.cse4@gmail.com','pass123','STUDENT'),(26,'meena.aids4@gmail.com','pass123','STUDENT'),(27,'monika.cse8@gmail.com','pass123','STUDENT'),(28,'naveen.aiml1@gmail.com','pass123','STUDENT'),(29,'pavithra.aiml6@gmail.com','pass123','STUDENT'),(30,'priya.aiml2@gmail.com','pass123','STUDENT'),(31,'rahul.aids5@gmail.com','pass123','STUDENT'),(32,'sanjay.aiml7@gmail.com','pass123','STUDENT'),(33,'sneha.aids6@gmail.com','pass123','STUDENT'),(34,'surya.cse3@gmail.com','pass123','STUDENT'),(35,'vikram.aids7@gmail.com','pass123','STUDENT'),(36,'admin@smartcampus.local','pass123','ADMIN');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-02-20 10:53:16

