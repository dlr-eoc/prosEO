--
-- PostgreSQL database dump
--

-- Dumped from database version 11.16 (Debian 11.16-1.pgdg90+1)
-- Dumped by pg_dump version 11.22

ALTER TABLE IF EXISTS  workflow_option_value_range DROP CONSTRAINT IF EXISTS fky9hi8gn5n5pdglqyl1qyowvn;
ALTER TABLE IF EXISTS  users_group_memberships DROP CONSTRAINT IF EXISTS fktodlfclgikl9ionfovl0t7wp0;
ALTER TABLE IF EXISTS  processing_order DROP CONSTRAINT IF EXISTS fkt2f7nkjj7muumygco1sj81hn1;
ALTER TABLE IF EXISTS  spacecraft DROP CONSTRAINT IF EXISTS fksp2jjwkpaehybfu5pwedol1c;
ALTER TABLE IF EXISTS  product_archive_available_product_classes DROP CONSTRAINT IF EXISTS fksmgbtsw0scm0bdg6bd49do29r;
ALTER TABLE IF EXISTS  mon_product_production_month DROP CONSTRAINT IF EXISTS fkshuxduj4xdpxqoty4f6desfun;
ALTER TABLE IF EXISTS  workflow DROP CONSTRAINT IF EXISTS fksbdoolsgnxt5ji4bfnmpe14y8;
ALTER TABLE IF EXISTS  mission_file_classes DROP CONSTRAINT IF EXISTS fks4suek1246jge02gcgpnoms5y;
ALTER TABLE IF EXISTS  workflow_input_filters DROP CONSTRAINT IF EXISTS fkqur70i7n8cka6j0jjo8exje2x;
ALTER TABLE IF EXISTS  product_file DROP CONSTRAINT IF EXISTS fkqs127y6vnoylxgo8aroqx4e8f;
ALTER TABLE IF EXISTS  mission_processing_modes DROP CONSTRAINT IF EXISTS fkqhg2duxhcpldh28nyh7nwnfcn;
ALTER TABLE IF EXISTS  mon_service_state_operation DROP CONSTRAINT IF EXISTS fkq96wp2nrf7tvih0otaf0ojtyg;
ALTER TABLE IF EXISTS  product_query_satisfying_products DROP CONSTRAINT IF EXISTS fkq768nqgupajiccjpbyawcqhtd;
ALTER TABLE IF EXISTS  job DROP CONSTRAINT IF EXISTS fko7lm1bpn9pqf1qq9o5fpfjtic;
ALTER TABLE IF EXISTS  processor DROP CONSTRAINT IF EXISTS fko4ocncq22u0j2prxw2dbk0dka;
ALTER TABLE IF EXISTS  workflow DROP CONSTRAINT IF EXISTS fkn0aaw3ptxvvfwb1tbuoowki42;
ALTER TABLE IF EXISTS  workflow DROP CONSTRAINT IF EXISTS fkmwefyimusp3lp3qlj7tsdgmm6;
ALTER TABLE IF EXISTS  product_download_history DROP CONSTRAINT IF EXISTS fkm3o1ca4ms7b4ereu9wvsxpeet;
ALTER TABLE IF EXISTS  processor_class DROP CONSTRAINT IF EXISTS fklxfogyfhmujn40qg0ooxfdwfv;
ALTER TABLE IF EXISTS  processing_order_class_output_parameters DROP CONSTRAINT IF EXISTS fklxehk5y7wbwpi3gxj00eg3p89;
ALTER TABLE IF EXISTS  job_step_output_parameters DROP CONSTRAINT IF EXISTS fklw2fh8ksho7gcvrfykeoep899;
ALTER TABLE IF EXISTS  configured_processor DROP CONSTRAINT IF EXISTS fkloteyhnalc56x161f4inujyt5;
ALTER TABLE IF EXISTS  processing_order_requested_orbits DROP CONSTRAINT IF EXISTS fkl52pcfs07440sihimxmpy6iva;
ALTER TABLE IF EXISTS  workflow_option DROP CONSTRAINT IF EXISTS fkl2fwnrxddum783emw1a3qwuot;
ALTER TABLE IF EXISTS  processing_order_input_product_classes DROP CONSTRAINT IF EXISTS fkl16rc9whni5al0b0t4ukf1dq6;
ALTER TABLE IF EXISTS  workflow DROP CONSTRAINT IF EXISTS fkl0agecusohc3yft3vq7xlaikr;
ALTER TABLE IF EXISTS  mon_ext_service_state_operation DROP CONSTRAINT IF EXISTS fkkrcxkfr1txuoeif89p2igbh6g;
ALTER TABLE IF EXISTS  mon_service_state_operation DROP CONSTRAINT IF EXISTS fkkp6fp3q2xd1x9ogghmy7pdht0;
ALTER TABLE IF EXISTS  product DROP CONSTRAINT IF EXISTS fkkobwm0e23qst5q2irk37fwmuy;
ALTER TABLE IF EXISTS  processing_order_requested_configured_processors DROP CONSTRAINT IF EXISTS fkkkhi2aj21ehrsok4ekhl1fd31;
ALTER TABLE IF EXISTS  processing_order_mon_order_progress DROP CONSTRAINT IF EXISTS fkkiu48p0ndrxpd71y09yn2q474;
ALTER TABLE IF EXISTS  product DROP CONSTRAINT IF EXISTS fkk9wbgbn6a2xyr7f7vl65uogis;
ALTER TABLE IF EXISTS  processing_order_mon_order_progress DROP CONSTRAINT IF EXISTS fkjwvq04w9s9sfhftuebjq7rop;
ALTER TABLE IF EXISTS  input_filter_filter_conditions DROP CONSTRAINT IF EXISTS fkjqbbl8slm6j7oco6vfg88duq2;
ALTER TABLE IF EXISTS  configuration_dyn_proc_parameters DROP CONSTRAINT IF EXISTS fkjpifxw2lvac6ipxqdimmy73k4;
ALTER TABLE IF EXISTS  simple_selection_rule DROP CONSTRAINT IF EXISTS fkje8biclfyorg1wm8uh1qf9d0;
ALTER TABLE IF EXISTS  workflow_input_filters DROP CONSTRAINT IF EXISTS fkjdmteac11nvr5yvxrinn35rfb;
ALTER TABLE IF EXISTS  product_query_satisfying_products DROP CONSTRAINT IF EXISTS fkj1us8b41hn4xc8ug12c530ei1;
ALTER TABLE IF EXISTS  processing_order_requested_product_classes DROP CONSTRAINT IF EXISTS fkj0e73npk4ljcr6lupi978clea;
ALTER TABLE IF EXISTS  mon_ext_service_state_operation DROP CONSTRAINT IF EXISTS fkiqb5ahpdcute0oip6q9wovayp;
ALTER TABLE IF EXISTS  product_class DROP CONSTRAINT IF EXISTS fkinocsatitcf1ofpp4wc7psua2;
ALTER TABLE IF EXISTS  simple_selection_rule_filter_conditions DROP CONSTRAINT IF EXISTS fki9hebpru8hilywjux8v76p2em;
ALTER TABLE IF EXISTS  configuration_input_file_file_names DROP CONSTRAINT IF EXISTS fki81ysbbwtpwxlhcm82eksdq1g;
ALTER TABLE IF EXISTS  orbit DROP CONSTRAINT IF EXISTS fki2gpip0vqngjwnvmguox9wi3f;
ALTER TABLE IF EXISTS  users_group_memberships DROP CONSTRAINT IF EXISTS fkhbcokg6kjsft20melhs8njcma;
ALTER TABLE IF EXISTS  processing_order_requested_orbits DROP CONSTRAINT IF EXISTS fkgruycyl8hgdsmac11yl37odi9;
ALTER TABLE IF EXISTS  processor_docker_run_parameters DROP CONSTRAINT IF EXISTS fkgqohmkxfbxo6ihxpgs84q5axp;
ALTER TABLE IF EXISTS  configuration_static_input_files DROP CONSTRAINT IF EXISTS fkgls3b4eoq74nhjslcn57reige;
ALTER TABLE IF EXISTS  processing_order DROP CONSTRAINT IF EXISTS fkgj4135cm664vfl5jt6v613y0e;
ALTER TABLE IF EXISTS  simple_selection_rule_simple_policies DROP CONSTRAINT IF EXISTS fkgijs10i27ucb2tosn56pqrqt6;
ALTER TABLE IF EXISTS  processing_order_requested_configured_processors DROP CONSTRAINT IF EXISTS fkgdnmmc4ri3f4w2d52e935d3jg;
ALTER TABLE IF EXISTS  processing_order_input_filters DROP CONSTRAINT IF EXISTS fkgbh8k5vigdykb0s1cwhag6br5;
ALTER TABLE IF EXISTS  configuration_configuration_files DROP CONSTRAINT IF EXISTS fkg6qj2gjs3td0wwcioda96uik5;
ALTER TABLE IF EXISTS  groups_group_members DROP CONSTRAINT IF EXISTS fkfjhm6ctnf3akprkg5ic279dyi;
ALTER TABLE IF EXISTS  simple_selection_rule_simple_policies DROP CONSTRAINT IF EXISTS fkfjb7qfppicnb1xj9vgi895dnb;
ALTER TABLE IF EXISTS  product_query DROP CONSTRAINT IF EXISTS fkfh82iydbxt4tvgiscy2qlj2m9;
ALTER TABLE IF EXISTS  processing_order_class_output_parameters DROP CONSTRAINT IF EXISTS fkf2c8fjwehnwek6aqehkdyig4r;
ALTER TABLE IF EXISTS  class_output_parameter_output_parameters DROP CONSTRAINT IF EXISTS fkeyipsy3fwc5pwmym1lypkrkh3;
ALTER TABLE IF EXISTS  simple_policy_delta_times DROP CONSTRAINT IF EXISTS fkerahx0bbgt0eeqanerq28kofp;
ALTER TABLE IF EXISTS  processing_order_input_product_classes DROP CONSTRAINT IF EXISTS fkei14w0cwbjj4d293kf0kccovn;
ALTER TABLE IF EXISTS  product_archive_available_product_classes DROP CONSTRAINT IF EXISTS fke8v1poev1p25mcv38i8ueg5dl;
ALTER TABLE IF EXISTS  product DROP CONSTRAINT IF EXISTS fke8busf8q6a8uvrh9a5od38tqo;
ALTER TABLE IF EXISTS  configured_processor DROP CONSTRAINT IF EXISTS fkdj5cx8yntdnuxvphpowp47of5;
ALTER TABLE IF EXISTS  processing_order_input_filters DROP CONSTRAINT IF EXISTS fkdhgcujq2nix39y2b7nbdpnlto;
ALTER TABLE IF EXISTS  mon_product_production_hour DROP CONSTRAINT IF EXISTS fkct5iw5b4h6q2y9oiy5acwph6s;
ALTER TABLE IF EXISTS  product_query DROP CONSTRAINT IF EXISTS fkc71ouv75rseha12h0fmlqt6a5;
ALTER TABLE IF EXISTS  configuration DROP CONSTRAINT IF EXISTS fkbvl2q3rfimgbvr8o6txnm5ea7;
ALTER TABLE IF EXISTS  task_breakpoint_file_names DROP CONSTRAINT IF EXISTS fkblitkg6msystnhjpjj5ya1tfh;
ALTER TABLE IF EXISTS  job_step DROP CONSTRAINT IF EXISTS fkbi6cqwlkj3nyqkvheqeg5qql0;
ALTER TABLE IF EXISTS  groups_group_members DROP CONSTRAINT IF EXISTS fkawl37vgnmf8ny5a9txq0q0mtq;
ALTER TABLE IF EXISTS  product_query_filter_conditions DROP CONSTRAINT IF EXISTS fkag48xcu5bmuq9yqls0d824bkj;
ALTER TABLE IF EXISTS  product_class DROP CONSTRAINT IF EXISTS fkafnqr7afqkr7vn6difh4r9e3j;
ALTER TABLE IF EXISTS  group_members DROP CONSTRAINT IF EXISTS fk_group_members_user;
ALTER TABLE IF EXISTS  group_members DROP CONSTRAINT IF EXISTS fk_group_members_group;
ALTER TABLE IF EXISTS  group_authorities DROP CONSTRAINT IF EXISTS fk_group_authorities_group;
ALTER TABLE IF EXISTS  authorities DROP CONSTRAINT IF EXISTS fk_authorities_users;
ALTER TABLE IF EXISTS  workflow_class_output_parameters DROP CONSTRAINT IF EXISTS fk9fpufpt1t7q9kxmie12mi4se7;
ALTER TABLE IF EXISTS  simple_selection_rule_configured_processors DROP CONSTRAINT IF EXISTS fk8p1jxkynyy47c9slyxbjp18iu;
ALTER TABLE IF EXISTS  simple_selection_rule DROP CONSTRAINT IF EXISTS fk8n3bq0ecxeti1ylukwkt7cnm;
ALTER TABLE IF EXISTS  job DROP CONSTRAINT IF EXISTS fk8jj66thbddwdxad89qxjeepxg;
ALTER TABLE IF EXISTS  product_parameters DROP CONSTRAINT IF EXISTS fk84to6rlvpri4i2pjqpvfn5jd8;
ALTER TABLE IF EXISTS  processing_order_output_parameters DROP CONSTRAINT IF EXISTS fk7udpjfeq21n6vsi6rxeycsoi9;
ALTER TABLE IF EXISTS  workflow_class_output_parameters DROP CONSTRAINT IF EXISTS fk7p2bf4mui50rg5kg15k3a53g4;
ALTER TABLE IF EXISTS  processing_order_class_output_parameters DROP CONSTRAINT IF EXISTS fk7m4kynfbpfam8fvs66fpk6elk;
ALTER TABLE IF EXISTS  processing_order_requested_product_classes DROP CONSTRAINT IF EXISTS fk7afxfgldbnrdi5joivn7agj86;
ALTER TABLE IF EXISTS  task DROP CONSTRAINT IF EXISTS fk6oktr0t8iad73hifdftqgwok9;
ALTER TABLE IF EXISTS  workflow_output_parameters DROP CONSTRAINT IF EXISTS fk6jtuu5c7q9lcwfl8dp07jlywi;
ALTER TABLE IF EXISTS  job DROP CONSTRAINT IF EXISTS fk6ek6xjklhsk2qduh5ejbcnb7c;
ALTER TABLE IF EXISTS  simple_selection_rule_configured_processors DROP CONSTRAINT IF EXISTS fk68ygyev0w1vb1jvnah0ry6vg9;
ALTER TABLE IF EXISTS  processing_order_dynamic_processing_parameters DROP CONSTRAINT IF EXISTS fk64135mif3wav3bqm7g4hh5ko7;
ALTER TABLE IF EXISTS  spacecraft_payloads DROP CONSTRAINT IF EXISTS fk5pbclfmfjdlc2xc6m3k96x6j9;
ALTER TABLE IF EXISTS  product DROP CONSTRAINT IF EXISTS fk5g7do1yby2n4monwfjw1q79kc;
ALTER TABLE IF EXISTS  product DROP CONSTRAINT IF EXISTS fk4oh5ogb84h479uxx9m2w2s1i3;
ALTER TABLE IF EXISTS  product_class DROP CONSTRAINT IF EXISTS fk4oc1a80q9jt8b0et2kl64j8av;
ALTER TABLE IF EXISTS  product_file DROP CONSTRAINT IF EXISTS fk4b360nnjmkd9r4w01jc7yer5h;
ALTER TABLE IF EXISTS  product_query DROP CONSTRAINT IF EXISTS fk2wed8wyw8vyboifjd64ytftp9;
ALTER TABLE IF EXISTS  product_file_aux_file_names DROP CONSTRAINT IF EXISTS fk24578k1macp0jxtdaiep0nku8;
ALTER TABLE IF EXISTS  configuration_static_input_files DROP CONSTRAINT IF EXISTS fk23vkvo6qmdg9xinr4drioro6w;
ALTER TABLE IF EXISTS  product_download_history DROP CONSTRAINT IF EXISTS fk23add5g47k6kbm3cgmw6hqqjh;
ALTER TABLE IF EXISTS  mon_product_production_day DROP CONSTRAINT IF EXISTS fk204rojkd37iypnvoyw0nr3iqn;
ALTER TABLE IF EXISTS  processing_order_input_filters DROP CONSTRAINT IF EXISTS fk1u9dj81sg3vcueaprup3hasqi;
ALTER TABLE IF EXISTS  workflow_class_output_parameters DROP CONSTRAINT IF EXISTS fk1gm53x3igc2k000lc5cy2sf1q;
ALTER TABLE IF EXISTS  workflow_input_filters DROP CONSTRAINT IF EXISTS fk1cuklmlh4xk0s3dqekio7gy74;
ALTER TABLE IF EXISTS  configuration_docker_run_parameters DROP CONSTRAINT IF EXISTS fk165qo4rdh6j4v72p19t5rluv3;
DROP INDEX IF EXISTS product_parameters_values;
DROP INDEX IF EXISTS product_file_names;
DROP INDEX IF EXISTS idxsoap2dcggqp95abimerpm6031;
DROP INDEX IF EXISTS idxqu0ou5l3tyyegjvfh0rvb8f4h;
DROP INDEX IF EXISTS idxqt20vfwmvgqe74tm87v1jdcj9;
DROP INDEX IF EXISTS idxqluoxbhk7kihm71dkalmcq1tq;
DROP INDEX IF EXISTS idxoqh21spbi520l31tsal474r6p;
DROP INDEX IF EXISTS idxojvxy5otq1rh1mu4heg6ny1yn;
DROP INDEX IF EXISTS idxnl37dyvy1o7gygku42gk4db78;
DROP INDEX IF EXISTS idxm1c1ucav1v0gpnn89cjp66b06;
DROP INDEX IF EXISTS idxl5ejqnuw4rb1sigroxfcwsxwq;
DROP INDEX IF EXISTS idxjsbb5wmnf9graowd1f9wud96k;
DROP INDEX IF EXISTS idxjh81k2qvy1nwnacjhx4c9e8uo;
DROP INDEX IF EXISTS idxhw65e77rikl7b5qq70e6kjgpg;
DROP INDEX IF EXISTS idxgin74m2ax2pg2c6yu60km46qa;
DROP INDEX IF EXISTS idxgcks0habumx5p6km1njxx4omf;
DROP INDEX IF EXISTS idxf38b0g96ksfrxorqgryoajj7y;
DROP INDEX IF EXISTS idxebh2ci5ivqufcgtxv4gax0mif;
DROP INDEX IF EXISTS idxdp0vd1b50igr05nxswcxupv6v;
DROP INDEX IF EXISTS idxckdfwsktiy2a7t08oulv9p4bj;
DROP INDEX IF EXISTS idxb1hlhb6srtxd7qpjtkm8a37jg;
DROP INDEX IF EXISTS idxautube4tmw46joub7nf9qaop2;
DROP INDEX IF EXISTS idx99jry69detoongt94mr0cb4jm;
DROP INDEX IF EXISTS idx8y2rqfvrrms3xi92l1nq6dm1m;
DROP INDEX IF EXISTS idx8uli6v1risvb0i9offqwpsaag;
DROP INDEX IF EXISTS idx870rjn0w07u5qc26c4dmfc8p0;
DROP INDEX IF EXISTS idx6u8n1u6c253dps752x6s7yaa7;
DROP INDEX IF EXISTS idx657h07h95ub6nyt49a4wib4ky;
DROP INDEX IF EXISTS idx4jtdla2jravgeu16yxlv6i1g1;
DROP INDEX IF EXISTS idx2uot336txpqpdo8je8x145a0;
ALTER TABLE IF EXISTS  workflow DROP CONSTRAINT IF EXISTS workflow_pkey;
ALTER TABLE IF EXISTS  workflow_output_parameters DROP CONSTRAINT IF EXISTS workflow_output_parameters_pkey;
ALTER TABLE IF EXISTS  workflow_option DROP CONSTRAINT IF EXISTS workflow_option_pkey;
ALTER TABLE IF EXISTS  workflow_input_filters DROP CONSTRAINT IF EXISTS workflow_input_filters_pkey;
ALTER TABLE IF EXISTS  workflow_class_output_parameters DROP CONSTRAINT IF EXISTS workflow_class_output_parameters_pkey;
ALTER TABLE IF EXISTS  users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE IF EXISTS  users_group_memberships DROP CONSTRAINT IF EXISTS users_group_memberships_pkey;
ALTER TABLE IF EXISTS  processor DROP CONSTRAINT IF EXISTS uktomhxtld2pvrabtanoq3t3odk;
ALTER TABLE IF EXISTS  mission DROP CONSTRAINT IF EXISTS uktio2ulw4k2037685uaayxtuub;
ALTER TABLE IF EXISTS  workflow_option DROP CONSTRAINT IF EXISTS ukt0udoa7mo0nk3swm1ff30gh5;
ALTER TABLE IF EXISTS  processing_order DROP CONSTRAINT IF EXISTS ukqx1os0kwk3rvvoe4bw7inowsq;
ALTER TABLE IF EXISTS  mon_order_state DROP CONSTRAINT IF EXISTS ukpm3kggmu7tijpl89jmyjnkg3r;
ALTER TABLE IF EXISTS  processor_class DROP CONSTRAINT IF EXISTS uknbv3u0tx6s1770tmlvylhodfw;
ALTER TABLE IF EXISTS  spacecraft DROP CONSTRAINT IF EXISTS uklogt34j6cnrocn49sw0uu30eh;
ALTER TABLE IF EXISTS  workflow DROP CONSTRAINT IF EXISTS ukhskpb9cv06lohyj8m2ekp9xio;
ALTER TABLE IF EXISTS  product_class DROP CONSTRAINT IF EXISTS ukgmc9l016fh1mcqque3k8iy3yu;
ALTER TABLE IF EXISTS  workflow DROP CONSTRAINT IF EXISTS ukg9qjvpphc4d2n8y10mpael4cd;
ALTER TABLE IF EXISTS  product_file DROP CONSTRAINT IF EXISTS ukdawt5bhyxxovxd4vgo4cw6ugn;
ALTER TABLE IF EXISTS  processing_order DROP CONSTRAINT IF EXISTS ukbxwgyibx5dkbl26jplnjifrsa;
ALTER TABLE IF EXISTS  users_group_memberships DROP CONSTRAINT IF EXISTS uk_e2ijwadyxqhcr2aldhs624px;
ALTER TABLE IF EXISTS  groups DROP CONSTRAINT IF EXISTS uk_7o859iyhxd19rv4hywgdvu2v4;
ALTER TABLE IF EXISTS  simple_selection_rule_simple_policies DROP CONSTRAINT IF EXISTS uk_7jrn9t62kdspngixrembpkrd7;
ALTER TABLE IF EXISTS  configuration_static_input_files DROP CONSTRAINT IF EXISTS uk_2y140wa1pggeycgihvnex0a9c;
ALTER TABLE IF EXISTS  groups_group_members DROP CONSTRAINT IF EXISTS uk_132lanwqs6liav9syek4s96xv;
ALTER TABLE IF EXISTS  processing_facility DROP CONSTRAINT IF EXISTS uk8cny9892if8tybde5p5brts6d;
ALTER TABLE IF EXISTS  orbit DROP CONSTRAINT IF EXISTS uk6tiqkg4pvqd1iyfmes8t2pd2j;
ALTER TABLE IF EXISTS  mon_ext_service DROP CONSTRAINT IF EXISTS uk6iqkhmagcms83dnb123yfd0s2;
ALTER TABLE IF EXISTS  product_query DROP CONSTRAINT IF EXISTS uk4dkam8lshg1hjjfk4mm4vsp50;
ALTER TABLE IF EXISTS  configured_processor DROP CONSTRAINT IF EXISTS uk49uwfv9jn1bfagrgu1fmxjlr8;
ALTER TABLE IF EXISTS  mon_service_state DROP CONSTRAINT IF EXISTS uk447s0lqn0lb4gcho5ciadm0r8;
ALTER TABLE IF EXISTS  product DROP CONSTRAINT IF EXISTS uk24bc4yyyk3fj3h7ku64i3yuog;
ALTER TABLE IF EXISTS  mon_service DROP CONSTRAINT IF EXISTS uk239unm9hg59upu3be0fkcu4rt;
ALTER TABLE IF EXISTS  product_archive DROP CONSTRAINT IF EXISTS uk19j4q7qi3o7ln0yucf43cfbps;
ALTER TABLE IF EXISTS  task DROP CONSTRAINT IF EXISTS task_pkey;
ALTER TABLE IF EXISTS  spacecraft DROP CONSTRAINT IF EXISTS spacecraft_pkey;
ALTER TABLE IF EXISTS  simple_selection_rule DROP CONSTRAINT IF EXISTS simple_selection_rule_pkey;
ALTER TABLE IF EXISTS  simple_selection_rule_filter_conditions DROP CONSTRAINT IF EXISTS simple_selection_rule_filter_conditions_pkey;
ALTER TABLE IF EXISTS  simple_selection_rule_configured_processors DROP CONSTRAINT IF EXISTS simple_selection_rule_configured_processors_pkey;
ALTER TABLE IF EXISTS  simple_policy DROP CONSTRAINT IF EXISTS simple_policy_pkey;
ALTER TABLE IF EXISTS  simple_policy_delta_times DROP CONSTRAINT IF EXISTS simple_policy_delta_times_pkey;
ALTER TABLE IF EXISTS  product_query_satisfying_products DROP CONSTRAINT IF EXISTS product_query_satisfying_products_pkey;
ALTER TABLE IF EXISTS  product_query DROP CONSTRAINT IF EXISTS product_query_pkey;
ALTER TABLE IF EXISTS  product_query_filter_conditions DROP CONSTRAINT IF EXISTS product_query_filter_conditions_pkey;
ALTER TABLE IF EXISTS  product DROP CONSTRAINT IF EXISTS product_pkey;
ALTER TABLE IF EXISTS  product_parameters DROP CONSTRAINT IF EXISTS product_parameters_pkey;
ALTER TABLE IF EXISTS  product_file DROP CONSTRAINT IF EXISTS product_file_pkey;
ALTER TABLE IF EXISTS  product_class DROP CONSTRAINT IF EXISTS product_class_pkey;
ALTER TABLE IF EXISTS  product_archive DROP CONSTRAINT IF EXISTS product_archive_pkey;
ALTER TABLE IF EXISTS  product_archive_available_product_classes DROP CONSTRAINT IF EXISTS product_archive_available_product_classes_pkey;
ALTER TABLE IF EXISTS  processor DROP CONSTRAINT IF EXISTS processor_pkey;
ALTER TABLE IF EXISTS  processor_docker_run_parameters DROP CONSTRAINT IF EXISTS processor_docker_run_parameters_pkey;
ALTER TABLE IF EXISTS  processor_class DROP CONSTRAINT IF EXISTS processor_class_pkey;
ALTER TABLE IF EXISTS  processing_order_requested_product_classes DROP CONSTRAINT IF EXISTS processing_order_requested_product_classes_pkey;
ALTER TABLE IF EXISTS  processing_order_requested_configured_processors DROP CONSTRAINT IF EXISTS processing_order_requested_configured_processors_pkey;
ALTER TABLE IF EXISTS  processing_order DROP CONSTRAINT IF EXISTS processing_order_pkey;
ALTER TABLE IF EXISTS  processing_order_output_parameters DROP CONSTRAINT IF EXISTS processing_order_output_parameters_pkey;
ALTER TABLE IF EXISTS  processing_order_input_product_classes DROP CONSTRAINT IF EXISTS processing_order_input_product_classes_pkey;
ALTER TABLE IF EXISTS  processing_order_input_filters DROP CONSTRAINT IF EXISTS processing_order_input_filters_pkey;
ALTER TABLE IF EXISTS  processing_order_dynamic_processing_parameters DROP CONSTRAINT IF EXISTS processing_order_dynamic_processing_parameters_pkey;
ALTER TABLE IF EXISTS  processing_order_class_output_parameters DROP CONSTRAINT IF EXISTS processing_order_class_output_parameters_pkey;
ALTER TABLE IF EXISTS  processing_facility DROP CONSTRAINT IF EXISTS processing_facility_pkey;
ALTER TABLE IF EXISTS  orbit DROP CONSTRAINT IF EXISTS orbit_pkey;
ALTER TABLE IF EXISTS  mon_service_state DROP CONSTRAINT IF EXISTS mon_service_state_pkey;
ALTER TABLE IF EXISTS  mon_service_state_operation DROP CONSTRAINT IF EXISTS mon_service_state_operation_pkey;
ALTER TABLE IF EXISTS  mon_service_state_operation_month DROP CONSTRAINT IF EXISTS mon_service_state_operation_month_pkey;
ALTER TABLE IF EXISTS  mon_service_state_operation_day DROP CONSTRAINT IF EXISTS mon_service_state_operation_day_pkey;
ALTER TABLE IF EXISTS  mon_service DROP CONSTRAINT IF EXISTS mon_service_pkey;
ALTER TABLE IF EXISTS  mon_product_production_month DROP CONSTRAINT IF EXISTS mon_product_production_month_pkey;
ALTER TABLE IF EXISTS  mon_product_production_hour DROP CONSTRAINT IF EXISTS mon_product_production_hour_pkey;
ALTER TABLE IF EXISTS  mon_product_production_day DROP CONSTRAINT IF EXISTS mon_product_production_day_pkey;
ALTER TABLE IF EXISTS  mon_order_state DROP CONSTRAINT IF EXISTS mon_order_state_pkey;
ALTER TABLE IF EXISTS  mon_ext_service_state_operation DROP CONSTRAINT IF EXISTS mon_ext_service_state_operation_pkey;
ALTER TABLE IF EXISTS  mon_ext_service_state_operation_month DROP CONSTRAINT IF EXISTS mon_ext_service_state_operation_month_pkey;
ALTER TABLE IF EXISTS  mon_ext_service_state_operation_day DROP CONSTRAINT IF EXISTS mon_ext_service_state_operation_day_pkey;
ALTER TABLE IF EXISTS  mon_ext_service DROP CONSTRAINT IF EXISTS mon_ext_service_pkey;
ALTER TABLE IF EXISTS  mission DROP CONSTRAINT IF EXISTS mission_pkey;
ALTER TABLE IF EXISTS  job_step DROP CONSTRAINT IF EXISTS job_step_pkey;
ALTER TABLE IF EXISTS  job_step_output_parameters DROP CONSTRAINT IF EXISTS job_step_output_parameters_pkey;
ALTER TABLE IF EXISTS  job DROP CONSTRAINT IF EXISTS job_pkey;
ALTER TABLE IF EXISTS  input_filter DROP CONSTRAINT IF EXISTS input_filter_pkey;
ALTER TABLE IF EXISTS  input_filter_filter_conditions DROP CONSTRAINT IF EXISTS input_filter_filter_conditions_pkey;
ALTER TABLE IF EXISTS  groups DROP CONSTRAINT IF EXISTS groups_pkey;
ALTER TABLE IF EXISTS  groups_group_members DROP CONSTRAINT IF EXISTS groups_group_members_pkey;
ALTER TABLE IF EXISTS  group_members DROP CONSTRAINT IF EXISTS group_members_pkey;
ALTER TABLE IF EXISTS  group_authorities DROP CONSTRAINT IF EXISTS group_authorities_pkey;
ALTER TABLE IF EXISTS  configured_processor DROP CONSTRAINT IF EXISTS configured_processor_pkey;
ALTER TABLE IF EXISTS  configuration_static_input_files DROP CONSTRAINT IF EXISTS configuration_static_input_files_pkey;
ALTER TABLE IF EXISTS  configuration DROP CONSTRAINT IF EXISTS configuration_pkey;
ALTER TABLE IF EXISTS  configuration_input_file DROP CONSTRAINT IF EXISTS configuration_input_file_pkey;
ALTER TABLE IF EXISTS  configuration_dyn_proc_parameters DROP CONSTRAINT IF EXISTS configuration_dyn_proc_parameters_pkey;
ALTER TABLE IF EXISTS  configuration_docker_run_parameters DROP CONSTRAINT IF EXISTS configuration_docker_run_parameters_pkey;
ALTER TABLE IF EXISTS  class_output_parameter DROP CONSTRAINT IF EXISTS class_output_parameter_pkey;
ALTER TABLE IF EXISTS  class_output_parameter_output_parameters DROP CONSTRAINT IF EXISTS class_output_parameter_output_parameters_pkey;
ALTER TABLE IF EXISTS  authorities DROP CONSTRAINT IF EXISTS authorities_pkey;
DROP TABLE IF EXISTS workflow_output_parameters;
DROP TABLE IF EXISTS workflow_option_value_range;
DROP TABLE IF EXISTS workflow_option;
DROP TABLE IF EXISTS workflow_input_filters;
DROP TABLE IF EXISTS workflow_class_output_parameters;
DROP TABLE IF EXISTS workflow;
DROP TABLE IF EXISTS users_group_memberships;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS task_breakpoint_file_names;
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS spacecraft_payloads;
DROP TABLE IF EXISTS spacecraft;
DROP TABLE IF EXISTS simple_selection_rule_simple_policies;
DROP TABLE IF EXISTS simple_selection_rule_filter_conditions;
DROP TABLE IF EXISTS simple_selection_rule_configured_processors;
DROP TABLE IF EXISTS simple_selection_rule;
DROP TABLE IF EXISTS simple_policy_delta_times;
DROP TABLE IF EXISTS simple_policy;
DROP TABLE IF EXISTS product_query_satisfying_products;
DROP TABLE IF EXISTS product_query_filter_conditions;
DROP TABLE IF EXISTS product_query;
DROP VIEW IF EXISTS product_processing_facilities;
DROP TABLE IF EXISTS product_parameters;
DROP TABLE IF EXISTS product_file_aux_file_names;
DROP TABLE IF EXISTS product_file;
DROP TABLE IF EXISTS product_download_history;
DROP TABLE IF EXISTS product_class;
DROP TABLE IF EXISTS product_archive_available_product_classes;
DROP TABLE IF EXISTS product_archive;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS processor_docker_run_parameters;
DROP TABLE IF EXISTS processor_class;
DROP TABLE IF EXISTS processor;
DROP TABLE IF EXISTS processing_order_requested_product_classes;
DROP TABLE IF EXISTS processing_order_requested_orbits;
DROP TABLE IF EXISTS processing_order_requested_configured_processors;
DROP TABLE IF EXISTS processing_order_output_parameters;
DROP TABLE IF EXISTS processing_order_mon_order_progress;
DROP TABLE IF EXISTS processing_order_input_product_classes;
DROP TABLE IF EXISTS processing_order_input_filters;
DROP TABLE IF EXISTS processing_order_dynamic_processing_parameters;
DROP TABLE IF EXISTS processing_order_class_output_parameters;
DROP TABLE IF EXISTS processing_order;
DROP TABLE IF EXISTS processing_facility;
DROP TABLE IF EXISTS orbit;
DROP TABLE IF EXISTS mon_service_state_operation_month;
DROP TABLE IF EXISTS mon_service_state_operation_day;
DROP TABLE IF EXISTS mon_service_state_operation;
DROP TABLE IF EXISTS mon_service_state;
DROP TABLE IF EXISTS mon_service;
DROP TABLE IF EXISTS mon_product_production_month;
DROP TABLE IF EXISTS mon_product_production_hour;
DROP TABLE IF EXISTS mon_product_production_day;
DROP TABLE IF EXISTS mon_order_state;
DROP TABLE IF EXISTS mon_ext_service_state_operation_month;
DROP TABLE IF EXISTS mon_ext_service_state_operation_day;
DROP TABLE IF EXISTS mon_ext_service_state_operation;
DROP TABLE IF EXISTS mon_ext_service;
DROP TABLE IF EXISTS mission_processing_modes;
DROP TABLE IF EXISTS mission_file_classes;
DROP TABLE IF EXISTS mission;
DROP TABLE IF EXISTS job_step_output_parameters;
DROP TABLE IF EXISTS job_step;
DROP TABLE IF EXISTS job;
DROP TABLE IF EXISTS input_filter_filter_conditions;
DROP TABLE IF EXISTS input_filter;
DROP SEQUENCE IF EXISTS hibernate_sequence;
DROP TABLE IF EXISTS groups_group_members;
DROP TABLE IF EXISTS groups;
DROP TABLE IF EXISTS group_members;
DROP TABLE IF EXISTS group_authorities;
DROP TABLE IF EXISTS configured_processor;
DROP TABLE IF EXISTS configuration_static_input_files;
DROP TABLE IF EXISTS configuration_input_file_file_names;
DROP TABLE IF EXISTS configuration_input_file;
DROP TABLE IF EXISTS configuration_dyn_proc_parameters;
DROP TABLE IF EXISTS configuration_docker_run_parameters;
DROP TABLE IF EXISTS configuration_configuration_files;
DROP TABLE IF EXISTS configuration;
DROP TABLE IF EXISTS class_output_parameter_output_parameters;
DROP TABLE IF EXISTS class_output_parameter;
DROP TABLE IF EXISTS authorities;
--
-- Name: authorities; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE authorities (
    username character varying(255) NOT NULL,
    authority character varying(255) NOT NULL
);




