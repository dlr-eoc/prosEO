--
-- PostgreSQL database dump
--

-- Dumped from database version 11.10 (Debian 11.10-1.pgdg90+1)
-- Dumped by pg_dump version 11.5

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
    uuid uuid,
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
    job_order_filename character varying(255),
    job_step_state character varying(255),
    processing_completion_time timestamp without time zone,
    processing_mode character varying(255),
    processing_start_time timestamp without time zone,
    processing_std_err text,
    processing_std_out text,
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
    product_file_template text
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
    facility_state character varying(255),
    local_storage_manager_url character varying(255),
    name character varying(255) NOT NULL,
    processing_engine_token text,
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
    execution_time timestamp without time zone,
    has_failed_job_steps boolean,
    identifier character varying(255) NOT NULL,
    order_state character varying(255),
    output_file_class character varying(255),
    processing_mode character varying(255),
    production_type character varying(255),
    slice_duration bigint,
    slice_overlap bigint,
    slicing_type character varying(255),
    start_time timestamp(6) without time zone,
    stop_time timestamp(6) without time zone,
    uuid uuid NOT NULL,
    mission_id bigint
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
-- Name: processing_order_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order_output_parameters (
    processing_order_id bigint NOT NULL,
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
    max_time integer,
    min_disk_space integer,
    processor_version character varying(255),
    sensing_time_flag boolean,
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
    file_class character varying(255),
    generation_time timestamp(6) without time zone,
    mode character varying(255),
    product_quality character varying(255),
    production_type character varying(255),
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
-- Name: product_class; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_class (
    id bigint NOT NULL,
    version integer NOT NULL,
    default_slice_duration bigint,
    default_slicing_type character varying(255),
    description character varying(255),
    processing_level character varying(255),
    product_file_template character varying(255),
    product_type character varying(255),
    visibility character varying(255),
    enclosing_class_id bigint,
    mission_id bigint,
    processor_class_id bigint
);


ALTER TABLE public.product_class OWNER TO postgres;

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
-- Name: simple_selection_rule_applicable_configured_processors; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.simple_selection_rule_applicable_configured_processors (
    simple_selection_rule_id bigint NOT NULL,
    applicable_configured_processors_id bigint NOT NULL
);


ALTER TABLE public.simple_selection_rule_applicable_configured_processors OWNER TO postgres;

--
-- Name: simple_selection_rule_filter_conditions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.simple_selection_rule_filter_conditions (
    simple_selection_rule_id bigint NOT NULL,
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
-- Name: task; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.task (
    id bigint NOT NULL,
    version integer NOT NULL,
    criticality_level integer,
    is_critical boolean,
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
    password_expiration_date timestamp without time zone NOT NULL
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
-- Name: simple_selection_rule_applicable_configured_processors simple_selection_rule_applicable_configured_processors_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule_applicable_configured_processors
    ADD CONSTRAINT simple_selection_rule_applicable_configured_processors_pkey PRIMARY KEY (simple_selection_rule_id, applicable_configured_processors_id);


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
-- Name: product uk24bc4yyyk3fj3h7ku64i3yuog; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT uk24bc4yyyk3fj3h7ku64i3yuog UNIQUE (uuid);


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
-- Name: groups uk_7o859iyhxd19rv4hywgdvu2v4; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.groups
    ADD CONSTRAINT uk_7o859iyhxd19rv4hywgdvu2v4 UNIQUE (group_name);


--
-- Name: users_group_memberships uk_e2ijwadyxqhcr2aldhs624px; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users_group_memberships
    ADD CONSTRAINT uk_e2ijwadyxqhcr2aldhs624px UNIQUE (group_memberships_id);


--
-- Name: processing_order ukbdceutrqoge4t26w7bixj9afg; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order
    ADD CONSTRAINT ukbdceutrqoge4t26w7bixj9afg UNIQUE (identifier);


--
-- Name: product_file ukdawt5bhyxxovxd4vgo4cw6ugn; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_file
    ADD CONSTRAINT ukdawt5bhyxxovxd4vgo4cw6ugn UNIQUE (product_id, processing_facility_id);


--
-- Name: product_class ukgmc9l016fh1mcqque3k8iy3yu; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_class
    ADD CONSTRAINT ukgmc9l016fh1mcqque3k8iy3yu UNIQUE (mission_id, product_type);


--
-- Name: processor_class uknbv3u0tx6s1770tmlvylhodfw; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processor_class
    ADD CONSTRAINT uknbv3u0tx6s1770tmlvylhodfw UNIQUE (mission_id, processor_name);


--
-- Name: configured_processor ukqluoxbhk7kihm71dkalmcq1tq; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configured_processor
    ADD CONSTRAINT ukqluoxbhk7kihm71dkalmcq1tq UNIQUE (identifier);


--
-- Name: processing_order ukqx1os0kwk3rvvoe4bw7inowsq; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order
    ADD CONSTRAINT ukqx1os0kwk3rvvoe4bw7inowsq UNIQUE (uuid);


--
-- Name: spacecraft ukt95fh7lk9yu00lk7wbcfkagde; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.spacecraft
    ADD CONSTRAINT ukt95fh7lk9yu00lk7wbcfkagde UNIQUE (code);


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
-- Name: idx657h07h95ub6nyt49a4wib4ky; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx657h07h95ub6nyt49a4wib4ky ON public.orbit USING btree (start_time);


--
-- Name: idx6u8n1u6c253dps752x6s7yaa7; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx6u8n1u6c253dps752x6s7yaa7 ON public.product_query USING btree (requested_product_class_id);


--
-- Name: idx99jry69detoongt94mr0cb4jm; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx99jry69detoongt94mr0cb4jm ON public.job_step USING btree (job_step_state);


--
-- Name: idxckdfwsktiy2a7t08oulv9p4bj; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxckdfwsktiy2a7t08oulv9p4bj ON public.processing_order USING btree (execution_time);


--
-- Name: idxdp0vd1b50igr05nxswcxupv6v; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxdp0vd1b50igr05nxswcxupv6v ON public.product USING btree (product_class_id, sensing_start_time);


--
-- Name: idxebh2ci5ivqufcgtxv4gax0mif; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxebh2ci5ivqufcgtxv4gax0mif ON public.product USING btree (product_class_id, generation_time);


--
-- Name: idxhw65e77rikl7b5qq70e6kjgpg; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxhw65e77rikl7b5qq70e6kjgpg ON public.job USING btree (job_state);


--
-- Name: idxojvxy5otq1rh1mu4heg6ny1yn; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxojvxy5otq1rh1mu4heg6ny1yn ON public.product USING btree (product_class_id, sensing_stop_time);


--
-- Name: configuration_docker_run_parameters fk165qo4rdh6j4v72p19t5rluv3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.configuration_docker_run_parameters
    ADD CONSTRAINT fk165qo4rdh6j4v72p19t5rluv3 FOREIGN KEY (configuration_id) REFERENCES public.configuration(id);


--
-- Name: processing_order_input_filters fk1u9dj81sg3vcueaprup3hasqi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_filters
    ADD CONSTRAINT fk1u9dj81sg3vcueaprup3hasqi FOREIGN KEY (input_filters_id) REFERENCES public.input_filter(id);


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
-- Name: simple_selection_rule_applicable_configured_processors fk2fc10i4kvwdl75twbs0f1jnae; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule_applicable_configured_processors
    ADD CONSTRAINT fk2fc10i4kvwdl75twbs0f1jnae FOREIGN KEY (applicable_configured_processors_id) REFERENCES public.configured_processor(id);


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
-- Name: job fk6ek6xjklhsk2qduh5ejbcnb7c; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job
    ADD CONSTRAINT fk6ek6xjklhsk2qduh5ejbcnb7c FOREIGN KEY (processing_facility_id) REFERENCES public.processing_facility(id);


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
-- Name: simple_selection_rule_applicable_configured_processors fkb9av1i977yys0ql1habohq1o7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.simple_selection_rule_applicable_configured_processors
    ADD CONSTRAINT fkb9av1i977yys0ql1habohq1o7 FOREIGN KEY (simple_selection_rule_id) REFERENCES public.simple_selection_rule(id);


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
-- Name: product fkk9wbgbn6a2xyr7f7vl65uogis; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product
    ADD CONSTRAINT fkk9wbgbn6a2xyr7f7vl65uogis FOREIGN KEY (job_step_id) REFERENCES public.job_step(id);


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
-- Name: processing_order_input_product_classes fkl16rc9whni5al0b0t4ukf1dq6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_product_classes
    ADD CONSTRAINT fkl16rc9whni5al0b0t4ukf1dq6 FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


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
-- Name: mission_file_classes fks4suek1246jge02gcgpnoms5y; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.mission_file_classes
    ADD CONSTRAINT fks4suek1246jge02gcgpnoms5y FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: spacecraft fksp2jjwkpaehybfu5pwedol1c; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.spacecraft
    ADD CONSTRAINT fksp2jjwkpaehybfu5pwedol1c FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: users_group_memberships fktodlfclgikl9ionfovl0t7wp0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users_group_memberships
    ADD CONSTRAINT fktodlfclgikl9ionfovl0t7wp0 FOREIGN KEY (group_memberships_id) REFERENCES public.group_members(id);


--
-- PostgreSQL database dump complete
--

