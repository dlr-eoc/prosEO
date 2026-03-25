--
-- prosEO Database Schema Update for prosEO 2.0.0
--
-- Date: 2025-12-17
--

--
-- Optional changes (introduced by switch to Hibernate 6)
-- Keep old behaviour for database ID generation (using hibernate_sequence only) by specifying the following in application.yml:
--   spring.jpa.properties.hibernate.id.db_structure_naming_strategy: legacy
--

--
-- Sequences per entity
--

--
-- Name: api_metrics_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.api_metrics_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: class_output_parameter_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.class_output_parameter_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.class_output_parameter_seq OWNER TO postgres;

--
-- Name: configuration_input_file_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.configuration_input_file_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.configuration_input_file_seq OWNER TO postgres;

--
-- Name: configuration_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.configuration_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.configuration_seq OWNER TO postgres;

--
-- Name: configured_processor_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.configured_processor_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.configured_processor_seq OWNER TO postgres;

--
-- Name: group_members_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.group_members_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.group_members_seq OWNER TO postgres;

--
-- Name: groups_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.groups_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.groups_seq OWNER TO postgres;

--
-- Name: input_filter_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.input_filter_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.input_filter_seq OWNER TO postgres;

--
-- Name: job_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.job_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.job_seq OWNER TO postgres;

--
-- Name: job_step_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.job_step_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.job_step_seq OWNER TO postgres;

--
-- Name: mission_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mission_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mission_seq OWNER TO postgres;

--
-- Name: mon_ext_service_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_ext_service_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_ext_service_seq OWNER TO postgres;

--
-- Name: mon_ext_service_state_operation_day_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_ext_service_state_operation_day_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_ext_service_state_operation_day_seq OWNER TO postgres;

--
-- Name: mon_ext_service_state_operation_month_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_ext_service_state_operation_month_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_ext_service_state_operation_month_seq OWNER TO postgres;

--
-- Name: mon_ext_service_state_operation_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_ext_service_state_operation_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_ext_service_state_operation_seq OWNER TO postgres;

--
-- Name: mon_order_state_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_order_state_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_order_state_seq OWNER TO postgres;

--
-- Name: mon_product_production_day_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_product_production_day_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_product_production_day_seq OWNER TO postgres;

--
-- Name: mon_product_production_hour_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_product_production_hour_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_product_production_hour_seq OWNER TO postgres;

--
-- Name: mon_product_production_month_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_product_production_month_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_product_production_month_seq OWNER TO postgres;

--
-- Name: mon_service_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_service_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_service_seq OWNER TO postgres;

--
-- Name: mon_service_state_operation_day_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_service_state_operation_day_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_service_state_operation_day_seq OWNER TO postgres;

--
-- Name: mon_service_state_operation_month_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_service_state_operation_month_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_service_state_operation_month_seq OWNER TO postgres;

--
-- Name: mon_service_state_operation_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_service_state_operation_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_service_state_operation_seq OWNER TO postgres;

--
-- Name: mon_service_state_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.mon_service_state_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.mon_service_state_seq OWNER TO postgres;

--
-- Name: orbit_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.orbit_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.orbit_seq OWNER TO postgres;

--
-- Name: processing_facility_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.processing_facility_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.processing_facility_seq OWNER TO postgres;

--
-- Name: processing_order_history_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.processing_order_history_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.processing_order_history_seq OWNER TO postgres;

--
-- Name: processing_order_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.processing_order_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.processing_order_seq OWNER TO postgres;

--
-- Name: processor_class_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.processor_class_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.processor_class_seq OWNER TO postgres;

--
-- Name: processor_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.processor_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.processor_seq OWNER TO postgres;

--
-- Name: product_archive_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.product_archive_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.product_archive_seq OWNER TO postgres;

--
-- Name: product_class_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.product_class_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.product_class_seq OWNER TO postgres;

--
-- Name: product_file_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.product_file_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.product_file_seq OWNER TO postgres;

--
-- Name: product_query_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.product_query_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.product_query_seq OWNER TO postgres;

