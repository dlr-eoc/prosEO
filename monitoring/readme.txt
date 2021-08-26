Run logging environment in Docker.

Edit docker-compose.yml

  Replace the "<...>" with appropriate values.

  If DOCKER_INFLUXDB_INIT_ADMIN_TOKEN=<my_influxdb_token> is set, it should be used in the setup step (empty directories for influxdb).
  Otherwise influxdb creates a new token which could be copied from "<path to nonitoring>/influxdb/config/influx-configs".
  
influxDB
  https://hub.docker.com/_/influxdb

  For consistent data are two directories needed for data and config file(s).
  For initial setup these directories have to be empty.

telegraf
  To log docker and docker images:
  https://github.com/influxdata/telegraf/blob/master/plugins/inputs/docker/README.md

  Setup the configuration in <path to monitoring>/telegraf/telegraf.conf
  At least the <my_influxdb_token> has to be replaced by the concrete value.

grafana
  https://grafana.com/docs/grafana/latest/installation/docker/
  
  For consistent data a directory need to be mounted.


