--
-- prosEO Database Schema Update
--
-- Date: 2020-12-09
--

--
-- Name: configuration; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.configuration
    ADD COLUMN mode character varying(255);
    

--
-- Name: processing_facility; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.processing_facility
    DROP COLUMN processing_engine_user,
    DROP COLUMN processing_engine_password,
    ADD COLUMN facility_state character varying(255),
    ADD COLUMN processing_engine_token text;
    
UPDATE public.processing_facility SET facility_state = "RUNNING";


--
-- Name: product_class; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.product_class
    ADD COLUMN processing_level character varying(255);

