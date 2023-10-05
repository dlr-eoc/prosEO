FROM postgres:11

ADD create_view_product_processing_facilities.sql /proseo/create_view_product_processing_facilities.sql
ADD populate_mon_service_state.sql /proseo/populate_mon_service_state.sql

# Fetch the default configuration, modify max_connections, and save it to the custom location
RUN sed -e 's/^.*max_connections =.*/max_connections = 200/' /usr/share/postgresql/postgresql.conf.sample > /etc/postgresql/my-postgresql.conf

# Default command to run PostgreSQL using the custom configuration
CMD ["postgres", "-c", "config_file=/etc/postgresql/my-postgresql.conf"]	