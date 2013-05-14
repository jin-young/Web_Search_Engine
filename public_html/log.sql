-- phpMyAdmin SQL Dump
-- version 3.5.5
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: May 14, 2013 at 08:19 PM
-- Server version: 5.5.29
-- PHP Version: 5.4.10

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";

--
-- Database: `websearch`
--

-- --------------------------------------------------------

--
-- Table structure for table `click_log`
--

CREATE TABLE `click_log` (
  `id` int(5) NOT NULL AUTO_INCREMENT,
  `ads_id` varchar(255) NOT NULL,
  `query` varchar(255) NOT NULL,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=41 ;

--
-- Dumping data for table `click_log`
--

INSERT INTO `click_log` (`id`, `ads_id`, `query`, `date`) VALUES
(38, ' data/wiki/Roots_Radics', 'lincoln', '2013-05-14 18:04:45'),
(39, ' data/wiki/Roots_Radics', 'lincoln', '2013-05-14 18:05:32'),
(40, ' data/wiki/Grammy_Award_for_Best_Classical_Vocal_Performance', 'lincoln', '2013-05-14 18:05:36');