--
-- Name: class_output_parameter; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE class_output_parameter (
    id bigint NOT NULL,
    version integer NOT NULL
);




--
-- Name: class_output_parameter_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE class_output_parameter_output_parameters (
    class_output_parameter_id bigint NOT NULL,
    parameter_clob text,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    output_parameters_key character varying(255) NOT NULL
);




--
-- Name: configuration; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE configuration (
    id bigint NOT NULL,
    version integer NOT NULL,
    configuration_version character varying(255),
    mode character varying(255),
    product_quality character varying(255),
    processor_class_id bigint
);




--
-- Name: configuration_configuration_files; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE configuration_configuration_files (
    configuration_id bigint NOT NULL,
    file_name character varying(255),
    file_version character varying(255)
);




--
-- Name: configuration_docker_run_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE configuration_docker_run_parameters (
    configuration_id bigint NOT NULL,
    docker_run_parameters character varying(255),
    docker_run_parameters_key character varying(255) NOT NULL
);




--
-- Name: configuration_dyn_proc_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE configuration_dyn_proc_parameters (
    configuration_id bigint NOT NULL,
    parameter_clob text,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    dyn_proc_parameters_key character varying(255) NOT NULL
);




--
-- Name: configuration_input_file; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE configuration_input_file (
    id bigint NOT NULL,
    version integer NOT NULL,
    file_name_type character varying(255),
    file_type character varying(255)
);




