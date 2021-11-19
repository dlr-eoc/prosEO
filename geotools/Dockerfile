FROM openjdk:11
MAINTAINER Thomas Bassler <thomas.bassler@drbassler.de>

#install configuration and shapes
COPY docker/application.yml /usr/share/proseo-geotools/application.yml
COPY docker/EW_SLC_area_NE-Canada.geojson /usr/share/proseo-geotools/EW_SLC_area_NE-Canada.geojson
COPY docker/EW_SLC_Svalbard.kml /usr/share/proseo-geotools/EW_SLC_Svalbard.kml
COPY docker/GSHHS_c_L1.dbf /usr/share/proseo-geotools/GSHHS_c_L1.dbf
COPY docker/GSHHS_c_L1.prj /usr/share/proseo-geotools/GSHHS_c_L1.prj
COPY docker/GSHHS_c_L1.shp /usr/share/proseo-geotools/GSHHS_c_L1.shp
COPY docker/GSHHS_c_L1.shx /usr/share/proseo-geotools/GSHHS_c_L1.shx
COPY docker/GSHHS_c_L5.dbf /usr/share/proseo-geotools/GSHHS_c_L5.dbf
COPY docker/GSHHS_c_L5.prj /usr/share/proseo-geotools/GSHHS_c_L5.prj
COPY docker/GSHHS_c_L5.shp /usr/share/proseo-geotools/GSHHS_c_L5.shp
COPY docker/GSHHS_c_L5.shx /usr/share/proseo-geotools/GSHHS_c_L5.shx
COPY docker/EW_SLC_area_NE-Canada.dbf /usr/share/proseo-geotools/EW_SLC_area_NE-Canada.dbf
COPY docker/EW_SLC_area_NE-Canada.prj /usr/share/proseo-geotools/EW_SLC_area_NE-Canada.prj
COPY docker/EW_SLC_area_NE-Canada.shp /usr/share/proseo-geotools/EW_SLC_area_NE-Canada.shp
COPY docker/EW_SLC_area_NE-Canada.shx /usr/share/proseo-geotools/EW_SLC_area_NE-Canada.shx
COPY docker/EW_SLC_Svalbard-polygon.dbf /usr/share/proseo-geotools/EW_SLC_Svalbard-polygon.dbf
COPY docker/EW_SLC_Svalbard-polygon.prj /usr/share/proseo-geotools/EW_SLC_Svalbard-polygon.prj
COPY docker/EW_SLC_Svalbard-polygon.shp /usr/share/proseo-geotools/EW_SLC_Svalbard-polygon.shp
COPY docker/EW_SLC_Svalbard-polygon.shx /usr/share/proseo-geotools/EW_SLC_Svalbard-polygon.shx

COPY target/proseo-geotools.jar /usr/share/proseo-geotools/proseo-geotools.jar

WORKDIR /usr/share/proseo-geotools
ENTRYPOINT ["java", "-jar", "/usr/share/proseo-geotools/proseo-geotools.jar"]
#ENTRYPOINT ["java", "-Xdebug",  "-Xrunjdwp:transport=dt_socket,address=192.168.20.155:8012", "-jar", "/usr/share/proseo-geotools/proseo-geotools.jar"]