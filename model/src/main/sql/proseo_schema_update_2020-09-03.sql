--
-- prosEO Database Schema Update
--
-- Date: 2020-09-03
--

--
-- Name: product_query_satisfying_products; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.product_query_satisfying_products (
    satisfied_product_queries_id bigint NOT NULL,
    satisfying_products_id bigint NOT NULL
);


ALTER TABLE public.product_query_satisfying_products OWNER TO postgres;

--
-- Name: product_query_satisfying_products product_query_satisfying_products_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--


ALTER TABLE ONLY public.product_query_satisfying_products
    ADD CONSTRAINT product_query_satisfying_products_pkey PRIMARY KEY (satisfied_product_queries_id, satisfying_products_id);

    
--
-- Name: product_query_satisfying_products fkj1us8b41hn4xc8ug12c530ei1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_query_satisfying_products
    ADD CONSTRAINT fkj1us8b41hn4xc8ug12c530ei1 FOREIGN KEY (satisfied_product_queries_id) REFERENCES public.product_query(id);


--
-- Name: product_query_satisfying_products fkq768nqgupajiccjpbyawcqhtd; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_query_satisfying_products
    ADD CONSTRAINT fkq768nqgupajiccjpbyawcqhtd FOREIGN KEY (satisfying_products_id) REFERENCES public.product(id);


--
-- Copy data from old table to new table
--

INSERT INTO public.product_query_satisfying_products
SELECT satisfied_product_queries_id, satisfying_products_id FROM public.product_satisfied_product_queries;


--
-- Name: product_satisfied_product_queries product_satisfied_product_queries_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_satisfied_product_queries
    DROP CONSTRAINT product_satisfied_product_queries_pkey;


--
-- Name: product_satisfied_product_queries fkk5rlrwoo04huxotyum6efdw4w; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_satisfied_product_queries
    DROP CONSTRAINT fkk5rlrwoo04huxotyum6efdw4w;


--
-- Name: product_satisfied_product_queries fknekgyp50knufdn0h716tuumbr; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.product_satisfied_product_queries
    DROP CONSTRAINT fknekgyp50knufdn0h716tuumbr;


--
-- Name: product_satisfied_product_queries; Type: TABLE; Schema: public; Owner: postgres
--

DROP TABLE public.product_satisfied_product_queries;