--
-- Name: product_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.product_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.product_seq OWNER TO postgres;

--
-- Name: simple_policy_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.simple_policy_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.simple_policy_seq OWNER TO postgres;

--
-- Name: simple_selection_rule_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.simple_selection_rule_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.simple_selection_rule_seq OWNER TO postgres;

--
-- Name: spacecraft_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.spacecraft_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.spacecraft_seq OWNER TO postgres;

--
-- Name: task_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.task_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.task_seq OWNER TO postgres;

--
-- Name: workflow_option_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.workflow_option_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.workflow_option_seq OWNER TO postgres;

--
-- Name: workflow_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.workflow_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.workflow_seq OWNER TO postgres;



--
-- Table changes: "version" without "NOT NULL", semantic constraints for enums etc.
--

ALTER TABLE public.api_metrics
  ALTER COLUMN version DROP NOT NULL,
  ALTER COLUMN metrictype TYPE smallint,
  ADD CONSTRAINT api_metrics_metrictype_check CHECK (((metrictype >= 0) AND (metrictype <= 1)));

ALTER TABLE public.class_output_parameter
  ALTER COLUMN version DROP NOT NULL;
  
ALTER TABLE public.class_output_parameter_output_parameters
  ADD CONSTRAINT class_output_parameter_output_parameters_parameter_type_check 
    CHECK (((parameter_type)::text = ANY ((ARRAY['STRING'::character varying, 'BOOLEAN'::character varying, 
      'INTEGER'::character varying, 'DOUBLE'::character varying, 'INSTANT'::character varying])::text[])));
  
ALTER TABLE public.configuration
  ALTER COLUMN version DROP NOT NULL,
  ADD CONSTRAINT configuration_product_quality_check 
    CHECK (((product_quality)::text = ANY ((ARRAY['NOMINAL'::character varying, 'EXPERIMENTAL'::character varying, 
      'TEST'::character varying, 'SYSTEMATIC'::character varying])::text[])));

ALTER TABLE public.configuration_dyn_proc_parameters
  ADD CONSTRAINT configuration_dyn_proc_parameters_parameter_type_check 
    CHECK (((parameter_type)::text = ANY ((ARRAY['STRING'::character varying, 'BOOLEAN'::character varying, 
      'INTEGER'::character varying, 'DOUBLE'::character varying, 'INSTANT'::character varying])::text[])));
    
ALTER TABLE public.configuration_input_file
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.configured_processor
  ALTER COLUMN version DROP NOT NULL;
  
ALTER TABLE public.input_filter_filter_conditions
  ADD CONSTRAINT input_filter_filter_conditions_parameter_type_check 
    CHECK (((parameter_type)::text = ANY ((ARRAY['STRING'::character varying, 'BOOLEAN'::character varying, 
      'INTEGER'::character varying, 'DOUBLE'::character varying, 'INSTANT'::character varying])::text[])));
      
ALTER TABLE public.job
  ALTER COLUMN version DROP NOT NULL,
  ADD CONSTRAINT job_job_state_check 
    CHECK (((job_state)::text = ANY ((ARRAY['INITIAL'::character varying, 'PLANNED'::character varying, 
      'RELEASED'::character varying, 'STARTED'::character varying, 'ON_HOLD'::character varying, 
      'COMPLETED'::character varying, 'FAILED'::character varying, 'CLOSED'::character varying])::text[])));
      
ALTER TABLE public.job_step
  ALTER COLUMN version DROP NOT NULL,
  ALTER COLUMN processing_completion_time TYPE timestamp(6) with time zone,
  ALTER COLUMN processing_start_time TYPE timestamp(6) with time zone,
  ALTER COLUMN stderr_log_level TYPE smallint,
  ALTER COLUMN stdout_log_level TYPE smallint,
  ADD CONSTRAINT job_step_job_step_state_check 
    CHECK (((job_step_state)::text = ANY ((ARRAY['PLANNED'::character varying, 'WAITING_INPUT'::character varying, 
      'READY'::character varying, 'RUNNING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying, 
      'CLOSED'::character varying])::text[]))),
  ADD CONSTRAINT job_step_stderr_log_level_check CHECK (((stderr_log_level >= 0) AND (stderr_log_level <= 4))),
  ADD CONSTRAINT job_step_stdout_log_level_check CHECK (((stdout_log_level >= 0) AND (stdout_log_level <= 4)));

