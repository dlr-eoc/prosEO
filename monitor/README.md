# prosEO Monitor

Initializing of Database

After first run of Monitor the tables mon_* are created. Then insert these static values (used in visualisation with Grafana)

INSERT into mon_service_state (id, VERSION, NAME) VALUES (1, 1, 'running');
INSERT into mon_service_state (id, VERSION, NAME) VALUES (2, 1, 'stopped');
INSERT into mon_service_state (id, VERSION, NAME) VALUES (3, 1, 'starting');
INSERT into mon_service_state (id, VERSION, NAME) VALUES (4, 1, 'stopping');
INSERT into mon_service_state (id, VERSION, NAME) VALUES (5, 1, 'degraded');

INSERT into mon_order_state (id, VERSION, NAME) VALUES (0, 1, 'INITIAL');
INSERT into mon_order_state (id, VERSION, NAME) VALUES (1, 1, 'APPROVED');
INSERT into mon_order_state (id, VERSION, NAME) VALUES (2, 1, 'PLANNED');
INSERT into mon_order_state (id, VERSION, NAME) VALUES (3, 1, 'RELEASED');
INSERT into mon_order_state (id, VERSION, NAME) VALUES (4, 1, 'RUNNING');
INSERT into mon_order_state (id, VERSION, NAME) VALUES (5, 1, 'SUSPENDING');
INSERT into mon_order_state (id, VERSION, NAME) VALUES (6, 1, 'COMPLETED');
INSERT into mon_order_state (id, VERSION, NAME) VALUES (7, 1, 'FAILED');
INSERT into mon_order_state (id, VERSION, NAME) VALUES (8, 1, 'CLOSED');

Prepare docker instances to get access of API.

Edit /lib/systemd/system/docker.service
Change ExecStart to:
  ExecStart=/usr/bin/dockerd -H fd:// --containerd=/run/containerd/containerd.sock -H=tcp://0.0.0.0:2375
and restart the service:
  systemctl daemon-reload
  service docker restart

In docker desktop activate in settings:
  Expose daemon on tcp://localhost:2375 without TLS
  