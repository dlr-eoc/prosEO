MERGE INTO users (username, password, enabled) KEY (username) VALUES ('PTM-testuser', '$2a$12$nIQjS8/qYKCemay7V8iVYOdTNdYkOqTTRt04FV5QXTNVgyti8iA8a', true);


MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_ROOT');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_CLI_USER');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_GUI_USER');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_PRIP_USER');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_USERMGR');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_MISSION_READER');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_MISSION_MGR');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_PRODUCTCLASS_READER');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_PRODUCTCLASS_MGR');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_PRODUCT_READER');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_PRODUCT_READER_RESTRICTED');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_PRODUCT_READER_ALL');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_PRODUCT_INGESTOR');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_PRODUCT_GENERATOR');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_PRODUCT_MGR');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_PROCESSOR_READER');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_PROCESSORCLASS_MGR');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_CONFIGURATION_MGR');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_FACILITY_READER');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_FACILITY_MGR');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_FACILITY_MONITOR');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_ORDER_READER');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_ORDER_MGR');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_ORDER_APPROVER');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_ORDER_PLANNER');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_ORDER_MONITOR');
MERGE INTO authorities (username, authority) KEY (username) VALUES ('PTM-testuser', 'ROLE_JOBSTEP_PROCESSOR');


--
-- PostgreSQL database dump, sorted to avoid constraint violations
--


INSERT INTO class_output_parameter VALUES (79, 1);


INSERT INTO class_output_parameter_output_parameters VALUES (79, NULL, 'INTEGER', '77', 'copernicusCollection');
INSERT INTO class_output_parameter_output_parameters VALUES (79, NULL, 'INTEGER', '2', 'revision');


INSERT INTO mission VALUES (1, 1, 'PTM', 'prosEO Test Mission', NULL, 'PTM-PDGS', '17473', 2592000000000000);


INSERT INTO mission_file_classes VALUES (1, 'TEST');
INSERT INTO mission_file_classes VALUES (1, 'OPER');


INSERT INTO mission_processing_modes VALUES (1, 'OPER');


INSERT INTO processor_class VALUES (23, 1, 'PTML1B', 1);
INSERT INTO processor_class VALUES (24, 1, 'PTML2', 1);
INSERT INTO processor_class VALUES (25, 1, 'PTML3', 1);


INSERT INTO processor VALUES (26, 1, 'localhost:5000/proseo-sample-wrapper:0.9.5-SNAPSHOT', false, 'MMFI_1_8', 0, 1024, '0.1.0', true, false, 23);
INSERT INTO processor VALUES (28, 1, 'localhost:5000/proseo-sample-wrapper:0.9.5-SNAPSHOT', false, 'MMFI_1_8', 0, 1024, '0.1.0', true, false, 24);
INSERT INTO processor VALUES (30, 1, 'localhost:5000/proseo-sample-wrapper:0.9.5-SNAPSHOT', false, 'MMFI_1_8', 0, 1024, '0.1.0', true, false, 25);


INSERT INTO configuration VALUES (32, 1, 'OPER_2020-03-25', 'OPER', 'TEST', 23);
INSERT INTO configuration VALUES (34, 1, 'OPER_2020-03-25', 'OPER', 'TEST', 24);
INSERT INTO configuration VALUES (35, 1, 'OPER_2020-03-25', 'OPER', 'TEST', 25);


INSERT INTO configuration_configuration_files VALUES (34, '/usr/share/sample-processor/conf/ptm_l2_config.xml', '1.0');