ALTER TABLE public.job_step_output_parameters
  ADD CONSTRAINT job_step_output_parameters_parameter_type_check 
    CHECK (((parameter_type)::text = ANY ((ARRAY['STRING'::character varying, 'BOOLEAN'::character varying, 
      'INTEGER'::character varying, 'DOUBLE'::character varying, 'INSTANT'::character varying])::text[])));

ALTER TABLE public.mission
  ALTER COLUMN version DROP NOT NULL,
  ALTER COLUMN order_retention_period TYPE numeric(21,0),
  ALTER COLUMN product_retention_period TYPE numeric(21,0);
  
ALTER TABLE public.mon_ext_service
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.mon_ext_service_state_operation
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.mon_ext_service_state_operation_day
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.mon_ext_service_state_operation_month
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.mon_order_state
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.mon_product_production_day
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.mon_product_production_hour
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.mon_product_production_month
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.mon_service
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.mon_service_state
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.mon_service_state_operation
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.mon_service_state_operation_day
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.mon_service_state_operation_month
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.orbit
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.processing_facility
  ALTER COLUMN version DROP NOT NULL,
  ADD CONSTRAINT processing_facility_default_storage_type_check 
    CHECK (((default_storage_type)::text = ANY ((ARRAY['S3'::character varying, 'ALLUXIO'::character varying, 
      'POSIX'::character varying, 'OTHER'::character varying])::text[]))),
  ADD CONSTRAINT processing_facility_facility_state_check 
  CHECK (((facility_state)::text = ANY ((ARRAY['DISABLED'::character varying, 'STOPPED'::character varying, 
  'STARTING'::character varying, 'RUNNING'::character varying, 'STOPPING'::character varying])::text[])));
 
ALTER TABLE public.processing_order
  ALTER COLUMN version DROP NOT NULL,
  ALTER COLUMN product_retention_period TYPE numeric(21,0),
  ALTER COLUMN slice_duration TYPE numeric(21,0),
  ALTER COLUMN slice_overlap TYPE numeric(21,0),
  ADD CONSTRAINT processing_order_order_source_check 
    CHECK (((order_source)::text = ANY ((ARRAY['CLI'::character varying, 'GUI'::character varying, 'ODIP'::character varying, 
    'OTHER'::character varying])::text[]))),
  ADD CONSTRAINT processing_order_order_state_check 
    CHECK (((order_state)::text = ANY ((ARRAY['INITIAL'::character varying, 'APPROVED'::character varying, 
      'PLANNING'::character varying, 'PLANNING_FAILED'::character varying, 'PLANNED'::character varying, 
      'RELEASING'::character varying, 'RELEASED'::character varying, 'RUNNING'::character varying, 'SUSPENDING'::character varying, 
      'COMPLETED'::character varying, 'FAILED'::character varying, 'CLOSED'::character varying])::text[]))),
  ADD CONSTRAINT processing_order_production_type_check 
    CHECK (((production_type)::text = ANY ((ARRAY['SYSTEMATIC'::character varying, 'ON_DEMAND_DEFAULT'::character varying, 
      'ON_DEMAND_NON_DEFAULT'::character varying])::text[]))),
  ADD CONSTRAINT processing_order_slicing_type_check 
    CHECK (((slicing_type)::text = ANY ((ARRAY['ORBIT'::character varying, 'CALENDAR_DAY'::character varying, 
      'CALENDAR_MONTH'::character varying, 'CALENDAR_YEAR'::character varying, 'TIME_SLICE'::character varying, 
      'NONE'::character varying])::text[])));

ALTER TABLE public.processing_order_dynamic_processing_parameters
  ADD CONSTRAINT processing_order_dynamic_processing_parame_parameter_type_check 
    CHECK (((parameter_type)::text = ANY ((ARRAY['STRING'::character varying, 'BOOLEAN'::character varying, 
    'INTEGER'::character varying, 'DOUBLE'::character varying, 'INSTANT'::character varying])::text[])));
    
