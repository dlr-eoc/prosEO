# ************************************************************
# Sequel Pro SQL dump
# Version 4499
#
# http://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: 127.0.0.1 (MySQL 10.0.36-MariaDB)
# Datenbank: proseo
# Erstellt am: 2019-07-23 16:06:55 +0000
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



# Export von Tabelle job
# ------------------------------------------------------------

DROP TABLE IF EXISTS `job`;

CREATE TABLE `job` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `job_state` int(11) DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `stop_time` datetime DEFAULT NULL,
  `orbit_id` bigint(20) DEFAULT NULL,
  `processing_facility_id` bigint(20) DEFAULT NULL,
  `processing_order_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8jj66thbddwdxad89qxjeepxg` (`orbit_id`),
  KEY `FK6ek6xjklhsk2qduh5ejbcnb7c` (`processing_facility_id`),
  KEY `FKo7lm1bpn9pqf1qq9o5fpfjtic` (`processing_order_id`),
  CONSTRAINT `FK6ek6xjklhsk2qduh5ejbcnb7c` FOREIGN KEY (`processing_facility_id`) REFERENCES `processing_facility` (`id`),
  CONSTRAINT `FK8jj66thbddwdxad89qxjeepxg` FOREIGN KEY (`orbit_id`) REFERENCES `orbit` (`id`),
  CONSTRAINT `FKo7lm1bpn9pqf1qq9o5fpfjtic` FOREIGN KEY (`processing_order_id`) REFERENCES `processing_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle job_filter_conditions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `job_filter_conditions`;

CREATE TABLE `job_filter_conditions` (
  `job_id` bigint(20) NOT NULL,
  `parameter_type` int(11) DEFAULT NULL,
  `parameter_value` tinyblob,
  `filter_conditions_key` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`filter_conditions_key`),
  CONSTRAINT `FK94wj5lo9qbb4ial4dyacj7dhj` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle job_output_parameters
# ------------------------------------------------------------

DROP TABLE IF EXISTS `job_output_parameters`;

CREATE TABLE `job_output_parameters` (
  `job_id` bigint(20) NOT NULL,
  `parameter_type` int(11) DEFAULT NULL,
  `parameter_value` tinyblob,
  `output_parameters_key` varchar(255) NOT NULL,
  PRIMARY KEY (`job_id`,`output_parameters_key`),
  CONSTRAINT `FKrsx8biruimd9raheaq6i77lqr` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle job_step
# ------------------------------------------------------------

DROP TABLE IF EXISTS `job_step`;

CREATE TABLE `job_step` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `job_step_state` int(11) DEFAULT NULL,
  `job_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbi6cqwlkj3nyqkvheqeg5qql0` (`job_id`),
  CONSTRAINT `FKbi6cqwlkj3nyqkvheqeg5qql0` FOREIGN KEY (`job_id`) REFERENCES `job` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle job_step_output_parameters
# ------------------------------------------------------------

DROP TABLE IF EXISTS `job_step_output_parameters`;

CREATE TABLE `job_step_output_parameters` (
  `job_step_id` bigint(20) NOT NULL,
  `parameter_type` int(11) DEFAULT NULL,
  `parameter_value` tinyblob,
  `output_parameters_key` varchar(255) NOT NULL,
  PRIMARY KEY (`job_step_id`,`output_parameters_key`),
  CONSTRAINT `FKlw2fh8ksho7gcvrfykeoep899` FOREIGN KEY (`job_step_id`) REFERENCES `job_step` (`id`)
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



# Export von Tabelle processing_order
# ------------------------------------------------------------

DROP TABLE IF EXISTS `processing_order`;

CREATE TABLE `processing_order` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `execution_time` datetime DEFAULT NULL,
  `identifier` varchar(255) DEFAULT NULL,
  `mission_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKgj4135cm664vfl5jt6v613y0e` (`mission_id`),
  CONSTRAINT `FKgj4135cm664vfl5jt6v613y0e` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle processing_order_filter_conditions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `processing_order_filter_conditions`;

CREATE TABLE `processing_order_filter_conditions` (
  `processing_order_id` bigint(20) NOT NULL,
  `parameter_type` int(11) DEFAULT NULL,
  `parameter_value` tinyblob,
  `filter_conditions_key` varchar(255) NOT NULL,
  PRIMARY KEY (`processing_order_id`,`filter_conditions_key`),
  CONSTRAINT `FK7c2kv2puafgrqguaudlrgj55h` FOREIGN KEY (`processing_order_id`) REFERENCES `processing_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle processing_order_output_parameters
# ------------------------------------------------------------

DROP TABLE IF EXISTS `processing_order_output_parameters`;

