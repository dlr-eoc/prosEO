--
-- prosEO Database Schema Update
--
-- Date: 2020-06-30
--

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
-- Name: configured_processor; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.configured_processor
    ADD COLUMN enabled boolean;

UPDATE public.configured_processor SET enabled = true;


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
-- Name: job_filter_conditions; Type: TABLE; Schema: public; Owner: postgres
--

DROP TABLE public.job_filter_conditions;


--
-- Name: job_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

DROP TABLE public.job_output_parameters;


--
-- Name: job_step; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.job_step
    ADD COLUMN job_order_filename character varying(255);
    

--
-- Name: processing_order; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.processing_order
    ADD COLUMN production_type character varying(255);

UPDATE public.processing_order SET production_type = 'ON_DEMAND_DEFAULT';


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
-- Copy data from old tables into new ones as best possible (using a DUMMY product class for input filter conditions)
--
DO $$
DECLARE
    current_mission_id bigint;
    dummy_product_class_id bigint;
    input_filter_id bigint;
    current_order_id bigint;
    filter_condition_count integer;
BEGIN
	FOR current_mission_id IN SELECT id FROM mission LOOP
        -- Create a dummy product class for each mission for input filters
        dummy_product_class_id := nextval('hibernate_sequence');
        INSERT INTO public.product_class (id, version, description, product_type, visibility, mission_id) VALUES (
            dummy_product_class_id,
            1,
            'Dummy Product Class',
            'DUMMY',
            'INTERNAL',
            current_mission_id
        );
        
        -- Copy old filter conditions to input filters for dummy product class
        FOR current_order_id, filter_condition_count IN SELECT id, count(*)
                FROM processing_order po
                JOIN processing_order_filter_conditions fc ON po.id = fc.processing_order_id
                WHERE mission_id = current_mission_id
                GROUP BY id
                HAVING count(*) > 0
        LOOP
            input_filter_id := nextval('hibernate_sequence');
            INSERT INTO public.processing_order_input_filters VALUES (
                current_order_id,
                input_filter_id,
                dummy_product_class_id
            );
            INSERT INTO public.input_filter VALUES (input_filter_id, 1);
            INSERT INTO public.input_filter_filter_conditions
                SELECT input_filter_id, parameter_type, parameter_value, filter_conditions_key
                    FROM processing_order_filter_conditions WHERE processing_order_id = current_order_id;
        END LOOP;
        
    END LOOP;
END
$$;

--
-- Name: processing_order_filter_conditions; Type: TABLE; Schema: public; Owner: postgres
--

DROP TABLE public.processing_order_filter_conditions;


--
-- Name: product; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.product
    ADD COLUMN production_type character varying(255);
    
UPDATE public.product SET production_type = 'ON_DEMAND_DEFAULT';


--
-- Name: product_class; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.product_class
    ADD COLUMN product_file_template character varying(255);


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
-- Name: processing_order_input_filters fk1u9dj81sg3vcueaprup3hasqi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_filters
    ADD CONSTRAINT fk1u9dj81sg3vcueaprup3hasqi FOREIGN KEY (input_filters_id) REFERENCES public.input_filter(id);


--
-- Name: processing_order_input_filters fkdhgcujq2nix39y2b7nbdpnlto; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_filters
    ADD CONSTRAINT fkdhgcujq2nix39y2b7nbdpnlto FOREIGN KEY (input_filters_key) REFERENCES public.product_class(id);


--
-- Name: processing_order_input_filters fkgbh8k5vigdykb0s1cwhag6br5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_filters
    ADD CONSTRAINT fkgbh8k5vigdykb0s1cwhag6br5 FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


--
-- Name: processing_order_class_output_parameters fk7m4kynfbpfam8fvs66fpk6elk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_class_output_parameters
    ADD CONSTRAINT fk7m4kynfbpfam8fvs66fpk6elk FOREIGN KEY (class_output_parameters_id) REFERENCES public.class_output_parameter(id);


--
-- Name: input_filter_filter_conditions fkjqbbl8slm6j7oco6vfg88duq2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.input_filter_filter_conditions
    ADD CONSTRAINT fkjqbbl8slm6j7oco6vfg88duq2 FOREIGN KEY (input_filter_id) REFERENCES public.input_filter(id);


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
-- Name: processing_order_class_output_parameters fklxehk5y7wbwpi3gxj00eg3p89; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_class_output_parameters
    ADD CONSTRAINT fklxehk5y7wbwpi3gxj00eg3p89 FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