ALTER TABLE public.processing_order_history
  ALTER COLUMN version DROP NOT NULL,
  ADD CONSTRAINT processing_order_history_order_state_check 
    CHECK (((order_state)::text = ANY ((ARRAY['INITIAL'::character varying, 'APPROVED'::character varying, 
      'PLANNING'::character varying, 'PLANNING_FAILED'::character varying, 'PLANNED'::character varying, 
      'RELEASING'::character varying, 'RELEASED'::character varying, 'RUNNING'::character varying, 'SUSPENDING'::character varying, 
      'COMPLETED'::character varying, 'FAILED'::character varying, 'CLOSED'::character varying])::text[])));

ALTER TABLE public.processing_order_mon_order_progress
  ALTER COLUMN all_job_steps DROP NOT NULL,
  ALTER COLUMN completed_job_steps DROP NOT NULL,
  ALTER COLUMN failed_job_steps DROP NOT NULL,
  ALTER COLUMN finished_job_steps DROP NOT NULL,
  ALTER COLUMN running_job_steps DROP NOT NULL;

ALTER TABLE public.processing_order_output_parameters
  ADD CONSTRAINT processing_order_output_parameters_parameter_type_check 
    CHECK (((parameter_type)::text = ANY ((ARRAY['STRING'::character varying, 'BOOLEAN'::character varying, 
      'INTEGER'::character varying, 'DOUBLE'::character varying, 'INSTANT'::character varying])::text[])));

ALTER TABLE public.processor
  ALTER COLUMN version DROP NOT NULL,
  ADD CONSTRAINT processor_job_order_version_check CHECK (((job_order_version)::text = ANY ((ARRAY['MMFI_1_8'::character varying, 'GMES_1_1'::character varying])::text[])));

ALTER TABLE public.processor_class
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.product
  ALTER COLUMN version DROP NOT NULL,
  ADD CONSTRAINT product_product_quality_check 
    CHECK (((product_quality)::text = ANY ((ARRAY['NOMINAL'::character varying, 'EXPERIMENTAL'::character varying, 
    'TEST'::character varying, 'SYSTEMATIC'::character varying])::text[]))),
  ADD CONSTRAINT product_production_type_check 
    CHECK (((production_type)::text = ANY ((ARRAY['SYSTEMATIC'::character varying, 'ON_DEMAND_DEFAULT'::character varying, 
    'ON_DEMAND_NON_DEFAULT'::character varying])::text[])));

ALTER TABLE public.product_archive
  ALTER COLUMN version DROP NOT NULL,
  ADD CONSTRAINT product_archive_archive_type_check 
    CHECK (((archive_type)::text = ANY ((ARRAY['AIP'::character varying, 'AUXIP'::character varying, 'PODIP'::character varying, 
    'PRIP'::character varying, 'SIMPLEAIP'::character varying])::text[])));

ALTER TABLE public.product_class
  ALTER COLUMN version DROP NOT NULL,
  ADD CONSTRAINT product_class_default_slicing_type_check 
    CHECK (((default_slicing_type)::text = ANY ((ARRAY['ORBIT'::character varying, 'CALENDAR_DAY'::character varying, 
      'CALENDAR_MONTH'::character varying, 'CALENDAR_YEAR'::character varying, 'TIME_SLICE'::character varying, 
      'NONE'::character varying])::text[]))),
  ADD CONSTRAINT product_class_processing_level_check 
    CHECK (((processing_level)::text = ANY ((ARRAY['L0'::character varying, 'L1'::character varying, 'L1A'::character varying, 
    'L1B'::character varying, 'L1C'::character varying, 'L2'::character varying, 'L2A'::character varying, 'L2B'::character varying,
    'L2C'::character varying, 'L3'::character varying, 'L4'::character varying])::text[]))),
  ADD CONSTRAINT product_class_visibility_check 
    CHECK (((visibility)::text = ANY ((ARRAY['INTERNAL'::character varying, 'RESTRICTED'::character varying, 
    'PUBLIC'::character varying])::text[])));

