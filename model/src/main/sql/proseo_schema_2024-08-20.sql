--
-- PostgreSQL database dump
--

-- Dumped from database version 11.16 (Debian 11.16-1.pgdg90+1)
-- Dumped by pg_dump version 11.16 (Debian 11.16-1.pgdg90+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: api_metrics; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.api_metrics (
    id bigint NOT NULL,
    version integer NOT NULL,
    count bigint NOT NULL,
    gauge character varying(255),
    metrictype integer,
    name character varying(255),
    "timestamp" timestamp without time zone
);


ALTER TABLE public.api_metrics OWNER TO postgres;

--
-- Name: authorities; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.authorities (
    username character varying(255) NOT NULL,
    authority character varying(255) NOT NULL
);


ALTER TABLE public.authorities OWNER TO postgres;

--
-- Name: class_output_parameter; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.class_output_parameter (
    id bigint NOT NULL,
    version integer NOT NULL
);


ALTER TABLE public.class_output_parameter OWNER TO postgres;

--
-- Name: class_output_parameter_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.class_output_parameter_output_parameters (
    class_output_parameter_id bigint NOT NULL,
    parameter_clob oid,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    output_parameters_key character varying(255) NOT NULL
);


ALTER TABLE public.class_output_parameter_output_parameters OWNER TO postgres;

--
-- Name: configuration; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.configuration (
    id bigint NOT NULL,
    version integer NOT NULL,
    configuration_version character varying(255),
    mode character varying(255),
    product_quality character varying(255),
    processor_class_id bigint
);


ALTER TABLE public.configuration OWNER TO postgres;

--
-- Name: configuration_configuration_files; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.configuration_configuration_files (
    configuration_id bigint NOT NULL,
    file_name character varying(255),
    file_version character varying(255)
);


ALTER TABLE public.configuration_configuration_files OWNER TO postgres;

--
-- Name: configuration_docker_run_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.configuration_docker_run_parameters (
    configuration_id bigint NOT NULL,
    docker_run_parameters character varying(255),
    docker_run_parameters_key character varying(255) NOT NULL
);


ALTER TABLE public.configuration_docker_run_parameters OWNER TO postgres;

--
-- Name: configuration_dyn_proc_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.configuration_dyn_proc_parameters (
    configuration_id bigint NOT NULL,
    parameter_clob oid,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    dyn_proc_parameters_key character varying(255) NOT NULL
);


ALTER TABLE public.configuration_dyn_proc_parameters OWNER TO postgres;

--
-- Name: configuration_input_file; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.configuration_input_file (
    id bigint NOT NULL,
    version integer NOT NULL,
    file_name_type character varying(255),
    file_type character varying(255)
);


ALTER TABLE public.configuration_input_file OWNER TO postgres;

--
-- Name: configuration_input_file_file_names; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.configuration_input_file_file_names (
    configuration_input_file_id bigint NOT NULL,
    file_names character varying(255)
);


ALTER TABLE public.configuration_input_file_file_names OWNER TO postgres;

--
-- Name: configuration_static_input_files; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.configuration_static_input_files (
    configuration_id bigint NOT NULL,
    static_input_files_id bigint NOT NULL
);


ALTER TABLE public.configuration_static_input_files OWNER TO postgres;

--
-- Name: configured_processor; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.configured_processor (
    id bigint NOT NULL,
    version integer NOT NULL,
    enabled boolean,
    identifier character varying(255),
    uuid uuid NOT NULL,
    configuration_id bigint,
    processor_id bigint
);


ALTER TABLE public.configured_processor OWNER TO postgres;

--
-- Name: group_authorities; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.group_authorities (
    group_id bigint NOT NULL,
    authority character varying(255) NOT NULL
);


ALTER TABLE public.group_authorities OWNER TO postgres;

--
-- Name: group_members; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.group_members (
    id bigint NOT NULL,
    group_id bigint NOT NULL,
    username character varying(255) NOT NULL
);


ALTER TABLE public.group_members OWNER TO postgres;

--
-- Name: groups; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.groups (
    id bigint NOT NULL,
    group_name character varying(255) NOT NULL
);


ALTER TABLE public.groups OWNER TO postgres;

--
-- Name: groups_group_members; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.groups_group_members (
    groups_id bigint NOT NULL,
    group_members_id bigint NOT NULL
);


ALTER TABLE public.groups_group_members OWNER TO postgres;

--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.hibernate_sequence OWNER TO postgres;

--
-- Name: input_filter; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.input_filter (
    id bigint NOT NULL,
    version integer NOT NULL
);


ALTER TABLE public.input_filter OWNER TO postgres;

--
-- Name: input_filter_filter_conditions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.input_filter_filter_conditions (
    input_filter_id bigint NOT NULL,
    parameter_clob oid,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    filter_conditions_key character varying(255) NOT NULL
);


ALTER TABLE public.input_filter_filter_conditions OWNER TO postgres;

--
-- Name: job; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.job (
    id bigint NOT NULL,
    version integer NOT NULL,
    has_failed_job_steps boolean,
    job_state character varying(255),
    priority integer,
    start_time timestamp(6) without time zone,
    stop_time timestamp(6) without time zone,
    orbit_id bigint,
    processing_facility_id bigint,
    processing_order_id bigint
);


ALTER TABLE public.job OWNER TO postgres;

--
-- Name: job_step; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.job_step (
    id bigint NOT NULL,
    version integer NOT NULL,
    is_failed boolean,
    job_order_filename character varying(255),
    job_step_state character varying(255),
    priority integer,
    processing_completion_time timestamp without time zone,
    processing_mode character varying(255),
    processing_start_time timestamp without time zone,
    processing_std_err oid,
    processing_std_out oid,
    stderr_log_level integer,
    stdout_log_level integer,
    job_id bigint
);


ALTER TABLE public.job_step OWNER TO postgres;

--
-- Name: job_step_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.job_step_output_parameters (
    job_step_id bigint NOT NULL,
    parameter_clob oid,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    output_parameters_key character varying(255) NOT NULL
);


ALTER TABLE public.job_step_output_parameters OWNER TO postgres;

--
-- Name: mission; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mission (
    id bigint NOT NULL,
    version integer NOT NULL,
    code character varying(255),
    name character varying(255),
    order_retention_period bigint,
    processing_centre character varying(255),
    product_file_template oid,
    product_retention_period bigint
);


ALTER TABLE public.mission OWNER TO postgres;

--
-- Name: mission_file_classes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mission_file_classes (
    mission_id bigint NOT NULL,
    file_classes character varying(255)
);


ALTER TABLE public.mission_file_classes OWNER TO postgres;

--
-- Name: mission_processing_modes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mission_processing_modes (
    mission_id bigint NOT NULL,
    processing_modes character varying(255)
);


ALTER TABLE public.mission_processing_modes OWNER TO postgres;

--
-- Name: mon_ext_service; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_ext_service (
    id bigint NOT NULL,
    version integer NOT NULL,
    name character varying(255) NOT NULL,
    name_id character varying(255) NOT NULL
);


ALTER TABLE public.mon_ext_service OWNER TO postgres;

--
-- Name: mon_ext_service_state_operation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_ext_service_state_operation (
    id bigint NOT NULL,
    version integer NOT NULL,
    datetime timestamp without time zone,
    mon_ext_service_id bigint,
    mon_service_state_id bigint
);


ALTER TABLE public.mon_ext_service_state_operation OWNER TO postgres;

--
-- Name: mon_ext_service_state_operation_day; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_ext_service_state_operation_day (
    id bigint NOT NULL,
    version integer NOT NULL,
    datetime timestamp without time zone,
    mon_ext_service_id bigint,
    up_time double precision
);


ALTER TABLE public.mon_ext_service_state_operation_day OWNER TO postgres;

--
-- Name: mon_ext_service_state_operation_month; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_ext_service_state_operation_month (
    id bigint NOT NULL,
    version integer NOT NULL,
    datetime timestamp without time zone,
    mon_ext_service_id bigint,
    up_time double precision
);


ALTER TABLE public.mon_ext_service_state_operation_month OWNER TO postgres;

--
-- Name: mon_order_state; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_order_state (
    id bigint NOT NULL,
    version integer NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.mon_order_state OWNER TO postgres;

--
-- Name: mon_product_production_day; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_product_production_day (
    id bigint NOT NULL,
    version integer NOT NULL,
    count integer NOT NULL,
    datetime timestamp without time zone,
    file_size bigint NOT NULL,
    production_latency_avg integer NOT NULL,
    production_latency_max integer NOT NULL,
    production_latency_min integer NOT NULL,
    production_type character varying(255),
    total_latency_avg integer NOT NULL,
    total_latency_max integer NOT NULL,
    total_latency_min integer NOT NULL,
    mission_id bigint
);


ALTER TABLE public.mon_product_production_day OWNER TO postgres;

--
-- Name: mon_product_production_hour; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_product_production_hour (
    id bigint NOT NULL,
    version integer NOT NULL,
    count integer NOT NULL,
    datetime timestamp without time zone,
    file_size bigint NOT NULL,
    production_latency_avg integer NOT NULL,
    production_latency_max integer NOT NULL,
    production_latency_min integer NOT NULL,
    production_type character varying(255),
    total_latency_avg integer NOT NULL,
    total_latency_max integer NOT NULL,
    total_latency_min integer NOT NULL,
    mission_id bigint
);


ALTER TABLE public.mon_product_production_hour OWNER TO postgres;

--
-- Name: mon_product_production_month; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_product_production_month (
    id bigint NOT NULL,
    version integer NOT NULL,
    count integer NOT NULL,
    datetime timestamp without time zone,
    file_size bigint NOT NULL,
    production_latency_avg integer NOT NULL,
    production_latency_max integer NOT NULL,
    production_latency_min integer NOT NULL,
    production_type character varying(255),
    total_latency_avg integer NOT NULL,
    total_latency_max integer NOT NULL,
    total_latency_min integer NOT NULL,
    mission_id bigint
);


ALTER TABLE public.mon_product_production_month OWNER TO postgres;

--
-- Name: mon_service; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_service (
    id bigint NOT NULL,
    version integer NOT NULL,
    name character varying(255) NOT NULL,
    name_id character varying(255) NOT NULL
);


ALTER TABLE public.mon_service OWNER TO postgres;

--
-- Name: mon_service_state; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_service_state (
    id bigint NOT NULL,
    version integer NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.mon_service_state OWNER TO postgres;

--
-- Name: mon_service_state_operation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_service_state_operation (
    id bigint NOT NULL,
    version integer NOT NULL,
    datetime timestamp without time zone,
    mon_service_id bigint,
    mon_service_state_id bigint
);


ALTER TABLE public.mon_service_state_operation OWNER TO postgres;

--
-- Name: mon_service_state_operation_day; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_service_state_operation_day (
    id bigint NOT NULL,
    version integer NOT NULL,
    datetime timestamp without time zone,
    mon_service_id bigint,
    up_time double precision
);


ALTER TABLE public.mon_service_state_operation_day OWNER TO postgres;

--
-- Name: mon_service_state_operation_month; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.mon_service_state_operation_month (
    id bigint NOT NULL,
    version integer NOT NULL,
    datetime timestamp without time zone,
    mon_service_id bigint,
    up_time double precision
);


ALTER TABLE public.mon_service_state_operation_month OWNER TO postgres;

--
-- Name: orbit; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.orbit (
    id bigint NOT NULL,
    version integer NOT NULL,
    orbit_number integer,
    start_time timestamp(6) without time zone,
    stop_time timestamp(6) without time zone,
    spacecraft_id bigint
);


ALTER TABLE public.orbit OWNER TO postgres;

--
-- Name: processing_facility; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_facility (
    id bigint NOT NULL,
    version integer NOT NULL,
    default_storage_type character varying(255),
    description character varying(255),
    external_storage_manager_url character varying(255),
    facility_state character varying(255),
    local_storage_manager_url character varying(255),
    max_jobs_per_node integer,
    name character varying(255) NOT NULL,
    processing_engine_token oid,
    processing_engine_url character varying(255),
    storage_manager_password character varying(255),
    storage_manager_url character varying(255),
    storage_manager_user character varying(255)
);


ALTER TABLE public.processing_facility OWNER TO postgres;

--
-- Name: processing_order; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order (
    id bigint NOT NULL,
    version integer NOT NULL,
    actual_completion_time timestamp without time zone,
    estimated_completion_time timestamp without time zone,
    eviction_time timestamp without time zone,
    execution_time timestamp without time zone,
    has_failed_job_steps boolean,
    identifier character varying(255) NOT NULL,
    input_file_name character varying(255),
    input_sensing_start_time timestamp(6) without time zone,
    input_sensing_stop_time timestamp(6) without time zone,
    endpoint_password character varying(255),
    endpoint_uri character varying(255),
    endpoint_username character varying(255),
    order_source character varying(255),
    order_state character varying(255),
    output_file_class character varying(255),
    priority integer,
    processing_mode character varying(255),
    product_retention_period bigint,
    production_type character varying(255),
    release_time timestamp without time zone,
    slice_duration bigint,
    slice_overlap bigint,
    slicing_type character varying(255),
    start_time timestamp(6) without time zone,
    state_message character varying(255),
    stop_time timestamp(6) without time zone,
    submission_time timestamp without time zone,
    uuid uuid NOT NULL,
    mission_id bigint,
    workflow_id bigint
);


ALTER TABLE public.processing_order OWNER TO postgres;

--
-- Name: processing_order_class_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order_class_output_parameters (
    processing_order_id bigint NOT NULL,
    class_output_parameters_id bigint NOT NULL,
    class_output_parameters_key bigint NOT NULL
);


ALTER TABLE public.processing_order_class_output_parameters OWNER TO postgres;

--
-- Name: processing_order_dynamic_processing_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order_dynamic_processing_parameters (
    processing_order_id bigint NOT NULL,
    parameter_clob oid,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    dynamic_processing_parameters_key character varying(255) NOT NULL
);


ALTER TABLE public.processing_order_dynamic_processing_parameters OWNER TO postgres;

--
-- Name: processing_order_input_filters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order_input_filters (
    processing_order_id bigint NOT NULL,
    input_filters_id bigint NOT NULL,
    input_filters_key bigint NOT NULL
);


ALTER TABLE public.processing_order_input_filters OWNER TO postgres;

--
-- Name: processing_order_input_product_classes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order_input_product_classes (
    processing_order_id bigint NOT NULL,
    input_product_classes_id bigint NOT NULL
);


ALTER TABLE public.processing_order_input_product_classes OWNER TO postgres;

--
-- Name: processing_order_mon_order_progress; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order_mon_order_progress (
    processing_order_id bigint NOT NULL,
    all_job_steps integer NOT NULL,
    completed_job_steps integer NOT NULL,
    datetime timestamp without time zone,
    failed_job_steps integer NOT NULL,
    finished_job_steps integer NOT NULL,
    mon_order_state_id bigint,
    running_job_steps integer NOT NULL
);


ALTER TABLE public.processing_order_mon_order_progress OWNER TO postgres;

--
-- Name: processing_order_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order_output_parameters (
    processing_order_id bigint NOT NULL,
    parameter_clob oid,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    output_parameters_key character varying(255) NOT NULL
);


ALTER TABLE public.processing_order_output_parameters OWNER TO postgres;

--
-- Name: processing_order_requested_configured_processors; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order_requested_configured_processors (
    processing_order_id bigint NOT NULL,
    requested_configured_processors_id bigint NOT NULL
);


ALTER TABLE public.processing_order_requested_configured_processors OWNER TO postgres;

--
-- Name: processing_order_requested_orbits; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order_requested_orbits (
    processing_order_id bigint NOT NULL,
    requested_orbits_id bigint NOT NULL
);


ALTER TABLE public.processing_order_requested_orbits OWNER TO postgres;

--
-- Name: processing_order_requested_product_classes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order_requested_product_classes (
    processing_order_id bigint NOT NULL,
    requested_product_classes_id bigint NOT NULL
);


ALTER TABLE public.processing_order_requested_product_classes OWNER TO postgres;

--
-- Name: processor; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processor (
    id bigint NOT NULL,
    version integer NOT NULL,
    docker_image character varying(255),
    is_test boolean,
    job_order_version character varying(255),
    max_time integer,
    min_disk_space integer,
    processor_version character varying(255),
    sensing_time_flag boolean,
    use_input_file_time_intervals boolean,
    processor_class_id bigint
);


ALTER TABLE public.processor OWNER TO postgres;

--
-- Name: processor_class; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processor_class (
    id bigint NOT NULL,
    version integer NOT NULL,
    processor_name character varying(255),
    mission_id bigint
);


ALTER TABLE public.processor_class OWNER TO postgres;

--
-- Name: processor_docker_run_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processor_docker_run_parameters (
    processor_id bigint NOT NULL,
    docker_run_parameters character varying(255),
    docker_run_parameters_key character varying(255) NOT NULL
);


ALTER TABLE public.processor_docker_run_parameters OWNER TO postgres;

--
-- Name: product; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product (
    id bigint NOT NULL,
    version integer NOT NULL,
    eviction_time timestamp(6) without time zone,
    file_class character varying(255),
    generation_time timestamp(6) without time zone,
    mode character varying(255),
    product_quality character varying(255),
    production_type character varying(255),
    publication_time timestamp(6) without time zone,
    raw_data_availability_time timestamp(6) without time zone,
    requested_start_time timestamp(6) without time zone,
    requested_stop_time timestamp(6) without time zone,
    sensing_start_time timestamp(6) without time zone,
    sensing_stop_time timestamp(6) without time zone,
    uuid uuid NOT NULL,
    configured_processor_id bigint,
    enclosing_product_id bigint,
    job_step_id bigint,
    orbit_id bigint,
    product_class_id bigint
);


ALTER TABLE public.product OWNER TO postgres;

--
-- Name: product_archive; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_archive (
    id bigint NOT NULL,
    version integer NOT NULL,
    archive_type character varying(255),
    base_uri character varying(255),
    client_id character varying(255),
    client_secret character varying(255),
    code character varying(255) NOT NULL,
    context character varying(255),
    name character varying(255),
    password character varying(255),
    send_auth_in_body boolean,
    token_required boolean,
    token_uri character varying(255),
    username character varying(255)
);


ALTER TABLE public.product_archive OWNER TO postgres;

--
-- Name: product_archive_available_product_classes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_archive_available_product_classes (
    product_archive_id bigint NOT NULL,
    available_product_classes_id bigint NOT NULL
);


ALTER TABLE public.product_archive_available_product_classes OWNER TO postgres;

--
-- Name: product_class; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_class (
    id bigint NOT NULL,
    version integer NOT NULL,
    default_slice_duration bigint,
    default_slicing_type character varying(255),
    description character varying(255),
    processing_level character varying(255),
    product_file_template oid,
    product_type character varying(255),
    visibility character varying(255),
    enclosing_class_id bigint,
    mission_id bigint,
    processor_class_id bigint
);


ALTER TABLE public.product_class OWNER TO postgres;

--
-- Name: product_download_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_download_history (
    product_id bigint NOT NULL,
    date_time timestamp(6) without time zone,
    product_file_id bigint,
    product_file_name character varying(255),
    product_file_size bigint,
    username character varying(255)
);


ALTER TABLE public.product_download_history OWNER TO postgres;

--
-- Name: product_file; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_file (
    id bigint NOT NULL,
    version integer NOT NULL,
    checksum character varying(255),
    checksum_time timestamp(6) without time zone,
    file_path character varying(255),
    file_size bigint,
    product_file_name character varying(255),
    storage_type character varying(255),
    zip_checksum character varying(255),
    zip_checksum_time timestamp(6) without time zone,
    zip_file_name character varying(255),
    zip_file_size bigint,
    processing_facility_id bigint,
    product_id bigint
);


ALTER TABLE public.product_file OWNER TO postgres;

--
-- Name: product_file_aux_file_names; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_file_aux_file_names (
    product_file_id bigint NOT NULL,
    aux_file_names character varying(255)
);


ALTER TABLE public.product_file_aux_file_names OWNER TO postgres;

--
-- Name: product_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_parameters (
    product_id bigint NOT NULL,
    parameter_clob oid,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    parameters_key character varying(255) NOT NULL
);


ALTER TABLE public.product_parameters OWNER TO postgres;

--
-- Name: product_query; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_query (
    id bigint NOT NULL,
    version integer NOT NULL,
    in_download boolean,
    is_satisfied boolean,
    jpql_query_condition text,
    minimum_coverage smallint,
    sql_query_condition text,
    generating_rule_id bigint,
    job_step_id bigint,
    requested_product_class_id bigint
);


ALTER TABLE public.product_query OWNER TO postgres;

--
-- Name: product_query_filter_conditions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_query_filter_conditions (
    product_query_id bigint NOT NULL,
    parameter_clob oid,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    filter_conditions_key character varying(255) NOT NULL
);


ALTER TABLE public.product_query_filter_conditions OWNER TO postgres;

--
-- Name: product_query_satisfying_products; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_query_satisfying_products (
    satisfied_product_queries_id bigint NOT NULL,
    satisfying_products_id bigint NOT NULL
);


ALTER TABLE public.product_query_satisfying_products OWNER TO postgres;

--
-- Name: simple_policy; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.simple_policy (
    id bigint NOT NULL,
    version integer NOT NULL,
    policy_type character varying(255) NOT NULL
);


ALTER TABLE public.simple_policy OWNER TO postgres;

--
-- Name: simple_policy_delta_times; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.simple_policy_delta_times (
    simple_policy_id bigint NOT NULL,
    duration bigint NOT NULL,
    unit integer,
    list_index integer NOT NULL
);


ALTER TABLE public.simple_policy_delta_times OWNER TO postgres;

--
-- Name: simple_selection_rule; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.simple_selection_rule (
    id bigint NOT NULL,
    version integer NOT NULL,
    filtered_source_product_type character varying(255),
    is_mandatory boolean,
    minimum_coverage smallint,
    mode character varying(255),
    source_product_class_id bigint,
    target_product_class_id bigint
);


ALTER TABLE public.simple_selection_rule OWNER TO postgres;

--
-- Name: simple_selection_rule_configured_processors; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.simple_selection_rule_configured_processors (
    simple_selection_rule_id bigint NOT NULL,
    configured_processors_id bigint NOT NULL
);


ALTER TABLE public.simple_selection_rule_configured_processors OWNER TO postgres;

--
-- Name: simple_selection_rule_filter_conditions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.simple_selection_rule_filter_conditions (
    simple_selection_rule_id bigint NOT NULL,
    parameter_clob oid,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    filter_conditions_key character varying(255) NOT NULL
);


ALTER TABLE public.simple_selection_rule_filter_conditions OWNER TO postgres;

--
-- Name: simple_selection_rule_simple_policies; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.simple_selection_rule_simple_policies (
    simple_selection_rule_id bigint NOT NULL,
    simple_policies_id bigint NOT NULL
);


ALTER TABLE public.simple_selection_rule_simple_policies OWNER TO postgres;

--
-- Name: spacecraft; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.spacecraft (
    id bigint NOT NULL,
    version integer NOT NULL,
    code character varying(255),
    name character varying(255),
    mission_id bigint
);


ALTER TABLE public.spacecraft OWNER TO postgres;

--
-- Name: spacecraft_payloads; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.spacecraft_payloads (
    spacecraft_id bigint NOT NULL,
    description character varying(255),
    name character varying(255)
);


ALTER TABLE public.spacecraft_payloads OWNER TO postgres;

--
-- Name: task; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.task (
    id bigint NOT NULL,
    version integer NOT NULL,
    criticality_level integer,
    is_critical boolean,
    min_memory integer,
    number_of_cpus integer,
    task_name character varying(255),
    task_version character varying(255),
    processor_id bigint
);


ALTER TABLE public.task OWNER TO postgres;

--
-- Name: task_breakpoint_file_names; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.task_breakpoint_file_names (
    task_id bigint NOT NULL,
    breakpoint_file_names character varying(255)
);


ALTER TABLE public.task_breakpoint_file_names OWNER TO postgres;

--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    username character varying(255) NOT NULL,
    enabled boolean NOT NULL,
    expiration_date timestamp without time zone NOT NULL,
    password character varying(255) NOT NULL,
    password_expiration_date timestamp without time zone NOT NULL,
    assigned bigint,
    last_access_date timestamp without time zone,
    used bigint
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_group_memberships; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users_group_memberships (
    users_username character varying(255) NOT NULL,
    group_memberships_id bigint NOT NULL
);


ALTER TABLE public.users_group_memberships OWNER TO postgres;

--
-- Name: workflow; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.workflow (
    id bigint NOT NULL,
    version integer NOT NULL,
    description character varying(255),
    enabled boolean,
    name character varying(255) NOT NULL,
    output_file_class character varying(255),
    processing_mode character varying(255),
    slice_duration bigint,
    slice_overlap bigint,
    slicing_type character varying(255),
    uuid uuid NOT NULL,
    workflow_version character varying(255) NOT NULL,
    configured_processor_id bigint,
    input_product_class_id bigint,
    mission_id bigint,
    output_product_class_id bigint
);


ALTER TABLE public.workflow OWNER TO postgres;

--
-- Name: workflow_class_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.workflow_class_output_parameters (
    workflow_id bigint NOT NULL,
    class_output_parameters_id bigint NOT NULL,
    class_output_parameters_key bigint NOT NULL
);


ALTER TABLE public.workflow_class_output_parameters OWNER TO postgres;

--
-- Name: workflow_input_filters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.workflow_input_filters (
    workflow_id bigint NOT NULL,
    input_filters_id bigint NOT NULL,
    input_filters_key bigint NOT NULL
);


ALTER TABLE public.workflow_input_filters OWNER TO postgres;

--
-- Name: workflow_option; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.workflow_option (
    id bigint NOT NULL,
    version integer NOT NULL,
    default_value character varying(255),
    description character varying(255),
    name character varying(255),
    type character varying(255),
    workflow_id bigint
);


ALTER TABLE public.workflow_option OWNER TO postgres;

--
-- Name: workflow_option_value_range; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.workflow_option_value_range (
    workflow_option_id bigint NOT NULL,
    value_range character varying(255)
);


ALTER TABLE public.workflow_option_value_range OWNER TO postgres;

--
-- Name: workflow_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.workflow_output_parameters (
    workflow_id bigint NOT NULL,
    parameter_clob oid,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    output_parameters_key character varying(255) NOT NULL
);


ALTER TABLE public.workflow_output_parameters OWNER TO postgres;

--
-- Name: api_metrics api_metrics_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.api_metrics
    ADD CONSTRAINT api_metrics_pkey PRIMARY KEY (id);


--
-- Name: authorities authorities_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.authorities
    ADD CONSTRAINT authorities_pkey PRIMARY KEY (username, authority);


--
-- Name: class_output_parameter_output_parameters class_output_parameter_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.class_output_parameter_output_parameters
    ADD CONSTRAINT class_output_parameter_output_parameters_pkey PRIMARY KEY (class_output_parameter_id, output_parameters_key);


--
-- Name: class_output_parameter class_output_parameter_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.class_output_parameter
    ADD CONSTRAINT class_output_parameter_pkey PRIMARY KEY (id);


--
-- Name: configuration_docker_run_parameters configuration_docker_run_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_docker_run_parameters
    ADD CONSTRAINT configuration_docker_run_parameters_pkey PRIMARY KEY (configuration_id, docker_run_parameters_key);


--
-- Name: configuration_dyn_proc_parameters configuration_dyn_proc_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_dyn_proc_parameters
    ADD CONSTRAINT configuration_dyn_proc_parameters_pkey PRIMARY KEY (configuration_id, dyn_proc_parameters_key);


--
-- Name: configuration_input_file configuration_input_file_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_input_file
    ADD CONSTRAINT configuration_input_file_pkey PRIMARY KEY (id);


--
-- Name: configuration configuration_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration
    ADD CONSTRAINT configuration_pkey PRIMARY KEY (id);


--
-- Name: configuration_static_input_files configuration_static_input_files_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_static_input_files
    ADD CONSTRAINT configuration_static_input_files_pkey PRIMARY KEY (configuration_id, static_input_files_id);


--
-- Name: configured_processor configured_processor_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configured_processor
    ADD CONSTRAINT configured_processor_pkey PRIMARY KEY (id);


--
-- Name: group_authorities group_authorities_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_authorities
    ADD CONSTRAINT group_authorities_pkey PRIMARY KEY (group_id, authority);


--
-- Name: group_members group_members_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT group_members_pkey PRIMARY KEY (id);


--
-- Name: groups_group_members groups_group_members_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.groups_group_members
    ADD CONSTRAINT groups_group_members_pkey PRIMARY KEY (groups_id, group_members_id);


--
-- Name: groups groups_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT groups_pkey PRIMARY KEY (id);


--
-- Name: input_filter_filter_conditions input_filter_filter_conditions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.input_filter_filter_conditions
    ADD CONSTRAINT input_filter_filter_conditions_pkey PRIMARY KEY (input_filter_id, filter_conditions_key);


--
-- Name: input_filter input_filter_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.input_filter
    ADD CONSTRAINT input_filter_pkey PRIMARY KEY (id);


--
-- Name: job job_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job
    ADD CONSTRAINT job_pkey PRIMARY KEY (id);


--
-- Name: job_step_output_parameters job_step_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job_step_output_parameters
    ADD CONSTRAINT job_step_output_parameters_pkey PRIMARY KEY (job_step_id, output_parameters_key);


--
-- Name: job_step job_step_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job_step
    ADD CONSTRAINT job_step_pkey PRIMARY KEY (id);


--
-- Name: mission mission_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mission
    ADD CONSTRAINT mission_pkey PRIMARY KEY (id);


--
-- Name: mon_ext_service mon_ext_service_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_ext_service
    ADD CONSTRAINT mon_ext_service_pkey PRIMARY KEY (id);


--
-- Name: mon_ext_service_state_operation_day mon_ext_service_state_operation_day_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_ext_service_state_operation_day
    ADD CONSTRAINT mon_ext_service_state_operation_day_pkey PRIMARY KEY (id);


--
-- Name: mon_ext_service_state_operation_month mon_ext_service_state_operation_month_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_ext_service_state_operation_month
    ADD CONSTRAINT mon_ext_service_state_operation_month_pkey PRIMARY KEY (id);


--
-- Name: mon_ext_service_state_operation mon_ext_service_state_operation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_ext_service_state_operation
    ADD CONSTRAINT mon_ext_service_state_operation_pkey PRIMARY KEY (id);


--
-- Name: mon_order_state mon_order_state_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_order_state
    ADD CONSTRAINT mon_order_state_pkey PRIMARY KEY (id);


--
-- Name: mon_product_production_day mon_product_production_day_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_product_production_day
    ADD CONSTRAINT mon_product_production_day_pkey PRIMARY KEY (id);


--
-- Name: mon_product_production_hour mon_product_production_hour_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_product_production_hour
    ADD CONSTRAINT mon_product_production_hour_pkey PRIMARY KEY (id);


--
-- Name: mon_product_production_month mon_product_production_month_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_product_production_month
    ADD CONSTRAINT mon_product_production_month_pkey PRIMARY KEY (id);


--
-- Name: mon_service mon_service_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_service
    ADD CONSTRAINT mon_service_pkey PRIMARY KEY (id);


--
-- Name: mon_service_state_operation_day mon_service_state_operation_day_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_service_state_operation_day
    ADD CONSTRAINT mon_service_state_operation_day_pkey PRIMARY KEY (id);


--
-- Name: mon_service_state_operation_month mon_service_state_operation_month_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_service_state_operation_month
    ADD CONSTRAINT mon_service_state_operation_month_pkey PRIMARY KEY (id);


--
-- Name: mon_service_state_operation mon_service_state_operation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_service_state_operation
    ADD CONSTRAINT mon_service_state_operation_pkey PRIMARY KEY (id);


--
-- Name: mon_service_state mon_service_state_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_service_state
    ADD CONSTRAINT mon_service_state_pkey PRIMARY KEY (id);


--
-- Name: orbit orbit_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orbit
    ADD CONSTRAINT orbit_pkey PRIMARY KEY (id);


--
-- Name: processing_facility processing_facility_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_facility
    ADD CONSTRAINT processing_facility_pkey PRIMARY KEY (id);


--
-- Name: processing_order_class_output_parameters processing_order_class_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_class_output_parameters
    ADD CONSTRAINT processing_order_class_output_parameters_pkey PRIMARY KEY (processing_order_id, class_output_parameters_key);


--
-- Name: processing_order_dynamic_processing_parameters processing_order_dynamic_processing_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_dynamic_processing_parameters
    ADD CONSTRAINT processing_order_dynamic_processing_parameters_pkey PRIMARY KEY (processing_order_id, dynamic_processing_parameters_key);


--
-- Name: processing_order_input_filters processing_order_input_filters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_filters
    ADD CONSTRAINT processing_order_input_filters_pkey PRIMARY KEY (processing_order_id, input_filters_key);


--
-- Name: processing_order_input_product_classes processing_order_input_product_classes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_product_classes
    ADD CONSTRAINT processing_order_input_product_classes_pkey PRIMARY KEY (processing_order_id, input_product_classes_id);


--
-- Name: processing_order_output_parameters processing_order_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_output_parameters
    ADD CONSTRAINT processing_order_output_parameters_pkey PRIMARY KEY (processing_order_id, output_parameters_key);


--
-- Name: processing_order processing_order_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order
    ADD CONSTRAINT processing_order_pkey PRIMARY KEY (id);


--
-- Name: processing_order_requested_configured_processors processing_order_requested_configured_processors_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_requested_configured_processors
    ADD CONSTRAINT processing_order_requested_configured_processors_pkey PRIMARY KEY (processing_order_id, requested_configured_processors_id);


--
-- Name: processing_order_requested_product_classes processing_order_requested_product_classes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_requested_product_classes
    ADD CONSTRAINT processing_order_requested_product_classes_pkey PRIMARY KEY (processing_order_id, requested_product_classes_id);


--
-- Name: processor_class processor_class_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processor_class
    ADD CONSTRAINT processor_class_pkey PRIMARY KEY (id);


--
-- Name: processor_docker_run_parameters processor_docker_run_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processor_docker_run_parameters
    ADD CONSTRAINT processor_docker_run_parameters_pkey PRIMARY KEY (processor_id, docker_run_parameters_key);


--
-- Name: processor processor_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processor
    ADD CONSTRAINT processor_pkey PRIMARY KEY (id);


--
-- Name: product_archive_available_product_classes product_archive_available_product_classes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_archive_available_product_classes
    ADD CONSTRAINT product_archive_available_product_classes_pkey PRIMARY KEY (product_archive_id, available_product_classes_id);


--
-- Name: product_archive product_archive_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_archive
    ADD CONSTRAINT product_archive_pkey PRIMARY KEY (id);


--
-- Name: product_class product_class_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_class
    ADD CONSTRAINT product_class_pkey PRIMARY KEY (id);


--
-- Name: product_file product_file_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_file
    ADD CONSTRAINT product_file_pkey PRIMARY KEY (id);


--
-- Name: product_parameters product_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_parameters
    ADD CONSTRAINT product_parameters_pkey PRIMARY KEY (product_id, parameters_key);


--
-- Name: product product_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT product_pkey PRIMARY KEY (id);


--
-- Name: product_query_filter_conditions product_query_filter_conditions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_query_filter_conditions
    ADD CONSTRAINT product_query_filter_conditions_pkey PRIMARY KEY (product_query_id, filter_conditions_key);


--
-- Name: product_query product_query_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_query
    ADD CONSTRAINT product_query_pkey PRIMARY KEY (id);


--
-- Name: product_query_satisfying_products product_query_satisfying_products_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_query_satisfying_products
    ADD CONSTRAINT product_query_satisfying_products_pkey PRIMARY KEY (satisfied_product_queries_id, satisfying_products_id);


--
-- Name: simple_policy_delta_times simple_policy_delta_times_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_policy_delta_times
    ADD CONSTRAINT simple_policy_delta_times_pkey PRIMARY KEY (simple_policy_id, list_index);


--
-- Name: simple_policy simple_policy_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_policy
    ADD CONSTRAINT simple_policy_pkey PRIMARY KEY (id);


--
-- Name: simple_selection_rule_configured_processors simple_selection_rule_configured_processors_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule_configured_processors
    ADD CONSTRAINT simple_selection_rule_configured_processors_pkey PRIMARY KEY (simple_selection_rule_id, configured_processors_id);


--
-- Name: simple_selection_rule_filter_conditions simple_selection_rule_filter_conditions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule_filter_conditions
    ADD CONSTRAINT simple_selection_rule_filter_conditions_pkey PRIMARY KEY (simple_selection_rule_id, filter_conditions_key);


--
-- Name: simple_selection_rule simple_selection_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule
    ADD CONSTRAINT simple_selection_rule_pkey PRIMARY KEY (id);


--
-- Name: spacecraft spacecraft_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.spacecraft
    ADD CONSTRAINT spacecraft_pkey PRIMARY KEY (id);


--
-- Name: task task_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT task_pkey PRIMARY KEY (id);


--
-- Name: product_archive uk19j4q7qi3o7ln0yucf43cfbps; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_archive
    ADD CONSTRAINT uk19j4q7qi3o7ln0yucf43cfbps UNIQUE (code);


--
-- Name: mon_service uk239unm9hg59upu3be0fkcu4rt; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_service
    ADD CONSTRAINT uk239unm9hg59upu3be0fkcu4rt UNIQUE (name);


--
-- Name: product uk24bc4yyyk3fj3h7ku64i3yuog; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT uk24bc4yyyk3fj3h7ku64i3yuog UNIQUE (uuid);


--
-- Name: mon_service_state uk447s0lqn0lb4gcho5ciadm0r8; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_service_state
    ADD CONSTRAINT uk447s0lqn0lb4gcho5ciadm0r8 UNIQUE (name);


--
-- Name: configured_processor uk49uwfv9jn1bfagrgu1fmxjlr8; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configured_processor
    ADD CONSTRAINT uk49uwfv9jn1bfagrgu1fmxjlr8 UNIQUE (uuid);


--
-- Name: product_query uk4dkam8lshg1hjjfk4mm4vsp50; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_query
    ADD CONSTRAINT uk4dkam8lshg1hjjfk4mm4vsp50 UNIQUE (job_step_id, requested_product_class_id);


--
-- Name: mon_ext_service uk6iqkhmagcms83dnb123yfd0s2; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_ext_service
    ADD CONSTRAINT uk6iqkhmagcms83dnb123yfd0s2 UNIQUE (name);


--
-- Name: orbit uk6tiqkg4pvqd1iyfmes8t2pd2j; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orbit
    ADD CONSTRAINT uk6tiqkg4pvqd1iyfmes8t2pd2j UNIQUE (spacecraft_id, orbit_number);


--
-- Name: processing_facility uk8cny9892if8tybde5p5brts6d; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_facility
    ADD CONSTRAINT uk8cny9892if8tybde5p5brts6d UNIQUE (name);


--
-- Name: groups_group_members uk_132lanwqs6liav9syek4s96xv; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.groups_group_members
    ADD CONSTRAINT uk_132lanwqs6liav9syek4s96xv UNIQUE (group_members_id);


--
-- Name: configuration_static_input_files uk_2y140wa1pggeycgihvnex0a9c; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_static_input_files
    ADD CONSTRAINT uk_2y140wa1pggeycgihvnex0a9c UNIQUE (static_input_files_id);


--
-- Name: simple_selection_rule_simple_policies uk_7jrn9t62kdspngixrembpkrd7; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule_simple_policies
    ADD CONSTRAINT uk_7jrn9t62kdspngixrembpkrd7 UNIQUE (simple_policies_id);


--
-- Name: users_group_memberships uk_e2ijwadyxqhcr2aldhs624px; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users_group_memberships
    ADD CONSTRAINT uk_e2ijwadyxqhcr2aldhs624px UNIQUE (group_memberships_id);


--
-- Name: groups uka1130rbom0hjmf18xtppoc3eq; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT uka1130rbom0hjmf18xtppoc3eq UNIQUE (group_name);


--
-- Name: processing_order ukbxwgyibx5dkbl26jplnjifrsa; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order
    ADD CONSTRAINT ukbxwgyibx5dkbl26jplnjifrsa UNIQUE (mission_id, identifier);


--
-- Name: product_file ukdawt5bhyxxovxd4vgo4cw6ugn; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_file
    ADD CONSTRAINT ukdawt5bhyxxovxd4vgo4cw6ugn UNIQUE (product_id, processing_facility_id);


--
-- Name: workflow ukg9qjvpphc4d2n8y10mpael4cd; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT ukg9qjvpphc4d2n8y10mpael4cd UNIQUE (mission_id, name, workflow_version);


--
-- Name: product_class ukgmc9l016fh1mcqque3k8iy3yu; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_class
    ADD CONSTRAINT ukgmc9l016fh1mcqque3k8iy3yu UNIQUE (mission_id, product_type);


--
-- Name: workflow ukhskpb9cv06lohyj8m2ekp9xio; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT ukhskpb9cv06lohyj8m2ekp9xio UNIQUE (uuid);


--
-- Name: spacecraft uklogt34j6cnrocn49sw0uu30eh; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.spacecraft
    ADD CONSTRAINT uklogt34j6cnrocn49sw0uu30eh UNIQUE (mission_id, code);


--
-- Name: processor_class uknbv3u0tx6s1770tmlvylhodfw; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processor_class
    ADD CONSTRAINT uknbv3u0tx6s1770tmlvylhodfw UNIQUE (mission_id, processor_name);


--
-- Name: mon_order_state ukpm3kggmu7tijpl89jmyjnkg3r; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_order_state
    ADD CONSTRAINT ukpm3kggmu7tijpl89jmyjnkg3r UNIQUE (name);


--
-- Name: processing_order ukqx1os0kwk3rvvoe4bw7inowsq; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order
    ADD CONSTRAINT ukqx1os0kwk3rvvoe4bw7inowsq UNIQUE (uuid);


--
-- Name: workflow_option ukt0udoa7mo0nk3swm1ff30gh5; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_option
    ADD CONSTRAINT ukt0udoa7mo0nk3swm1ff30gh5 UNIQUE (workflow_id, name);


--
-- Name: mission uktio2ulw4k2037685uaayxtuub; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mission
    ADD CONSTRAINT uktio2ulw4k2037685uaayxtuub UNIQUE (code);


--
-- Name: processor uktomhxtld2pvrabtanoq3t3odk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processor
    ADD CONSTRAINT uktomhxtld2pvrabtanoq3t3odk UNIQUE (processor_class_id, processor_version);


--
-- Name: users_group_memberships users_group_memberships_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users_group_memberships
    ADD CONSTRAINT users_group_memberships_pkey PRIMARY KEY (users_username, group_memberships_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (username);


--
-- Name: workflow_class_output_parameters workflow_class_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_class_output_parameters
    ADD CONSTRAINT workflow_class_output_parameters_pkey PRIMARY KEY (workflow_id, class_output_parameters_key);


--
-- Name: workflow_input_filters workflow_input_filters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_input_filters
    ADD CONSTRAINT workflow_input_filters_pkey PRIMARY KEY (workflow_id, input_filters_key);


--
-- Name: workflow_option workflow_option_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_option
    ADD CONSTRAINT workflow_option_pkey PRIMARY KEY (id);


--
-- Name: workflow_output_parameters workflow_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_output_parameters
    ADD CONSTRAINT workflow_output_parameters_pkey PRIMARY KEY (workflow_id, output_parameters_key);


--
-- Name: workflow workflow_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT workflow_pkey PRIMARY KEY (id);


--
-- Name: idx2qkgbww8yosr941oqytoo823x; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx2qkgbww8yosr941oqytoo823x ON public.api_metrics USING btree (name);


--
-- Name: idx2uot336txpqpdo8je8x145a0; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx2uot336txpqpdo8je8x145a0 ON public.product USING btree (publication_time);


--
-- Name: idx4jtdla2jravgeu16yxlv6i1g1; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx4jtdla2jravgeu16yxlv6i1g1 ON public.mon_product_production_day USING btree (mission_id, production_type);


--
-- Name: idx657h07h95ub6nyt49a4wib4ky; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx657h07h95ub6nyt49a4wib4ky ON public.orbit USING btree (start_time);


--
-- Name: idx6u8n1u6c253dps752x6s7yaa7; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx6u8n1u6c253dps752x6s7yaa7 ON public.product_query USING btree (requested_product_class_id);


--
-- Name: idx870rjn0w07u5qc26c4dmfc8p0; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx870rjn0w07u5qc26c4dmfc8p0 ON public.mon_product_production_month USING btree (mission_id, production_type);


--
-- Name: idx8uli6v1risvb0i9offqwpsaag; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx8uli6v1risvb0i9offqwpsaag ON public.mon_service_state_operation USING btree (datetime);


--
-- Name: idx8y2rqfvrrms3xi92l1nq6dm1m; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx8y2rqfvrrms3xi92l1nq6dm1m ON public.mon_service_state_operation_day USING btree (datetime);


--
-- Name: idx99jry69detoongt94mr0cb4jm; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx99jry69detoongt94mr0cb4jm ON public.job_step USING btree (job_step_state);


--
-- Name: idxautube4tmw46joub7nf9qaop2; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxautube4tmw46joub7nf9qaop2 ON public.mon_ext_service_state_operation_day USING btree (datetime);


--
-- Name: idxb1hlhb6srtxd7qpjtkm8a37jg; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxb1hlhb6srtxd7qpjtkm8a37jg ON public.product USING btree (enclosing_product_id);


--
-- Name: idxckdfwsktiy2a7t08oulv9p4bj; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxckdfwsktiy2a7t08oulv9p4bj ON public.processing_order USING btree (execution_time);


--
-- Name: idxcquj0irj1wkt95t0krnmwrkj2; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxcquj0irj1wkt95t0krnmwrkj2 ON public.api_metrics USING btree ("timestamp");


--
-- Name: idxdp0vd1b50igr05nxswcxupv6v; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxdp0vd1b50igr05nxswcxupv6v ON public.product USING btree (product_class_id, sensing_start_time);


--
-- Name: idxebh2ci5ivqufcgtxv4gax0mif; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxebh2ci5ivqufcgtxv4gax0mif ON public.product USING btree (product_class_id, generation_time);


--
-- Name: idxf38b0g96ksfrxorqgryoajj7y; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxf38b0g96ksfrxorqgryoajj7y ON public.mon_product_production_hour USING btree (datetime);


--
-- Name: idxgcks0habumx5p6km1njxx4omf; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxgcks0habumx5p6km1njxx4omf ON public.mon_product_production_day USING btree (datetime);


--
-- Name: idxgin74m2ax2pg2c6yu60km46qa; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxgin74m2ax2pg2c6yu60km46qa ON public.mon_ext_service_state_operation USING btree (datetime);


--
-- Name: idxhw65e77rikl7b5qq70e6kjgpg; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxhw65e77rikl7b5qq70e6kjgpg ON public.job USING btree (job_state);


--
-- Name: idxjh81k2qvy1nwnacjhx4c9e8uo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxjh81k2qvy1nwnacjhx4c9e8uo ON public.mon_product_production_month USING btree (datetime);


--
-- Name: idxjsbb5wmnf9graowd1f9wud96k; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxjsbb5wmnf9graowd1f9wud96k ON public.mon_ext_service_state_operation USING btree (mon_ext_service_id);


--
-- Name: idxl5ejqnuw4rb1sigroxfcwsxwq; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxl5ejqnuw4rb1sigroxfcwsxwq ON public.mon_service_state_operation USING btree (mon_service_state_id);


--
-- Name: idxm1c1ucav1v0gpnn89cjp66b06; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxm1c1ucav1v0gpnn89cjp66b06 ON public.mon_service_state_operation USING btree (mon_service_id);


--
-- Name: idxnl37dyvy1o7gygku42gk4db78; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxnl37dyvy1o7gygku42gk4db78 ON public.mon_product_production_hour USING btree (mission_id, production_type);


--
-- Name: idxojvxy5otq1rh1mu4heg6ny1yn; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxojvxy5otq1rh1mu4heg6ny1yn ON public.product USING btree (product_class_id, sensing_stop_time);


--
-- Name: idxoqh21spbi520l31tsal474r6p; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxoqh21spbi520l31tsal474r6p ON public.mon_service_state_operation_month USING btree (datetime);


--
-- Name: idxqluoxbhk7kihm71dkalmcq1tq; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxqluoxbhk7kihm71dkalmcq1tq ON public.configured_processor USING btree (identifier);


--
-- Name: idxqt20vfwmvgqe74tm87v1jdcj9; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxqt20vfwmvgqe74tm87v1jdcj9 ON public.mon_ext_service_state_operation_month USING btree (datetime);


--
-- Name: idxqu0ou5l3tyyegjvfh0rvb8f4h; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxqu0ou5l3tyyegjvfh0rvb8f4h ON public.product USING btree (eviction_time);


--
-- Name: idxsoap2dcggqp95abimerpm6031; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxsoap2dcggqp95abimerpm6031 ON public.mon_ext_service_state_operation USING btree (mon_service_state_id);


--
-- Name: configuration_docker_run_parameters fk165qo4rdh6j4v72p19t5rluv3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_docker_run_parameters
    ADD CONSTRAINT fk165qo4rdh6j4v72p19t5rluv3 FOREIGN KEY (configuration_id) REFERENCES public.configuration(id);


--
-- Name: workflow_input_filters fk1cuklmlh4xk0s3dqekio7gy74; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_input_filters
    ADD CONSTRAINT fk1cuklmlh4xk0s3dqekio7gy74 FOREIGN KEY (input_filters_key) REFERENCES public.product_class(id);


--
-- Name: workflow_class_output_parameters fk1gm53x3igc2k000lc5cy2sf1q; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_class_output_parameters
    ADD CONSTRAINT fk1gm53x3igc2k000lc5cy2sf1q FOREIGN KEY (workflow_id) REFERENCES public.workflow(id);


--
-- Name: processing_order_input_filters fk1u9dj81sg3vcueaprup3hasqi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_filters
    ADD CONSTRAINT fk1u9dj81sg3vcueaprup3hasqi FOREIGN KEY (input_filters_id) REFERENCES public.input_filter(id);


--
-- Name: mon_product_production_day fk204rojkd37iypnvoyw0nr3iqn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_product_production_day
    ADD CONSTRAINT fk204rojkd37iypnvoyw0nr3iqn FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: product_download_history fk23add5g47k6kbm3cgmw6hqqjh; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_download_history
    ADD CONSTRAINT fk23add5g47k6kbm3cgmw6hqqjh FOREIGN KEY (product_file_id) REFERENCES public.product_file(id);


--
-- Name: configuration_static_input_files fk23vkvo6qmdg9xinr4drioro6w; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_static_input_files
    ADD CONSTRAINT fk23vkvo6qmdg9xinr4drioro6w FOREIGN KEY (static_input_files_id) REFERENCES public.configuration_input_file(id);


--
-- Name: product_file_aux_file_names fk24578k1macp0jxtdaiep0nku8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_file_aux_file_names
    ADD CONSTRAINT fk24578k1macp0jxtdaiep0nku8 FOREIGN KEY (product_file_id) REFERENCES public.product_file(id);


--
-- Name: product_query fk2wed8wyw8vyboifjd64ytftp9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_query
    ADD CONSTRAINT fk2wed8wyw8vyboifjd64ytftp9 FOREIGN KEY (requested_product_class_id) REFERENCES public.product_class(id);


--
-- Name: product_file fk4b360nnjmkd9r4w01jc7yer5h; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_file
    ADD CONSTRAINT fk4b360nnjmkd9r4w01jc7yer5h FOREIGN KEY (processing_facility_id) REFERENCES public.processing_facility(id);


--
-- Name: product_class fk4oc1a80q9jt8b0et2kl64j8av; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_class
    ADD CONSTRAINT fk4oc1a80q9jt8b0et2kl64j8av FOREIGN KEY (processor_class_id) REFERENCES public.processor_class(id);


--
-- Name: product fk4oh5ogb84h479uxx9m2w2s1i3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT fk4oh5ogb84h479uxx9m2w2s1i3 FOREIGN KEY (configured_processor_id) REFERENCES public.configured_processor(id);


--
-- Name: product fk5g7do1yby2n4monwfjw1q79kc; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT fk5g7do1yby2n4monwfjw1q79kc FOREIGN KEY (product_class_id) REFERENCES public.product_class(id);


--
-- Name: spacecraft_payloads fk5pbclfmfjdlc2xc6m3k96x6j9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.spacecraft_payloads
    ADD CONSTRAINT fk5pbclfmfjdlc2xc6m3k96x6j9 FOREIGN KEY (spacecraft_id) REFERENCES public.spacecraft(id);


--
-- Name: processing_order_dynamic_processing_parameters fk64135mif3wav3bqm7g4hh5ko7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_dynamic_processing_parameters
    ADD CONSTRAINT fk64135mif3wav3bqm7g4hh5ko7 FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


--
-- Name: simple_selection_rule_configured_processors fk68ygyev0w1vb1jvnah0ry6vg9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule_configured_processors
    ADD CONSTRAINT fk68ygyev0w1vb1jvnah0ry6vg9 FOREIGN KEY (simple_selection_rule_id) REFERENCES public.simple_selection_rule(id);


--
-- Name: job fk6ek6xjklhsk2qduh5ejbcnb7c; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job
    ADD CONSTRAINT fk6ek6xjklhsk2qduh5ejbcnb7c FOREIGN KEY (processing_facility_id) REFERENCES public.processing_facility(id);


--
-- Name: workflow_output_parameters fk6jtuu5c7q9lcwfl8dp07jlywi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_output_parameters
    ADD CONSTRAINT fk6jtuu5c7q9lcwfl8dp07jlywi FOREIGN KEY (workflow_id) REFERENCES public.workflow(id);


--
-- Name: task fk6oktr0t8iad73hifdftqgwok9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task
    ADD CONSTRAINT fk6oktr0t8iad73hifdftqgwok9 FOREIGN KEY (processor_id) REFERENCES public.processor(id);


--
-- Name: processing_order_requested_product_classes fk7afxfgldbnrdi5joivn7agj86; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_requested_product_classes
    ADD CONSTRAINT fk7afxfgldbnrdi5joivn7agj86 FOREIGN KEY (requested_product_classes_id) REFERENCES public.product_class(id);


--
-- Name: processing_order_class_output_parameters fk7m4kynfbpfam8fvs66fpk6elk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_class_output_parameters
    ADD CONSTRAINT fk7m4kynfbpfam8fvs66fpk6elk FOREIGN KEY (class_output_parameters_id) REFERENCES public.class_output_parameter(id);


--
-- Name: workflow_class_output_parameters fk7p2bf4mui50rg5kg15k3a53g4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_class_output_parameters
    ADD CONSTRAINT fk7p2bf4mui50rg5kg15k3a53g4 FOREIGN KEY (class_output_parameters_key) REFERENCES public.product_class(id);


--
-- Name: processing_order_output_parameters fk7udpjfeq21n6vsi6rxeycsoi9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_output_parameters
    ADD CONSTRAINT fk7udpjfeq21n6vsi6rxeycsoi9 FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


--
-- Name: product_parameters fk84to6rlvpri4i2pjqpvfn5jd8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_parameters
    ADD CONSTRAINT fk84to6rlvpri4i2pjqpvfn5jd8 FOREIGN KEY (product_id) REFERENCES public.product(id);


--
-- Name: job fk8jj66thbddwdxad89qxjeepxg; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job
    ADD CONSTRAINT fk8jj66thbddwdxad89qxjeepxg FOREIGN KEY (orbit_id) REFERENCES public.orbit(id);


--
-- Name: simple_selection_rule fk8n3bq0ecxeti1ylukwkt7cnm; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule
    ADD CONSTRAINT fk8n3bq0ecxeti1ylukwkt7cnm FOREIGN KEY (source_product_class_id) REFERENCES public.product_class(id);


--
-- Name: simple_selection_rule_configured_processors fk8p1jxkynyy47c9slyxbjp18iu; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule_configured_processors
    ADD CONSTRAINT fk8p1jxkynyy47c9slyxbjp18iu FOREIGN KEY (configured_processors_id) REFERENCES public.configured_processor(id);


--
-- Name: workflow_class_output_parameters fk9fpufpt1t7q9kxmie12mi4se7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_class_output_parameters
    ADD CONSTRAINT fk9fpufpt1t7q9kxmie12mi4se7 FOREIGN KEY (class_output_parameters_id) REFERENCES public.class_output_parameter(id);


--
-- Name: authorities fk_authorities_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.authorities
    ADD CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES public.users(username);


--
-- Name: group_authorities fk_group_authorities_group; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_authorities
    ADD CONSTRAINT fk_group_authorities_group FOREIGN KEY (group_id) REFERENCES public.groups(id);


--
-- Name: group_members fk_group_members_group; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES public.groups(id);


--
-- Name: group_members fk_group_members_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.group_members
    ADD CONSTRAINT fk_group_members_user FOREIGN KEY (username) REFERENCES public.users(username);


--
-- Name: product_class fkafnqr7afqkr7vn6difh4r9e3j; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_class
    ADD CONSTRAINT fkafnqr7afqkr7vn6difh4r9e3j FOREIGN KEY (enclosing_class_id) REFERENCES public.product_class(id);


--
-- Name: product_query_filter_conditions fkag48xcu5bmuq9yqls0d824bkj; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_query_filter_conditions
    ADD CONSTRAINT fkag48xcu5bmuq9yqls0d824bkj FOREIGN KEY (product_query_id) REFERENCES public.product_query(id);


--
-- Name: groups_group_members fkawl37vgnmf8ny5a9txq0q0mtq; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.groups_group_members
    ADD CONSTRAINT fkawl37vgnmf8ny5a9txq0q0mtq FOREIGN KEY (group_members_id) REFERENCES public.group_members(id);


--
-- Name: job_step fkbi6cqwlkj3nyqkvheqeg5qql0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job_step
    ADD CONSTRAINT fkbi6cqwlkj3nyqkvheqeg5qql0 FOREIGN KEY (job_id) REFERENCES public.job(id);


--
-- Name: task_breakpoint_file_names fkblitkg6msystnhjpjj5ya1tfh; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.task_breakpoint_file_names
    ADD CONSTRAINT fkblitkg6msystnhjpjj5ya1tfh FOREIGN KEY (task_id) REFERENCES public.task(id);


--
-- Name: configuration fkbvl2q3rfimgbvr8o6txnm5ea7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration
    ADD CONSTRAINT fkbvl2q3rfimgbvr8o6txnm5ea7 FOREIGN KEY (processor_class_id) REFERENCES public.processor_class(id);


--
-- Name: product_query fkc71ouv75rseha12h0fmlqt6a5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_query
    ADD CONSTRAINT fkc71ouv75rseha12h0fmlqt6a5 FOREIGN KEY (job_step_id) REFERENCES public.job_step(id);


--
-- Name: mon_product_production_hour fkct5iw5b4h6q2y9oiy5acwph6s; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_product_production_hour
    ADD CONSTRAINT fkct5iw5b4h6q2y9oiy5acwph6s FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: processing_order_input_filters fkdhgcujq2nix39y2b7nbdpnlto; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_filters
    ADD CONSTRAINT fkdhgcujq2nix39y2b7nbdpnlto FOREIGN KEY (input_filters_key) REFERENCES public.product_class(id);


--
-- Name: configured_processor fkdj5cx8yntdnuxvphpowp47of5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configured_processor
    ADD CONSTRAINT fkdj5cx8yntdnuxvphpowp47of5 FOREIGN KEY (configuration_id) REFERENCES public.configuration(id);


--
-- Name: product fke8busf8q6a8uvrh9a5od38tqo; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT fke8busf8q6a8uvrh9a5od38tqo FOREIGN KEY (enclosing_product_id) REFERENCES public.product(id);


--
-- Name: product_archive_available_product_classes fke8v1poev1p25mcv38i8ueg5dl; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_archive_available_product_classes
    ADD CONSTRAINT fke8v1poev1p25mcv38i8ueg5dl FOREIGN KEY (product_archive_id) REFERENCES public.product_archive(id);


--
-- Name: processing_order_input_product_classes fkei14w0cwbjj4d293kf0kccovn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_product_classes
    ADD CONSTRAINT fkei14w0cwbjj4d293kf0kccovn FOREIGN KEY (input_product_classes_id) REFERENCES public.product_class(id);


--
-- Name: simple_policy_delta_times fkerahx0bbgt0eeqanerq28kofp; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_policy_delta_times
    ADD CONSTRAINT fkerahx0bbgt0eeqanerq28kofp FOREIGN KEY (simple_policy_id) REFERENCES public.simple_policy(id);


--
-- Name: class_output_parameter_output_parameters fkeyipsy3fwc5pwmym1lypkrkh3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.class_output_parameter_output_parameters
    ADD CONSTRAINT fkeyipsy3fwc5pwmym1lypkrkh3 FOREIGN KEY (class_output_parameter_id) REFERENCES public.class_output_parameter(id);


--
-- Name: processing_order_class_output_parameters fkf2c8fjwehnwek6aqehkdyig4r; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_class_output_parameters
    ADD CONSTRAINT fkf2c8fjwehnwek6aqehkdyig4r FOREIGN KEY (class_output_parameters_key) REFERENCES public.product_class(id);


--
-- Name: product_query fkfh82iydbxt4tvgiscy2qlj2m9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_query
    ADD CONSTRAINT fkfh82iydbxt4tvgiscy2qlj2m9 FOREIGN KEY (generating_rule_id) REFERENCES public.simple_selection_rule(id);


--
-- Name: simple_selection_rule_simple_policies fkfjb7qfppicnb1xj9vgi895dnb; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule_simple_policies
    ADD CONSTRAINT fkfjb7qfppicnb1xj9vgi895dnb FOREIGN KEY (simple_selection_rule_id) REFERENCES public.simple_selection_rule(id);


--
-- Name: groups_group_members fkfjhm6ctnf3akprkg5ic279dyi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.groups_group_members
    ADD CONSTRAINT fkfjhm6ctnf3akprkg5ic279dyi FOREIGN KEY (groups_id) REFERENCES public.groups(id);


--
-- Name: configuration_configuration_files fkg6qj2gjs3td0wwcioda96uik5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_configuration_files
    ADD CONSTRAINT fkg6qj2gjs3td0wwcioda96uik5 FOREIGN KEY (configuration_id) REFERENCES public.configuration(id);


--
-- Name: processing_order_input_filters fkgbh8k5vigdykb0s1cwhag6br5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_filters
    ADD CONSTRAINT fkgbh8k5vigdykb0s1cwhag6br5 FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


--
-- Name: processing_order_requested_configured_processors fkgdnmmc4ri3f4w2d52e935d3jg; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_requested_configured_processors
    ADD CONSTRAINT fkgdnmmc4ri3f4w2d52e935d3jg FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


--
-- Name: simple_selection_rule_simple_policies fkgijs10i27ucb2tosn56pqrqt6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule_simple_policies
    ADD CONSTRAINT fkgijs10i27ucb2tosn56pqrqt6 FOREIGN KEY (simple_policies_id) REFERENCES public.simple_policy(id);


--
-- Name: processing_order fkgj4135cm664vfl5jt6v613y0e; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order
    ADD CONSTRAINT fkgj4135cm664vfl5jt6v613y0e FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: configuration_static_input_files fkgls3b4eoq74nhjslcn57reige; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_static_input_files
    ADD CONSTRAINT fkgls3b4eoq74nhjslcn57reige FOREIGN KEY (configuration_id) REFERENCES public.configuration(id);


--
-- Name: processor_docker_run_parameters fkgqohmkxfbxo6ihxpgs84q5axp; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processor_docker_run_parameters
    ADD CONSTRAINT fkgqohmkxfbxo6ihxpgs84q5axp FOREIGN KEY (processor_id) REFERENCES public.processor(id);


--
-- Name: processing_order_requested_orbits fkgruycyl8hgdsmac11yl37odi9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_requested_orbits
    ADD CONSTRAINT fkgruycyl8hgdsmac11yl37odi9 FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


--
-- Name: users_group_memberships fkhbcokg6kjsft20melhs8njcma; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users_group_memberships
    ADD CONSTRAINT fkhbcokg6kjsft20melhs8njcma FOREIGN KEY (users_username) REFERENCES public.users(username);


--
-- Name: orbit fki2gpip0vqngjwnvmguox9wi3f; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orbit
    ADD CONSTRAINT fki2gpip0vqngjwnvmguox9wi3f FOREIGN KEY (spacecraft_id) REFERENCES public.spacecraft(id);


--
-- Name: configuration_input_file_file_names fki81ysbbwtpwxlhcm82eksdq1g; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_input_file_file_names
    ADD CONSTRAINT fki81ysbbwtpwxlhcm82eksdq1g FOREIGN KEY (configuration_input_file_id) REFERENCES public.configuration_input_file(id);


--
-- Name: simple_selection_rule_filter_conditions fki9hebpru8hilywjux8v76p2em; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule_filter_conditions
    ADD CONSTRAINT fki9hebpru8hilywjux8v76p2em FOREIGN KEY (simple_selection_rule_id) REFERENCES public.simple_selection_rule(id);


--
-- Name: product_class fkinocsatitcf1ofpp4wc7psua2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_class
    ADD CONSTRAINT fkinocsatitcf1ofpp4wc7psua2 FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: mon_ext_service_state_operation fkiqb5ahpdcute0oip6q9wovayp; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_ext_service_state_operation
    ADD CONSTRAINT fkiqb5ahpdcute0oip6q9wovayp FOREIGN KEY (mon_service_state_id) REFERENCES public.mon_service_state(id);


--
-- Name: processing_order_requested_product_classes fkj0e73npk4ljcr6lupi978clea; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_requested_product_classes
    ADD CONSTRAINT fkj0e73npk4ljcr6lupi978clea FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


--
-- Name: product_query_satisfying_products fkj1us8b41hn4xc8ug12c530ei1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_query_satisfying_products
    ADD CONSTRAINT fkj1us8b41hn4xc8ug12c530ei1 FOREIGN KEY (satisfied_product_queries_id) REFERENCES public.product_query(id);


--
-- Name: workflow_input_filters fkjdmteac11nvr5yvxrinn35rfb; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_input_filters
    ADD CONSTRAINT fkjdmteac11nvr5yvxrinn35rfb FOREIGN KEY (input_filters_id) REFERENCES public.input_filter(id);


--
-- Name: simple_selection_rule fkje8biclfyorg1wm8uh1qf9d0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule
    ADD CONSTRAINT fkje8biclfyorg1wm8uh1qf9d0 FOREIGN KEY (target_product_class_id) REFERENCES public.product_class(id);


--
-- Name: configuration_dyn_proc_parameters fkjpifxw2lvac6ipxqdimmy73k4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_dyn_proc_parameters
    ADD CONSTRAINT fkjpifxw2lvac6ipxqdimmy73k4 FOREIGN KEY (configuration_id) REFERENCES public.configuration(id);


--
-- Name: input_filter_filter_conditions fkjqbbl8slm6j7oco6vfg88duq2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.input_filter_filter_conditions
    ADD CONSTRAINT fkjqbbl8slm6j7oco6vfg88duq2 FOREIGN KEY (input_filter_id) REFERENCES public.input_filter(id);


--
-- Name: processing_order_mon_order_progress fkjwvq04w9s9sfhftuebjq7rop; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_mon_order_progress
    ADD CONSTRAINT fkjwvq04w9s9sfhftuebjq7rop FOREIGN KEY (mon_order_state_id) REFERENCES public.mon_order_state(id);


--
-- Name: product fkk9wbgbn6a2xyr7f7vl65uogis; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT fkk9wbgbn6a2xyr7f7vl65uogis FOREIGN KEY (job_step_id) REFERENCES public.job_step(id);


--
-- Name: processing_order_mon_order_progress fkkiu48p0ndrxpd71y09yn2q474; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_mon_order_progress
    ADD CONSTRAINT fkkiu48p0ndrxpd71y09yn2q474 FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


--
-- Name: processing_order_requested_configured_processors fkkkhi2aj21ehrsok4ekhl1fd31; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_requested_configured_processors
    ADD CONSTRAINT fkkkhi2aj21ehrsok4ekhl1fd31 FOREIGN KEY (requested_configured_processors_id) REFERENCES public.configured_processor(id);


--
-- Name: product fkkobwm0e23qst5q2irk37fwmuy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT fkkobwm0e23qst5q2irk37fwmuy FOREIGN KEY (orbit_id) REFERENCES public.orbit(id);


--
-- Name: mon_service_state_operation fkkp6fp3q2xd1x9ogghmy7pdht0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_service_state_operation
    ADD CONSTRAINT fkkp6fp3q2xd1x9ogghmy7pdht0 FOREIGN KEY (mon_service_id) REFERENCES public.mon_service(id);


--
-- Name: mon_ext_service_state_operation fkkrcxkfr1txuoeif89p2igbh6g; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_ext_service_state_operation
    ADD CONSTRAINT fkkrcxkfr1txuoeif89p2igbh6g FOREIGN KEY (mon_ext_service_id) REFERENCES public.mon_ext_service(id);


--
-- Name: workflow fkl0agecusohc3yft3vq7xlaikr; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT fkl0agecusohc3yft3vq7xlaikr FOREIGN KEY (input_product_class_id) REFERENCES public.product_class(id);


--
-- Name: processing_order_input_product_classes fkl16rc9whni5al0b0t4ukf1dq6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_product_classes
    ADD CONSTRAINT fkl16rc9whni5al0b0t4ukf1dq6 FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


--
-- Name: workflow_option fkl2fwnrxddum783emw1a3qwuot; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_option
    ADD CONSTRAINT fkl2fwnrxddum783emw1a3qwuot FOREIGN KEY (workflow_id) REFERENCES public.workflow(id);


--
-- Name: processing_order_requested_orbits fkl52pcfs07440sihimxmpy6iva; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_requested_orbits
    ADD CONSTRAINT fkl52pcfs07440sihimxmpy6iva FOREIGN KEY (requested_orbits_id) REFERENCES public.orbit(id);


--
-- Name: configured_processor fkloteyhnalc56x161f4inujyt5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configured_processor
    ADD CONSTRAINT fkloteyhnalc56x161f4inujyt5 FOREIGN KEY (processor_id) REFERENCES public.processor(id);


--
-- Name: job_step_output_parameters fklw2fh8ksho7gcvrfykeoep899; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job_step_output_parameters
    ADD CONSTRAINT fklw2fh8ksho7gcvrfykeoep899 FOREIGN KEY (job_step_id) REFERENCES public.job_step(id);


--
-- Name: processing_order_class_output_parameters fklxehk5y7wbwpi3gxj00eg3p89; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_class_output_parameters
    ADD CONSTRAINT fklxehk5y7wbwpi3gxj00eg3p89 FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


--
-- Name: processor_class fklxfogyfhmujn40qg0ooxfdwfv; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processor_class
    ADD CONSTRAINT fklxfogyfhmujn40qg0ooxfdwfv FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: product_download_history fkm3o1ca4ms7b4ereu9wvsxpeet; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_download_history
    ADD CONSTRAINT fkm3o1ca4ms7b4ereu9wvsxpeet FOREIGN KEY (product_id) REFERENCES public.product(id);


--
-- Name: workflow fkmwefyimusp3lp3qlj7tsdgmm6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT fkmwefyimusp3lp3qlj7tsdgmm6 FOREIGN KEY (output_product_class_id) REFERENCES public.product_class(id);


--
-- Name: workflow fkn0aaw3ptxvvfwb1tbuoowki42; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT fkn0aaw3ptxvvfwb1tbuoowki42 FOREIGN KEY (configured_processor_id) REFERENCES public.configured_processor(id);


--
-- Name: processor fko4ocncq22u0j2prxw2dbk0dka; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processor
    ADD CONSTRAINT fko4ocncq22u0j2prxw2dbk0dka FOREIGN KEY (processor_class_id) REFERENCES public.processor_class(id);


--
-- Name: job fko7lm1bpn9pqf1qq9o5fpfjtic; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job
    ADD CONSTRAINT fko7lm1bpn9pqf1qq9o5fpfjtic FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


--
-- Name: product_query_satisfying_products fkq768nqgupajiccjpbyawcqhtd; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_query_satisfying_products
    ADD CONSTRAINT fkq768nqgupajiccjpbyawcqhtd FOREIGN KEY (satisfying_products_id) REFERENCES public.product(id);


--
-- Name: mon_service_state_operation fkq96wp2nrf7tvih0otaf0ojtyg; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_service_state_operation
    ADD CONSTRAINT fkq96wp2nrf7tvih0otaf0ojtyg FOREIGN KEY (mon_service_state_id) REFERENCES public.mon_service_state(id);


--
-- Name: mission_processing_modes fkqhg2duxhcpldh28nyh7nwnfcn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mission_processing_modes
    ADD CONSTRAINT fkqhg2duxhcpldh28nyh7nwnfcn FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: product_file fkqs127y6vnoylxgo8aroqx4e8f; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_file
    ADD CONSTRAINT fkqs127y6vnoylxgo8aroqx4e8f FOREIGN KEY (product_id) REFERENCES public.product(id);


--
-- Name: workflow_input_filters fkqur70i7n8cka6j0jjo8exje2x; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_input_filters
    ADD CONSTRAINT fkqur70i7n8cka6j0jjo8exje2x FOREIGN KEY (workflow_id) REFERENCES public.workflow(id);


--
-- Name: mission_file_classes fks4suek1246jge02gcgpnoms5y; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mission_file_classes
    ADD CONSTRAINT fks4suek1246jge02gcgpnoms5y FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: workflow fksbdoolsgnxt5ji4bfnmpe14y8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT fksbdoolsgnxt5ji4bfnmpe14y8 FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: mon_product_production_month fkshuxduj4xdpxqoty4f6desfun; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mon_product_production_month
    ADD CONSTRAINT fkshuxduj4xdpxqoty4f6desfun FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: product_archive_available_product_classes fksmgbtsw0scm0bdg6bd49do29r; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_archive_available_product_classes
    ADD CONSTRAINT fksmgbtsw0scm0bdg6bd49do29r FOREIGN KEY (available_product_classes_id) REFERENCES public.product_class(id);


--
-- Name: spacecraft fksp2jjwkpaehybfu5pwedol1c; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.spacecraft
    ADD CONSTRAINT fksp2jjwkpaehybfu5pwedol1c FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: processing_order fkt2f7nkjj7muumygco1sj81hn1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order
    ADD CONSTRAINT fkt2f7nkjj7muumygco1sj81hn1 FOREIGN KEY (workflow_id) REFERENCES public.workflow(id);


--
-- Name: users_group_memberships fktodlfclgikl9ionfovl0t7wp0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users_group_memberships
    ADD CONSTRAINT fktodlfclgikl9ionfovl0t7wp0 FOREIGN KEY (group_memberships_id) REFERENCES public.group_members(id);


--
-- Name: workflow_option_value_range fky9hi8gn5n5pdglqyl1qyowvn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.workflow_option_value_range
    ADD CONSTRAINT fky9hi8gn5n5pdglqyl1qyowvn FOREIGN KEY (workflow_option_id) REFERENCES public.workflow_option(id);


--
-- PostgreSQL database dump complete
--

