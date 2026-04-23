--
-- prosEO Database Schema Update for prosEO 2.1.0
--
-- Date: 2026-04-23
--

--
-- Data model correction: Unique constraint changed from name to nameId
--
ALTER TABLE ONLY public.mon_service
    DROP CONSTRAINT IF EXISTS uk239unm9hg59upu3be0fkcu4rt,
    ADD CONSTRAINT ukacf8th9woalj8upxo9rqqrfn3 UNIQUE (name_id);

ALTER TABLE ONLY public.mon_ext_service
    DROP CONSTRAINT IF EXISTS uk6iqkhmagcms83dnb123yfd0s2,
    ADD CONSTRAINT ukojd6hfgp9ym4ennt3ardainn5 UNIQUE (name_id);
    
    
--
-- Schema additions for automatic generation of processing orders
--

--
-- Name: calendar_order_trigger; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.calendar_order_trigger (
    id bigint NOT NULL,
    version integer,
    enabled boolean,
    execution_delay numeric(21,0),
    name character varying(255),
    priority integer,
    cron_expression character varying(255),
    mission_id bigint,
    order_template_id bigint
);


ALTER TABLE public.calendar_order_trigger OWNER TO postgres;

--
-- Name: calendar_order_trigger_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.calendar_order_trigger_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.calendar_order_trigger_seq OWNER TO postgres;

--
-- Name: data_driven_order_trigger; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.data_driven_order_trigger (
    id bigint NOT NULL,
    version integer,
    enabled boolean,
    execution_delay numeric(21,0),
    name character varying(255),
    priority integer,
    input_file_class character varying(255),
    input_processing_mode character varying(255),
    mission_id bigint,
    order_template_id bigint,
    input_product_class_id bigint
);


ALTER TABLE public.data_driven_order_trigger OWNER TO postgres;

--
-- Name: data_driven_order_trigger_parameters_to_copy; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.data_driven_order_trigger_parameters_to_copy (
    data_driven_order_trigger_id bigint NOT NULL,
    parameters_to_copy character varying(255)
);


ALTER TABLE public.data_driven_order_trigger_parameters_to_copy OWNER TO postgres;

--
-- Name: data_driven_order_trigger_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.data_driven_order_trigger_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.data_driven_order_trigger_seq OWNER TO postgres;

--
-- Name: datatake_order_trigger; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.datatake_order_trigger (
    id bigint NOT NULL,
    version integer,
    enabled boolean,
    execution_delay numeric(21,0),
    name character varying(255),
    priority integer,
    delta_time numeric(21,0),
    datatake_type character varying(255),
    last_datatake_start_time timestamp(6) with time zone,
    mission_id bigint,
    order_template_id bigint
);


ALTER TABLE public.datatake_order_trigger OWNER TO postgres;

--
-- Name: datatake_order_trigger_parameters_to_copy; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.datatake_order_trigger_parameters_to_copy (
    datatake_order_trigger_id bigint NOT NULL,
    parameters_to_copy character varying(255)
);


ALTER TABLE public.datatake_order_trigger_parameters_to_copy OWNER TO postgres;

--
-- Name: datatake_order_trigger_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.datatake_order_trigger_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.datatake_order_trigger_seq OWNER TO postgres;

--
-- Name: orbit_order_trigger; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.orbit_order_trigger (
    id bigint NOT NULL,
    version integer,
    enabled boolean,
    execution_delay numeric(21,0),
    name character varying(255),
    priority integer,
    delta_time numeric(21,0),
    mission_id bigint,
    order_template_id bigint,
    last_orbit_id bigint,
    spacecraft_id bigint
);


ALTER TABLE public.orbit_order_trigger OWNER TO postgres;

--
-- Name: orbit_order_trigger_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.orbit_order_trigger_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.orbit_order_trigger_seq OWNER TO postgres;