ALTER TABLE public.product_file
  ALTER COLUMN version DROP NOT NULL,
  ADD CONSTRAINT product_file_storage_type_check 
    CHECK (((storage_type)::text = ANY ((ARRAY['S3'::character varying, 'ALLUXIO'::character varying, 'POSIX'::character varying, 
      'OTHER'::character varying])::text[])));

ALTER TABLE public.product_parameters
  ADD CONSTRAINT product_parameters_parameter_type_check 
    CHECK (((parameter_type)::text = ANY ((ARRAY['STRING'::character varying, 'BOOLEAN'::character varying, 
    'INTEGER'::character varying, 'DOUBLE'::character varying, 'INSTANT'::character varying])::text[])));

CREATE TABLE product_query_copy (
	id BIGINT NOT NULL,
	version INTEGER NULL DEFAULT NULL,
	in_download BOOLEAN NULL DEFAULT NULL,
	is_satisfied BOOLEAN NULL DEFAULT NULL,
	jpql_query_condition TEXT NULL DEFAULT NULL,
	minimum_coverage SMALLINT NULL DEFAULT NULL,
	sql_query_condition TEXT NULL DEFAULT NULL,
	generating_rule_id BIGINT NULL DEFAULT NULL,
	job_step_id BIGINT NULL DEFAULT NULL,
	requested_product_class_id BIGINT NULL DEFAULT NULL
	);
	
insert into product_query_copy (id, jpql_query_condition, sql_query_condition) 
	SELECT p.id, p.jpql_query_condition, p.sql_query_condition FROM product_query p;

UPDATE product_query p SET jpql_query_condition = NULL, sql_query_condition = NULL;

ALTER TABLE public.product_query
  ALTER COLUMN version DROP NOT NULL,
  ALTER COLUMN jpql_query_condition TYPE oid USING jpql_query_condition::oid,
  ALTER COLUMN sql_query_condition TYPE oid USING sql_query_condition::oid;
  
UPDATE product_query p SET jpql_query_condition = lo_from_bytea(0, DECODE (pc.jpql_query_condition, 'escape')), sql_query_condition = lo_from_bytea(0, DECODE (pc.sql_query_condition, 'escape')) 
  FROM product_query_copy pc WHERE p.id = pc.id;
  
DROP TABLE product_query_copy;

ALTER TABLE public.product_query_filter_conditions
  ADD CONSTRAINT product_query_filter_conditions_parameter_type_check 
    CHECK (((parameter_type)::text = ANY ((ARRAY['STRING'::character varying, 'BOOLEAN'::character varying, 
    'INTEGER'::character varying, 'DOUBLE'::character varying, 'INSTANT'::character varying])::text[])));

ALTER TABLE public.simple_policy
  ALTER COLUMN version DROP NOT NULL,
  ADD CONSTRAINT simple_policy_policy_type_check 
    CHECK (((policy_type)::text = ANY ((ARRAY['ValCover'::character varying, 'LatestValCover'::character varying, 
    'ValIntersect'::character varying, 'LatestValIntersect'::character varying, 'LatestValidityClosest'::character varying, 
    'BestCenteredCover'::character varying, 'LatestValCoverClosest'::character varying, 'LargestOverlap'::character varying, 
    'LargestOverlap85'::character varying, 'LatestValidity'::character varying, 'LatestValCoverNewestValidity'::character varying, 
    'ClosestStartValidity'::character varying, 'ClosestStopValidity'::character varying, 'LatestStartValidity'::character varying, 
    'LatestStopValidity'::character varying, 'ValIntersectWithoutDuplicates'::character varying, 
    'LastCreated'::character varying])::text[])));

ALTER TABLE public.simple_policy_delta_times
  ALTER COLUMN duration DROP NOT NULL,
  ALTER COLUMN unit TYPE smallint,
  ADD CONSTRAINT simple_policy_delta_times_unit_check CHECK (((unit >= 0) AND (unit <= 6)));

