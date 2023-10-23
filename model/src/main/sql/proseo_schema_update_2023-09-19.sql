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
-- Add mission relationship to workflow
--
UPDATE public.workflow SET workflow_version = '1.0' WHERE workflow_version IS NULL;

ALTER TABLE public.workflow
  ADD COLUMN mission_id bigint,
  ALTER COLUMN workflow_version SET NOT NULL;

ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT fksbdoolsgnxt5ji4bfnmpe14y8 FOREIGN KEY (mission_id) REFERENCES public.mission(id);


--
-- Modify workflow indices
--
ALTER TABLE ONLY public.workflow
    DROP CONSTRAINT uk3je18ux0wru0pxv6un40yhbn4;
    
ALTER TABLE ONLY public.workflow
    ADD CONSTRAINT ukg9qjvpphc4d2n8y10mpael4cd UNIQUE (mission_id, name, workflow_version);

