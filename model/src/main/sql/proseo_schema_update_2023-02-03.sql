--
-- prosEO Database Schema Update for prosEO 0.9.4
--
-- Date: 2023-02-03
--

--
-- Attribute applicableConfiguredProcessors in SimpleSelectionRule renamed to configuredProcessors
--

ALTER TABLE ONLY public.simple_selection_rule_applicable_configured_processors
    DROP CONSTRAINT simple_selection_rule_applicable_configured_processors_pkey;

ALTER TABLE ONLY public.simple_selection_rule_applicable_configured_processors
    DROP CONSTRAINT fk2fc10i4kvwdl75twbs0f1jnae;

ALTER TABLE ONLY public.simple_selection_rule_applicable_configured_processors
    DROP CONSTRAINT fkb9av1i977yys0ql1habohq1o7;
    
ALTER TABLE simple_selection_rule_applicable_configured_processors 
    RENAME TO simple_selection_rule_configured_processors;

ALTER TABLE simple_selection_rule_configured_processors 
    RENAME applicable_configured_processors_id TO configured_processors_id;

ALTER TABLE ONLY public.simple_selection_rule_configured_processors
    ADD CONSTRAINT simple_selection_rule_configured_processors_pkey PRIMARY KEY (simple_selection_rule_id, configured_processors_id);
    
ALTER TABLE ONLY public.simple_selection_rule_configured_processors
    ADD CONSTRAINT fk68ygyev0w1vb1jvnah0ry6vg9 FOREIGN KEY (simple_selection_rule_id) REFERENCES public.simple_selection_rule(id);

ALTER TABLE ONLY public.simple_selection_rule_configured_processors
    ADD CONSTRAINT fk8p1jxkynyy47c9slyxbjp18iu FOREIGN KEY (configured_processors_id) REFERENCES public.configured_processor(id);

    
--
-- New indices on product table to improve PRIP performance
--

CREATE INDEX idx2uot336txpqpdo8je8x145a0 ON public.product USING btree (publication_time);

CREATE INDEX idxb1hlhb6srtxd7qpjtkm8a37jg ON public.product USING btree (enclosing_product_id);