ALTER TABLE public.simple_selection_rule
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.simple_selection_rule_filter_conditions
  ADD CONSTRAINT simple_selection_rule_filter_conditions_parameter_type_check 
    CHECK (((parameter_type)::text = ANY ((ARRAY['STRING'::character varying, 'BOOLEAN'::character varying, 
    'INTEGER'::character varying, 'DOUBLE'::character varying, 'INSTANT'::character varying])::text[])));

ALTER TABLE public.spacecraft
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.task
  ALTER COLUMN version DROP NOT NULL;

ALTER TABLE public.users
  ALTER COLUMN expiration_date TYPE timestamp(6) without time zone,
  ALTER COLUMN password_expiration_date TYPE timestamp(6) without time zone,
  ALTER COLUMN last_access_date TYPE timestamp(6) without time zone;

ALTER TABLE public.workflow
  ALTER COLUMN version DROP NOT NULL,
  ALTER COLUMN slice_duration TYPE numeric(21,0),
  ALTER COLUMN slice_overlap TYPE numeric(21,0),
  ADD CONSTRAINT workflow_slicing_type_check 
    CHECK (((slicing_type)::text = ANY ((ARRAY['ORBIT'::character varying, 'CALENDAR_DAY'::character varying, 
    'CALENDAR_MONTH'::character varying, 'CALENDAR_YEAR'::character varying, 'TIME_SLICE'::character varying, 
    'NONE'::character varying])::text[])));
  
ALTER TABLE public.workflow_option
  ALTER COLUMN version DROP NOT NULL,
  ADD CONSTRAINT workflow_option_type_check 
    CHECK (((type)::text = ANY ((ARRAY['STRING'::character varying, 'NUMBER'::character varying, 
    'DATENUMBER'::character varying])::text[])));
  
ALTER TABLE public.workflow_output_parameters
  ADD CONSTRAINT workflow_output_parameters_parameter_type_check 
    CHECK (((parameter_type)::text = ANY ((ARRAY['STRING'::character varying, 'BOOLEAN'::character varying, 
    'INTEGER'::character varying, 'DOUBLE'::character varying, 'INSTANT'::character varying])::text[])));

ALTER TABLE ONLY public.groups_group_members
    DROP CONSTRAINT uk_132lanwqs6liav9syek4s96xv,
    ADD CONSTRAINT uk132lanwqs6liav9syek4s96xv UNIQUE (group_members_id);

ALTER TABLE ONLY public.configuration_static_input_files
    DROP CONSTRAINT uk_2y140wa1pggeycgihvnex0a9c,
    ADD CONSTRAINT uk2y140wa1pggeycgihvnex0a9c UNIQUE (static_input_files_id);

ALTER TABLE ONLY public.product
    ADD CONSTRAINT uk522jf0inen9r4jt9m9rhsf5bc UNIQUE (job_step_id);

ALTER TABLE ONLY public.simple_selection_rule_simple_policies
    DROP CONSTRAINT uk_7jrn9t62kdspngixrembpkrd7,
    ADD CONSTRAINT uk7jrn9t62kdspngixrembpkrd7 UNIQUE (simple_policies_id);

ALTER TABLE ONLY public.users_group_memberships
    DROP CONSTRAINT uk_e2ijwadyxqhcr2aldhs624px,
    ADD CONSTRAINT uke2ijwadyxqhcr2aldhs624px UNIQUE (group_memberships_id);


