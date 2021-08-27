Run logging environment in Docker.

Edit docker-compose.yml

  Replace the "<...>" with appropriate values.

  If DOCKER_INFLUXDB_INIT_ADMIN_TOKEN=<my_influxdb_token> is set, it should be used in the setup step (empty directories for influxdb).
  Otherwise influxdb creates a new token which could be copied from "<path to nonitoring>/influxdb/config/influx-configs".
  
influxDB
  https://hub.docker.com/_/influxdb

  For consistent data are two directories needed for data and config file(s).
  For initial setup these directories have to be empty.

  For configuration it is possible to use the influx client, see
    https://docs.influxdata.com/influxdb/v2.0/reference/cli/influx/
  for details:

  docker exec influxdb2 influx bucket create -n operation -o proseo
  docker exec influxdb2 influx bucket create -n order -o proseo
  docker exec influxdb2 influx bucket create -n production -o proseo
  docker exec influxdb2 influx bucket create -n telegraf -o proseo -r 30d

  docker exec influxdb2 influx user create --name example-username --password ExAmPl3PA55W0rD


telegraf
  To log docker and docker images:
  https://github.com/influxdata/telegraf/blob/master/plugins/inputs/docker/README.md

  Setup the configuration in <path to monitoring>/telegraf/telegraf.conf
  At least the <my_influxdb_token> has to be replaced by the concrete value.

grafana
  https://grafana.com/docs/grafana/latest/installation/docker/
  
  For consistent data a directory need to be mounted.