--
-- Name: order_template; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_template (
    id bigint NOT NULL,
    version integer,
    enabled boolean,
    name character varying(255) NOT NULL,
    output_file_class character varying(255),
    priority integer,
    processing_mode character varying(255),
    slice_duration numeric(21,0),
    slice_overlap numeric(21,0),
    slicing_type character varying(255),
    auto_close boolean DEFAULT false NOT NULL,
    auto_release boolean DEFAULT false NOT NULL,
    input_data_timeout_period numeric(21,0),
    endpoint_password character varying(255),
    endpoint_uri character varying(255),
    endpoint_username character varying(255),
    on_input_data_timeout_fail boolean DEFAULT true NOT NULL,
    product_retention_period numeric(21,0),
    production_type character varying(255),
    mission_id bigint,
    CONSTRAINT order_template_production_type_check CHECK (((production_type)::text = ANY ((ARRAY['SYSTEMATIC'::character varying, 'ON_DEMAND_DEFAULT'::character varying, 'ON_DEMAND_NON_DEFAULT'::character varying])::text[]))),
    CONSTRAINT order_template_slicing_type_check CHECK (((slicing_type)::text = ANY ((ARRAY['ORBIT'::character varying, 'CALENDAR_DAY'::character varying, 'CALENDAR_MONTH'::character varying, 'CALENDAR_YEAR'::character varying, 'TIME_SLICE'::character varying, 'NONE'::character varying])::text[])))
);


ALTER TABLE public.order_template OWNER TO postgres;

--
-- Name: order_template_class_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_template_class_output_parameters (
    order_template_id bigint NOT NULL,
    class_output_parameters_id bigint NOT NULL,
    class_output_parameters_key bigint NOT NULL
);


ALTER TABLE public.order_template_class_output_parameters OWNER TO postgres;

--
-- Name: order_template_dynamic_processing_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_template_dynamic_processing_parameters (
    order_template_id bigint NOT NULL,
    parameter_clob oid,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    dynamic_processing_parameters_key character varying(255) NOT NULL,
    CONSTRAINT order_template_dynamic_processing_paramete_parameter_type_check CHECK (((parameter_type)::text = ANY ((ARRAY['STRING'::character varying, 'BOOLEAN'::character varying, 'INTEGER'::character varying, 'DOUBLE'::character varying, 'INSTANT'::character varying])::text[])))
);


ALTER TABLE public.order_template_dynamic_processing_parameters OWNER TO postgres;

--
-- Name: order_template_input_filters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_template_input_filters (
    order_template_id bigint NOT NULL,
    input_filters_id bigint NOT NULL,
    input_filters_key bigint NOT NULL
);


ALTER TABLE public.order_template_input_filters OWNER TO postgres;

--
-- Name: order_template_input_product_classes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_template_input_product_classes (
    order_template_id bigint NOT NULL,
    input_product_classes_id bigint NOT NULL
);


ALTER TABLE public.order_template_input_product_classes OWNER TO postgres;

--
-- Name: order_template_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_template_output_parameters (
    order_template_id bigint NOT NULL,
    parameter_clob oid,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    output_parameters_key character varying(255) NOT NULL,
    CONSTRAINT order_template_output_parameters_parameter_type_check CHECK (((parameter_type)::text = ANY ((ARRAY['STRING'::character varying, 'BOOLEAN'::character varying, 'INTEGER'::character varying, 'DOUBLE'::character varying, 'INSTANT'::character varying])::text[])))
);


ALTER TABLE public.order_template_output_parameters OWNER TO postgres;

--
-- Name: order_template_requested_configured_processors; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_template_requested_configured_processors (
    order_template_id bigint NOT NULL,
    requested_configured_processors_id bigint NOT NULL
);


ALTER TABLE public.order_template_requested_configured_processors OWNER TO postgres;

--
-- Name: order_template_requested_product_classes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.order_template_requested_product_classes (
    order_template_id bigint NOT NULL,
    requested_product_classes_id bigint NOT NULL
);


ALTER TABLE public.order_template_requested_product_classes OWNER TO postgres;

--
-- Name: order_template_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.order_template_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.order_template_seq OWNER TO postgres;

--
-- Name: time_interval_order_trigger; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.time_interval_order_trigger (
    id bigint NOT NULL,
    version integer,
    enabled boolean,
    execution_delay numeric(21,0),
    name character varying(255),
    priority integer,
    next_trigger_time timestamp(6) with time zone,
    trigger_interval numeric(21,0),
    mission_id bigint,
    order_template_id bigint
);


ALTER TABLE public.time_interval_order_trigger OWNER TO postgres;

--
-- Name: time_interval_order_trigger_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.time_interval_order_trigger_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.time_interval_order_trigger_seq OWNER TO postgres;

--
-- Name: calendar_order_trigger calendar_order_trigger_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.calendar_order_trigger
    ADD CONSTRAINT calendar_order_trigger_pkey PRIMARY KEY (id);