--
-- Update new sequences with initial values from existing tables
-- (next value returned by the sequence will be "<max id value of table> + 1 + <allocation size>" as per
-- the Hibernate 6 Migration Guide)
--
SELECT SETVAL('api_metrics_seq', (SELECT max(id) + 50 FROM api_metrics));
SELECT SETVAL('class_output_parameter_seq', (SELECT max(id) + 50 FROM class_output_parameter));
SELECT SETVAL('configuration_input_file_seq', (SELECT max(id) + 50 FROM configuration_input_file));
SELECT SETVAL('configuration_seq', (SELECT max(id) + 50 FROM configuration));
SELECT SETVAL('configured_processor_seq', (SELECT max(id) + 50 FROM configured_processor));
SELECT SETVAL('group_members_seq', (SELECT max(id) + 50 FROM group_members));
SELECT SETVAL('groups_seq', (SELECT max(id) + 50 FROM groups));
SELECT SETVAL('input_filter_seq', (SELECT max(id) + 50 FROM input_filter));
SELECT SETVAL('job_seq', (SELECT max(id) + 50 FROM job));
SELECT SETVAL('job_step_seq', (SELECT max(id) + 50 FROM job_step));
SELECT SETVAL('mission_seq', (SELECT max(id) + 50 FROM mission));
SELECT SETVAL('mon_ext_service_seq', (SELECT max(id) + 50 FROM mon_ext_service));
SELECT SETVAL('mon_ext_service_state_operation_day_seq', (SELECT max(id) + 50 FROM mon_ext_service_state_operation_day));
SELECT SETVAL('mon_ext_service_state_operation_month_seq', (SELECT max(id) + 50 FROM mon_ext_service_state_operation_month));
SELECT SETVAL('mon_ext_service_state_operation_seq', (SELECT max(id) + 50 FROM mon_ext_service_state_operation));
SELECT SETVAL('mon_order_state_seq', (SELECT max(id) + 50 FROM mon_order_state));
SELECT SETVAL('mon_product_production_day_seq', (SELECT max(id) + 50 FROM mon_product_production_day));
SELECT SETVAL('mon_product_production_hour_seq', (SELECT max(id) + 50 FROM mon_product_production_hour));
SELECT SETVAL('mon_product_production_month_seq', (SELECT max(id) + 50 FROM mon_product_production_month));
SELECT SETVAL('mon_service_seq', (SELECT max(id) + 50 FROM mon_service));
SELECT SETVAL('mon_service_state_operation_day_seq', (SELECT max(id) + 50 FROM mon_service_state_operation_day));
SELECT SETVAL('mon_service_state_operation_month_seq', (SELECT max(id) + 50 FROM mon_service_state_operation_month));
SELECT SETVAL('mon_service_state_operation_seq', (SELECT max(id) + 50 FROM mon_service_state_operation));
SELECT SETVAL('mon_service_state_seq', (SELECT max(id) + 50 FROM mon_service_state));
SELECT SETVAL('orbit_seq', (SELECT max(id) + 50 FROM orbit));
SELECT SETVAL('processing_facility_seq', (SELECT max(id) + 50 FROM processing_facility));
SELECT SETVAL('processing_order_history_seq', (SELECT max(id) + 50 FROM processing_order_history));
SELECT SETVAL('processing_order_seq', (SELECT max(id) + 50 FROM processing_order));
SELECT SETVAL('processor_class_seq', (SELECT max(id) + 50 FROM processor_class));
SELECT SETVAL('processor_seq', (SELECT max(id) + 50 FROM processor));
SELECT SETVAL('product_archive_seq', (SELECT max(id) + 50 FROM product_archive));
SELECT SETVAL('product_class_seq', (SELECT max(id) + 50 FROM product_class));
SELECT SETVAL('product_file_seq', (SELECT max(id) + 50 FROM product_file));
SELECT SETVAL('product_query_seq', (SELECT max(id) + 50 FROM product_query));
SELECT SETVAL('product_seq', (SELECT max(id) + 50 FROM product));
SELECT SETVAL('simple_policy_seq', (SELECT max(id) + 50 FROM simple_policy));
SELECT SETVAL('simple_selection_rule_seq', (SELECT max(id) + 50 FROM simple_selection_rule));
SELECT SETVAL('spacecraft_seq', (SELECT max(id) + 50 FROM spacecraft));
SELECT SETVAL('task_seq', (SELECT max(id) + 50 FROM task));
SELECT SETVAL('workflow_option_seq', (SELECT max(id) + 50 FROM workflow_option));
SELECT SETVAL('workflow_seq', (SELECT max(id) + 50 FROM workflow));

DROP SEQUENCE hibernate_sequence;

