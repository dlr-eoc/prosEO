# ************************************************************
# Sequel Pro SQL dump
# Version 4499
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 127.0.0.1 (MySQL 10.0.36-MariaDB)
# Datenbank: proseo
# Erstellt am: 2019-07-22 15:48:46 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Export von Tabelle configured_processor
# ------------------------------------------------------------

DROP TABLE IF EXISTS `configured_processor`;

CREATE TABLE `configured_processor` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `identifier` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle hibernate_sequence
# ------------------------------------------------------------

DROP TABLE IF EXISTS `hibernate_sequence`;

CREATE TABLE `hibernate_sequence` (
  `next_val` bigint(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle mission
# ------------------------------------------------------------

DROP TABLE IF EXISTS `mission`;

CREATE TABLE `mission` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `code` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle mission_product_classes
# ------------------------------------------------------------

DROP TABLE IF EXISTS `mission_product_classes`;

CREATE TABLE `mission_product_classes` (
  `mission_id` bigint(20) NOT NULL,
  `product_classes_id` bigint(20) NOT NULL,
  PRIMARY KEY (`mission_id`,`product_classes_id`),
  UNIQUE KEY `UK_jr4i6v1cbrm12sg72tulyfi0p` (`product_classes_id`),
  CONSTRAINT `FK5s2ciqvcsjelkxcjdv4ko6f15` FOREIGN KEY (`product_classes_id`) REFERENCES `product_class` (`id`),
  CONSTRAINT `FKsi5a3wxphhkw9oulagoucwsrs` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle mission_spacecrafts
# ------------------------------------------------------------

DROP TABLE IF EXISTS `mission_spacecrafts`;

CREATE TABLE `mission_spacecrafts` (
  `mission_id` bigint(20) NOT NULL,
  `spacecrafts_id` bigint(20) NOT NULL,
  PRIMARY KEY (`mission_id`,`spacecrafts_id`),
  UNIQUE KEY `UK_oampl6nep2l3xsoyxg77vgo5x` (`spacecrafts_id`),
  CONSTRAINT `FKahk4mcxncd72yspbd1mat19ls` FOREIGN KEY (`spacecrafts_id`) REFERENCES `spacecraft` (`id`),
  CONSTRAINT `FKi6jm59mev9i8903fv1mrx6vi7` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle orbit
# ------------------------------------------------------------

DROP TABLE IF EXISTS `orbit`;

CREATE TABLE `orbit` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `orbit_number` int(11) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `stop_time` datetime DEFAULT NULL,
  `spacecraft_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKi2gpip0vqngjwnvmguox9wi3f` (`spacecraft_id`),
  CONSTRAINT `FKi2gpip0vqngjwnvmguox9wi3f` FOREIGN KEY (`spacecraft_id`) REFERENCES `spacecraft` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle processing_facility
# ------------------------------------------------------------

DROP TABLE IF EXISTS `processing_facility`;

CREATE TABLE `processing_facility` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle product
# ------------------------------------------------------------

DROP TABLE IF EXISTS `product`;

CREATE TABLE `product` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `sensing_start_time` datetime DEFAULT NULL,
  `sensing_stop_time` datetime DEFAULT NULL,
  `enclosing_product_id` bigint(20) DEFAULT NULL,
  `orbit_id` bigint(20) DEFAULT NULL,
  `product_class_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKe8busf8q6a8uvrh9a5od38tqo` (`enclosing_product_id`),
  KEY `FKkobwm0e23qst5q2irk37fwmuy` (`orbit_id`),
  KEY `FK5g7do1yby2n4monwfjw1q79kc` (`product_class_id`),
  CONSTRAINT `FK5g7do1yby2n4monwfjw1q79kc` FOREIGN KEY (`product_class_id`) REFERENCES `product_class` (`id`),
  CONSTRAINT `FKe8busf8q6a8uvrh9a5od38tqo` FOREIGN KEY (`enclosing_product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `FKkobwm0e23qst5q2irk37fwmuy` FOREIGN KEY (`orbit_id`) REFERENCES `orbit` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle product_class
# ------------------------------------------------------------

DROP TABLE IF EXISTS `product_class`;

CREATE TABLE `product_class` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `mission_type` varchar(255) DEFAULT NULL,
  `type` varchar(255) DEFAULT NULL,
  `enclosing_class_id` bigint(20) DEFAULT NULL,
  `mission_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKafnqr7afqkr7vn6difh4r9e3j` (`enclosing_class_id`),
  KEY `FKinocsatitcf1ofpp4wc7psua2` (`mission_id`),
  CONSTRAINT `FKafnqr7afqkr7vn6difh4r9e3j` FOREIGN KEY (`enclosing_class_id`) REFERENCES `product_class` (`id`),
  CONSTRAINT `FKinocsatitcf1ofpp4wc7psua2` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle product_class_component_classes
# ------------------------------------------------------------

DROP TABLE IF EXISTS `product_class_component_classes`;

CREATE TABLE `product_class_component_classes` (
  `product_class_id` bigint(20) NOT NULL,
  `component_classes_id` bigint(20) NOT NULL,
  PRIMARY KEY (`product_class_id`,`component_classes_id`),
  UNIQUE KEY `UK_9qt63w814b6thrhupfo283ugd` (`component_classes_id`),
  CONSTRAINT `FKgc9ve6enuuj60v87qqebvhfs6` FOREIGN KEY (`component_classes_id`) REFERENCES `product_class` (`id`),
  CONSTRAINT `FKpm1g11yvln9yft0rf2c6bx01x` FOREIGN KEY (`product_class_id`) REFERENCES `product_class` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle product_component_products
# ------------------------------------------------------------

DROP TABLE IF EXISTS `product_component_products`;

CREATE TABLE `product_component_products` (
  `product_id` bigint(20) NOT NULL,
  `component_products_id` bigint(20) NOT NULL,
  PRIMARY KEY (`product_id`,`component_products_id`),
  UNIQUE KEY `UK_t3bjfc49t4ktj6spajut8byl4` (`component_products_id`),
  CONSTRAINT `FK2hta153fm4bpgowo0ewhi3729` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `FKt1g788du7g8ll5k0t6bxv93m2` FOREIGN KEY (`component_products_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle product_file
# ------------------------------------------------------------

DROP TABLE IF EXISTS `product_file`;

CREATE TABLE `product_file` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `file_path` varchar(255) DEFAULT NULL,
  `product_file_name` varchar(255) DEFAULT NULL,
  `storage_type` int(11) DEFAULT NULL,
  `processing_facility_id` bigint(20) DEFAULT NULL,
  `product_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4b360nnjmkd9r4w01jc7yer5h` (`processing_facility_id`),
  KEY `FKqs127y6vnoylxgo8aroqx4e8f` (`product_id`),
  CONSTRAINT `FK4b360nnjmkd9r4w01jc7yer5h` FOREIGN KEY (`processing_facility_id`) REFERENCES `processing_facility` (`id`),
  CONSTRAINT `FKqs127y6vnoylxgo8aroqx4e8f` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle product_file_aux_file_names
# ------------------------------------------------------------

DROP TABLE IF EXISTS `product_file_aux_file_names`;

CREATE TABLE `product_file_aux_file_names` (
  `product_file_id` bigint(20) NOT NULL,
  `aux_file_names` varchar(255) DEFAULT NULL,
  KEY `FK24578k1macp0jxtdaiep0nku8` (`product_file_id`),
  CONSTRAINT `FK24578k1macp0jxtdaiep0nku8` FOREIGN KEY (`product_file_id`) REFERENCES `product_file` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle product_parameters
# ------------------------------------------------------------

DROP TABLE IF EXISTS `product_parameters`;

CREATE TABLE `product_parameters` (
  `product_id` bigint(20) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `value` tinyblob,
  `parameters_key` varchar(255) NOT NULL,
  PRIMARY KEY (`product_id`,`parameters_key`),
  CONSTRAINT `FK84to6rlvpri4i2pjqpvfn5jd8` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle simple_policy
# ------------------------------------------------------------

DROP TABLE IF EXISTS `simple_policy`;

CREATE TABLE `simple_policy` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `type` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle simple_policy_delta_times
# ------------------------------------------------------------

DROP TABLE IF EXISTS `simple_policy_delta_times`;

CREATE TABLE `simple_policy_delta_times` (
  `simple_policy_id` bigint(20) NOT NULL,
  `duration` bigint(20) DEFAULT NULL,
  `unit` int(11) DEFAULT NULL,
  `list_index` int(11) NOT NULL,
  PRIMARY KEY (`simple_policy_id`,`list_index`),
  CONSTRAINT `FKerahx0bbgt0eeqanerq28kofp` FOREIGN KEY (`simple_policy_id`) REFERENCES `simple_policy` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle simple_selection_rule
# ------------------------------------------------------------

DROP TABLE IF EXISTS `simple_selection_rule`;

CREATE TABLE `simple_selection_rule` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `is_mandatory` bit(1) DEFAULT NULL,
  `source_product_class_id` bigint(20) DEFAULT NULL,
  `target_product_class_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8n3bq0ecxeti1ylukwkt7cnm` (`source_product_class_id`),
  KEY `FKje8biclfyorg1wm8uh1qf9d0` (`target_product_class_id`),
  CONSTRAINT `FK8n3bq0ecxeti1ylukwkt7cnm` FOREIGN KEY (`source_product_class_id`) REFERENCES `product_class` (`id`),
  CONSTRAINT `FKje8biclfyorg1wm8uh1qf9d0` FOREIGN KEY (`target_product_class_id`) REFERENCES `product_class` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle simple_selection_rule_filter_conditions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `simple_selection_rule_filter_conditions`;

CREATE TABLE `simple_selection_rule_filter_conditions` (
  `simple_selection_rule_id` bigint(20) NOT NULL,
  `type` int(11) DEFAULT NULL,
  `value` tinyblob,
  `filter_conditions_key` varchar(255) NOT NULL,
  PRIMARY KEY (`simple_selection_rule_id`,`filter_conditions_key`),
  CONSTRAINT `FKi9hebpru8hilywjux8v76p2em` FOREIGN KEY (`simple_selection_rule_id`) REFERENCES `simple_selection_rule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle simple_selection_rule_simple_policies
# ------------------------------------------------------------

DROP TABLE IF EXISTS `simple_selection_rule_simple_policies`;

CREATE TABLE `simple_selection_rule_simple_policies` (
  `simple_selection_rule_id` bigint(20) NOT NULL,
  `simple_policies_id` bigint(20) NOT NULL,
  UNIQUE KEY `UK_7jrn9t62kdspngixrembpkrd7` (`simple_policies_id`),
  KEY `FKfjb7qfppicnb1xj9vgi895dnb` (`simple_selection_rule_id`),
  CONSTRAINT `FKfjb7qfppicnb1xj9vgi895dnb` FOREIGN KEY (`simple_selection_rule_id`) REFERENCES `simple_selection_rule` (`id`),
  CONSTRAINT `FKgijs10i27ucb2tosn56pqrqt6` FOREIGN KEY (`simple_policies_id`) REFERENCES `simple_policy` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle spacecraft
# ------------------------------------------------------------

DROP TABLE IF EXISTS `spacecraft`;

CREATE TABLE `spacecraft` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `code` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `mission_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKsp2jjwkpaehybfu5pwedol1c` (`mission_id`),
  CONSTRAINT `FKsp2jjwkpaehybfu5pwedol1c` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle spacecraft_orbits
# ------------------------------------------------------------

DROP TABLE IF EXISTS `spacecraft_orbits`;

CREATE TABLE `spacecraft_orbits` (
  `spacecraft_id` bigint(20) NOT NULL,
  `orbits_id` bigint(20) NOT NULL,
  UNIQUE KEY `UK_f3svvd4tu8tngxfhu73n4gqkh` (`orbits_id`),
  KEY `FKrtoeaidujeyeatvlf5sx48aq4` (`spacecraft_id`),
  CONSTRAINT `FKaeulgpxyjwgrrlsri7albb2fq` FOREIGN KEY (`orbits_id`) REFERENCES `orbit` (`id`),
  CONSTRAINT `FKrtoeaidujeyeatvlf5sx48aq4` FOREIGN KEY (`spacecraft_id`) REFERENCES `spacecraft` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