--
-- Name: data_driven_order_trigger data_driven_order_trigger_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.data_driven_order_trigger
    ADD CONSTRAINT data_driven_order_trigger_pkey PRIMARY KEY (id);


--
-- Name: datatake_order_trigger datatake_order_trigger_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.datatake_order_trigger
    ADD CONSTRAINT datatake_order_trigger_pkey PRIMARY KEY (id);


--
-- Name: orbit_order_trigger orbit_order_trigger_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orbit_order_trigger
    ADD CONSTRAINT orbit_order_trigger_pkey PRIMARY KEY (id);


--
-- Name: order_template_class_output_parameters order_template_class_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_class_output_parameters
    ADD CONSTRAINT order_template_class_output_parameters_pkey PRIMARY KEY (order_template_id, class_output_parameters_key);


--
-- Name: order_template_dynamic_processing_parameters order_template_dynamic_processing_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_dynamic_processing_parameters
    ADD CONSTRAINT order_template_dynamic_processing_parameters_pkey PRIMARY KEY (order_template_id, dynamic_processing_parameters_key);


--
-- Name: order_template_input_filters order_template_input_filters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_input_filters
    ADD CONSTRAINT order_template_input_filters_pkey PRIMARY KEY (order_template_id, input_filters_key);


--
-- Name: order_template_input_product_classes order_template_input_product_classes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_input_product_classes
    ADD CONSTRAINT order_template_input_product_classes_pkey PRIMARY KEY (order_template_id, input_product_classes_id);


--
-- Name: order_template_output_parameters order_template_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_output_parameters
    ADD CONSTRAINT order_template_output_parameters_pkey PRIMARY KEY (order_template_id, output_parameters_key);


--
-- Name: order_template order_template_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template
    ADD CONSTRAINT order_template_pkey PRIMARY KEY (id);


--
-- Name: order_template_requested_configured_processors order_template_requested_configured_processors_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_requested_configured_processors
    ADD CONSTRAINT order_template_requested_configured_processors_pkey PRIMARY KEY (order_template_id, requested_configured_processors_id);


--
-- Name: order_template_requested_product_classes order_template_requested_product_classes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_requested_product_classes
    ADD CONSTRAINT order_template_requested_product_classes_pkey PRIMARY KEY (order_template_id, requested_product_classes_id);


--
-- Name: time_interval_order_trigger time_interval_order_trigger_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.time_interval_order_trigger
    ADD CONSTRAINT time_interval_order_trigger_pkey PRIMARY KEY (id);


--
-- Name: time_interval_order_trigger uk1t9fbt4q4vpvtl12scxvsa11v; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.time_interval_order_trigger
    ADD CONSTRAINT uk1t9fbt4q4vpvtl12scxvsa11v UNIQUE (mission_id, name);


--
-- Name: data_driven_order_trigger uk2mw824tj5uvs1xthak0cdx6jv; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.data_driven_order_trigger
    ADD CONSTRAINT uk2mw824tj5uvs1xthak0cdx6jv UNIQUE (mission_id, name);


--
-- Name: datatake_order_trigger uk60i49xw84d1jqb10gj12huf85; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.datatake_order_trigger
    ADD CONSTRAINT uk60i49xw84d1jqb10gj12huf85 UNIQUE (mission_id, name);


--
-- Name: orbit_order_trigger uk868m6a0ipfeyjegxx1xfi15fv; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orbit_order_trigger
    ADD CONSTRAINT uk868m6a0ipfeyjegxx1xfi15fv UNIQUE (mission_id, name);


--
-- Name: calendar_order_trigger ukdx1vtodammch2e2pe8oiow3o3; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.calendar_order_trigger
    ADD CONSTRAINT ukdx1vtodammch2e2pe8oiow3o3 UNIQUE (mission_id, name);


--
-- Name: order_template ukr797spmuqejkr5xedpw2f5n25; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template
    ADD CONSTRAINT ukr797spmuqejkr5xedpw2f5n25 UNIQUE (mission_id, name);


--
-- Name: data_driven_order_trigger fk14ulnpq0evwqi1fussvt8wqaf; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.data_driven_order_trigger
    ADD CONSTRAINT fk14ulnpq0evwqi1fussvt8wqaf FOREIGN KEY (input_product_class_id) REFERENCES public.product_class(id);


