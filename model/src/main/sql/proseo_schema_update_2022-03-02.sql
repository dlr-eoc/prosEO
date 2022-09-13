--
-- prosEO Database Schema Update
--
-- Date: 2022-03-02
--

--
-- Update Job states (previous state INITIAL now corresponds to PLANNED)
--

UPDATE public.job
    SET job_state = 'PLANNED'
    WHERE job_state = 'INITIAL';

    
--
-- Update JobStep states (previous state INITIAL now corresponds to PLANNED)
--
   
UPDATE public.job_step
    SET job_step_state = 'PLANNED'
    WHERE job_step_state = 'INITIAL';
