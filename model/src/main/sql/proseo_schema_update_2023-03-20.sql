--
-- prosEO Database Schema Update for prosEO 0.9.5-ODIP (preliminary)
--
-- Date: 2023-03-20
--

--
-- Add priority for job steps
--
ALTER TABLE public.job_step
  ADD COLUMN priority integer;
  

--
-- Add ODIP-relevant columns to processing_order
--
ALTER TABLE public.processing_order
  ADD COLUMN actual_completion_time timestamp without time zone,
  ADD COLUMN estimated_completion_time timestamp without time zone,
  ADD COLUMN input_file_name character varying(255),
  ADD COLUMN input_sensing_start_time timestamp(6) without time zone,
  ADD COLUMN input_sensing_stop_time timestamp(6) without time zone,
  ADD COLUMN endpoint_password character varying(255),
  ADD COLUMN endpoint_uri character varying(255),
  ADD COLUMN endpoint_username character varying(255),
  ADD COLUMN priority integer,
  ADD COLUMN release_time timestamp without time zone,
  ADD COLUMN state_message character varying(255),
  ADD COLUMN submission_time timestamp without time zone,
  ADD COLUMN workflow_id bigint;
  
  
--
-- Add dynamic processing parameters to processing_order
--
CREATE TABLE public.processing_order_dynamic_processing_parameters (
    processing_order_id bigint NOT NULL,
    parameter_clob text,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    dynamic_processing_parameters_key character varying(255) NOT NULL
);


ALTER TABLE public.processing_order_dynamic_processing_parameters OWNER TO postgres;

ALTER TABLE ONLY public.processing_order_dynamic_processing_parameters
    ADD CONSTRAINT processing_order_dynamic_processing_parameters_pkey PRIMARY KEY (processing_order_id, dynamic_processing_parameters_key);

ALTER TABLE ONLY public.processing_order_dynamic_processing_parameters
    ADD CONSTRAINT fk64135mif3wav3bqm7g4hh5ko7 FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


--
-- Add workflows
--
CREATE TABLE public.workflow (
    id bigint NOT NULL,
    version integer NOT NULL,
    description character varying(255),
    name character varying(255) NOT NULL,
    uuid uuid NOT NULL,
    workflow_version character varying(255),
    configured_processor_id bigint,
    input_product_class_id bigint,
    output_product_class_id bigint
);

ALTER TABLE public.workflow OWNER TO postgres;

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT uk3je18ux0wru0pxv6un40yhbn4 UNIQUE (name);

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT ukhskpb9cv06lohyj8m2ekp9xio UNIQUE (uuid);

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT workflow_pkey PRIMARY KEY (id);


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

ALTER TABLE ONLY public.workflow_option
    ADD CONSTRAINT ukt0udoa7mo0nk3swm1ff30gh5 UNIQUE (workflow_id, name);

ALTER TABLE ONLY public.workflow_option
    ADD CONSTRAINT workflow_option_pkey PRIMARY KEY (id);


CREATE TABLE public.workflow_option_value_range (
    workflow_option_id bigint NOT NULL,
    value_range character varying(255)
);

ALTER TABLE public.workflow_option_value_range OWNER TO postgres;


--
-- Add foreign key relationships for workflow-related tables
--
ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT fkl0agecusohc3yft3vq7xlaikr FOREIGN KEY (input_product_class_id) REFERENCES public.product_class(id);

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT fkmwefyimusp3lp3qlj7tsdgmm6 FOREIGN KEY (output_product_class_id) REFERENCES public.product_class(id);

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT fkn0aaw3ptxvvfwb1tbuoowki42 FOREIGN KEY (configured_processor_id) REFERENCES public.configured_processor(id);

ALTER TABLE ONLY public.processing_order
    ADD CONSTRAINT fkt2f7nkjj7muumygco1sj81hn1 FOREIGN KEY (workflow_id) REFERENCES public.workflow(id);

ALTER TABLE ONLY public.workflow_option
    ADD CONSTRAINT fkl2fwnrxddum783emw1a3qwuot FOREIGN KEY (workflow_id) REFERENCES public.workflow(id);

ALTER TABLE ONLY public.workflow_option_value_range
    ADD CONSTRAINT fky9hi8gn5n5pdglqyl1qyowvn FOREIGN KEY (workflow_option_id) REFERENCES public.workflow_option(id);


