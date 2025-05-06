--
-- prosEO Database Schema Update for prosEO 0.9.5
--
-- Date: 2023-09-19
--

--
-- Add download indicator to product query
--
ALTER TABLE public.product_query
  ADD COLUMN in_download boolean;

UPDATE public.product_query SET in_download = false;



--
-- Add table for class ApiMetrics
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

ALTER TABLE ONLY public.api_metrics
    ADD CONSTRAINT api_metrics_pkey PRIMARY KEY (id);

CREATE INDEX idx2qkgbww8yosr941oqytoo823x ON public.api_metrics USING btree (name);

CREATE INDEX idxcquj0irj1wkt95t0krnmwrkj2 ON public.api_metrics USING btree ("timestamp");


--
-- Change "text" attributes to "oid"
--
ALTER TABLE public.class_output_parameter_output_parameters
  ALTER parameter_clob oid;

ALTER TABLE public.configuration_dyn_proc_parameters
  ALTER parameter_clob oid;

ALTER TABLE public.input_filter_filter_conditions
  ALTER parameter_clob oid;

ALTER TABLE public.job_step
  ALTER processing_std_err oid,
  ALTER processing_std_out oid;

ALTER TABLE public.job_step_output_parameters
  ALTER parameter_clob oid;

ALTER TABLE public.mission
  ALTER product_file_template oid;

ALTER TABLE public.processing_facility
  ALTER processing_engine_token oid;

ALTER TABLE public.processing_order_dynamic_processing_parameters
  ALTER parameter_clob oid;

ALTER TABLE public.processing_order_output_parameters
  ALTER parameter_clob oid;

ALTER TABLE public.product_class
  ALTER product_file_template oid;

ALTER TABLE public.product_parameters
  ALTER parameter_clob oid;

ALTER TABLE public.product_query_filter_conditions
  ALTER parameter_clob oid;

ALTER TABLE public.simple_selection_rule_filter_conditions
  ALTER parameter_clob oid;

ALTER TABLE public.workflow_output_parameters
  ALTER parameter_clob oid;


--
-- Make UUID mandatory for Configured Processors
--
ALTER TABLE public.configured_processor
  ALTER uuid uuid NOT NULL;


--
-- Rename uniqueness constraint on group names
--
ALTER TABLE ONLY public.groups
    DROP CONSTRAINT IF EXISTS uk_7o859iyhxd19rv4hywgdvu2v4;
ALTER TABLE ONLY public.groups
    ADD CONSTRAINT uka1130rbom0hjmf18xtppoc3eq UNIQUE (group_name);