--
-- Name: calendar_order_trigger fk1td48mqbsnwp38tak37qe6mpm; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.calendar_order_trigger
    ADD CONSTRAINT fk1td48mqbsnwp38tak37qe6mpm FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: orbit_order_trigger fk3dhykmg158mskeps4rqtsb9k3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orbit_order_trigger
    ADD CONSTRAINT fk3dhykmg158mskeps4rqtsb9k3 FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: order_template_class_output_parameters fk4984ar8dhbsj8xbtg852wgjny; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_class_output_parameters
    ADD CONSTRAINT fk4984ar8dhbsj8xbtg852wgjny FOREIGN KEY (class_output_parameters_id) REFERENCES public.class_output_parameter(id);


--
-- Name: datatake_order_trigger fk5ku272pm9bvy3v4rm2n9qopxh; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.datatake_order_trigger
    ADD CONSTRAINT fk5ku272pm9bvy3v4rm2n9qopxh FOREIGN KEY (order_template_id) REFERENCES public.order_template(id);


--
-- Name: calendar_order_trigger fk5vftjcj62y05gvtrvspxnbqjw; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.calendar_order_trigger
    ADD CONSTRAINT fk5vftjcj62y05gvtrvspxnbqjw FOREIGN KEY (order_template_id) REFERENCES public.order_template(id);


--
-- Name: order_template_requested_configured_processors fk64x98vvcnvrxxmymjq38jqsnf; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_requested_configured_processors
    ADD CONSTRAINT fk64x98vvcnvrxxmymjq38jqsnf FOREIGN KEY (requested_configured_processors_id) REFERENCES public.configured_processor(id);


--
-- Name: orbit_order_trigger fk6rvotl5g61xuw8o0d07fj3wis; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orbit_order_trigger
    ADD CONSTRAINT fk6rvotl5g61xuw8o0d07fj3wis FOREIGN KEY (order_template_id) REFERENCES public.order_template(id);


--
-- Name: order_template_requested_configured_processors fk73xp3f253mwtxu95vbd19it6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_requested_configured_processors
    ADD CONSTRAINT fk73xp3f253mwtxu95vbd19it6 FOREIGN KEY (order_template_id) REFERENCES public.order_template(id);


--
-- Name: datatake_order_trigger_parameters_to_copy fk7mqnrsbriavwfaubteu45tgto; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.datatake_order_trigger_parameters_to_copy
    ADD CONSTRAINT fk7mqnrsbriavwfaubteu45tgto FOREIGN KEY (datatake_order_trigger_id) REFERENCES public.datatake_order_trigger(id);


--
-- Name: order_template_input_filters fk8m95vn771191j6kaf3wlyfhrn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_input_filters
    ADD CONSTRAINT fk8m95vn771191j6kaf3wlyfhrn FOREIGN KEY (input_filters_id) REFERENCES public.input_filter(id);


--
-- Name: order_template_requested_product_classes fkas408ueio6hp3ayont2supypd; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_requested_product_classes
    ADD CONSTRAINT fkas408ueio6hp3ayont2supypd FOREIGN KEY (order_template_id) REFERENCES public.order_template(id);


--
-- Name: orbit_order_trigger fkemcadm2w73h5jawoghr9yrt7f; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orbit_order_trigger
    ADD CONSTRAINT fkemcadm2w73h5jawoghr9yrt7f FOREIGN KEY (spacecraft_id) REFERENCES public.spacecraft(id);


--
-- Name: order_template_class_output_parameters fkey4ujp5k2odm7cofcn0wh6xx0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_class_output_parameters
    ADD CONSTRAINT fkey4ujp5k2odm7cofcn0wh6xx0 FOREIGN KEY (class_output_parameters_key) REFERENCES public.product_class(id);


--
-- Name: order_template_input_product_classes fkg8p9yjh18cfr64bk0rhce7pun; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_input_product_classes
    ADD CONSTRAINT fkg8p9yjh18cfr64bk0rhce7pun FOREIGN KEY (order_template_id) REFERENCES public.order_template(id);


--
-- Name: time_interval_order_trigger fkht1lv9uhalribfalwdiossvyd; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.time_interval_order_trigger
    ADD CONSTRAINT fkht1lv9uhalribfalwdiossvyd FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: order_template_input_filters fkhtukhh60isuph1xgwobxp3oe6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_input_filters
    ADD CONSTRAINT fkhtukhh60isuph1xgwobxp3oe6 FOREIGN KEY (order_template_id) REFERENCES public.order_template(id);