INSERT INTO configuration_dyn_proc_parameters VALUES (32, NULL, 'STRING', 'null', 'logging.dumplog');
INSERT INTO configuration_dyn_proc_parameters VALUES (32, NULL, 'INTEGER', '16', 'Threads');
INSERT INTO configuration_dyn_proc_parameters VALUES (32, NULL, 'STRING', 'OPER', 'Processing_Mode');
INSERT INTO configuration_dyn_proc_parameters VALUES (32, NULL, 'STRING', 'notice', 'logging.root');
INSERT INTO configuration_dyn_proc_parameters VALUES (34, NULL, 'STRING', 'null', 'logging.dumplog');
INSERT INTO configuration_dyn_proc_parameters VALUES (34, NULL, 'INTEGER', '10', 'Threads');
INSERT INTO configuration_dyn_proc_parameters VALUES (34, NULL, 'STRING', 'OPER', 'Processing_Mode');
INSERT INTO configuration_dyn_proc_parameters VALUES (34, NULL, 'STRING', 'notice', 'logging.root');
INSERT INTO configuration_dyn_proc_parameters VALUES (35, NULL, 'STRING', 'null', 'logging.dumplog');
INSERT INTO configuration_dyn_proc_parameters VALUES (35, NULL, 'INTEGER', '16', 'Threads');
INSERT INTO configuration_dyn_proc_parameters VALUES (35, NULL, 'STRING', 'OPER', 'Processing_Mode');
INSERT INTO configuration_dyn_proc_parameters VALUES (35, NULL, 'STRING', 'notice', 'logging.root');


INSERT INTO configuration_input_file VALUES (33, 1, 'Physical', 'processing_configuration');


INSERT INTO configuration_input_file_file_names VALUES (33, '/usr/share/sample-processor/conf/ptm_l1b_config.xml');


INSERT INTO configuration_static_input_files VALUES (32, 33);


INSERT INTO configured_processor VALUES (36, 1, true, 'PTML1B_0.1.0_OPER_2020-03-25', '71a3a0b5-6999-427e-afb7-85f211ce48df', 32, 26);
INSERT INTO configured_processor VALUES (37, 1, true, 'PTML2_0.1.0_OPER_2020-03-25', '7d4e991b-d8d5-42f2-a2bc-1713958c4b62', 34, 28);
INSERT INTO configured_processor VALUES (38, 1, true, 'PTML3_0.1.0_OPER_2020-03-25', '9e2f1890-17d8-43e2-89bd-964170c9cacf', 35, 30);


INSERT INTO input_filter VALUES (77, 1);
INSERT INTO input_filter VALUES (78, 1);
INSERT INTO input_filter VALUES (81, 1);
INSERT INTO input_filter VALUES (82, 1);


INSERT INTO input_filter_filter_conditions VALUES (77, NULL, 'STRING', 'OPER', 'fileClass');
INSERT INTO input_filter_filter_conditions VALUES (77, NULL, 'INTEGER', '1', 'revision');
INSERT INTO input_filter_filter_conditions VALUES (78, NULL, 'STRING', 'TEST', 'fileClass');
INSERT INTO input_filter_filter_conditions VALUES (78, NULL, 'INTEGER', '2', 'revision');
INSERT INTO input_filter_filter_conditions VALUES (81, NULL, 'STRING', 'TEST', 'fileClass');
INSERT INTO input_filter_filter_conditions VALUES (81, NULL, 'INTEGER', '99', 'revision');
INSERT INTO input_filter_filter_conditions VALUES (82, NULL, 'STRING', 'TEST', 'fileClass');
INSERT INTO input_filter_filter_conditions VALUES (82, NULL, 'INTEGER', '99', 'revision');


INSERT INTO mon_service_state VALUES (1, 1, 'running');
INSERT INTO mon_service_state VALUES (2, 1, 'stopped');
INSERT INTO mon_service_state VALUES (3, 1, 'starting');
INSERT INTO mon_service_state VALUES (4, 1, 'stopping');
INSERT INTO mon_service_state VALUES (5, 1, 'degraded');


INSERT INTO spacecraft VALUES (2, 1, 'PTS', 'prosEO Test Satellite', 1);


INSERT INTO spacecraft_payloads VALUES (2, 'Super sensor for PTM', 'PTS-SENSOR');


INSERT INTO orbit VALUES (17, 1, 3000, '2019-11-04 09:00:00.2', '2019-11-04 10:41:10.3', 2);
INSERT INTO orbit VALUES (18, 1, 3001, '2019-11-04 10:41:10.3', '2019-11-04 12:22:20.4', 2);
INSERT INTO orbit VALUES (19, 1, 3002, '2019-11-04 12:22:20.4', '2019-11-04 14:03:30.5', 2);
INSERT INTO orbit VALUES (20, 1, 3003, '2019-11-04 14:03:30.5', '2019-11-04 15:44:40.6', 2);
INSERT INTO orbit VALUES (21, 1, 3004, '2019-11-04 15:44:40.6', '2019-11-04 17:25:50.7', 2);
INSERT INTO orbit VALUES (22, 1, 3005, '2019-11-04 17:25:50.7', '2019-11-04 19:07:00.8', 2);


