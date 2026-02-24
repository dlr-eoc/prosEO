prosEO 2.0.0 Database Upgrade
=============================

This procedure assumes that the PostgreSQL data directory `/var/lib/postgresql/data` is mounted from the Docker host into the
database container. Further assuming that the user `postgres` has the root directory `/var/lib/postgresql` (as is the case
in the official PostgreSQL container images) we use the relative path `data/` to exchange files between the "outside world"
and PostgreSQL.

1. Stop the prosEO brain on the source system, leaving only the database running
2. Login to the database container: `docker exec -it proseo_proseo-db_1 su - postgres`
3. Dump the prosEO database (2 alternatives):
   1. To SQL text file: `pg_dump proseo >data/proseo-dump.sql`, or
   2. To PostgreSQL custom dump format: `pg_dump --format=custom proseo >data/proseo-dump.dump`
4. Logout from database container
5. Back-up the old mount directory if needed. Start the database container (PostgreSQL 17) only, 
   ensuring that the mount directory is empty to avoid version incompatibilities
6. Transfer dump file `proseo-dump.sql` (or `proseo-dump.dump`) to the target system
7. Make the dump file and the database upgrade script(s) accessible inside the container by volume bind mount, e.g. by
   copying the files to the new pgdata directory
8. Login to the database container (as above)
9. Create the prosEO database (using `template0` to avoid any potential leftovers from an earlier instance, esp. regarding large
   objects): `dropdb proseo; createdb -T template0 proseo`, as well as the grafana role to ensure the dump imports successfully: `createuser grafana   
10. Import the database dump file:
   1. From SQL text file: `psql proseo <data/proseo-dump.sql`, or
   2. From custom dump format: `pg_restore -d proseo data/proseo-dump.dumpl`
11. Apply all database updates up to and including prosEO 1.2.0, if not yet done
12. Run database upgrade script for prosEO 2.0.0: `psql proseo </path/to/proseo_schema_update_2025-12-17.sql`
13. Log into the database and reset the max_connections parameter to the limit needed by prosEO with
    `ALTER SYSTEM SET max_connections = 500;`
14. Logout from the database container and restart it
15. Start prosEO on the target system

Note that depending of the size of the database exporting and importing may take some time.
