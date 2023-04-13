--
-- prosEO Database Schema Update for prosEO 0.9.5-ODIP (preliminary)
--
-- Date: 2023-03-27
--

--
-- Add order source to processing order
--
ALTER TABLE public.processing_order
  ADD COLUMN order_source character varying(255);
  

--
-- Add requested times to product
--
ALTER TABLE public.product
  ADD COLUMN requested_start_time timestamp(6) without time zone,
  ADD COLUMN requested_stop_time timestamp(6) without time zone;


--
-- Add scalar and list attributes for order generation to workflow
--
ALTER TABLE public.workflow
  ADD COLUMN enabled boolean,
  ADD COLUMN output_file_class character varying(255),
  ADD COLUMN processing_mode character varying(255),
  ADD COLUMN slice_duration bigint,
  ADD COLUMN slice_overlap bigint,
  ADD COLUMN slicing_type character varying(255);

ALTER TABLE public.workflow OWNER TO postgres;

CREATE TABLE public.workflow_class_output_parameters (
    workflow_id bigint NOT NULL,
    class_output_parameters_id bigint NOT NULL,
    class_output_parameters_key bigint NOT NULL
);

ALTER TABLE public.workflow_class_output_parameters OWNER TO postgres;

CREATE TABLE public.workflow_input_filters (
    workflow_id bigint NOT NULL,
    input_filters_id bigint NOT NULL,
    input_filters_key bigint NOT NULL
);

ALTER TABLE public.workflow_input_filters OWNER TO postgres;

CREATE TABLE public.workflow_output_parameters (
    workflow_id bigint NOT NULL,
    parameter_clob text,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    output_parameters_key character varying(255) NOT NULL
);

ALTER TABLE public.workflow_output_parameters OWNER TO postgres;

ALTER TABLE ONLY public.workflow_class_output_parameters
    ADD CONSTRAINT workflow_class_output_parameters_pkey PRIMARY KEY (workflow_id, class_output_parameters_key);

ALTER TABLE ONLY public.workflow_class_output_parameters
    ADD CONSTRAINT fk1gm53x3igc2k000lc5cy2sf1q FOREIGN KEY (workflow_id) REFERENCES public.workflow(id);

ALTER TABLE ONLY public.workflow_class_output_parameters
    ADD CONSTRAINT fk7p2bf4mui50rg5kg15k3a53g4 FOREIGN KEY (class_output_parameters_key) REFERENCES public.product_class(id);

ALTER TABLE ONLY public.workflow_class_output_parameters
    ADD CONSTRAINT fk9fpufpt1t7q9kxmie12mi4se7 FOREIGN KEY (class_output_parameters_id) REFERENCES public.class_output_parameter(id);

ALTER TABLE ONLY public.workflow_input_filters
    ADD CONSTRAINT workflow_input_filters_pkey PRIMARY KEY (workflow_id, input_filters_key);

ALTER TABLE ONLY public.workflow_input_filters
    ADD CONSTRAINT fk1cuklmlh4xk0s3dqekio7gy74 FOREIGN KEY (input_filters_key) REFERENCES public.product_class(id);
    
ALTER TABLE ONLY public.workflow_input_filters
    ADD CONSTRAINT fkjdmteac11nvr5yvxrinn35rfb FOREIGN KEY (input_filters_id) REFERENCES public.input_filter(id);

ALTER TABLE ONLY public.workflow_input_filters
    ADD CONSTRAINT fkqur70i7n8cka6j0jjo8exje2x FOREIGN KEY (workflow_id) REFERENCES public.workflow(id);

ALTER TABLE ONLY public.workflow_output_parameters
    ADD CONSTRAINT workflow_output_parameters_pkey PRIMARY KEY (workflow_id, output_parameters_key);

ALTER TABLE ONLY public.workflow_output_parameters
    ADD CONSTRAINT fk6jtuu5c7q9lcwfl8dp07jlywi FOREIGN KEY (workflow_id) REFERENCES public.workflow(id);

    

--
-- Add tables for product archives
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

CREATE TABLE public.product_archive_available_product_classes (
    product_archive_id bigint NOT NULL,
    available_product_classes_id bigint NOT NULL
);

ALTER TABLE public.product_archive_available_product_classes OWNER TO postgres;

ALTER TABLE ONLY public.product_archive
    ADD CONSTRAINT product_archive_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.product_archive
    ADD CONSTRAINT uk19j4q7qi3o7ln0yucf43cfbps UNIQUE (code);
    
ALTER TABLE ONLY public.product_archive_available_product_classes
    ADD CONSTRAINT product_archive_available_product_classes_pkey PRIMARY KEY (product_archive_id, available_product_classes_id);

ALTER TABLE ONLY public.product_archive_available_product_classes
    ADD CONSTRAINT fke8v1poev1p25mcv38i8ueg5dl FOREIGN KEY (product_archive_id) REFERENCES public.product_archive(id);

ALTER TABLE ONLY public.product_archive_available_product_classes
    ADD CONSTRAINT fksmgbtsw0scm0bdg6bd49do29r FOREIGN KEY (available_product_classes_id) REFERENCES public.product_class(id);


