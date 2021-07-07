--
-- prosEO Database Schema Update
--
-- Date: 2021-07-07
--

--
-- Name: mission; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.mission
    ADD COLUMN processing_centre character varying(255),
    ADD COLUMN product_retention_period bigint;

    
--
-- Name: processing_facility; Type: TABLE; Schema: public; Owner: postgres
--
   
ALTER TABLE public.processing_facility
    ADD COLUMN max_jobs_per_node integer;
    
UPDATE public.processing_facility SET max_jobs_per_node = 1;


--
-- Name: processing_order; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.processing_order
    ADD COLUMN product_retention_period bigint;
    
    
--
-- Name: product; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.product
    ADD COLUMN eviction_time timestamp(6) without time zone,
    ADD COLUMN publication_time timestamp(6) without time zone,
    ADD COLUMN raw_data_availability_time timestamp(6) without time zone;


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

ALTER TABLE public.task
    ADD COLUMN min_memory integer;


--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.users
    ADD COLUMN assigned integer,
    ADD COLUMN last_access_date timestamp without time zone,
    ADD COLUMN used integer;


--
-- Name: spacecraft_payloads fk5pbclfmfjdlc2xc6m3k96x6j9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.spacecraft_payloads
    ADD CONSTRAINT fk5pbclfmfjdlc2xc6m3k96x6j9 FOREIGN KEY (spacecraft_id) REFERENCES public.spacecraft(id);


--
-- Name: idxqu0ou5l3tyyegjvfh0rvb8f4h; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxqu0ou5l3tyyegjvfh0rvb8f4h ON public.product USING btree (eviction_time);