--
-- Name: configuration_input_file_file_names; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE configuration_input_file_file_names (
    configuration_input_file_id bigint NOT NULL,
    file_names character varying(255)
);




--
-- Name: configuration_static_input_files; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE configuration_static_input_files (
    configuration_id bigint NOT NULL,
    static_input_files_id bigint NOT NULL
);




--
-- Name: configured_processor; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE configured_processor (
    id bigint NOT NULL,
    version integer NOT NULL,
    enabled boolean,
    identifier character varying(255),
    uuid uuid,
    configuration_id bigint,
    processor_id bigint
);




--
-- Name: group_authorities; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE group_authorities (
    group_id bigint NOT NULL,
    authority character varying(255) NOT NULL
);




--
-- Name: group_members; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE group_members (
    id bigint NOT NULL,
    group_id bigint NOT NULL,
    username character varying(255) NOT NULL
);




--
-- Name: groups; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE groups (
    id bigint NOT NULL,
    group_name character varying(255) NOT NULL
);




--
-- Name: groups_group_members; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE groups_group_members (
    groups_id bigint NOT NULL,
    group_members_id bigint NOT NULL
);




--
-- Name: hibernate_sequence; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;




--
-- Name: input_filter; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE input_filter (
    id bigint NOT NULL,
    version integer NOT NULL
);




--
-- Name: input_filter_filter_conditions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE input_filter_filter_conditions (
    input_filter_id bigint NOT NULL,
    parameter_clob text,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    filter_conditions_key character varying(255) NOT NULL
);




--
-- Name: job; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE job (
    id bigint NOT NULL,
    version integer NOT NULL,
    has_failed_job_steps boolean,
    job_state character varying(255),
    priority integer,
    start_time timestamp(6) without time zone,
    stop_time timestamp(6) without time zone,
    orbit_id bigint,
    processing_facility_id bigint,
    processing_order_id bigint
);




--
-- Name: job_step; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE job_step (
    id bigint NOT NULL,
    version integer NOT NULL,
    is_failed boolean,
    job_order_filename character varying(255),
    job_step_state character varying(255),
    priority integer,
    processing_completion_time timestamp without time zone,
    processing_mode character varying(255),
    processing_start_time timestamp without time zone,
    processing_std_err text,
    processing_std_out text,
    stderr_log_level integer,
    stdout_log_level integer,
    job_id bigint
);




--
-- Name: job_step_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE job_step_output_parameters (
    job_step_id bigint NOT NULL,
    parameter_clob text,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    output_parameters_key character varying(255) NOT NULL
);




--
-- Name: mission; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mission (
    id bigint NOT NULL,
    version integer NOT NULL,
    code character varying(255),
    name character varying(255),
    order_retention_period bigint,
    processing_centre character varying(255),
    product_file_template text,
    product_retention_period bigint
);




--
-- Name: mission_file_classes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mission_file_classes (
    mission_id bigint NOT NULL,
    file_classes character varying(255)
);




--
-- Name: mission_processing_modes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mission_processing_modes (
    mission_id bigint NOT NULL,
    processing_modes character varying(255)
);




--
-- Name: mon_ext_service; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_ext_service (
    id bigint NOT NULL,
    version integer NOT NULL,
    name character varying(255) NOT NULL,
    name_id character varying(255) NOT NULL
);




--
-- Name: mon_ext_service_state_operation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_ext_service_state_operation (
    id bigint NOT NULL,
    version integer NOT NULL,
    datetime timestamp without time zone,
    mon_ext_service_id bigint,
    mon_service_state_id bigint
);




--
-- Name: mon_ext_service_state_operation_day; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_ext_service_state_operation_day (
    id bigint NOT NULL,
    version integer NOT NULL,
    datetime timestamp without time zone,
    mon_ext_service_id bigint,
    up_time double precision
);




--
-- Name: mon_ext_service_state_operation_month; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_ext_service_state_operation_month (
    id bigint NOT NULL,
    version integer NOT NULL,
    datetime timestamp without time zone,
    mon_ext_service_id bigint,
    up_time double precision
);




--
-- Name: mon_order_state; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_order_state (
    id bigint NOT NULL,
    version integer NOT NULL,
    name character varying(255) NOT NULL
);




--
-- Name: mon_product_production_day; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_product_production_day (
    id bigint NOT NULL,
    version integer NOT NULL,
    count integer NOT NULL,
    datetime timestamp without time zone,
    file_size bigint NOT NULL,
    production_latency_avg integer NOT NULL,
    production_latency_max integer NOT NULL,
    production_latency_min integer NOT NULL,
    production_type character varying(255),
    total_latency_avg integer NOT NULL,
    total_latency_max integer NOT NULL,
    total_latency_min integer NOT NULL,
    mission_id bigint
);




--
-- Name: mon_product_production_hour; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_product_production_hour (
    id bigint NOT NULL,
    version integer NOT NULL,
    count integer NOT NULL,
    datetime timestamp without time zone,
    file_size bigint NOT NULL,
    production_latency_avg integer NOT NULL,
    production_latency_max integer NOT NULL,
    production_latency_min integer NOT NULL,
    production_type character varying(255),
    total_latency_avg integer NOT NULL,
    total_latency_max integer NOT NULL,
    total_latency_min integer NOT NULL,
    mission_id bigint
);




--
-- Name: mon_product_production_month; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_product_production_month (
    id bigint NOT NULL,
    version integer NOT NULL,
    count integer NOT NULL,
    datetime timestamp without time zone,
    file_size bigint NOT NULL,
    production_latency_avg integer NOT NULL,
    production_latency_max integer NOT NULL,
    production_latency_min integer NOT NULL,
    production_type character varying(255),
    total_latency_avg integer NOT NULL,
    total_latency_max integer NOT NULL,
    total_latency_min integer NOT NULL,
    mission_id bigint
);




--
-- Name: mon_service; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_service (
    id bigint NOT NULL,
    version integer NOT NULL,
    name character varying(255) NOT NULL,
    name_id character varying(255) NOT NULL
);




--
-- Name: mon_service_state; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_service_state (
    id bigint NOT NULL,
    version integer NOT NULL,
    name character varying(255) NOT NULL
);




--
-- Name: mon_service_state_operation; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_service_state_operation (
    id bigint NOT NULL,
    version integer NOT NULL,
    datetime timestamp without time zone,
    mon_service_id bigint,
    mon_service_state_id bigint
);




--
-- Name: mon_service_state_operation_day; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_service_state_operation_day (
    id bigint NOT NULL,
    version integer NOT NULL,
    datetime timestamp without time zone,
    mon_service_id bigint,
    up_time double precision
);




--
-- Name: mon_service_state_operation_month; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE mon_service_state_operation_month (
    id bigint NOT NULL,
    version integer NOT NULL,
    datetime timestamp without time zone,
    mon_service_id bigint,
    up_time double precision
);




--
-- Name: orbit; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE orbit (
    id bigint NOT NULL,
    version integer NOT NULL,
    orbit_number integer,
    start_time timestamp(6) without time zone,
    stop_time timestamp(6) without time zone,
    spacecraft_id bigint
);




--
-- Name: processing_facility; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processing_facility (
    id bigint NOT NULL,
    version integer NOT NULL,
    default_storage_type character varying(255),
    description character varying(255),
    external_storage_manager_url character varying(255),
    facility_state character varying(255),
    local_storage_manager_url character varying(255),
    max_jobs_per_node integer,
    name character varying(255) NOT NULL,
    processing_engine_token text,
    processing_engine_url character varying(255),
    storage_manager_password character varying(255),
    storage_manager_url character varying(255),
    storage_manager_user character varying(255)
);




--
-- Name: processing_order; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processing_order (
    id bigint NOT NULL,
    version integer NOT NULL,
    actual_completion_time timestamp without time zone,
    estimated_completion_time timestamp without time zone,
    eviction_time timestamp without time zone,
    execution_time timestamp without time zone,
    has_failed_job_steps boolean,
    identifier character varying(255) NOT NULL,
    input_file_name character varying(255),
    input_sensing_start_time timestamp(6) without time zone,
    input_sensing_stop_time timestamp(6) without time zone,
    endpoint_password character varying(255),
    endpoint_uri character varying(255),
    endpoint_username character varying(255),
    order_source character varying(255),
    order_state character varying(255),
    output_file_class character varying(255),
    priority integer,
    processing_mode character varying(255),
    product_retention_period bigint,
    production_type character varying(255),
    release_time timestamp without time zone,
    slice_duration bigint,
    slice_overlap bigint,
    slicing_type character varying(255),
    start_time timestamp(6) without time zone,
    state_message character varying(255),
    stop_time timestamp(6) without time zone,
    submission_time timestamp without time zone,
    uuid uuid NOT NULL,
    mission_id bigint,
    workflow_id bigint
);




--
-- Name: processing_order_class_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processing_order_class_output_parameters (
    processing_order_id bigint NOT NULL,
    class_output_parameters_id bigint NOT NULL,
    class_output_parameters_key bigint NOT NULL
);




--
-- Name: processing_order_dynamic_processing_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processing_order_dynamic_processing_parameters (
    processing_order_id bigint NOT NULL,
    parameter_clob text,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    dynamic_processing_parameters_key character varying(255) NOT NULL
);




--
-- Name: processing_order_input_filters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processing_order_input_filters (
    processing_order_id bigint NOT NULL,
    input_filters_id bigint NOT NULL,
    input_filters_key bigint NOT NULL
);




--
-- Name: processing_order_input_product_classes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processing_order_input_product_classes (
    processing_order_id bigint NOT NULL,
    input_product_classes_id bigint NOT NULL
);




--
-- Name: processing_order_mon_order_progress; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processing_order_mon_order_progress (
    processing_order_id bigint NOT NULL,
    all_job_steps integer NOT NULL,
    completed_job_steps integer NOT NULL,
    datetime timestamp without time zone,
    failed_job_steps integer NOT NULL,
    finished_job_steps integer NOT NULL,
    mon_order_state_id bigint,
    running_job_steps integer NOT NULL
);




--
-- Name: processing_order_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processing_order_output_parameters (
    processing_order_id bigint NOT NULL,
    parameter_clob text,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    output_parameters_key character varying(255) NOT NULL
);




--
-- Name: processing_order_requested_configured_processors; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processing_order_requested_configured_processors (
    processing_order_id bigint NOT NULL,
    requested_configured_processors_id bigint NOT NULL
);




--
-- Name: processing_order_requested_orbits; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processing_order_requested_orbits (
    processing_order_id bigint NOT NULL,
    requested_orbits_id bigint NOT NULL
);




--
-- Name: processing_order_requested_product_classes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processing_order_requested_product_classes (
    processing_order_id bigint NOT NULL,
    requested_product_classes_id bigint NOT NULL
);




--
-- Name: processor; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processor (
    id bigint NOT NULL,
    version integer NOT NULL,
    docker_image character varying(255),
    is_test boolean,
    job_order_version character varying(255),
    max_time integer,
    min_disk_space integer,
    processor_version character varying(255),
    sensing_time_flag boolean,
    use_input_file_time_intervals boolean,
    processor_class_id bigint
);




--
-- Name: processor_class; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processor_class (
    id bigint NOT NULL,
    version integer NOT NULL,
    processor_name character varying(255),
    mission_id bigint
);




--
-- Name: processor_docker_run_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE processor_docker_run_parameters (
    processor_id bigint NOT NULL,
    docker_run_parameters character varying(255),
    docker_run_parameters_key character varying(255) NOT NULL
);




--
-- Name: product; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE product (
    id bigint NOT NULL,
    version integer NOT NULL,
    eviction_time timestamp(6) without time zone,
    file_class character varying(255),
    generation_time timestamp(6) without time zone,
    mode character varying(255),
    product_quality character varying(255),
    production_type character varying(255),
    publication_time timestamp(6) without time zone,
    raw_data_availability_time timestamp(6) without time zone,
    requested_start_time timestamp(6) without time zone,
    requested_stop_time timestamp(6) without time zone,
    sensing_start_time timestamp(6) without time zone,
    sensing_stop_time timestamp(6) without time zone,
    uuid uuid NOT NULL,
    configured_processor_id bigint,
    enclosing_product_id bigint,
    job_step_id bigint,
    orbit_id bigint,
    product_class_id bigint
);




--
-- Name: product_archive; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE product_archive (
    id bigint NOT NULL,
    version integer NOT NULL,
    archive_type character varying(255),
    base_uri character varying(255),
    client_id character varying(255),
    client_secret character varying(255),
    code character varying(255) NOT NULL,
    context character varying(255),
    name character varying(255),
    password character varying(255),
    send_auth_in_body boolean,
    token_required boolean,
    token_uri character varying(255),
    username character varying(255)
);




--
-- Name: product_archive_available_product_classes; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE product_archive_available_product_classes (
    product_archive_id bigint NOT NULL,
    available_product_classes_id bigint NOT NULL
);




--
-- Name: product_class; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE product_class (
    id bigint NOT NULL,
    version integer NOT NULL,
    default_slice_duration bigint,
    default_slicing_type character varying(255),
    description character varying(255),
    processing_level character varying(255),
    product_file_template text,
    product_type character varying(255),
    visibility character varying(255),
    enclosing_class_id bigint,
    mission_id bigint,
    processor_class_id bigint
);




--
-- Name: product_download_history; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE product_download_history (
    product_id bigint NOT NULL,
    date_time timestamp(6) without time zone,
    product_file_id bigint,
    product_file_name character varying(255),
    product_file_size bigint,
    username character varying(255)
);




--
-- Name: product_file; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE product_file (
    id bigint NOT NULL,
    version integer NOT NULL,
    checksum character varying(255),
    checksum_time timestamp(6) without time zone,
    file_path character varying(255),
    file_size bigint,
    product_file_name character varying(255),
    storage_type character varying(255),
    zip_checksum character varying(255),
    zip_checksum_time timestamp(6) without time zone,
    zip_file_name character varying(255),
    zip_file_size bigint,
    processing_facility_id bigint,
    product_id bigint
);




--
-- Name: product_file_aux_file_names; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE product_file_aux_file_names (
    product_file_id bigint NOT NULL,
    aux_file_names character varying(255)
);




--
-- Name: product_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE product_parameters (
    product_id bigint NOT NULL,
    parameter_clob text,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    parameters_key character varying(255) NOT NULL
);




--
-- Name: product_processing_facilities; Type: VIEW; Schema: public; Owner: postgres
--

-- CREATE VIEW product_processing_facilities AS
--  WITH RECURSIVE available_facilities(product_id, enclosing_product_id, processing_facility_id, depth, path, cycle) AS (
--          SELECT p.id,
--             p.enclosing_product_id,
--             pf.processing_facility_id,
--             1,
--             ARRAY[p.id] AS "array",
--             false AS bool
--            FROM (product p
--              LEFT JOIN product_file pf ON ((p.id = pf.product_id)))
--         UNION
--          SELECT p.id,
--             p.enclosing_product_id,
--                 CASE
--                     WHEN (af.processing_facility_id IS NULL) THEN pf.processing_facility_id
--                     ELSE af.processing_facility_id
--                 END AS processing_facility_id,
--             (af.depth + 1),
--             (af.path || p.id),
--             (p.id = ANY (af.path))
--            FROM ((product p
--              JOIN available_facilities af ON ((p.id = af.enclosing_product_id)))
--              LEFT JOIN product_file pf ON ((p.id = pf.product_id)))
--           WHERE (((af.processing_facility_id IS NULL) OR (pf.processing_facility_id IS NULL) OR (af.processing_facility_id = pf.processing_facility_id)) AND (NOT af.cycle))
--         )
--  SELECT available_facilities.product_id,
--     available_facilities.enclosing_product_id,
--     available_facilities.processing_facility_id,
--     available_facilities.depth,
--     available_facilities.path,
--     available_facilities.cycle
--    FROM available_facilities
--   WHERE (available_facilities.processing_facility_id IS NOT NULL);

CREATE OR REPLACE VIEW product_processing_facilities AS
SELECT p.id AS product_id,
      p.enclosing_product_id AS enclosing_product_id,
      pf.processing_facility_id AS processing_facility_id,
      1 AS depth,
      ARRAY[p.id] AS path,
      false AS cycle
FROM product p JOIN product_file pf ON p.id = pf.product_id;


--
-- Name: product_query; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE product_query (
    id bigint NOT NULL,
    version integer NOT NULL,
    in_download boolean,
    is_satisfied boolean,
    jpql_query_condition text,
    minimum_coverage smallint,
    sql_query_condition text,
    generating_rule_id bigint,
    job_step_id bigint,
    requested_product_class_id bigint
);




--
-- Name: product_query_filter_conditions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE product_query_filter_conditions (
    product_query_id bigint NOT NULL,
    parameter_clob text,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    filter_conditions_key character varying(255) NOT NULL
);




--
-- Name: product_query_satisfying_products; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE product_query_satisfying_products (
    satisfied_product_queries_id bigint NOT NULL,
    satisfying_products_id bigint NOT NULL
);




--
-- Name: simple_policy; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE simple_policy (
    id bigint NOT NULL,
    version integer NOT NULL,
    policy_type character varying(255) NOT NULL
);




--
-- Name: simple_policy_delta_times; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE simple_policy_delta_times (
    simple_policy_id bigint NOT NULL,
    duration bigint NOT NULL,
    unit integer,
    list_index integer NOT NULL
);