--
-- Name: datatake_order_trigger fki1sp2ds74tjckv7t33e0q2dxm; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.datatake_order_trigger
    ADD CONSTRAINT fki1sp2ds74tjckv7t33e0q2dxm FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: order_template_dynamic_processing_parameters fki9usfidbqjjmcs72n688s3tun; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_dynamic_processing_parameters
    ADD CONSTRAINT fki9usfidbqjjmcs72n688s3tun FOREIGN KEY (order_template_id) REFERENCES public.order_template(id);


--
-- Name: order_template_class_output_parameters fkj209j91dt5u63pdflqdx8624b; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_class_output_parameters
    ADD CONSTRAINT fkj209j91dt5u63pdflqdx8624b FOREIGN KEY (order_template_id) REFERENCES public.order_template(id);


--
-- Name: order_template_input_filters fkj2uum6nr3wdst59jnpdf6c7ke; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_input_filters
    ADD CONSTRAINT fkj2uum6nr3wdst59jnpdf6c7ke FOREIGN KEY (input_filters_key) REFERENCES public.product_class(id);


--
-- Name: data_driven_order_trigger fkjso3jgk2w63ybvgr2bv1iuku4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.data_driven_order_trigger
    ADD CONSTRAINT fkjso3jgk2w63ybvgr2bv1iuku4 FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: order_template_input_product_classes fkk93r5fbhea054oc82ey9rga60; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_input_product_classes
    ADD CONSTRAINT fkk93r5fbhea054oc82ey9rga60 FOREIGN KEY (input_product_classes_id) REFERENCES public.product_class(id);


--
-- Name: order_template_output_parameters fklrn8qgbqajvnnfpdv9r0bmpie; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_output_parameters
    ADD CONSTRAINT fklrn8qgbqajvnnfpdv9r0bmpie FOREIGN KEY (order_template_id) REFERENCES public.order_template(id);


--
-- Name: data_driven_order_trigger_parameters_to_copy fkm2sxp3hgg8dx81hwie5rnvdbv; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.data_driven_order_trigger_parameters_to_copy
    ADD CONSTRAINT fkm2sxp3hgg8dx81hwie5rnvdbv FOREIGN KEY (data_driven_order_trigger_id) REFERENCES public.data_driven_order_trigger(id);


--
-- Name: order_template fkn47hw5w90ekqsi04fkditriql; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template
    ADD CONSTRAINT fkn47hw5w90ekqsi04fkditriql FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Name: order_template_requested_product_classes fknhfuvurmegj5crawhwpp1o6xq; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.order_template_requested_product_classes
    ADD CONSTRAINT fknhfuvurmegj5crawhwpp1o6xq FOREIGN KEY (requested_product_classes_id) REFERENCES public.product_class(id);


--
-- Name: orbit_order_trigger fkotqu2mutk4v3yo70yc9b5ewnd; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.orbit_order_trigger
    ADD CONSTRAINT fkotqu2mutk4v3yo70yc9b5ewnd FOREIGN KEY (last_orbit_id) REFERENCES public.orbit(id);


--
-- Name: time_interval_order_trigger fkp9j6qxomdhnmx56alh87wwpin; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.time_interval_order_trigger
    ADD CONSTRAINT fkp9j6qxomdhnmx56alh87wwpin FOREIGN KEY (order_template_id) REFERENCES public.order_template(id);


--
-- Name: data_driven_order_trigger fkswiqj72r22lhrj5excyugd9vf; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.data_driven_order_trigger
    ADD CONSTRAINT fkswiqj72r22lhrj5excyugd9vf FOREIGN KEY (order_template_id) REFERENCES public.order_template(id);



--
-- Additional attributes for processing orders and workflows
--

ALTER TABLE public.processing_order
    ADD COLUMN auto_close boolean DEFAULT false NOT NULL,
    ADD COLUMN auto_release boolean DEFAULT false NOT NULL,
    ADD COLUMN closing_time timestamp without time zone,
    ADD COLUMN input_data_timeout_period numeric(21,0),
    ADD COLUMN on_input_data_timeout_fail boolean DEFAULT true NOT NULL;

ALTER TABLE public.workflow
    ADD COLUMN priority integer;
