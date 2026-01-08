--
-- prosEO Database Schema Update for prosEO 1.2.0
--
-- Date: 2025-08-07
--

--
-- Add table for class ProcessingOrderHistory
--
CREATE TABLE public.processing_order_history (
    id bigint NOT NULL,
    version integer NOT NULL,
    completion_time timestamp without time zone,
    creation_time timestamp without time zone,
    deletion_time timestamp without time zone,
    identifier character varying(255) NOT NULL,
    mission_code character varying(255),
    order_state character varying(255),
    release_time timestamp without time zone
);


ALTER TABLE public.processing_order_history OWNER TO postgres;

--
-- Name: processing_order_history_product_types; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.processing_order_history_product_types (
    processing_order_history_id bigint NOT NULL,
    product_types character varying(255)
);


ALTER TABLE public.processing_order_history_product_types OWNER TO postgres;

ALTER TABLE ONLY public.processing_order_history
    ADD CONSTRAINT processing_order_history_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.processing_order_history
    ADD CONSTRAINT ukbokmof3ehpoi3hsc1mdhqgvar UNIQUE (mission_code, identifier);

ALTER TABLE ONLY public.processing_order_history_product_types
    ADD CONSTRAINT fkbwtjp244fa6xcsmac1pdisy9k FOREIGN KEY (processing_order_history_id) REFERENCES public.processing_order_history(id);