--
-- Name: simple_selection_rule; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE simple_selection_rule (
    id bigint NOT NULL,
    version integer NOT NULL,
    filtered_source_product_type character varying(255),
    is_mandatory boolean,
    minimum_coverage smallint,
    mode character varying(255),
    source_product_class_id bigint,
    target_product_class_id bigint
);




--
-- Name: simple_selection_rule_configured_processors; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE simple_selection_rule_configured_processors (
    simple_selection_rule_id bigint NOT NULL,
    configured_processors_id bigint NOT NULL
);




--
-- Name: simple_selection_rule_filter_conditions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE simple_selection_rule_filter_conditions (
    simple_selection_rule_id bigint NOT NULL,
    parameter_clob text,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    filter_conditions_key character varying(255) NOT NULL
);




--
-- Name: simple_selection_rule_simple_policies; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE simple_selection_rule_simple_policies (
    simple_selection_rule_id bigint NOT NULL,
    simple_policies_id bigint NOT NULL
);




--
-- Name: spacecraft; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE spacecraft (
    id bigint NOT NULL,
    version integer NOT NULL,
    code character varying(255),
    name character varying(255),
    mission_id bigint
);




--
-- Name: spacecraft_payloads; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE spacecraft_payloads (
    spacecraft_id bigint NOT NULL,
    description character varying(255),
    name character varying(255)
);




--
-- Name: task; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE task (
    id bigint NOT NULL,
    version integer NOT NULL,
    criticality_level integer,
    is_critical boolean,
    min_memory integer,
    number_of_cpus integer,
    task_name character varying(255),
    task_version character varying(255),
    processor_id bigint
);




--
-- Name: task_breakpoint_file_names; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE task_breakpoint_file_names (
    task_id bigint NOT NULL,
    breakpoint_file_names character varying(255)
);




--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE users (
    username character varying(255) NOT NULL,
    enabled boolean NOT NULL,
    expiration_date timestamp without time zone NOT NULL,
    password character varying(255) NOT NULL,
    password_expiration_date timestamp without time zone NOT NULL,
    assigned bigint,
    last_access_date timestamp without time zone,
    used bigint
);




--
-- Name: users_group_memberships; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE users_group_memberships (
    users_username character varying(255) NOT NULL,
    group_memberships_id bigint NOT NULL
);




--
-- Name: workflow; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE workflow (
    id bigint NOT NULL,
    version integer NOT NULL,
    description character varying(255),
    enabled boolean,
    name character varying(255) NOT NULL,
    output_file_class character varying(255),
    processing_mode character varying(255),
    slice_duration bigint,
    slice_overlap bigint,
    slicing_type character varying(255),
    uuid uuid NOT NULL,
    workflow_version character varying(255) NOT NULL,
    configured_processor_id bigint,
    input_product_class_id bigint,
    mission_id bigint,
    output_product_class_id bigint
);




--
-- Name: workflow_class_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE workflow_class_output_parameters (
    workflow_id bigint NOT NULL,
    class_output_parameters_id bigint NOT NULL,
    class_output_parameters_key bigint NOT NULL
);




--
-- Name: workflow_input_filters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE workflow_input_filters (
    workflow_id bigint NOT NULL,
    input_filters_id bigint NOT NULL,
    input_filters_key bigint NOT NULL
);




--
-- Name: workflow_option; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE workflow_option (
    id bigint NOT NULL,
    version integer NOT NULL,
    default_value character varying(255),
    description character varying(255),
    name character varying(255),
    type character varying(255),
    workflow_id bigint
);




--
-- Name: workflow_option_value_range; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE workflow_option_value_range (
    workflow_option_id bigint NOT NULL,
    value_range character varying(255)
);




--
-- Name: workflow_output_parameters; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE workflow_output_parameters (
    workflow_id bigint NOT NULL,
    parameter_clob text,
    parameter_type character varying(255) NOT NULL,
    parameter_value character varying(255),
    output_parameters_key character varying(255) NOT NULL
);


--
--
-- Data for Name: authorities; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO authorities VALUES ('sysadm', 'ROLE_CLI_USER');
INSERT INTO authorities VALUES ('sysadm', 'ROLE_ROOT');
INSERT INTO authorities VALUES ('PTM-usermgr', 'ROLE_USERMGR');
INSERT INTO authorities VALUES ('PTM-usermgr', 'ROLE_GUI_USER');
INSERT INTO authorities VALUES ('PTM-usermgr', 'ROLE_CLI_USER');
INSERT INTO authorities VALUES ('PTM-usermgr', 'ROLE_MISSION_READER');
INSERT INTO authorities VALUES ('PTM-proseo', 'ROLE_PRIP_USER');
INSERT INTO authorities VALUES ('PTM-proseo', 'ROLE_GUI_USER');
INSERT INTO authorities VALUES ('PTM-proseo', 'ROLE_CLI_USER');


--
-- Data for Name: class_output_parameter; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO class_output_parameter VALUES (79, 1);


--
-- Data for Name: class_output_parameter_output_parameters; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO class_output_parameter_output_parameters VALUES (79, NULL, 'INTEGER', '77', 'copernicusCollection');
INSERT INTO class_output_parameter_output_parameters VALUES (79, NULL, 'INTEGER', '2', 'revision');


--
-- Data for Name: configuration; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO configuration VALUES (32, 1, 'OPER_2020-03-25', 'OPER', 'TEST', 23);
INSERT INTO configuration VALUES (34, 1, 'OPER_2020-03-25', 'OPER', 'TEST', 24);
INSERT INTO configuration VALUES (35, 1, 'OPER_2020-03-25', 'OPER', 'TEST', 25);


--
-- Data for Name: configuration_configuration_files; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO configuration_configuration_files VALUES (34, '/usr/share/sample-processor/conf/ptm_l2_config.xml', '1.0');


--
-- Data for Name: configuration_docker_run_parameters; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: configuration_dyn_proc_parameters; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO configuration_dyn_proc_parameters VALUES (32, NULL, 'STRING', 'null', 'logging.dumplog');
INSERT INTO configuration_dyn_proc_parameters VALUES (32, NULL, 'INTEGER', '16', 'Threads');
INSERT INTO configuration_dyn_proc_parameters VALUES (32, NULL, 'STRING', 'OPER', 'Processing_Mode');
INSERT INTO configuration_dyn_proc_parameters VALUES (32, NULL, 'STRING', 'notice', 'logging.root');
INSERT INTO configuration_dyn_proc_parameters VALUES (34, NULL, 'STRING', 'null', 'logging.dumplog');
INSERT INTO configuration_dyn_proc_parameters VALUES (34, NULL, 'INTEGER', '10', 'Threads');
INSERT INTO configuration_dyn_proc_parameters VALUES (34, NULL, 'STRING', 'OPER', 'Processing_Mode');
INSERT INTO configuration_dyn_proc_parameters VALUES (34, NULL, 'STRING', 'notice', 'logging.root');
INSERT INTO configuration_dyn_proc_parameters VALUES (35, NULL, 'STRING', 'null', 'logging.dumplog');
INSERT INTO configuration_dyn_proc_parameters VALUES (35, NULL, 'INTEGER', '16', 'Threads');
INSERT INTO configuration_dyn_proc_parameters VALUES (35, NULL, 'STRING', 'OPER', 'Processing_Mode');
INSERT INTO configuration_dyn_proc_parameters VALUES (35, NULL, 'STRING', 'notice', 'logging.root');


--
-- Data for Name: configuration_input_file; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO configuration_input_file VALUES (33, 1, 'Physical', 'processing_configuration');


--
-- Data for Name: configuration_input_file_file_names; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO configuration_input_file_file_names VALUES (33, '/usr/share/sample-processor/conf/ptm_l1b_config.xml');


--
-- Data for Name: configuration_static_input_files; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO configuration_static_input_files VALUES (32, 33);


--
-- Data for Name: configured_processor; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO configured_processor VALUES (36, 1, true, 'PTML1B_0.1.0_OPER_2020-03-25', '71a3a0b5-6999-427e-afb7-85f211ce48df', 32, 26);
INSERT INTO configured_processor VALUES (37, 1, true, 'PTML2_0.1.0_OPER_2020-03-25', '7d4e991b-d8d5-42f2-a2bc-1713958c4b62', 34, 28);
INSERT INTO configured_processor VALUES (38, 1, true, 'PTML3_0.1.0_OPER_2020-03-25', '9e2f1890-17d8-43e2-89bd-964170c9cacf', 35, 30);


--
-- Data for Name: group_authorities; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO group_authorities VALUES (3, 'ROLE_PRODUCTCLASS_READER');
INSERT INTO group_authorities VALUES (3, 'ROLE_MISSION_READER');
INSERT INTO group_authorities VALUES (3, 'ROLE_ORDER_READER');
INSERT INTO group_authorities VALUES (3, 'ROLE_FACILITY_READER');
INSERT INTO group_authorities VALUES (3, 'ROLE_FACILITY_MONITOR');
INSERT INTO group_authorities VALUES (3, 'ROLE_ORDER_PLANNER');
INSERT INTO group_authorities VALUES (3, 'ROLE_ORDER_MONITOR');
INSERT INTO group_authorities VALUES (3, 'ROLE_PRODUCT_READER_ALL');
INSERT INTO group_authorities VALUES (3, 'ROLE_PROCESSOR_READER');
INSERT INTO group_authorities VALUES (3, 'ROLE_ORDER_MGR');
INSERT INTO group_authorities VALUES (4, 'ROLE_PRODUCTCLASS_READER');
INSERT INTO group_authorities VALUES (4, 'ROLE_FACILITY_READER');
INSERT INTO group_authorities VALUES (4, 'ROLE_PRODUCT_INGESTOR');
INSERT INTO group_authorities VALUES (4, 'ROLE_PRODUCT_MGR');
INSERT INTO group_authorities VALUES (4, 'ROLE_MISSION_READER');
INSERT INTO group_authorities VALUES (4, 'ROLE_PRODUCT_READER_ALL');
INSERT INTO group_authorities VALUES (4, 'ROLE_FACILITY_MONITOR');
INSERT INTO group_authorities VALUES (5, 'ROLE_ARCHIVE_READER');
INSERT INTO group_authorities VALUES (5, 'ROLE_FACILITY_MONITOR');
INSERT INTO group_authorities VALUES (5, 'ROLE_ARCHIVE_MGR');
INSERT INTO group_authorities VALUES (5, 'ROLE_CONFIGURATION_MGR');
INSERT INTO group_authorities VALUES (5, 'ROLE_PROCESSORCLASS_MGR');
INSERT INTO group_authorities VALUES (5, 'ROLE_PRODUCT_READER_ALL');
INSERT INTO group_authorities VALUES (5, 'ROLE_PRODUCTCLASS_MGR');
INSERT INTO group_authorities VALUES (5, 'ROLE_FACILITY_MGR');
INSERT INTO group_authorities VALUES (5, 'ROLE_MISSION_MGR');
INSERT INTO group_authorities VALUES (5, 'ROLE_WORKFLOW_MGR');
INSERT INTO group_authorities VALUES (5, 'ROLE_PROCESSOR_READER');
INSERT INTO group_authorities VALUES (5, 'ROLE_PRODUCTCLASS_READER');
INSERT INTO group_authorities VALUES (5, 'ROLE_FACILITY_READER');
INSERT INTO group_authorities VALUES (5, 'ROLE_MISSION_READER');
INSERT INTO group_authorities VALUES (5, 'ROLE_ORDER_READER');
INSERT INTO group_authorities VALUES (5, 'ROLE_ORDER_MONITOR');
INSERT INTO group_authorities VALUES (6, 'ROLE_ORDER_READER');
INSERT INTO group_authorities VALUES (6, 'ROLE_ORDER_APPROVER');
INSERT INTO group_authorities VALUES (7, 'ROLE_PRODUCT_READER');
INSERT INTO group_authorities VALUES (7, 'ROLE_PRIP_USER');
INSERT INTO group_authorities VALUES (8, 'ROLE_PRODUCT_READER_RESTRICTED');
INSERT INTO group_authorities VALUES (8, 'ROLE_PRODUCT_INGESTOR');
INSERT INTO group_authorities VALUES (8, 'ROLE_PRIP_USER');
INSERT INTO group_authorities VALUES (9, 'ROLE_JOBSTEP_PROCESSOR');
INSERT INTO group_authorities VALUES (9, 'ROLE_PRODUCT_INGESTOR');
INSERT INTO group_authorities VALUES (9, 'ROLE_PRODUCT_GENERATOR');


--
-- Data for Name: group_members; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO group_members VALUES (10, 3, 'PTM-proseo');
INSERT INTO group_members VALUES (11, 4, 'PTM-proseo');
INSERT INTO group_members VALUES (12, 5, 'PTM-proseo');
INSERT INTO group_members VALUES (13, 6, 'PTM-proseo');
INSERT INTO group_members VALUES (14, 7, 'PTM-sciuserprip');
INSERT INTO group_members VALUES (15, 8, 'PTM-cfidevprip');
INSERT INTO group_members VALUES (16, 9, 'PTM-wrapper');


--
-- Data for Name: groups; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO groups VALUES (3, 'PTM-operator');
INSERT INTO groups VALUES (4, 'PTM-archivist');
INSERT INTO groups VALUES (5, 'PTM-engineer');
INSERT INTO groups VALUES (6, 'PTM-approver');
INSERT INTO groups VALUES (7, 'PTM-prippublic');
INSERT INTO groups VALUES (8, 'PTM-externalprocessor');
INSERT INTO groups VALUES (9, 'PTM-internalprocessor');


--
-- Data for Name: groups_group_members; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO groups_group_members VALUES (3, 10);
INSERT INTO groups_group_members VALUES (4, 11);
INSERT INTO groups_group_members VALUES (5, 12);
INSERT INTO groups_group_members VALUES (6, 13);
INSERT INTO groups_group_members VALUES (7, 14);
INSERT INTO groups_group_members VALUES (8, 15);
INSERT INTO groups_group_members VALUES (9, 16);


--
-- Data for Name: input_filter; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO input_filter VALUES (77, 1);
INSERT INTO input_filter VALUES (78, 1);
INSERT INTO input_filter VALUES (81, 1);
INSERT INTO input_filter VALUES (82, 1);


--
-- Data for Name: input_filter_filter_conditions; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO input_filter_filter_conditions VALUES (77, NULL, 'STRING', 'OPER', 'fileClass');
INSERT INTO input_filter_filter_conditions VALUES (77, NULL, 'INTEGER', '1', 'revision');
INSERT INTO input_filter_filter_conditions VALUES (78, NULL, 'STRING', 'TEST', 'fileClass');
INSERT INTO input_filter_filter_conditions VALUES (78, NULL, 'INTEGER', '2', 'revision');
INSERT INTO input_filter_filter_conditions VALUES (81, NULL, 'STRING', 'TEST', 'fileClass');
INSERT INTO input_filter_filter_conditions VALUES (81, NULL, 'INTEGER', '99', 'revision');
INSERT INTO input_filter_filter_conditions VALUES (82, NULL, 'STRING', 'TEST', 'fileClass');
INSERT INTO input_filter_filter_conditions VALUES (82, NULL, 'INTEGER', '99', 'revision');


--
-- Data for Name: job; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: job_step; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: job_step_output_parameters; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: mission; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO mission VALUES (1, 1, 'PTM', 'prosEO Test Mission', NULL, 'PTM-PDGS', '17473', 2592000000000000);


--
-- Data for Name: mission_file_classes; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO mission_file_classes VALUES (1, 'TEST');
INSERT INTO mission_file_classes VALUES (1, 'OPER');


--
-- Data for Name: mission_processing_modes; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO mission_processing_modes VALUES (1, 'OPER');


--
-- Data for Name: mon_ext_service; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: mon_ext_service_state_operation; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: mon_ext_service_state_operation_day; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: mon_ext_service_state_operation_month; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: mon_order_state; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: mon_product_production_day; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: mon_product_production_hour; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: mon_product_production_month; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: mon_service; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: mon_service_state; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO mon_service_state VALUES (1, 1, 'running');
INSERT INTO mon_service_state VALUES (2, 1, 'stopped');
INSERT INTO mon_service_state VALUES (3, 1, 'starting');
INSERT INTO mon_service_state VALUES (4, 1, 'stopping');
INSERT INTO mon_service_state VALUES (5, 1, 'degraded');


--
-- Data for Name: mon_service_state_operation; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: mon_service_state_operation_day; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: mon_service_state_operation_month; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: orbit; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO orbit VALUES (17, 1, 3000, '2019-11-04 09:00:00.2', '2019-11-04 10:41:10.3', 2);
INSERT INTO orbit VALUES (18, 1, 3001, '2019-11-04 10:41:10.3', '2019-11-04 12:22:20.4', 2);
INSERT INTO orbit VALUES (19, 1, 3002, '2019-11-04 12:22:20.4', '2019-11-04 14:03:30.5', 2);
INSERT INTO orbit VALUES (20, 1, 3003, '2019-11-04 14:03:30.5', '2019-11-04 15:44:40.6', 2);
INSERT INTO orbit VALUES (21, 1, 3004, '2019-11-04 15:44:40.6', '2019-11-04 17:25:50.7', 2);
INSERT INTO orbit VALUES (22, 1, 3005, '2019-11-04 17:25:50.7', '2019-11-04 19:07:00.8', 2);


--
-- Data for Name: processing_facility; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO processing_facility VALUES (61, 2, 'S3', 'Docker Desktop Minikube', 'http://192.168.20.155:8083/proseo/storage-mgr/v0.1', 'STOPPED', 'http://192.168.20.155:8083/proseo/storage-mgr/v0.1', 1, 'localhost', '17482', 'https://kubernetes.docker.internal:6443', 'pw', 'http://192.168.20.155:8083/proseo/storage-mgr/v0.1', 'user');


--
-- Data for Name: processing_order; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO processing_order VALUES (80, 1, NULL, NULL, NULL, NULL, false, 'L2_orbits_3000-3002', NULL, NULL, NULL, NULL, NULL, NULL, 'OTHER', 'INITIAL', 'TEST', 50, 'OPER', NULL, 'ON_DEMAND_DEFAULT', NULL, NULL, 0, 'ORBIT', '2019-11-04 09:00:00.2', NULL, '2019-11-04 14:03:30.5', NULL, '99c9d76f-8a1e-465b-977f-464b97b95450', 1, NULL);
INSERT INTO processing_order VALUES (83, 1, NULL, NULL, NULL, NULL, false, 'L3_products_9:30-17:30', NULL, NULL, NULL, NULL, NULL, NULL, 'OTHER', 'INITIAL', 'TEST', 50, 'OPER', NULL, 'ON_DEMAND_DEFAULT', NULL, 14400000000000, 0, 'TIME_SLICE', '2019-11-04 09:30:00', NULL, '2019-11-04 17:00:00', NULL, 'b45ec444-e132-46ed-b588-44b3caca55bc', 1, NULL);


