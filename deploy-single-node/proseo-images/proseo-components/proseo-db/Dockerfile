FROM postgres:11

ADD create_view_product_processing_facilities.sql /proseo/create_view_product_processing_facilities.sql
ADD populate_mon_service_state.sql /proseo/populate_mon_service_state.sql

# TODO Find a different way to set the connections param (postgresql.conf does not exist at this point)
#RUN sed -i -e 's/max_connections = 100/max_connections = 200/' /var/lib/postgresql/data/postgresql.conf
