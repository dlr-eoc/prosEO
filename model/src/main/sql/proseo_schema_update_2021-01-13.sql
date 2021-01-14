--
-- prosEO Database Schema Update
--
-- Date: 2021-01-13
--

--
-- Name: processor; Type: TABLE; Schema: public; Owner: postgres
--

ALTER TABLE public.processor
    ADD COLUMN job_order_version character varying(255),
    ADD COLUMN use_input_file_time_intervals boolean;
    
UPDATE public.processor SET job_order_version = 'MMFI_1_8', use_input_file_time_intervals = false;