--
-- Data for Name: processing_order_class_output_parameters; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO processing_order_class_output_parameters VALUES (80, 79, 42);


--
-- Data for Name: processing_order_dynamic_processing_parameters; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: processing_order_input_filters; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO processing_order_input_filters VALUES (80, 78, 42);
INSERT INTO processing_order_input_filters VALUES (80, 77, 39);
INSERT INTO processing_order_input_filters VALUES (83, 81, 44);
INSERT INTO processing_order_input_filters VALUES (83, 82, 45);


--
-- Data for Name: processing_order_input_product_classes; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO processing_order_input_product_classes VALUES (83, 44);
INSERT INTO processing_order_input_product_classes VALUES (83, 45);


--
-- Data for Name: processing_order_mon_order_progress; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: processing_order_output_parameters; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO processing_order_output_parameters VALUES (80, NULL, 'INTEGER', '77', 'copernicusCollection');
INSERT INTO processing_order_output_parameters VALUES (80, NULL, 'INTEGER', '99', 'revision');
INSERT INTO processing_order_output_parameters VALUES (83, NULL, 'INTEGER', '77', 'copernicusCollection');
INSERT INTO processing_order_output_parameters VALUES (83, NULL, 'INTEGER', '99', 'revision');


--
-- Data for Name: processing_order_requested_configured_processors; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO processing_order_requested_configured_processors VALUES (80, 37);
INSERT INTO processing_order_requested_configured_processors VALUES (83, 38);


--
-- Data for Name: processing_order_requested_orbits; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO processing_order_requested_orbits VALUES (80, 17);
INSERT INTO processing_order_requested_orbits VALUES (80, 18);
INSERT INTO processing_order_requested_orbits VALUES (80, 19);


--
-- Data for Name: processing_order_requested_product_classes; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO processing_order_requested_product_classes VALUES (80, 44);
INSERT INTO processing_order_requested_product_classes VALUES (80, 45);
INSERT INTO processing_order_requested_product_classes VALUES (83, 46);


--
-- Data for Name: processor; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO processor VALUES (26, 1, 'localhost:5000/proseo-sample-wrapper:0.9.5-SNAPSHOT', false, 'MMFI_1_8', 0, 1024, '0.1.0', true, false, 23);
INSERT INTO processor VALUES (28, 1, 'localhost:5000/proseo-sample-wrapper:0.9.5-SNAPSHOT', false, 'MMFI_1_8', 0, 1024, '0.1.0', true, false, 24);
INSERT INTO processor VALUES (30, 1, 'localhost:5000/proseo-sample-wrapper:0.9.5-SNAPSHOT', false, 'MMFI_1_8', 0, 1024, '0.1.0', true, false, 25);


--
-- Data for Name: processor_class; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO processor_class VALUES (23, 1, 'PTML1B', 1);
INSERT INTO processor_class VALUES (24, 1, 'PTML2', 1);
INSERT INTO processor_class VALUES (25, 1, 'PTML3', 1);


