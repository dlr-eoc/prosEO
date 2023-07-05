TODO after first start in Docker
================================

Log in to the running Docker container and perform:
- `psql proseo < /proseo/create_view_product_processing_facilities.sql`
- `psql proseo < /proseo/populate_mon_service_state.sql`
- `psql proseo < /proseo/create_product_indices.sql`
- `sed -i -e 's/max_connections = 100/max_connections = 200/' /var/lib/postgresql/data/postgresql.conf`

Then restart (do NOT recreate) the container.