INSERT INTO processing_facility VALUES (61, 2, 'S3', 'Docker Desktop Minikube', 'http://192.168.20.155:8083/proseo/storage-mgr/v0.1', 'STOPPED', 'http://192.168.20.155:8083/proseo/storage-mgr/v0.1', 1, 'localhost', '17482', 'https://kubernetes.docker.internal:6443', 'pw', 'http://192.168.20.155:8083/proseo/storage-mgr/v0.1', 'user');


INSERT INTO product_class VALUES (39, 1, NULL, NULL, NULL, NULL, NULL, 'PTM_L0', 'RESTRICTED', NULL, 1, NULL);
INSERT INTO product_class VALUES (40, 1, NULL, NULL, NULL, NULL, NULL, 'AUX_IERS_B', 'INTERNAL', NULL, 1, NULL);
INSERT INTO product_class VALUES (41, 1, NULL, 'ORBIT', NULL, 'L1B', NULL, 'PTM_L1B', 'PUBLIC', NULL, 1, 23);
INSERT INTO product_class VALUES (42, 1, NULL, NULL, NULL, 'L1B', NULL, 'PTM_L1B_P1', 'PUBLIC', 41, 1, NULL);
INSERT INTO product_class VALUES (43, 1, NULL, NULL, NULL, 'L1B', NULL, 'PTM_L1B_P2', 'PUBLIC', 41, 1, NULL);
INSERT INTO product_class VALUES (44, 1, NULL, 'ORBIT', NULL, 'L2A', NULL, 'PTM_L2_A', 'PUBLIC', NULL, 1, 24);
INSERT INTO product_class VALUES (45, 1, NULL, 'ORBIT', NULL, 'L2B', NULL, 'PTM_L2_B', 'PUBLIC', NULL, 1, 24);
INSERT INTO product_class VALUES (46, 1, 14400000000000, 'TIME_SLICE', NULL, 'L3', NULL, 'PTM_L3', 'PUBLIC', NULL, 1, 25);


INSERT INTO processing_order VALUES (80, 1, NULL, NULL, NULL, NULL, false, 'L2_orbits_3000-3002', NULL, NULL, NULL, NULL, NULL, NULL, 'OTHER', 'INITIAL', 'TEST', 50, 'OPER', NULL, 'ON_DEMAND_DEFAULT', NULL, NULL, 0, 'ORBIT', '2019-11-04 09:00:00.2', NULL, '2019-11-04 14:03:30.5', NULL, '99c9d76f-8a1e-465b-977f-464b97b95450', 1, NULL);
INSERT INTO processing_order VALUES (83, 1, NULL, NULL, NULL, NULL, false, 'L3_products_9:30-17:30', NULL, NULL, NULL, NULL, NULL, NULL, 'OTHER', 'INITIAL', 'TEST', 50, 'OPER', NULL, 'ON_DEMAND_DEFAULT', NULL, 14400000000000, 0, 'TIME_SLICE', '2019-11-04 09:30:00', NULL, '2019-11-04 17:00:00', NULL, 'b45ec444-e132-46ed-b588-44b3caca55bc', 1, NULL);


INSERT INTO processing_order_class_output_parameters VALUES (80, 79, 42);


INSERT INTO processing_order_input_filters VALUES (80, 78, 42);
INSERT INTO processing_order_input_filters VALUES (80, 77, 39);
INSERT INTO processing_order_input_filters VALUES (83, 81, 44);
INSERT INTO processing_order_input_filters VALUES (83, 82, 45);


INSERT INTO processing_order_input_product_classes VALUES (83, 44);
INSERT INTO processing_order_input_product_classes VALUES (83, 45);


