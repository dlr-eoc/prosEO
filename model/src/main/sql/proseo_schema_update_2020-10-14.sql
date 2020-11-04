--
-- prosEO Database Schema Update (data only)
--
-- Date: 2020-10-14
--

INSERT INTO public.authorities
  SELECT DISTINCT username, 'ROLE_CLI_USER' FROM public.users
  WHERE username NOT LIKE '%esa%' AND username NOT LIKE '%prip' AND username NOT LIKE '%wrapper'
  ON CONFLICT DO NOTHING;

INSERT INTO public.authorities
  SELECT DISTINCT username, 'ROLE_GUI_USER' FROM public.users
  WHERE username NOT LIKE '%esa%' AND username NOT LIKE '%prip' AND username NOT LIKE '%wrapper'
  ON CONFLICT DO NOTHING;

INSERT INTO public.authorities
  SELECT DISTINCT username, 'ROLE_PRIP_USER' FROM public.users
  WHERE username NOT LIKE '%wrapper'
  ON CONFLICT DO NOTHING;

DELETE FROM public.authorities WHERE authority = 'ROLE_USER';