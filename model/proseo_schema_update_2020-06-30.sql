--
-- prosEO Database Schema Update
--
-- Date: 2020-06-30
--

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
-- Name: parameterized_output; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.parameterized_output (
    id bigint NOT NULL,
    version integer NOT NULL
);


ALTER TABLE public.parameterized_output OWNER TO postgres;

--
-- Name: parameterized_output_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.parameterized_output_output_parameters (
    parameterized_output_id bigint NOT NULL,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    output_parameters_key character varying(255) NOT NULL
);


ALTER TABLE public.parameterized_output_output_parameters OWNER TO postgres;


--
-- Name: processing_order; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.processing_order
    ADD COLUMN production_type character varying(255);

UPDATE public.processing_order SET production_type = 'ON_DEMAND_DEFAULT';


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
-- Name: processing_order_parameterized_outputs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order_parameterized_outputs (
    processing_order_id bigint NOT NULL,
    parameterized_outputs_id bigint NOT NULL,
    parameterized_outputs_key bigint NOT NULL
);


ALTER TABLE public.processing_order_parameterized_outputs OWNER TO postgres;


--
-- Copy data from old tables into new ones as best possible (using a DUMMY product class for input filter conditions)
--
DO $$
DECLARE
    current_mission_id bigint;
    dummy_product_class_id bigint;
    input_filter_id bigint;
    parameterized_output_id bigint;
    current_order_id bigint;
    filter_condition_count integer;
    current_product_class RECORD;
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
        
        -- Copy old output parameters to parameterized output for each old requested product class
        FOR current_product_class IN SELECT processing_order_id, requested_product_classes_id
                FROM processing_order_requested_product_classes
                WHERE processing_order_id IN (SELECT id FROM processing_order WHERE mission_id = current_mission_id)
        LOOP
            parameterized_output_id = nextval('hibernate_sequence');
            INSERT INTO public.processing_order_parameterized_outputs VALUES (
                current_product_class.processing_order_id,
                parameterized_output_id,
                current_product_class.requested_product_classes_id
            );
            INSERT INTO public.parameterized_output VALUES (parameterized_output_id, 1);
            INSERT INTO public.parameterized_output_output_parameters
                SELECT parameterized_output_id, parameter_type, parameter_value, output_parameters_key
                    FROM public.processing_order_output_parameters
                    WHERE processing_order_id = current_product_class.processing_order_id;
        END LOOP;
    END LOOP;
END
$$;

--
-- Name: processing_order_filter_conditions; Type: TABLE; Schema: public; Owner: postgres
--

DROP TABLE public.processing_order_filter_conditions;


--
-- Name: processing_order_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

DROP TABLE public.processing_order_output_parameters;


--
-- Name: processing_order_requested_product_classes; Type: TABLE; Schema: public; Owner: postgres
--

DROP TABLE public.processing_order_requested_product_classes;


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
-- Name: parameterized_output_output_parameters parameterized_output_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parameterized_output_output_parameters
    ADD CONSTRAINT parameterized_output_output_parameters_pkey PRIMARY KEY (parameterized_output_id, output_parameters_key);


--
-- Name: parameterized_output parameterized_output_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parameterized_output
    ADD CONSTRAINT parameterized_output_pkey PRIMARY KEY (id);


--
-- Name: processing_order_input_filters processing_order_input_filters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_filters
    ADD CONSTRAINT processing_order_input_filters_pkey PRIMARY KEY (processing_order_id, input_filters_key);


--
-- Name: processing_order_parameterized_outputs processing_order_parameterized_outputs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_parameterized_outputs
    ADD CONSTRAINT processing_order_parameterized_outputs_pkey PRIMARY KEY (processing_order_id, parameterized_outputs_key);


--
-- Name: processing_order_input_filters fk1u9dj81sg3vcueaprup3hasqi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_input_filters
    ADD CONSTRAINT fk1u9dj81sg3vcueaprup3hasqi FOREIGN KEY (input_filters_id) REFERENCES public.input_filter(id);


--
-- Name: parameterized_output_output_parameters fk8ui9alofyquuvso69amgcohy2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.parameterized_output_output_parameters
    ADD CONSTRAINT fk8ui9alofyquuvso69amgcohy2 FOREIGN KEY (parameterized_output_id) REFERENCES public.parameterized_output(id);


--
-- Name: processing_order_parameterized_outputs fkckmc7llemva6id4drv0557957; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_parameterized_outputs
    ADD CONSTRAINT fkckmc7llemva6id4drv0557957 FOREIGN KEY (processing_order_id) REFERENCES public.processing_order(id);


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
-- Name: input_filter_filter_conditions fkjqbbl8slm6j7oco6vfg88duq2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.input_filter_filter_conditions
    ADD CONSTRAINT fkjqbbl8slm6j7oco6vfg88duq2 FOREIGN KEY (input_filter_id) REFERENCES public.input_filter(id);


--
-- Name: processing_order_parameterized_outputs fkn7ht4uc5k0cxaj6u5bo5wideh; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_parameterized_outputs
    ADD CONSTRAINT fkn7ht4uc5k0cxaj6u5bo5wideh FOREIGN KEY (parameterized_outputs_key) REFERENCES public.product_class(id);


--
-- Name: processing_order_parameterized_outputs fkq7qvx4ad5y95nsd20hu57veld; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order_parameterized_outputs
    ADD CONSTRAINT fkq7qvx4ad5y95nsd20hu57veld FOREIGN KEY (parameterized_outputs_id) REFERENCES public.parameterized_output(id);