INSERT INTO processing_order_output_parameters VALUES (80, NULL, 'INTEGER', '77', 'copernicusCollection');
INSERT INTO processing_order_output_parameters VALUES (80, NULL, 'INTEGER', '99', 'revision');
INSERT INTO processing_order_output_parameters VALUES (83, NULL, 'INTEGER', '77', 'copernicusCollection');
INSERT INTO processing_order_output_parameters VALUES (83, NULL, 'INTEGER', '99', 'revision');


INSERT INTO processing_order_requested_configured_processors VALUES (80, 37);
INSERT INTO processing_order_requested_configured_processors VALUES (83, 38);


INSERT INTO processing_order_requested_orbits VALUES (80, 17);
INSERT INTO processing_order_requested_orbits VALUES (80, 18);
INSERT INTO processing_order_requested_orbits VALUES (80, 19);


INSERT INTO processing_order_requested_product_classes VALUES (80, 44);
INSERT INTO processing_order_requested_product_classes VALUES (80, 45);
INSERT INTO processing_order_requested_product_classes VALUES (83, 46);


INSERT INTO product VALUES (62, 1, NULL, 'OPER', '2019-11-04 12:00:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 09:00:00', '2019-11-04 09:45:00', '861fbeb0-792a-4d59-a961-6709879271af', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (63, 1, NULL, 'OPER', '2019-11-04 12:01:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 09:45:00', '2019-11-04 10:30:00', 'e8c702c5-06e6-4cee-af5d-068d060c63b8', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (64, 1, NULL, 'OPER', '2019-11-04 12:02:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 10:30:00', '2019-11-04 11:15:00', '438c0146-d410-4016-8262-1d605ea20554', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (65, 1, NULL, 'OPER', '2019-11-04 12:03:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 11:15:00', '2019-11-04 12:00:00', '7f684ec1-e4d6-4cc5-aaa3-45afe4b80587', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (66, 1, NULL, 'OPER', '2019-11-04 15:00:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 12:00:00', '2019-11-04 12:45:00', '86c7ea7f-d9e1-4ff4-a41a-51cc4bb8ab83', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (67, 1, NULL, 'OPER', '2019-11-04 15:01:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 12:45:00', '2019-11-04 13:30:00', 'a0c196bf-6d03-442a-b271-049b80d12662', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (68, 1, NULL, 'OPER', '2019-11-04 15:02:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 13:30:00', '2019-11-04 14:15:00', 'b658c7e6-3bc3-435b-afb0-1f869d01b2c7', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (69, 1, NULL, 'OPER', '2019-11-04 15:03:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 14:15:00', '2019-11-04 15:00:00', '42cccd71-82da-4e15-b2fd-df9434c5caba', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (70, 1, NULL, 'OPER', '2019-11-04 18:00:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 15:00:00', '2019-11-04 15:45:00', 'd473b129-4c97-4502-9086-4d78d05ef7ad', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (71, 1, NULL, 'OPER', '2019-11-04 18:01:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 15:45:00', '2019-11-04 16:30:00', '4e63c63e-454f-40e4-8759-5a169c681cc6', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (72, 1, NULL, 'OPER', '2019-11-04 18:02:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 16:30:00', '2019-11-04 17:15:00', '0a968378-5048-47a7-89f8-63ca8daee116', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (73, 1, NULL, 'OPER', '2019-11-04 18:03:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 17:15:00', '2019-11-04 18:00:00', '8619500b-2a42-47dd-b37c-9ff9e53b1cad', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (74, 1, NULL, 'OPER', '2019-11-04 21:00:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 18:00:00', '2019-11-04 18:45:00', 'c994209c-7a6c-439e-b1d0-2c3ce7cca22f', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (75, 1, NULL, 'OPER', '2019-11-04 21:01:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 18:45:00', '2019-11-04 19:30:00', '37a8f5ec-161f-463e-8c9a-cfdbf29f513b', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (76, 1, NULL, 'OPER', '2019-10-01 12:00:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-09-01 00:00:00', '2019-10-01 00:00:00', 'f2ac4e68-6c52-4b9c-8fab-044f91cc66bf', NULL, NULL, NULL, NULL, 40);
INSERT INTO product VALUES (99, 1, NULL, 'OPER', '2019-11-04 12:00:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.514', NULL, NULL, NULL, '2019-11-04 09:00:00', '2019-11-04 09:45:00', '74cc4c7f-449a-4857-95ac-70c8ab301e72', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (100, 1, NULL, 'OPER', '2019-11-04 12:01:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.525', NULL, NULL, NULL, '2019-11-04 09:45:00', '2019-11-04 10:30:00', '3e8c96dd-a544-4565-9144-6d810271e0ac', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (101, 1, NULL, 'OPER', '2019-11-04 12:02:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.532', NULL, NULL, NULL, '2019-11-04 10:30:00', '2019-11-04 11:15:00', 'e5ef3f55-dcf8-4691-8d50-acca95edacbf', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (102, 1, NULL, 'OPER', '2019-11-04 12:03:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.54', NULL, NULL, NULL, '2019-11-04 11:15:00', '2019-11-04 12:00:00', '0c949449-e344-45af-85e4-8fa08d7b0f26', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (103, 1, NULL, 'OPER', '2019-11-04 15:00:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.546', NULL, NULL, NULL, '2019-11-04 12:00:00', '2019-11-04 12:45:00', 'eabb977c-f526-4404-a7f4-3941abe87840', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (104, 1, NULL, 'OPER', '2019-11-04 15:01:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.554', NULL, NULL, NULL, '2019-11-04 12:45:00', '2019-11-04 13:30:00', 'c68f0f93-7d0f-421e-ad73-65a074893c37', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (105, 1, NULL, 'OPER', '2019-11-04 15:02:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.56', NULL, NULL, NULL, '2019-11-04 13:30:00', '2019-11-04 14:15:00', '9b5a84bf-4fbe-4e15-9bf2-b73f36c465bb', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (106, 1, NULL, 'OPER', '2019-11-04 15:03:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.565', NULL, NULL, NULL, '2019-11-04 14:15:00', '2019-11-04 15:00:00', '496ca4c6-a569-4790-b244-c655c5bc107c', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (107, 1, NULL, 'OPER', '2019-11-04 18:00:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.572', NULL, NULL, NULL, '2019-11-04 15:00:00', '2019-11-04 15:45:00', 'aecac1e3-de19-4f1e-9dd2-8e3fcdfdf35e', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (108, 1, NULL, 'OPER', '2019-11-04 18:01:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.578', NULL, NULL, NULL, '2019-11-04 15:45:00', '2019-11-04 16:30:00', '00d8ab7a-95c9-412d-a2a1-0c685333b17a', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (109, 1, NULL, 'OPER', '2019-11-04 18:02:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.584', NULL, NULL, NULL, '2019-11-04 16:30:00', '2019-11-04 17:15:00', 'b61bf9a0-0b41-4982-b39e-beffc84995a0', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (110, 1, NULL, 'OPER', '2019-11-04 18:03:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.59', NULL, NULL, NULL, '2019-11-04 17:15:00', '2019-11-04 18:00:00', 'e98bd550-c680-44b2-8af9-62ac5f005fad', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (111, 1, NULL, 'OPER', '2019-11-04 21:00:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.596', NULL, NULL, NULL, '2019-11-04 18:00:00', '2019-11-04 18:45:00', 'dbdd8eb8-34d1-40a4-a789-b7ccff9e7710', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (112, 1, NULL, 'OPER', '2019-11-04 21:01:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.601', NULL, NULL, NULL, '2019-11-04 18:45:00', '2019-11-04 19:30:00', 'de322a30-6452-48bc-add0-9b879bb1c0ee', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (113, 1, NULL, 'OPER', '2019-10-01 12:00:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.608', NULL, NULL, NULL, '2019-09-01 00:00:00', '2019-10-01 00:00:00', '9432d65e-c55e-46a8-b51d-af8bc7cf77f5', NULL, NULL, NULL, NULL, 40);


INSERT INTO product_file VALUES (114, 1, '50545d8ad8486dd9297014cf2e769f71', '2019-11-04 12:00:10', 's3://proseo-data-001/99', 84, 'PTM_L0_20191104090000_20191104094500_20191104120000.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 99);
INSERT INTO product_file VALUES (115, 1, '3ca720d0d2a7dee0ba95a9b50047b5a0', '2019-11-04 12:01:10', 's3://proseo-data-001/100', 84, 'PTM_L0_20191104094500_20191104103000_20191104120100.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 100);
INSERT INTO product_file VALUES (116, 1, 'f65f2591c400a85ced1d7557a6644138', '2019-11-04 12:02:10', 's3://proseo-data-001/101', 84, 'PTM_L0_20191104103000_20191104111500_20191104120200.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 101);
INSERT INTO product_file VALUES (117, 1, '58a029e6b09ad1e749fc1ff523dbbf80', '2019-11-04 12:03:10', 's3://proseo-data-001/102', 84, 'PTM_L0_20191104111500_20191104120000_20191104120300.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 102);
INSERT INTO product_file VALUES (118, 1, '9ecc9bbc7abb0672efd51a39e8fc2c59', '2019-11-04 15:00:10', 's3://proseo-data-001/103', 84, 'PTM_L0_20191104120000_20191104124500_20191104150000.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 103);
INSERT INTO product_file VALUES (119, 1, 'fb720888a0d9bae6b16c1f9607c4de27', '2019-11-04 15:01:10', 's3://proseo-data-001/104', 84, 'PTM_L0_20191104124500_20191104133000_20191104150100.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 104);
INSERT INTO product_file VALUES (120, 1, '4ef20d31f2e051d16cf06db2bae2c76e', '2019-11-04 15:02:10', 's3://proseo-data-001/105', 84, 'PTM_L0_20191104133000_20191104141500_20191104150200.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 105);
INSERT INTO product_file VALUES (121, 1, '8b3a2f2f386c683ce9b1c68bb52b34d2', '2019-11-04 15:03:10', 's3://proseo-data-001/106', 84, 'PTM_L0_20191104141500_20191104150000_20191104150300.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 106);
INSERT INTO product_file VALUES (122, 1, '6bea82661ad200b8dc8a912bbc9c89f6', '2019-11-04 18:00:10', 's3://proseo-data-001/107', 84, 'PTM_L0_20191104150000_20191104154500_20191104180000.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 107);
INSERT INTO product_file VALUES (123, 1, 'a1953fa0c117f803c26902243bb0d3aa', '2019-11-04 18:01:10', 's3://proseo-data-001/108', 84, 'PTM_L0_20191104154500_20191104163000_20191104180100.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 108);
INSERT INTO product_file VALUES (124, 1, 'a9fb7fc052b5aced6f6dc9d753bbe790', '2019-11-04 18:02:10', 's3://proseo-data-001/109', 84, 'PTM_L0_20191104163000_20191104171500_20191104180200.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 109);
INSERT INTO product_file VALUES (125, 1, 'b8b5304d83100a56114cd5ca6a6bc581', '2019-11-04 18:03:10', 's3://proseo-data-001/110', 84, 'PTM_L0_20191104171500_20191104180000_20191104180300.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 110);
INSERT INTO product_file VALUES (126, 1, '9211729e47c5e2487de302f1ca48f4b9', '2019-11-04 21:00:10', 's3://proseo-data-001/111', 84, 'PTM_L0_20191104180000_20191104184500_20191104210000.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 111);
INSERT INTO product_file VALUES (127, 1, '95eb634992099c7ebb7bf5b76b243da1', '2019-11-04 21:01:10', 's3://proseo-data-001/112', 84, 'PTM_L0_20191104184500_20191104193000_20191104210100.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 112);
INSERT INTO product_file VALUES (128, 1, 'b754f424e3dad8f1c107ed6b8ad9d06a', '2019-10-01 12:01:00', 's3://proseo-data-001/113', 51090, 'bulletinb-380.xml', 'S3', NULL, NULL, NULL, NULL, 61, 113);


INSERT INTO product_parameters VALUES (62, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (63, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (64, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (65, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (66, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (67, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (68, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (69, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (70, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (71, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (72, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (73, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (74, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (75, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (76, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (99, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (100, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (101, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (102, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (103, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (104, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (105, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (106, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (107, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (108, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (109, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (110, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (111, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (112, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (113, NULL, 'INTEGER', '1', 'revision');


INSERT INTO simple_policy VALUES (48, 1, 'LatestValIntersect');
INSERT INTO simple_policy VALUES (50, 1, 'ValIntersect');
INSERT INTO simple_policy VALUES (52, 1, 'LatestValCover');
INSERT INTO simple_policy VALUES (54, 1, 'LatestValCover');
INSERT INTO simple_policy VALUES (56, 1, 'ValIntersect');
INSERT INTO simple_policy VALUES (58, 1, 'ValIntersect');


INSERT INTO simple_policy_delta_times VALUES (48, 60, 6, 0);
INSERT INTO simple_policy_delta_times VALUES (48, 60, 6, 1);
INSERT INTO simple_policy_delta_times VALUES (50, 0, 6, 0);
INSERT INTO simple_policy_delta_times VALUES (50, 0, 6, 1);
INSERT INTO simple_policy_delta_times VALUES (52, 0, 6, 0);
INSERT INTO simple_policy_delta_times VALUES (52, 0, 6, 1);
INSERT INTO simple_policy_delta_times VALUES (54, 0, 6, 0);
INSERT INTO simple_policy_delta_times VALUES (54, 0, 6, 1);
INSERT INTO simple_policy_delta_times VALUES (56, 0, 6, 0);
INSERT INTO simple_policy_delta_times VALUES (56, 0, 6, 1);
INSERT INTO simple_policy_delta_times VALUES (58, 0, 6, 0);
INSERT INTO simple_policy_delta_times VALUES (58, 0, 6, 1);


INSERT INTO simple_selection_rule VALUES (47, 1, 'AUX_IERS_B', true, 0, 'OPER', 40, 41);
INSERT INTO simple_selection_rule VALUES (49, 1, 'PTM_L0', true, 0, 'OPER', 39, 41);
INSERT INTO simple_selection_rule VALUES (51, 1, 'PTM_L1B', true, 0, 'OPER', 41, 44);
INSERT INTO simple_selection_rule VALUES (53, 1, 'PTM_L1B_P1', true, 0, 'OPER', 42, 45);
INSERT INTO simple_selection_rule VALUES (55, 1, 'PTM_L2_B', true, 90, 'OPER', 45, 46);
INSERT INTO simple_selection_rule VALUES (57, 1, 'PTM_L2_A', true, 90, 'OPER', 44, 46);


INSERT INTO simple_selection_rule_configured_processors VALUES (47, 36);
INSERT INTO simple_selection_rule_configured_processors VALUES (49, 36);
INSERT INTO simple_selection_rule_configured_processors VALUES (51, 37);
INSERT INTO simple_selection_rule_configured_processors VALUES (53, 37);
INSERT INTO simple_selection_rule_configured_processors VALUES (55, 38);
INSERT INTO simple_selection_rule_configured_processors VALUES (57, 38);


INSERT INTO simple_selection_rule_simple_policies VALUES (47, 48);
INSERT INTO simple_selection_rule_simple_policies VALUES (49, 50);
INSERT INTO simple_selection_rule_simple_policies VALUES (51, 52);
INSERT INTO simple_selection_rule_simple_policies VALUES (53, 54);
INSERT INTO simple_selection_rule_simple_policies VALUES (55, 56);
INSERT INTO simple_selection_rule_simple_policies VALUES (57, 58);


INSERT INTO task VALUES (27, 1, 10, true, NULL, 3, 'ptm_l01b', '0.1.0', 26);
INSERT INTO task VALUES (29, 1, 10, true, NULL, 3, 'ptm_l2', '0.1.0', 28);
INSERT INTO task VALUES (31, 1, 10, true, NULL, 3, 'ptm_l3', '0.1.0', 30);


INSERT INTO workflow VALUES (59, 1, 'Create level 3 products from level 2', true, 'PTML2-to-L3', 'TEST', 'OPER', NULL, 0, 'NONE', 'd4466bcb-3256-416a-9d9c-dc86592e43bb', '1.0', 38, 44, 1, 46);


INSERT INTO workflow_option VALUES (60, 1, '16', '', 'Threads', 'NUMBER', 59);


INSERT INTO workflow_output_parameters VALUES (59, NULL, 'INTEGER', '99', 'revision');