CREATE TABLE `processing_order_output_parameters` (
  `processing_order_id` bigint(20) NOT NULL,
  `parameter_type` int(11) DEFAULT NULL,
  `parameter_value` tinyblob,
  `output_parameters_key` varchar(255) NOT NULL,
  PRIMARY KEY (`processing_order_id`,`output_parameters_key`),
  CONSTRAINT `FK7udpjfeq21n6vsi6rxeycsoi9` FOREIGN KEY (`processing_order_id`) REFERENCES `processing_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle processing_order_promised_products
# ------------------------------------------------------------

DROP TABLE IF EXISTS `processing_order_promised_products`;

CREATE TABLE `processing_order_promised_products` (
  `processing_order_id` bigint(20) NOT NULL,
  `promised_products_id` bigint(20) NOT NULL,
  PRIMARY KEY (`processing_order_id`,`promised_products_id`),
  KEY `FK4dlhkv571p16s1ax73pg46r7t` (`promised_products_id`),
  CONSTRAINT `FK4dlhkv571p16s1ax73pg46r7t` FOREIGN KEY (`promised_products_id`) REFERENCES `product` (`id`),
  CONSTRAINT `FKatxc5wyvlncskrhyhrxum5mni` FOREIGN KEY (`processing_order_id`) REFERENCES `processing_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle processing_order_requested_configured_processors
# ------------------------------------------------------------

DROP TABLE IF EXISTS `processing_order_requested_configured_processors`;

CREATE TABLE `processing_order_requested_configured_processors` (
  `processing_order_id` bigint(20) NOT NULL,
  `requested_configured_processors_id` bigint(20) NOT NULL,
  PRIMARY KEY (`processing_order_id`,`requested_configured_processors_id`),
  KEY `FKkkhi2aj21ehrsok4ekhl1fd31` (`requested_configured_processors_id`),
  CONSTRAINT `FKgdnmmc4ri3f4w2d52e935d3jg` FOREIGN KEY (`processing_order_id`) REFERENCES `processing_order` (`id`),
  CONSTRAINT `FKkkhi2aj21ehrsok4ekhl1fd31` FOREIGN KEY (`requested_configured_processors_id`) REFERENCES `configured_processor` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle processing_order_requested_orbits
# ------------------------------------------------------------

DROP TABLE IF EXISTS `processing_order_requested_orbits`;

CREATE TABLE `processing_order_requested_orbits` (
  `processing_order_id` bigint(20) NOT NULL,
  `requested_orbits_id` bigint(20) NOT NULL,
  KEY `FKl52pcfs07440sihimxmpy6iva` (`requested_orbits_id`),
  KEY `FKgruycyl8hgdsmac11yl37odi9` (`processing_order_id`),
  CONSTRAINT `FKgruycyl8hgdsmac11yl37odi9` FOREIGN KEY (`processing_order_id`) REFERENCES `processing_order` (`id`),
  CONSTRAINT `FKl52pcfs07440sihimxmpy6iva` FOREIGN KEY (`requested_orbits_id`) REFERENCES `orbit` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle processing_order_requested_product_classes
# ------------------------------------------------------------

DROP TABLE IF EXISTS `processing_order_requested_product_classes`;

CREATE TABLE `processing_order_requested_product_classes` (
  `processing_order_id` bigint(20) NOT NULL,
  `requested_product_classes_id` bigint(20) NOT NULL,
  PRIMARY KEY (`processing_order_id`,`requested_product_classes_id`),
  KEY `FK7afxfgldbnrdi5joivn7agj86` (`requested_product_classes_id`),
  CONSTRAINT `FK7afxfgldbnrdi5joivn7agj86` FOREIGN KEY (`requested_product_classes_id`) REFERENCES `product_class` (`id`),
  CONSTRAINT `FKj0e73npk4ljcr6lupi978clea` FOREIGN KEY (`processing_order_id`) REFERENCES `processing_order` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle product
# ------------------------------------------------------------

DROP TABLE IF EXISTS `product`;

CREATE TABLE `product` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `sensing_start_time` datetime DEFAULT NULL,
  `sensing_stop_time` datetime DEFAULT NULL,
  `configured_processor_id` bigint(20) DEFAULT NULL,
  `enclosing_product_id` bigint(20) DEFAULT NULL,
  `job_step_id` bigint(20) DEFAULT NULL,
  `orbit_id` bigint(20) DEFAULT NULL,
  `product_class_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4oh5ogb84h479uxx9m2w2s1i3` (`configured_processor_id`),
  KEY `FKe8busf8q6a8uvrh9a5od38tqo` (`enclosing_product_id`),
  KEY `FKk9wbgbn6a2xyr7f7vl65uogis` (`job_step_id`),
  KEY `FKkobwm0e23qst5q2irk37fwmuy` (`orbit_id`),
  KEY `FK5g7do1yby2n4monwfjw1q79kc` (`product_class_id`),
  CONSTRAINT `FK4oh5ogb84h479uxx9m2w2s1i3` FOREIGN KEY (`configured_processor_id`) REFERENCES `configured_processor` (`id`),
  CONSTRAINT `FK5g7do1yby2n4monwfjw1q79kc` FOREIGN KEY (`product_class_id`) REFERENCES `product_class` (`id`),
  CONSTRAINT `FKe8busf8q6a8uvrh9a5od38tqo` FOREIGN KEY (`enclosing_product_id`) REFERENCES `product` (`id`),
  CONSTRAINT `FKk9wbgbn6a2xyr7f7vl65uogis` FOREIGN KEY (`job_step_id`) REFERENCES `job_step` (`id`),
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
  `product_type` varchar(255) DEFAULT NULL,
  `enclosing_class_id` bigint(20) DEFAULT NULL,
  `mission_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKafnqr7afqkr7vn6difh4r9e3j` (`enclosing_class_id`),
  KEY `FKinocsatitcf1ofpp4wc7psua2` (`mission_id`),
  CONSTRAINT `FKafnqr7afqkr7vn6difh4r9e3j` FOREIGN KEY (`enclosing_class_id`) REFERENCES `product_class` (`id`),
  CONSTRAINT `FKinocsatitcf1ofpp4wc7psua2` FOREIGN KEY (`mission_id`) REFERENCES `mission` (`id`)
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
  `parameter_type` int(11) DEFAULT NULL,
  `parameter_value` tinyblob,
  `parameters_key` varchar(255) NOT NULL,
  PRIMARY KEY (`product_id`,`parameters_key`),
  CONSTRAINT `FK84to6rlvpri4i2pjqpvfn5jd8` FOREIGN KEY (`product_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle product_query
# ------------------------------------------------------------

DROP TABLE IF EXISTS `product_query`;

CREATE TABLE `product_query` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `is_satisfied` bit(1) DEFAULT NULL,
  `jpql_query_condition` text,
  `sql_query_condition` text,
  `job_step_id` bigint(20) DEFAULT NULL,
  `requested_product_class_id` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKc71ouv75rseha12h0fmlqt6a5` (`job_step_id`),
  KEY `FK2wed8wyw8vyboifjd64ytftp9` (`requested_product_class_id`),
  CONSTRAINT `FK2wed8wyw8vyboifjd64ytftp9` FOREIGN KEY (`requested_product_class_id`) REFERENCES `product_class` (`id`),
  CONSTRAINT `FKc71ouv75rseha12h0fmlqt6a5` FOREIGN KEY (`job_step_id`) REFERENCES `job_step` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle product_query_filter_conditions
# ------------------------------------------------------------

DROP TABLE IF EXISTS `product_query_filter_conditions`;

CREATE TABLE `product_query_filter_conditions` (
  `product_query_id` bigint(20) NOT NULL,
  `parameter_type` int(11) DEFAULT NULL,
  `parameter_value` tinyblob,
  `filter_conditions_key` varchar(255) NOT NULL,
  PRIMARY KEY (`product_query_id`,`filter_conditions_key`),
  CONSTRAINT `FKag48xcu5bmuq9yqls0d824bkj` FOREIGN KEY (`product_query_id`) REFERENCES `product_query` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle product_satisfied_product_queries
# ------------------------------------------------------------

DROP TABLE IF EXISTS `product_satisfied_product_queries`;

CREATE TABLE `product_satisfied_product_queries` (
  `satisfying_products_id` bigint(20) NOT NULL,
  `satisfied_product_queries_id` bigint(20) NOT NULL,
  PRIMARY KEY (`satisfying_products_id`,`satisfied_product_queries_id`),
  KEY `FKk5rlrwoo04huxotyum6efdw4w` (`satisfied_product_queries_id`),
  CONSTRAINT `FKk5rlrwoo04huxotyum6efdw4w` FOREIGN KEY (`satisfied_product_queries_id`) REFERENCES `product_query` (`id`),
  CONSTRAINT `FKnekgyp50knufdn0h716tuumbr` FOREIGN KEY (`satisfying_products_id`) REFERENCES `product` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



# Export von Tabelle simple_policy
# ------------------------------------------------------------

DROP TABLE IF EXISTS `simple_policy`;

CREATE TABLE `simple_policy` (
  `id` bigint(20) NOT NULL,
  `version` int(11) NOT NULL,
  `policy_type` int(11) DEFAULT NULL,
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
  `parameter_type` int(11) DEFAULT NULL,
  `parameter_value` tinyblob,
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




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
