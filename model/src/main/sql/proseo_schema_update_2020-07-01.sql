--
-- prosEO Database Schema Update
--
-- Date: 2020-07-01
--

--
-- Name: processing_order; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.processing_order
    ADD COLUMN has_failed_job_steps boolean;

UPDATE public.processing_order SET has_failed_job_steps = false;


--
-- Name: job; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.job
    ADD COLUMN has_failed_job_steps boolean;

UPDATE public.job SET has_failed_job_steps = false;
