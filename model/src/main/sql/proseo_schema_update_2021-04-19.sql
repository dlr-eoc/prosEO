--
-- prosEO Database Schema Update
--
-- Date: 2021-04-19
--

--
-- Name: processing_order ukbdceutrqoge4t26w7bixj9afg; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order
    DROP CONSTRAINT ukbdceutrqoge4t26w7bixj9afg;

--
-- Name: processing_order ukbxwgyibx5dkbl26jplnjifrsa; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.processing_order
    ADD CONSTRAINT ukbxwgyibx5dkbl26jplnjifrsa UNIQUE (mission_id, identifier);

    
--
-- Name: spacecraft ukt95fh7lk9yu00lk7wbcfkagde; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.spacecraft
    DROP CONSTRAINT ukt95fh7lk9yu00lk7wbcfkagde;

--
-- Name: spacecraft uklogt34j6cnrocn49sw0uu30eh; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.spacecraft
    ADD CONSTRAINT uklogt34j6cnrocn49sw0uu30eh UNIQUE (mission_id, code);

