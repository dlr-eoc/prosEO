--
-- PRELIMINARY
--
-- prosEO Database Schema Update for prosEO 2.1.0
--
-- Date: 2026-04-20
--

--
-- PRELIMINARY: Unique constraint changed from name to nameId
--
ALTER TABLE ONLY public.mon_service
    DROP CONSTRAINT uk239unm9hg59upu3be0fkcu4rt,
    ADD CONSTRAINT uk239unm9hg59upu3be0fkcu4rt UNIQUE (nameId);

ALTER TABLE ONLY public.mon_ext_service
    DROP CONSTRAINT uk6iqkhmagcms83dnb123yfd0s2,
    ADD CONSTRAINT uk6iqkhmagcms83dnb123yfd0s2 UNIQUE (nameId);