--
-- Data for Name: processor_docker_run_parameters; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: product; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO product VALUES (62, 1, NULL, 'OPER', '2019-11-04 12:00:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 09:00:00', '2019-11-04 09:45:00', '861fbeb0-792a-4d59-a961-6709879271af', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (63, 1, NULL, 'OPER', '2019-11-04 12:01:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 09:45:00', '2019-11-04 10:30:00', 'e8c702c5-06e6-4cee-af5d-068d060c63b8', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (64, 1, NULL, 'OPER', '2019-11-04 12:02:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 10:30:00', '2019-11-04 11:15:00', '438c0146-d410-4016-8262-1d605ea20554', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (65, 1, NULL, 'OPER', '2019-11-04 12:03:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 11:15:00', '2019-11-04 12:00:00', '7f684ec1-e4d6-4cc5-aaa3-45afe4b80587', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (66, 1, NULL, 'OPER', '2019-11-04 15:00:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 12:00:00', '2019-11-04 12:45:00', '86c7ea7f-d9e1-4ff4-a41a-51cc4bb8ab83', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (67, 1, NULL, 'OPER', '2019-11-04 15:01:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 12:45:00', '2019-11-04 13:30:00', 'a0c196bf-6d03-442a-b271-049b80d12662', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (68, 1, NULL, 'OPER', '2019-11-04 15:02:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 13:30:00', '2019-11-04 14:15:00', 'b658c7e6-3bc3-435b-afb0-1f869d01b2c7', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (69, 1, NULL, 'OPER', '2019-11-04 15:03:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 14:15:00', '2019-11-04 15:00:00', '42cccd71-82da-4e15-b2fd-df9434c5caba', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (70, 1, NULL, 'OPER', '2019-11-04 18:00:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 15:00:00', '2019-11-04 15:45:00', 'd473b129-4c97-4502-9086-4d78d05ef7ad', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (71, 1, NULL, 'OPER', '2019-11-04 18:01:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 15:45:00', '2019-11-04 16:30:00', '4e63c63e-454f-40e4-8759-5a169c681cc6', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (72, 1, NULL, 'OPER', '2019-11-04 18:02:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 16:30:00', '2019-11-04 17:15:00', '0a968378-5048-47a7-89f8-63ca8daee116', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (73, 1, NULL, 'OPER', '2019-11-04 18:03:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 17:15:00', '2019-11-04 18:00:00', '8619500b-2a42-47dd-b37c-9ff9e53b1cad', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (74, 1, NULL, 'OPER', '2019-11-04 21:00:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 18:00:00', '2019-11-04 18:45:00', 'c994209c-7a6c-439e-b1d0-2c3ce7cca22f', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (75, 1, NULL, 'OPER', '2019-11-04 21:01:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-11-04 18:45:00', '2019-11-04 19:30:00', '37a8f5ec-161f-463e-8c9a-cfdbf29f513b', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (76, 1, NULL, 'OPER', '2019-10-01 12:00:00', 'OPER', 'TEST', NULL, NULL, NULL, NULL, NULL, '2019-09-01 00:00:00', '2019-10-01 00:00:00', 'f2ac4e68-6c52-4b9c-8fab-044f91cc66bf', NULL, NULL, NULL, NULL, 40);
INSERT INTO product VALUES (99, 1, NULL, 'OPER', '2019-11-04 12:00:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.514', NULL, NULL, NULL, '2019-11-04 09:00:00', '2019-11-04 09:45:00', '74cc4c7f-449a-4857-95ac-70c8ab301e72', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (100, 1, NULL, 'OPER', '2019-11-04 12:01:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.525', NULL, NULL, NULL, '2019-11-04 09:45:00', '2019-11-04 10:30:00', '3e8c96dd-a544-4565-9144-6d810271e0ac', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (101, 1, NULL, 'OPER', '2019-11-04 12:02:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.532', NULL, NULL, NULL, '2019-11-04 10:30:00', '2019-11-04 11:15:00', 'e5ef3f55-dcf8-4691-8d50-acca95edacbf', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (102, 1, NULL, 'OPER', '2019-11-04 12:03:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.54', NULL, NULL, NULL, '2019-11-04 11:15:00', '2019-11-04 12:00:00', '0c949449-e344-45af-85e4-8fa08d7b0f26', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (103, 1, NULL, 'OPER', '2019-11-04 15:00:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.546', NULL, NULL, NULL, '2019-11-04 12:00:00', '2019-11-04 12:45:00', 'eabb977c-f526-4404-a7f4-3941abe87840', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (104, 1, NULL, 'OPER', '2019-11-04 15:01:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.554', NULL, NULL, NULL, '2019-11-04 12:45:00', '2019-11-04 13:30:00', 'c68f0f93-7d0f-421e-ad73-65a074893c37', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (105, 1, NULL, 'OPER', '2019-11-04 15:02:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.56', NULL, NULL, NULL, '2019-11-04 13:30:00', '2019-11-04 14:15:00', '9b5a84bf-4fbe-4e15-9bf2-b73f36c465bb', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (106, 1, NULL, 'OPER', '2019-11-04 15:03:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.565', NULL, NULL, NULL, '2019-11-04 14:15:00', '2019-11-04 15:00:00', '496ca4c6-a569-4790-b244-c655c5bc107c', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (107, 1, NULL, 'OPER', '2019-11-04 18:00:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.572', NULL, NULL, NULL, '2019-11-04 15:00:00', '2019-11-04 15:45:00', 'aecac1e3-de19-4f1e-9dd2-8e3fcdfdf35e', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (108, 1, NULL, 'OPER', '2019-11-04 18:01:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.578', NULL, NULL, NULL, '2019-11-04 15:45:00', '2019-11-04 16:30:00', '00d8ab7a-95c9-412d-a2a1-0c685333b17a', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (109, 1, NULL, 'OPER', '2019-11-04 18:02:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.584', NULL, NULL, NULL, '2019-11-04 16:30:00', '2019-11-04 17:15:00', 'b61bf9a0-0b41-4982-b39e-beffc84995a0', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (110, 1, NULL, 'OPER', '2019-11-04 18:03:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.59', NULL, NULL, NULL, '2019-11-04 17:15:00', '2019-11-04 18:00:00', 'e98bd550-c680-44b2-8af9-62ac5f005fad', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (111, 1, NULL, 'OPER', '2019-11-04 21:00:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.596', NULL, NULL, NULL, '2019-11-04 18:00:00', '2019-11-04 18:45:00', 'dbdd8eb8-34d1-40a4-a789-b7ccff9e7710', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (112, 1, NULL, 'OPER', '2019-11-04 21:01:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.601', NULL, NULL, NULL, '2019-11-04 18:45:00', '2019-11-04 19:30:00', 'de322a30-6452-48bc-add0-9b879bb1c0ee', NULL, NULL, NULL, NULL, 39);
INSERT INTO product VALUES (113, 1, NULL, 'OPER', '2019-10-01 12:00:00', 'OPER', 'TEST', NULL, '2024-01-26 08:09:21.608', NULL, NULL, NULL, '2019-09-01 00:00:00', '2019-10-01 00:00:00', '9432d65e-c55e-46a8-b51d-af8bc7cf77f5', NULL, NULL, NULL, NULL, 40);


--
-- Data for Name: product_archive; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: product_archive_available_product_classes; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: product_class; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO product_class VALUES (39, 1, NULL, NULL, NULL, NULL, NULL, 'PTM_L0', 'RESTRICTED', NULL, 1, NULL);
INSERT INTO product_class VALUES (40, 1, NULL, NULL, NULL, NULL, NULL, 'AUX_IERS_B', 'INTERNAL', NULL, 1, NULL);
INSERT INTO product_class VALUES (41, 1, NULL, 'ORBIT', NULL, 'L1B', NULL, 'PTM_L1B', 'PUBLIC', NULL, 1, 23);
INSERT INTO product_class VALUES (42, 1, NULL, NULL, NULL, 'L1B', NULL, 'PTM_L1B_P1', 'PUBLIC', 41, 1, NULL);
INSERT INTO product_class VALUES (43, 1, NULL, NULL, NULL, 'L1B', NULL, 'PTM_L1B_P2', 'PUBLIC', 41, 1, NULL);
INSERT INTO product_class VALUES (44, 1, NULL, 'ORBIT', NULL, 'L2A', NULL, 'PTM_L2_A', 'PUBLIC', NULL, 1, 24);
INSERT INTO product_class VALUES (45, 1, NULL, 'ORBIT', NULL, 'L2B', NULL, 'PTM_L2_B', 'PUBLIC', NULL, 1, 24);
INSERT INTO product_class VALUES (46, 1, 14400000000000, 'TIME_SLICE', NULL, 'L3', NULL, 'PTM_L3', 'PUBLIC', NULL, 1, 25);


--
-- Data for Name: product_download_history; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: product_file; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO product_file VALUES (114, 1, '50545d8ad8486dd9297014cf2e769f71', '2019-11-04 12:00:10', 's3://proseo-data-001/99', 84, 'PTM_L0_20191104090000_20191104094500_20191104120000.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 99);
INSERT INTO product_file VALUES (115, 1, '3ca720d0d2a7dee0ba95a9b50047b5a0', '2019-11-04 12:01:10', 's3://proseo-data-001/100', 84, 'PTM_L0_20191104094500_20191104103000_20191104120100.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 100);
INSERT INTO product_file VALUES (116, 1, 'f65f2591c400a85ced1d7557a6644138', '2019-11-04 12:02:10', 's3://proseo-data-001/101', 84, 'PTM_L0_20191104103000_20191104111500_20191104120200.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 101);
INSERT INTO product_file VALUES (117, 1, '58a029e6b09ad1e749fc1ff523dbbf80', '2019-11-04 12:03:10', 's3://proseo-data-001/102', 84, 'PTM_L0_20191104111500_20191104120000_20191104120300.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 102);
INSERT INTO product_file VALUES (118, 1, '9ecc9bbc7abb0672efd51a39e8fc2c59', '2019-11-04 15:00:10', 's3://proseo-data-001/103', 84, 'PTM_L0_20191104120000_20191104124500_20191104150000.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 103);
INSERT INTO product_file VALUES (119, 1, 'fb720888a0d9bae6b16c1f9607c4de27', '2019-11-04 15:01:10', 's3://proseo-data-001/104', 84, 'PTM_L0_20191104124500_20191104133000_20191104150100.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 104);
INSERT INTO product_file VALUES (120, 1, '4ef20d31f2e051d16cf06db2bae2c76e', '2019-11-04 15:02:10', 's3://proseo-data-001/105', 84, 'PTM_L0_20191104133000_20191104141500_20191104150200.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 105);
INSERT INTO product_file VALUES (121, 1, '8b3a2f2f386c683ce9b1c68bb52b34d2', '2019-11-04 15:03:10', 's3://proseo-data-001/106', 84, 'PTM_L0_20191104141500_20191104150000_20191104150300.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 106);
INSERT INTO product_file VALUES (122, 1, '6bea82661ad200b8dc8a912bbc9c89f6', '2019-11-04 18:00:10', 's3://proseo-data-001/107', 84, 'PTM_L0_20191104150000_20191104154500_20191104180000.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 107);
INSERT INTO product_file VALUES (123, 1, 'a1953fa0c117f803c26902243bb0d3aa', '2019-11-04 18:01:10', 's3://proseo-data-001/108', 84, 'PTM_L0_20191104154500_20191104163000_20191104180100.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 108);
INSERT INTO product_file VALUES (124, 1, 'a9fb7fc052b5aced6f6dc9d753bbe790', '2019-11-04 18:02:10', 's3://proseo-data-001/109', 84, 'PTM_L0_20191104163000_20191104171500_20191104180200.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 109);
INSERT INTO product_file VALUES (125, 1, 'b8b5304d83100a56114cd5ca6a6bc581', '2019-11-04 18:03:10', 's3://proseo-data-001/110', 84, 'PTM_L0_20191104171500_20191104180000_20191104180300.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 110);
INSERT INTO product_file VALUES (126, 1, '9211729e47c5e2487de302f1ca48f4b9', '2019-11-04 21:00:10', 's3://proseo-data-001/111', 84, 'PTM_L0_20191104180000_20191104184500_20191104210000.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 111);
INSERT INTO product_file VALUES (127, 1, '95eb634992099c7ebb7bf5b76b243da1', '2019-11-04 21:01:10', 's3://proseo-data-001/112', 84, 'PTM_L0_20191104184500_20191104193000_20191104210100.RAW', 'S3', NULL, NULL, NULL, NULL, 61, 112);
INSERT INTO product_file VALUES (128, 1, 'b754f424e3dad8f1c107ed6b8ad9d06a', '2019-10-01 12:01:00', 's3://proseo-data-001/113', 51090, 'bulletinb-380.xml', 'S3', NULL, NULL, NULL, NULL, 61, 113);


--
-- Data for Name: product_file_aux_file_names; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: product_parameters; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO product_parameters VALUES (62, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (63, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (64, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (65, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (66, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (67, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (68, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (69, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (70, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (71, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (72, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (73, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (74, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (75, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (76, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (99, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (100, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (101, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (102, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (103, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (104, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (105, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (106, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (107, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (108, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (109, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (110, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (111, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (112, NULL, 'INTEGER', '1', 'revision');
INSERT INTO product_parameters VALUES (113, NULL, 'INTEGER', '1', 'revision');


--
-- Data for Name: product_query; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: product_query_filter_conditions; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: product_query_satisfying_products; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: simple_policy; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO simple_policy VALUES (48, 1, 'LatestValIntersect');
INSERT INTO simple_policy VALUES (50, 1, 'ValIntersect');
INSERT INTO simple_policy VALUES (52, 1, 'LatestValCover');
INSERT INTO simple_policy VALUES (54, 1, 'LatestValCover');
INSERT INTO simple_policy VALUES (56, 1, 'ValIntersect');
INSERT INTO simple_policy VALUES (58, 1, 'ValIntersect');


--
-- Data for Name: simple_policy_delta_times; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO simple_policy_delta_times VALUES (48, 60, 6, 0);
INSERT INTO simple_policy_delta_times VALUES (48, 60, 6, 1);
INSERT INTO simple_policy_delta_times VALUES (50, 0, 6, 0);
INSERT INTO simple_policy_delta_times VALUES (50, 0, 6, 1);
INSERT INTO simple_policy_delta_times VALUES (52, 0, 6, 0);
INSERT INTO simple_policy_delta_times VALUES (52, 0, 6, 1);
INSERT INTO simple_policy_delta_times VALUES (54, 0, 6, 0);
INSERT INTO simple_policy_delta_times VALUES (54, 0, 6, 1);
INSERT INTO simple_policy_delta_times VALUES (56, 0, 6, 0);
INSERT INTO simple_policy_delta_times VALUES (56, 0, 6, 1);
INSERT INTO simple_policy_delta_times VALUES (58, 0, 6, 0);
INSERT INTO simple_policy_delta_times VALUES (58, 0, 6, 1);


--
-- Data for Name: simple_selection_rule; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO simple_selection_rule VALUES (47, 1, 'AUX_IERS_B', true, 0, 'OPER', 40, 41);
INSERT INTO simple_selection_rule VALUES (49, 1, 'PTM_L0', true, 0, 'OPER', 39, 41);
INSERT INTO simple_selection_rule VALUES (51, 1, 'PTM_L1B', true, 0, 'OPER', 41, 44);
INSERT INTO simple_selection_rule VALUES (53, 1, 'PTM_L1B_P1', true, 0, 'OPER', 42, 45);
INSERT INTO simple_selection_rule VALUES (55, 1, 'PTM_L2_B', true, 90, 'OPER', 45, 46);
INSERT INTO simple_selection_rule VALUES (57, 1, 'PTM_L2_A', true, 90, 'OPER', 44, 46);


--
-- Data for Name: simple_selection_rule_configured_processors; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO simple_selection_rule_configured_processors VALUES (47, 36);
INSERT INTO simple_selection_rule_configured_processors VALUES (49, 36);
INSERT INTO simple_selection_rule_configured_processors VALUES (51, 37);
INSERT INTO simple_selection_rule_configured_processors VALUES (53, 37);
INSERT INTO simple_selection_rule_configured_processors VALUES (55, 38);
INSERT INTO simple_selection_rule_configured_processors VALUES (57, 38);


--
-- Data for Name: simple_selection_rule_filter_conditions; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: simple_selection_rule_simple_policies; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO simple_selection_rule_simple_policies VALUES (47, 48);
INSERT INTO simple_selection_rule_simple_policies VALUES (49, 50);
INSERT INTO simple_selection_rule_simple_policies VALUES (51, 52);
INSERT INTO simple_selection_rule_simple_policies VALUES (53, 54);
INSERT INTO simple_selection_rule_simple_policies VALUES (55, 56);
INSERT INTO simple_selection_rule_simple_policies VALUES (57, 58);


--
-- Data for Name: spacecraft; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO spacecraft VALUES (2, 1, 'PTS', 'prosEO Test Satellite', 1);


--
-- Data for Name: spacecraft_payloads; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO spacecraft_payloads VALUES (2, 'Super sensor for PTM', 'PTS-SENSOR');


--
-- Data for Name: task; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO task VALUES (27, 1, 10, true, NULL, 3, 'ptm_l01b', '0.1.0', 26);
INSERT INTO task VALUES (29, 1, 10, true, NULL, 3, 'ptm_l2', '0.1.0', 28);
INSERT INTO task VALUES (31, 1, 10, true, NULL, 3, 'ptm_l3', '0.1.0', 30);


--
-- Data for Name: task_breakpoint_file_names; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO users VALUES ('sysadm', true, '2124-01-02 07:35:31.034', '$2a$10$PJvCRje33WVan/KymNBipuOPsbSkCUWBQZqy1c4d0T584F4jKjrF2', '2124-01-02 07:35:31.034', NULL, NULL, NULL);
INSERT INTO users VALUES ('PTM-usermgr', true, '2124-01-02 07:50:22.461', '$2a$10$loQW4zayaJx.g/zVhjcu2eU9jXGv3edOm9TKp7rUL/UiY8zVXXDwC', '2124-01-02 07:50:22.461', NULL, NULL, NULL);
INSERT INTO users VALUES ('PTM-proseo', true, '2124-01-02 07:50:22.695', '$2a$10$vhVovJnhUSZHZ/VJIHF8kulOBzu7xujnfTbuqf/vJNiYlJPCnoMlC', '2124-01-02 07:50:22.695', NULL, NULL, NULL);
INSERT INTO users VALUES ('PTM-sciuserprip', true, '2124-01-02 07:50:22.832', '$2a$10$1tIPJXMa4YW/bLCsFDLnlePeOwIBsbmanSuXKaUIRGhmheHFA4T5S', '2124-01-02 07:50:22.832', NULL, NULL, NULL);
INSERT INTO users VALUES ('PTM-cfidevprip', true, '2124-01-02 07:50:22.964', '$2a$10$/dEhGP/7IW352lJUH/8jWeHFHO.R3dNlmDMK5sGlwngv8HfFHHxaS', '2124-01-02 07:50:22.964', NULL, NULL, NULL);
INSERT INTO users VALUES ('PTM-wrapper', true, '2124-01-02 07:50:23.098', '$2a$10$m2ogJ8sqX4vUdm8xwpa9ge7EOBll4KVIiCJ1rKOnTdHfJnM7lVrHC', '2124-01-02 07:50:23.098', NULL, NULL, NULL);


--
-- Data for Name: users_group_memberships; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO users_group_memberships VALUES ('PTM-proseo', 10);
INSERT INTO users_group_memberships VALUES ('PTM-proseo', 11);
INSERT INTO users_group_memberships VALUES ('PTM-proseo', 12);
INSERT INTO users_group_memberships VALUES ('PTM-proseo', 13);
INSERT INTO users_group_memberships VALUES ('PTM-sciuserprip', 14);
INSERT INTO users_group_memberships VALUES ('PTM-cfidevprip', 15);
INSERT INTO users_group_memberships VALUES ('PTM-wrapper', 16);


--
-- Data for Name: workflow; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO workflow VALUES (59, 1, 'Create level 3 products from level 2', true, 'PTML2-to-L3', 'TEST', 'OPER', NULL, 0, 'NONE', 'd4466bcb-3256-416a-9d9c-dc86592e43bb', '1.0', 38, 44, 1, 46);


--
-- Data for Name: workflow_class_output_parameters; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: workflow_input_filters; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: workflow_option; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO workflow_option VALUES (60, 1, '16', '', 'Threads', 'NUMBER', 59);


--
-- Data for Name: workflow_option_value_range; Type: TABLE DATA; Schema: public; Owner: postgres
--



--
-- Data for Name: workflow_output_parameters; Type: TABLE DATA; Schema: public; Owner: postgres
--

INSERT INTO workflow_output_parameters VALUES (59, NULL, 'INTEGER', '99', 'revision');


--
-- Name: hibernate_sequence; Type: SEQUENCE SET; Schema: public; Owner: postgres
--


--
-- Data for Name: BLOBS; Type: BLOBS; Schema: -; Owner: 
--

--
-- Name: authorities authorities_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  authorities
    ADD CONSTRAINT authorities_pkey PRIMARY KEY (username, authority);


--
-- Name: class_output_parameter_output_parameters class_output_parameter_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  class_output_parameter_output_parameters
    ADD CONSTRAINT class_output_parameter_output_parameters_pkey PRIMARY KEY (class_output_parameter_id, output_parameters_key);


--
-- Name: class_output_parameter class_output_parameter_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  class_output_parameter
    ADD CONSTRAINT class_output_parameter_pkey PRIMARY KEY (id);


--
-- Name: configuration_docker_run_parameters configuration_docker_run_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration_docker_run_parameters
    ADD CONSTRAINT configuration_docker_run_parameters_pkey PRIMARY KEY (configuration_id, docker_run_parameters_key);


--
-- Name: configuration_dyn_proc_parameters configuration_dyn_proc_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration_dyn_proc_parameters
    ADD CONSTRAINT configuration_dyn_proc_parameters_pkey PRIMARY KEY (configuration_id, dyn_proc_parameters_key);


--
-- Name: configuration_input_file configuration_input_file_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration_input_file
    ADD CONSTRAINT configuration_input_file_pkey PRIMARY KEY (id);


--
-- Name: configuration configuration_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration
    ADD CONSTRAINT configuration_pkey PRIMARY KEY (id);


--
-- Name: configuration_static_input_files configuration_static_input_files_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration_static_input_files
    ADD CONSTRAINT configuration_static_input_files_pkey PRIMARY KEY (configuration_id, static_input_files_id);


--
-- Name: configured_processor configured_processor_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configured_processor
    ADD CONSTRAINT configured_processor_pkey PRIMARY KEY (id);


--
-- Name: group_authorities group_authorities_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  group_authorities
    ADD CONSTRAINT group_authorities_pkey PRIMARY KEY (group_id, authority);


--
-- Name: group_members group_members_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  group_members
    ADD CONSTRAINT group_members_pkey PRIMARY KEY (id);


--
-- Name: groups_group_members groups_group_members_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  groups_group_members
    ADD CONSTRAINT groups_group_members_pkey PRIMARY KEY (groups_id, group_members_id);


--
-- Name: groups groups_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  groups
    ADD CONSTRAINT groups_pkey PRIMARY KEY (id);


--
-- Name: input_filter_filter_conditions input_filter_filter_conditions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  input_filter_filter_conditions
    ADD CONSTRAINT input_filter_filter_conditions_pkey PRIMARY KEY (input_filter_id, filter_conditions_key);


--
-- Name: input_filter input_filter_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  input_filter
    ADD CONSTRAINT input_filter_pkey PRIMARY KEY (id);


--
-- Name: job job_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  job
    ADD CONSTRAINT job_pkey PRIMARY KEY (id);


--
-- Name: job_step_output_parameters job_step_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  job_step_output_parameters
    ADD CONSTRAINT job_step_output_parameters_pkey PRIMARY KEY (job_step_id, output_parameters_key);


--
-- Name: job_step job_step_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  job_step
    ADD CONSTRAINT job_step_pkey PRIMARY KEY (id);


--
-- Name: mission mission_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mission
    ADD CONSTRAINT mission_pkey PRIMARY KEY (id);


--
-- Name: mon_ext_service mon_ext_service_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_ext_service
    ADD CONSTRAINT mon_ext_service_pkey PRIMARY KEY (id);


--
-- Name: mon_ext_service_state_operation_day mon_ext_service_state_operation_day_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_ext_service_state_operation_day
    ADD CONSTRAINT mon_ext_service_state_operation_day_pkey PRIMARY KEY (id);


--
-- Name: mon_ext_service_state_operation_month mon_ext_service_state_operation_month_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_ext_service_state_operation_month
    ADD CONSTRAINT mon_ext_service_state_operation_month_pkey PRIMARY KEY (id);


--
-- Name: mon_ext_service_state_operation mon_ext_service_state_operation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_ext_service_state_operation
    ADD CONSTRAINT mon_ext_service_state_operation_pkey PRIMARY KEY (id);


--
-- Name: mon_order_state mon_order_state_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_order_state
    ADD CONSTRAINT mon_order_state_pkey PRIMARY KEY (id);


--
-- Name: mon_product_production_day mon_product_production_day_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_product_production_day
    ADD CONSTRAINT mon_product_production_day_pkey PRIMARY KEY (id);


--
-- Name: mon_product_production_hour mon_product_production_hour_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_product_production_hour
    ADD CONSTRAINT mon_product_production_hour_pkey PRIMARY KEY (id);


--
-- Name: mon_product_production_month mon_product_production_month_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_product_production_month
    ADD CONSTRAINT mon_product_production_month_pkey PRIMARY KEY (id);


--
-- Name: mon_service mon_service_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_service
    ADD CONSTRAINT mon_service_pkey PRIMARY KEY (id);


--
-- Name: mon_service_state_operation_day mon_service_state_operation_day_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_service_state_operation_day
    ADD CONSTRAINT mon_service_state_operation_day_pkey PRIMARY KEY (id);


--
-- Name: mon_service_state_operation_month mon_service_state_operation_month_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_service_state_operation_month
    ADD CONSTRAINT mon_service_state_operation_month_pkey PRIMARY KEY (id);


--
-- Name: mon_service_state_operation mon_service_state_operation_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_service_state_operation
    ADD CONSTRAINT mon_service_state_operation_pkey PRIMARY KEY (id);


--
-- Name: mon_service_state mon_service_state_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_service_state
    ADD CONSTRAINT mon_service_state_pkey PRIMARY KEY (id);


--
-- Name: orbit orbit_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  orbit
    ADD CONSTRAINT orbit_pkey PRIMARY KEY (id);


--
-- Name: processing_facility processing_facility_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_facility
    ADD CONSTRAINT processing_facility_pkey PRIMARY KEY (id);


--
-- Name: processing_order_class_output_parameters processing_order_class_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_class_output_parameters
    ADD CONSTRAINT processing_order_class_output_parameters_pkey PRIMARY KEY (processing_order_id, class_output_parameters_key);


--
-- Name: processing_order_dynamic_processing_parameters processing_order_dynamic_processing_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_dynamic_processing_parameters
    ADD CONSTRAINT processing_order_dynamic_processing_parameters_pkey PRIMARY KEY (processing_order_id, dynamic_processing_parameters_key);


--
-- Name: processing_order_input_filters processing_order_input_filters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_input_filters
    ADD CONSTRAINT processing_order_input_filters_pkey PRIMARY KEY (processing_order_id, input_filters_key);


--
-- Name: processing_order_input_product_classes processing_order_input_product_classes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_input_product_classes
    ADD CONSTRAINT processing_order_input_product_classes_pkey PRIMARY KEY (processing_order_id, input_product_classes_id);


--
-- Name: processing_order_output_parameters processing_order_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_output_parameters
    ADD CONSTRAINT processing_order_output_parameters_pkey PRIMARY KEY (processing_order_id, output_parameters_key);


--
-- Name: processing_order processing_order_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order
    ADD CONSTRAINT processing_order_pkey PRIMARY KEY (id);


--
-- Name: processing_order_requested_configured_processors processing_order_requested_configured_processors_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_requested_configured_processors
    ADD CONSTRAINT processing_order_requested_configured_processors_pkey PRIMARY KEY (processing_order_id, requested_configured_processors_id);


--
-- Name: processing_order_requested_product_classes processing_order_requested_product_classes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_requested_product_classes
    ADD CONSTRAINT processing_order_requested_product_classes_pkey PRIMARY KEY (processing_order_id, requested_product_classes_id);


--
-- Name: processor_class processor_class_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processor_class
    ADD CONSTRAINT processor_class_pkey PRIMARY KEY (id);


--
-- Name: processor_docker_run_parameters processor_docker_run_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processor_docker_run_parameters
    ADD CONSTRAINT processor_docker_run_parameters_pkey PRIMARY KEY (processor_id, docker_run_parameters_key);


--
-- Name: processor processor_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processor
    ADD CONSTRAINT processor_pkey PRIMARY KEY (id);


--
-- Name: product_archive_available_product_classes product_archive_available_product_classes_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_archive_available_product_classes
    ADD CONSTRAINT product_archive_available_product_classes_pkey PRIMARY KEY (product_archive_id, available_product_classes_id);


--
-- Name: product_archive product_archive_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_archive
    ADD CONSTRAINT product_archive_pkey PRIMARY KEY (id);


--
-- Name: product_class product_class_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_class
    ADD CONSTRAINT product_class_pkey PRIMARY KEY (id);


--
-- Name: product_file product_file_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_file
    ADD CONSTRAINT product_file_pkey PRIMARY KEY (id);


--
-- Name: product_parameters product_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_parameters
    ADD CONSTRAINT product_parameters_pkey PRIMARY KEY (product_id, parameters_key);


--
-- Name: product product_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product
    ADD CONSTRAINT product_pkey PRIMARY KEY (id);


--
-- Name: product_query_filter_conditions product_query_filter_conditions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_query_filter_conditions
    ADD CONSTRAINT product_query_filter_conditions_pkey PRIMARY KEY (product_query_id, filter_conditions_key);


--
-- Name: product_query product_query_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_query
    ADD CONSTRAINT product_query_pkey PRIMARY KEY (id);


--
-- Name: product_query_satisfying_products product_query_satisfying_products_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_query_satisfying_products
    ADD CONSTRAINT product_query_satisfying_products_pkey PRIMARY KEY (satisfied_product_queries_id, satisfying_products_id);


--
-- Name: simple_policy_delta_times simple_policy_delta_times_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_policy_delta_times
    ADD CONSTRAINT simple_policy_delta_times_pkey PRIMARY KEY (simple_policy_id, list_index);


--
-- Name: simple_policy simple_policy_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_policy
    ADD CONSTRAINT simple_policy_pkey PRIMARY KEY (id);


--
-- Name: simple_selection_rule_configured_processors simple_selection_rule_configured_processors_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_selection_rule_configured_processors
    ADD CONSTRAINT simple_selection_rule_configured_processors_pkey PRIMARY KEY (simple_selection_rule_id, configured_processors_id);


--
-- Name: simple_selection_rule_filter_conditions simple_selection_rule_filter_conditions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_selection_rule_filter_conditions
    ADD CONSTRAINT simple_selection_rule_filter_conditions_pkey PRIMARY KEY (simple_selection_rule_id, filter_conditions_key);


--
-- Name: simple_selection_rule simple_selection_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_selection_rule
    ADD CONSTRAINT simple_selection_rule_pkey PRIMARY KEY (id);


--
-- Name: spacecraft spacecraft_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  spacecraft
    ADD CONSTRAINT spacecraft_pkey PRIMARY KEY (id);


--
-- Name: task task_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  task
    ADD CONSTRAINT task_pkey PRIMARY KEY (id);


--
-- Name: product_archive uk19j4q7qi3o7ln0yucf43cfbps; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_archive
    ADD CONSTRAINT uk19j4q7qi3o7ln0yucf43cfbps UNIQUE (code);


--
-- Name: mon_service uk239unm9hg59upu3be0fkcu4rt; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_service
    ADD CONSTRAINT uk239unm9hg59upu3be0fkcu4rt UNIQUE (name);


--
-- Name: product uk24bc4yyyk3fj3h7ku64i3yuog; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product
    ADD CONSTRAINT uk24bc4yyyk3fj3h7ku64i3yuog UNIQUE (uuid);


--
-- Name: mon_service_state uk447s0lqn0lb4gcho5ciadm0r8; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_service_state
    ADD CONSTRAINT uk447s0lqn0lb4gcho5ciadm0r8 UNIQUE (name);


--
-- Name: configured_processor uk49uwfv9jn1bfagrgu1fmxjlr8; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configured_processor
    ADD CONSTRAINT uk49uwfv9jn1bfagrgu1fmxjlr8 UNIQUE (uuid);


--
-- Name: product_query uk4dkam8lshg1hjjfk4mm4vsp50; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_query
    ADD CONSTRAINT uk4dkam8lshg1hjjfk4mm4vsp50 UNIQUE (job_step_id, requested_product_class_id);


--
-- Name: mon_ext_service uk6iqkhmagcms83dnb123yfd0s2; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_ext_service
    ADD CONSTRAINT uk6iqkhmagcms83dnb123yfd0s2 UNIQUE (name);


--
-- Name: orbit uk6tiqkg4pvqd1iyfmes8t2pd2j; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  orbit
    ADD CONSTRAINT uk6tiqkg4pvqd1iyfmes8t2pd2j UNIQUE (spacecraft_id, orbit_number);


--
-- Name: processing_facility uk8cny9892if8tybde5p5brts6d; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_facility
    ADD CONSTRAINT uk8cny9892if8tybde5p5brts6d UNIQUE (name);


--
-- Name: groups_group_members uk_132lanwqs6liav9syek4s96xv; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  groups_group_members
    ADD CONSTRAINT uk_132lanwqs6liav9syek4s96xv UNIQUE (group_members_id);


--
-- Name: configuration_static_input_files uk_2y140wa1pggeycgihvnex0a9c; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration_static_input_files
    ADD CONSTRAINT uk_2y140wa1pggeycgihvnex0a9c UNIQUE (static_input_files_id);


--
-- Name: simple_selection_rule_simple_policies uk_7jrn9t62kdspngixrembpkrd7; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_selection_rule_simple_policies
    ADD CONSTRAINT uk_7jrn9t62kdspngixrembpkrd7 UNIQUE (simple_policies_id);


--
-- Name: groups uk_7o859iyhxd19rv4hywgdvu2v4; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  groups
    ADD CONSTRAINT uk_7o859iyhxd19rv4hywgdvu2v4 UNIQUE (group_name);


--
-- Name: users_group_memberships uk_e2ijwadyxqhcr2aldhs624px; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  users_group_memberships
    ADD CONSTRAINT uk_e2ijwadyxqhcr2aldhs624px UNIQUE (group_memberships_id);


--
-- Name: processing_order ukbxwgyibx5dkbl26jplnjifrsa; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order
    ADD CONSTRAINT ukbxwgyibx5dkbl26jplnjifrsa UNIQUE (mission_id, identifier);


--
-- Name: product_file ukdawt5bhyxxovxd4vgo4cw6ugn; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_file
    ADD CONSTRAINT ukdawt5bhyxxovxd4vgo4cw6ugn UNIQUE (product_id, processing_facility_id);


--
-- Name: workflow ukg9qjvpphc4d2n8y10mpael4cd; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow
    ADD CONSTRAINT ukg9qjvpphc4d2n8y10mpael4cd UNIQUE (mission_id, name, workflow_version);


--
-- Name: product_class ukgmc9l016fh1mcqque3k8iy3yu; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_class
    ADD CONSTRAINT ukgmc9l016fh1mcqque3k8iy3yu UNIQUE (mission_id, product_type);


--
-- Name: workflow ukhskpb9cv06lohyj8m2ekp9xio; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow
    ADD CONSTRAINT ukhskpb9cv06lohyj8m2ekp9xio UNIQUE (uuid);


--
-- Name: spacecraft uklogt34j6cnrocn49sw0uu30eh; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  spacecraft
    ADD CONSTRAINT uklogt34j6cnrocn49sw0uu30eh UNIQUE (mission_id, code);


--
-- Name: processor_class uknbv3u0tx6s1770tmlvylhodfw; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processor_class
    ADD CONSTRAINT uknbv3u0tx6s1770tmlvylhodfw UNIQUE (mission_id, processor_name);


--
-- Name: mon_order_state ukpm3kggmu7tijpl89jmyjnkg3r; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_order_state
    ADD CONSTRAINT ukpm3kggmu7tijpl89jmyjnkg3r UNIQUE (name);


--
-- Name: processing_order ukqx1os0kwk3rvvoe4bw7inowsq; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order
    ADD CONSTRAINT ukqx1os0kwk3rvvoe4bw7inowsq UNIQUE (uuid);


--
-- Name: workflow_option ukt0udoa7mo0nk3swm1ff30gh5; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_option
    ADD CONSTRAINT ukt0udoa7mo0nk3swm1ff30gh5 UNIQUE (workflow_id, name);


--
-- Name: mission uktio2ulw4k2037685uaayxtuub; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mission
    ADD CONSTRAINT uktio2ulw4k2037685uaayxtuub UNIQUE (code);


--
-- Name: processor uktomhxtld2pvrabtanoq3t3odk; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processor
    ADD CONSTRAINT uktomhxtld2pvrabtanoq3t3odk UNIQUE (processor_class_id, processor_version);


--
-- Name: users_group_memberships users_group_memberships_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  users_group_memberships
    ADD CONSTRAINT users_group_memberships_pkey PRIMARY KEY (users_username, group_memberships_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  users
    ADD CONSTRAINT users_pkey PRIMARY KEY (username);


--
-- Name: workflow_class_output_parameters workflow_class_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_class_output_parameters
    ADD CONSTRAINT workflow_class_output_parameters_pkey PRIMARY KEY (workflow_id, class_output_parameters_key);


--
-- Name: workflow_input_filters workflow_input_filters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_input_filters
    ADD CONSTRAINT workflow_input_filters_pkey PRIMARY KEY (workflow_id, input_filters_key);


--
-- Name: workflow_option workflow_option_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_option
    ADD CONSTRAINT workflow_option_pkey PRIMARY KEY (id);


--
-- Name: workflow_output_parameters workflow_output_parameters_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_output_parameters
    ADD CONSTRAINT workflow_output_parameters_pkey PRIMARY KEY (workflow_id, output_parameters_key);


--
-- Name: workflow workflow_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow
    ADD CONSTRAINT workflow_pkey PRIMARY KEY (id);


--
-- Name: idx2uot336txpqpdo8je8x145a0; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx2uot336txpqpdo8je8x145a0 ON product (publication_time);


--
-- Name: idx4jtdla2jravgeu16yxlv6i1g1; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx4jtdla2jravgeu16yxlv6i1g1 ON mon_product_production_day (mission_id, production_type);


--
-- Name: idx657h07h95ub6nyt49a4wib4ky; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx657h07h95ub6nyt49a4wib4ky ON orbit (start_time);


--
-- Name: idx6u8n1u6c253dps752x6s7yaa7; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx6u8n1u6c253dps752x6s7yaa7 ON product_query (requested_product_class_id);


--
-- Name: idx870rjn0w07u5qc26c4dmfc8p0; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx870rjn0w07u5qc26c4dmfc8p0 ON mon_product_production_month (mission_id, production_type);


--
-- Name: idx8uli6v1risvb0i9offqwpsaag; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx8uli6v1risvb0i9offqwpsaag ON mon_service_state_operation (datetime);


--
-- Name: idx8y2rqfvrrms3xi92l1nq6dm1m; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx8y2rqfvrrms3xi92l1nq6dm1m ON mon_service_state_operation_day (datetime);


--
-- Name: idx99jry69detoongt94mr0cb4jm; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx99jry69detoongt94mr0cb4jm ON job_step (job_step_state);


--
-- Name: idxautube4tmw46joub7nf9qaop2; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxautube4tmw46joub7nf9qaop2 ON mon_ext_service_state_operation_day (datetime);


--
-- Name: idxb1hlhb6srtxd7qpjtkm8a37jg; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxb1hlhb6srtxd7qpjtkm8a37jg ON product (enclosing_product_id);


--
-- Name: idxckdfwsktiy2a7t08oulv9p4bj; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxckdfwsktiy2a7t08oulv9p4bj ON processing_order (execution_time);


--
-- Name: idxdp0vd1b50igr05nxswcxupv6v; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxdp0vd1b50igr05nxswcxupv6v ON product (product_class_id, sensing_start_time);


--
-- Name: idxebh2ci5ivqufcgtxv4gax0mif; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxebh2ci5ivqufcgtxv4gax0mif ON product (product_class_id, generation_time);


--
-- Name: idxf38b0g96ksfrxorqgryoajj7y; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxf38b0g96ksfrxorqgryoajj7y ON mon_product_production_hour (datetime);


--
-- Name: idxgcks0habumx5p6km1njxx4omf; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxgcks0habumx5p6km1njxx4omf ON mon_product_production_day (datetime);


--
-- Name: idxgin74m2ax2pg2c6yu60km46qa; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxgin74m2ax2pg2c6yu60km46qa ON mon_ext_service_state_operation (datetime);


--
-- Name: idxhw65e77rikl7b5qq70e6kjgpg; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxhw65e77rikl7b5qq70e6kjgpg ON job (job_state);


--
-- Name: idxjh81k2qvy1nwnacjhx4c9e8uo; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxjh81k2qvy1nwnacjhx4c9e8uo ON mon_product_production_month (datetime);


--
-- Name: idxjsbb5wmnf9graowd1f9wud96k; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxjsbb5wmnf9graowd1f9wud96k ON mon_ext_service_state_operation (mon_ext_service_id);


--
-- Name: idxl5ejqnuw4rb1sigroxfcwsxwq; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxl5ejqnuw4rb1sigroxfcwsxwq ON mon_service_state_operation (mon_service_state_id);


--
-- Name: idxm1c1ucav1v0gpnn89cjp66b06; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxm1c1ucav1v0gpnn89cjp66b06 ON mon_service_state_operation (mon_service_id);


--
-- Name: idxnl37dyvy1o7gygku42gk4db78; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxnl37dyvy1o7gygku42gk4db78 ON mon_product_production_hour (mission_id, production_type);


--
-- Name: idxojvxy5otq1rh1mu4heg6ny1yn; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxojvxy5otq1rh1mu4heg6ny1yn ON product (product_class_id, sensing_stop_time);


--
-- Name: idxoqh21spbi520l31tsal474r6p; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxoqh21spbi520l31tsal474r6p ON mon_service_state_operation_month (datetime);


--
-- Name: idxqluoxbhk7kihm71dkalmcq1tq; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxqluoxbhk7kihm71dkalmcq1tq ON configured_processor (identifier);


--
-- Name: idxqt20vfwmvgqe74tm87v1jdcj9; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxqt20vfwmvgqe74tm87v1jdcj9 ON mon_ext_service_state_operation_month (datetime);


--
-- Name: idxqu0ou5l3tyyegjvfh0rvb8f4h; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxqu0ou5l3tyyegjvfh0rvb8f4h ON product (eviction_time);


--
-- Name: idxsoap2dcggqp95abimerpm6031; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idxsoap2dcggqp95abimerpm6031 ON mon_ext_service_state_operation (mon_service_state_id);


--
-- Name: product_file_names; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX product_file_names ON product_file (product_file_name);


--
-- Name: product_parameters_values; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX product_parameters_values ON product_parameters (parameters_key, parameter_value);


--
-- Name: configuration_docker_run_parameters fk165qo4rdh6j4v72p19t5rluv3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration_docker_run_parameters
    ADD CONSTRAINT fk165qo4rdh6j4v72p19t5rluv3 FOREIGN KEY (configuration_id) REFERENCES configuration(id);


--
-- Name: workflow_input_filters fk1cuklmlh4xk0s3dqekio7gy74; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_input_filters
    ADD CONSTRAINT fk1cuklmlh4xk0s3dqekio7gy74 FOREIGN KEY (input_filters_key) REFERENCES product_class(id);


--
-- Name: workflow_class_output_parameters fk1gm53x3igc2k000lc5cy2sf1q; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_class_output_parameters
    ADD CONSTRAINT fk1gm53x3igc2k000lc5cy2sf1q FOREIGN KEY (workflow_id) REFERENCES workflow(id);


--
-- Name: processing_order_input_filters fk1u9dj81sg3vcueaprup3hasqi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_input_filters
    ADD CONSTRAINT fk1u9dj81sg3vcueaprup3hasqi FOREIGN KEY (input_filters_id) REFERENCES input_filter(id);


--
-- Name: mon_product_production_day fk204rojkd37iypnvoyw0nr3iqn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_product_production_day
    ADD CONSTRAINT fk204rojkd37iypnvoyw0nr3iqn FOREIGN KEY (mission_id) REFERENCES mission(id);


--
-- Name: product_download_history fk23add5g47k6kbm3cgmw6hqqjh; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_download_history
    ADD CONSTRAINT fk23add5g47k6kbm3cgmw6hqqjh FOREIGN KEY (product_file_id) REFERENCES product_file(id);


--
-- Name: configuration_static_input_files fk23vkvo6qmdg9xinr4drioro6w; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration_static_input_files
    ADD CONSTRAINT fk23vkvo6qmdg9xinr4drioro6w FOREIGN KEY (static_input_files_id) REFERENCES configuration_input_file(id);


--
-- Name: product_file_aux_file_names fk24578k1macp0jxtdaiep0nku8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_file_aux_file_names
    ADD CONSTRAINT fk24578k1macp0jxtdaiep0nku8 FOREIGN KEY (product_file_id) REFERENCES product_file(id);


--
-- Name: product_query fk2wed8wyw8vyboifjd64ytftp9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_query
    ADD CONSTRAINT fk2wed8wyw8vyboifjd64ytftp9 FOREIGN KEY (requested_product_class_id) REFERENCES product_class(id);


--
-- Name: product_file fk4b360nnjmkd9r4w01jc7yer5h; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_file
    ADD CONSTRAINT fk4b360nnjmkd9r4w01jc7yer5h FOREIGN KEY (processing_facility_id) REFERENCES processing_facility(id);


--
-- Name: product_class fk4oc1a80q9jt8b0et2kl64j8av; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_class
    ADD CONSTRAINT fk4oc1a80q9jt8b0et2kl64j8av FOREIGN KEY (processor_class_id) REFERENCES processor_class(id);


--
-- Name: product fk4oh5ogb84h479uxx9m2w2s1i3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product
    ADD CONSTRAINT fk4oh5ogb84h479uxx9m2w2s1i3 FOREIGN KEY (configured_processor_id) REFERENCES configured_processor(id);


--
-- Name: product fk5g7do1yby2n4monwfjw1q79kc; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product
    ADD CONSTRAINT fk5g7do1yby2n4monwfjw1q79kc FOREIGN KEY (product_class_id) REFERENCES product_class(id);


--
-- Name: spacecraft_payloads fk5pbclfmfjdlc2xc6m3k96x6j9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  spacecraft_payloads
    ADD CONSTRAINT fk5pbclfmfjdlc2xc6m3k96x6j9 FOREIGN KEY (spacecraft_id) REFERENCES spacecraft(id);


--
-- Name: processing_order_dynamic_processing_parameters fk64135mif3wav3bqm7g4hh5ko7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_dynamic_processing_parameters
    ADD CONSTRAINT fk64135mif3wav3bqm7g4hh5ko7 FOREIGN KEY (processing_order_id) REFERENCES processing_order(id);


--
-- Name: simple_selection_rule_configured_processors fk68ygyev0w1vb1jvnah0ry6vg9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_selection_rule_configured_processors
    ADD CONSTRAINT fk68ygyev0w1vb1jvnah0ry6vg9 FOREIGN KEY (simple_selection_rule_id) REFERENCES simple_selection_rule(id);


--
-- Name: job fk6ek6xjklhsk2qduh5ejbcnb7c; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  job
    ADD CONSTRAINT fk6ek6xjklhsk2qduh5ejbcnb7c FOREIGN KEY (processing_facility_id) REFERENCES processing_facility(id);


--
-- Name: workflow_output_parameters fk6jtuu5c7q9lcwfl8dp07jlywi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_output_parameters
    ADD CONSTRAINT fk6jtuu5c7q9lcwfl8dp07jlywi FOREIGN KEY (workflow_id) REFERENCES workflow(id);


--
-- Name: task fk6oktr0t8iad73hifdftqgwok9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  task
    ADD CONSTRAINT fk6oktr0t8iad73hifdftqgwok9 FOREIGN KEY (processor_id) REFERENCES processor(id);


--
-- Name: processing_order_requested_product_classes fk7afxfgldbnrdi5joivn7agj86; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_requested_product_classes
    ADD CONSTRAINT fk7afxfgldbnrdi5joivn7agj86 FOREIGN KEY (requested_product_classes_id) REFERENCES product_class(id);


--
-- Name: processing_order_class_output_parameters fk7m4kynfbpfam8fvs66fpk6elk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_class_output_parameters
    ADD CONSTRAINT fk7m4kynfbpfam8fvs66fpk6elk FOREIGN KEY (class_output_parameters_id) REFERENCES class_output_parameter(id);


--
-- Name: workflow_class_output_parameters fk7p2bf4mui50rg5kg15k3a53g4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_class_output_parameters
    ADD CONSTRAINT fk7p2bf4mui50rg5kg15k3a53g4 FOREIGN KEY (class_output_parameters_key) REFERENCES product_class(id);


--
-- Name: processing_order_output_parameters fk7udpjfeq21n6vsi6rxeycsoi9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_output_parameters
    ADD CONSTRAINT fk7udpjfeq21n6vsi6rxeycsoi9 FOREIGN KEY (processing_order_id) REFERENCES processing_order(id);


--
-- Name: product_parameters fk84to6rlvpri4i2pjqpvfn5jd8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_parameters
    ADD CONSTRAINT fk84to6rlvpri4i2pjqpvfn5jd8 FOREIGN KEY (product_id) REFERENCES product(id);


--
-- Name: job fk8jj66thbddwdxad89qxjeepxg; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  job
    ADD CONSTRAINT fk8jj66thbddwdxad89qxjeepxg FOREIGN KEY (orbit_id) REFERENCES orbit(id);


--
-- Name: simple_selection_rule fk8n3bq0ecxeti1ylukwkt7cnm; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_selection_rule
    ADD CONSTRAINT fk8n3bq0ecxeti1ylukwkt7cnm FOREIGN KEY (source_product_class_id) REFERENCES product_class(id);


--
-- Name: simple_selection_rule_configured_processors fk8p1jxkynyy47c9slyxbjp18iu; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_selection_rule_configured_processors
    ADD CONSTRAINT fk8p1jxkynyy47c9slyxbjp18iu FOREIGN KEY (configured_processors_id) REFERENCES configured_processor(id);


--
-- Name: workflow_class_output_parameters fk9fpufpt1t7q9kxmie12mi4se7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_class_output_parameters
    ADD CONSTRAINT fk9fpufpt1t7q9kxmie12mi4se7 FOREIGN KEY (class_output_parameters_id) REFERENCES class_output_parameter(id);


--
-- Name: authorities fk_authorities_users; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  authorities
    ADD CONSTRAINT fk_authorities_users FOREIGN KEY (username) REFERENCES users(username);


--
-- Name: group_authorities fk_group_authorities_group; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  group_authorities
    ADD CONSTRAINT fk_group_authorities_group FOREIGN KEY (group_id) REFERENCES groups(id);


--
-- Name: group_members fk_group_members_group; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  group_members
    ADD CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES groups(id);


--
-- Name: group_members fk_group_members_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  group_members
    ADD CONSTRAINT fk_group_members_user FOREIGN KEY (username) REFERENCES users(username);


--
-- Name: product_class fkafnqr7afqkr7vn6difh4r9e3j; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_class
    ADD CONSTRAINT fkafnqr7afqkr7vn6difh4r9e3j FOREIGN KEY (enclosing_class_id) REFERENCES product_class(id);


--
-- Name: product_query_filter_conditions fkag48xcu5bmuq9yqls0d824bkj; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_query_filter_conditions
    ADD CONSTRAINT fkag48xcu5bmuq9yqls0d824bkj FOREIGN KEY (product_query_id) REFERENCES product_query(id);


--
-- Name: groups_group_members fkawl37vgnmf8ny5a9txq0q0mtq; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  groups_group_members
    ADD CONSTRAINT fkawl37vgnmf8ny5a9txq0q0mtq FOREIGN KEY (group_members_id) REFERENCES group_members(id);


--
-- Name: job_step fkbi6cqwlkj3nyqkvheqeg5qql0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  job_step
    ADD CONSTRAINT fkbi6cqwlkj3nyqkvheqeg5qql0 FOREIGN KEY (job_id) REFERENCES job(id);


--
-- Name: task_breakpoint_file_names fkblitkg6msystnhjpjj5ya1tfh; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  task_breakpoint_file_names
    ADD CONSTRAINT fkblitkg6msystnhjpjj5ya1tfh FOREIGN KEY (task_id) REFERENCES task(id);


--
-- Name: configuration fkbvl2q3rfimgbvr8o6txnm5ea7; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration
    ADD CONSTRAINT fkbvl2q3rfimgbvr8o6txnm5ea7 FOREIGN KEY (processor_class_id) REFERENCES processor_class(id);


--
-- Name: product_query fkc71ouv75rseha12h0fmlqt6a5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_query
    ADD CONSTRAINT fkc71ouv75rseha12h0fmlqt6a5 FOREIGN KEY (job_step_id) REFERENCES job_step(id);


--
-- Name: mon_product_production_hour fkct5iw5b4h6q2y9oiy5acwph6s; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_product_production_hour
    ADD CONSTRAINT fkct5iw5b4h6q2y9oiy5acwph6s FOREIGN KEY (mission_id) REFERENCES mission(id);


--
-- Name: processing_order_input_filters fkdhgcujq2nix39y2b7nbdpnlto; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_input_filters
    ADD CONSTRAINT fkdhgcujq2nix39y2b7nbdpnlto FOREIGN KEY (input_filters_key) REFERENCES product_class(id);


--
-- Name: configured_processor fkdj5cx8yntdnuxvphpowp47of5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configured_processor
    ADD CONSTRAINT fkdj5cx8yntdnuxvphpowp47of5 FOREIGN KEY (configuration_id) REFERENCES configuration(id);


--
-- Name: product fke8busf8q6a8uvrh9a5od38tqo; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product
    ADD CONSTRAINT fke8busf8q6a8uvrh9a5od38tqo FOREIGN KEY (enclosing_product_id) REFERENCES product(id);


--
-- Name: product_archive_available_product_classes fke8v1poev1p25mcv38i8ueg5dl; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_archive_available_product_classes
    ADD CONSTRAINT fke8v1poev1p25mcv38i8ueg5dl FOREIGN KEY (product_archive_id) REFERENCES product_archive(id);


--
-- Name: processing_order_input_product_classes fkei14w0cwbjj4d293kf0kccovn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_input_product_classes
    ADD CONSTRAINT fkei14w0cwbjj4d293kf0kccovn FOREIGN KEY (input_product_classes_id) REFERENCES product_class(id);


--
-- Name: simple_policy_delta_times fkerahx0bbgt0eeqanerq28kofp; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_policy_delta_times
    ADD CONSTRAINT fkerahx0bbgt0eeqanerq28kofp FOREIGN KEY (simple_policy_id) REFERENCES simple_policy(id);


--
-- Name: class_output_parameter_output_parameters fkeyipsy3fwc5pwmym1lypkrkh3; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  class_output_parameter_output_parameters
    ADD CONSTRAINT fkeyipsy3fwc5pwmym1lypkrkh3 FOREIGN KEY (class_output_parameter_id) REFERENCES class_output_parameter(id);


--
-- Name: processing_order_class_output_parameters fkf2c8fjwehnwek6aqehkdyig4r; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_class_output_parameters
    ADD CONSTRAINT fkf2c8fjwehnwek6aqehkdyig4r FOREIGN KEY (class_output_parameters_key) REFERENCES product_class(id);


--
-- Name: product_query fkfh82iydbxt4tvgiscy2qlj2m9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_query
    ADD CONSTRAINT fkfh82iydbxt4tvgiscy2qlj2m9 FOREIGN KEY (generating_rule_id) REFERENCES simple_selection_rule(id);


--
-- Name: simple_selection_rule_simple_policies fkfjb7qfppicnb1xj9vgi895dnb; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_selection_rule_simple_policies
    ADD CONSTRAINT fkfjb7qfppicnb1xj9vgi895dnb FOREIGN KEY (simple_selection_rule_id) REFERENCES simple_selection_rule(id);


--
-- Name: groups_group_members fkfjhm6ctnf3akprkg5ic279dyi; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  groups_group_members
    ADD CONSTRAINT fkfjhm6ctnf3akprkg5ic279dyi FOREIGN KEY (groups_id) REFERENCES groups(id);


--
-- Name: configuration_configuration_files fkg6qj2gjs3td0wwcioda96uik5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration_configuration_files
    ADD CONSTRAINT fkg6qj2gjs3td0wwcioda96uik5 FOREIGN KEY (configuration_id) REFERENCES configuration(id);


--
-- Name: processing_order_input_filters fkgbh8k5vigdykb0s1cwhag6br5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_input_filters
    ADD CONSTRAINT fkgbh8k5vigdykb0s1cwhag6br5 FOREIGN KEY (processing_order_id) REFERENCES processing_order(id);


--
-- Name: processing_order_requested_configured_processors fkgdnmmc4ri3f4w2d52e935d3jg; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_requested_configured_processors
    ADD CONSTRAINT fkgdnmmc4ri3f4w2d52e935d3jg FOREIGN KEY (processing_order_id) REFERENCES processing_order(id);


--
-- Name: simple_selection_rule_simple_policies fkgijs10i27ucb2tosn56pqrqt6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_selection_rule_simple_policies
    ADD CONSTRAINT fkgijs10i27ucb2tosn56pqrqt6 FOREIGN KEY (simple_policies_id) REFERENCES simple_policy(id);


--
-- Name: processing_order fkgj4135cm664vfl5jt6v613y0e; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order
    ADD CONSTRAINT fkgj4135cm664vfl5jt6v613y0e FOREIGN KEY (mission_id) REFERENCES mission(id);


--
-- Name: configuration_static_input_files fkgls3b4eoq74nhjslcn57reige; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration_static_input_files
    ADD CONSTRAINT fkgls3b4eoq74nhjslcn57reige FOREIGN KEY (configuration_id) REFERENCES configuration(id);


--
-- Name: processor_docker_run_parameters fkgqohmkxfbxo6ihxpgs84q5axp; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processor_docker_run_parameters
    ADD CONSTRAINT fkgqohmkxfbxo6ihxpgs84q5axp FOREIGN KEY (processor_id) REFERENCES processor(id);


--
-- Name: processing_order_requested_orbits fkgruycyl8hgdsmac11yl37odi9; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_requested_orbits
    ADD CONSTRAINT fkgruycyl8hgdsmac11yl37odi9 FOREIGN KEY (processing_order_id) REFERENCES processing_order(id);


--
-- Name: users_group_memberships fkhbcokg6kjsft20melhs8njcma; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  users_group_memberships
    ADD CONSTRAINT fkhbcokg6kjsft20melhs8njcma FOREIGN KEY (users_username) REFERENCES users(username);


--
-- Name: orbit fki2gpip0vqngjwnvmguox9wi3f; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  orbit
    ADD CONSTRAINT fki2gpip0vqngjwnvmguox9wi3f FOREIGN KEY (spacecraft_id) REFERENCES spacecraft(id);


--
-- Name: configuration_input_file_file_names fki81ysbbwtpwxlhcm82eksdq1g; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration_input_file_file_names
    ADD CONSTRAINT fki81ysbbwtpwxlhcm82eksdq1g FOREIGN KEY (configuration_input_file_id) REFERENCES configuration_input_file(id);


--
-- Name: simple_selection_rule_filter_conditions fki9hebpru8hilywjux8v76p2em; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_selection_rule_filter_conditions
    ADD CONSTRAINT fki9hebpru8hilywjux8v76p2em FOREIGN KEY (simple_selection_rule_id) REFERENCES simple_selection_rule(id);


--
-- Name: product_class fkinocsatitcf1ofpp4wc7psua2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_class
    ADD CONSTRAINT fkinocsatitcf1ofpp4wc7psua2 FOREIGN KEY (mission_id) REFERENCES mission(id);


--
-- Name: mon_ext_service_state_operation fkiqb5ahpdcute0oip6q9wovayp; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_ext_service_state_operation
    ADD CONSTRAINT fkiqb5ahpdcute0oip6q9wovayp FOREIGN KEY (mon_service_state_id) REFERENCES mon_service_state(id);


--
-- Name: processing_order_requested_product_classes fkj0e73npk4ljcr6lupi978clea; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_requested_product_classes
    ADD CONSTRAINT fkj0e73npk4ljcr6lupi978clea FOREIGN KEY (processing_order_id) REFERENCES processing_order(id);


--
-- Name: product_query_satisfying_products fkj1us8b41hn4xc8ug12c530ei1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_query_satisfying_products
    ADD CONSTRAINT fkj1us8b41hn4xc8ug12c530ei1 FOREIGN KEY (satisfied_product_queries_id) REFERENCES product_query(id);


--
-- Name: workflow_input_filters fkjdmteac11nvr5yvxrinn35rfb; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_input_filters
    ADD CONSTRAINT fkjdmteac11nvr5yvxrinn35rfb FOREIGN KEY (input_filters_id) REFERENCES input_filter(id);


--
-- Name: simple_selection_rule fkje8biclfyorg1wm8uh1qf9d0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  simple_selection_rule
    ADD CONSTRAINT fkje8biclfyorg1wm8uh1qf9d0 FOREIGN KEY (target_product_class_id) REFERENCES product_class(id);


--
-- Name: configuration_dyn_proc_parameters fkjpifxw2lvac6ipxqdimmy73k4; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configuration_dyn_proc_parameters
    ADD CONSTRAINT fkjpifxw2lvac6ipxqdimmy73k4 FOREIGN KEY (configuration_id) REFERENCES configuration(id);


--
-- Name: input_filter_filter_conditions fkjqbbl8slm6j7oco6vfg88duq2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  input_filter_filter_conditions
    ADD CONSTRAINT fkjqbbl8slm6j7oco6vfg88duq2 FOREIGN KEY (input_filter_id) REFERENCES input_filter(id);


--
-- Name: processing_order_mon_order_progress fkjwvq04w9s9sfhftuebjq7rop; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_mon_order_progress
    ADD CONSTRAINT fkjwvq04w9s9sfhftuebjq7rop FOREIGN KEY (mon_order_state_id) REFERENCES mon_order_state(id);


--
-- Name: product fkk9wbgbn6a2xyr7f7vl65uogis; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product
    ADD CONSTRAINT fkk9wbgbn6a2xyr7f7vl65uogis FOREIGN KEY (job_step_id) REFERENCES job_step(id);


--
-- Name: processing_order_mon_order_progress fkkiu48p0ndrxpd71y09yn2q474; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_mon_order_progress
    ADD CONSTRAINT fkkiu48p0ndrxpd71y09yn2q474 FOREIGN KEY (processing_order_id) REFERENCES processing_order(id);


--
-- Name: processing_order_requested_configured_processors fkkkhi2aj21ehrsok4ekhl1fd31; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_requested_configured_processors
    ADD CONSTRAINT fkkkhi2aj21ehrsok4ekhl1fd31 FOREIGN KEY (requested_configured_processors_id) REFERENCES configured_processor(id);


--
-- Name: product fkkobwm0e23qst5q2irk37fwmuy; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product
    ADD CONSTRAINT fkkobwm0e23qst5q2irk37fwmuy FOREIGN KEY (orbit_id) REFERENCES orbit(id);


--
-- Name: mon_service_state_operation fkkp6fp3q2xd1x9ogghmy7pdht0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_service_state_operation
    ADD CONSTRAINT fkkp6fp3q2xd1x9ogghmy7pdht0 FOREIGN KEY (mon_service_id) REFERENCES mon_service(id);


--
-- Name: mon_ext_service_state_operation fkkrcxkfr1txuoeif89p2igbh6g; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_ext_service_state_operation
    ADD CONSTRAINT fkkrcxkfr1txuoeif89p2igbh6g FOREIGN KEY (mon_ext_service_id) REFERENCES mon_ext_service(id);


--
-- Name: workflow fkl0agecusohc3yft3vq7xlaikr; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow
    ADD CONSTRAINT fkl0agecusohc3yft3vq7xlaikr FOREIGN KEY (input_product_class_id) REFERENCES product_class(id);


--
-- Name: processing_order_input_product_classes fkl16rc9whni5al0b0t4ukf1dq6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_input_product_classes
    ADD CONSTRAINT fkl16rc9whni5al0b0t4ukf1dq6 FOREIGN KEY (processing_order_id) REFERENCES processing_order(id);


--
-- Name: workflow_option fkl2fwnrxddum783emw1a3qwuot; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_option
    ADD CONSTRAINT fkl2fwnrxddum783emw1a3qwuot FOREIGN KEY (workflow_id) REFERENCES workflow(id);


--
-- Name: processing_order_requested_orbits fkl52pcfs07440sihimxmpy6iva; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_requested_orbits
    ADD CONSTRAINT fkl52pcfs07440sihimxmpy6iva FOREIGN KEY (requested_orbits_id) REFERENCES orbit(id);


--
-- Name: configured_processor fkloteyhnalc56x161f4inujyt5; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  configured_processor
    ADD CONSTRAINT fkloteyhnalc56x161f4inujyt5 FOREIGN KEY (processor_id) REFERENCES processor(id);


--
-- Name: job_step_output_parameters fklw2fh8ksho7gcvrfykeoep899; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  job_step_output_parameters
    ADD CONSTRAINT fklw2fh8ksho7gcvrfykeoep899 FOREIGN KEY (job_step_id) REFERENCES job_step(id);


--
-- Name: processing_order_class_output_parameters fklxehk5y7wbwpi3gxj00eg3p89; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order_class_output_parameters
    ADD CONSTRAINT fklxehk5y7wbwpi3gxj00eg3p89 FOREIGN KEY (processing_order_id) REFERENCES processing_order(id);


--
-- Name: processor_class fklxfogyfhmujn40qg0ooxfdwfv; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processor_class
    ADD CONSTRAINT fklxfogyfhmujn40qg0ooxfdwfv FOREIGN KEY (mission_id) REFERENCES mission(id);


--
-- Name: product_download_history fkm3o1ca4ms7b4ereu9wvsxpeet; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_download_history
    ADD CONSTRAINT fkm3o1ca4ms7b4ereu9wvsxpeet FOREIGN KEY (product_id) REFERENCES product(id);


--
-- Name: workflow fkmwefyimusp3lp3qlj7tsdgmm6; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow
    ADD CONSTRAINT fkmwefyimusp3lp3qlj7tsdgmm6 FOREIGN KEY (output_product_class_id) REFERENCES product_class(id);


--
-- Name: workflow fkn0aaw3ptxvvfwb1tbuoowki42; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow
    ADD CONSTRAINT fkn0aaw3ptxvvfwb1tbuoowki42 FOREIGN KEY (configured_processor_id) REFERENCES configured_processor(id);


--
-- Name: processor fko4ocncq22u0j2prxw2dbk0dka; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processor
    ADD CONSTRAINT fko4ocncq22u0j2prxw2dbk0dka FOREIGN KEY (processor_class_id) REFERENCES processor_class(id);


--
-- Name: job fko7lm1bpn9pqf1qq9o5fpfjtic; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  job
    ADD CONSTRAINT fko7lm1bpn9pqf1qq9o5fpfjtic FOREIGN KEY (processing_order_id) REFERENCES processing_order(id);


--
-- Name: product_query_satisfying_products fkq768nqgupajiccjpbyawcqhtd; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_query_satisfying_products
    ADD CONSTRAINT fkq768nqgupajiccjpbyawcqhtd FOREIGN KEY (satisfying_products_id) REFERENCES product(id);


--
-- Name: mon_service_state_operation fkq96wp2nrf7tvih0otaf0ojtyg; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_service_state_operation
    ADD CONSTRAINT fkq96wp2nrf7tvih0otaf0ojtyg FOREIGN KEY (mon_service_state_id) REFERENCES mon_service_state(id);


--
-- Name: mission_processing_modes fkqhg2duxhcpldh28nyh7nwnfcn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mission_processing_modes
    ADD CONSTRAINT fkqhg2duxhcpldh28nyh7nwnfcn FOREIGN KEY (mission_id) REFERENCES mission(id);


--
-- Name: product_file fkqs127y6vnoylxgo8aroqx4e8f; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_file
    ADD CONSTRAINT fkqs127y6vnoylxgo8aroqx4e8f FOREIGN KEY (product_id) REFERENCES product(id);


--
-- Name: workflow_input_filters fkqur70i7n8cka6j0jjo8exje2x; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_input_filters
    ADD CONSTRAINT fkqur70i7n8cka6j0jjo8exje2x FOREIGN KEY (workflow_id) REFERENCES workflow(id);


--
-- Name: mission_file_classes fks4suek1246jge02gcgpnoms5y; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mission_file_classes
    ADD CONSTRAINT fks4suek1246jge02gcgpnoms5y FOREIGN KEY (mission_id) REFERENCES mission(id);


--
-- Name: workflow fksbdoolsgnxt5ji4bfnmpe14y8; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow
    ADD CONSTRAINT fksbdoolsgnxt5ji4bfnmpe14y8 FOREIGN KEY (mission_id) REFERENCES mission(id);


--
-- Name: mon_product_production_month fkshuxduj4xdpxqoty4f6desfun; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  mon_product_production_month
    ADD CONSTRAINT fkshuxduj4xdpxqoty4f6desfun FOREIGN KEY (mission_id) REFERENCES mission(id);


--
-- Name: product_archive_available_product_classes fksmgbtsw0scm0bdg6bd49do29r; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  product_archive_available_product_classes
    ADD CONSTRAINT fksmgbtsw0scm0bdg6bd49do29r FOREIGN KEY (available_product_classes_id) REFERENCES product_class(id);


--
-- Name: spacecraft fksp2jjwkpaehybfu5pwedol1c; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  spacecraft
    ADD CONSTRAINT fksp2jjwkpaehybfu5pwedol1c FOREIGN KEY (mission_id) REFERENCES mission(id);


--
-- Name: processing_order fkt2f7nkjj7muumygco1sj81hn1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  processing_order
    ADD CONSTRAINT fkt2f7nkjj7muumygco1sj81hn1 FOREIGN KEY (workflow_id) REFERENCES workflow(id);


--
-- Name: users_group_memberships fktodlfclgikl9ionfovl0t7wp0; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  users_group_memberships
    ADD CONSTRAINT fktodlfclgikl9ionfovl0t7wp0 FOREIGN KEY (group_memberships_id) REFERENCES group_members(id);


--
-- Name: workflow_option_value_range fky9hi8gn5n5pdglqyl1qyowvn; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE  workflow_option_value_range
    ADD CONSTRAINT fky9hi8gn5n5pdglqyl1qyowvn FOREIGN KEY (workflow_option_id) REFERENCES workflow_option(id);




--
-- PostgreSQL database dump complete
--

