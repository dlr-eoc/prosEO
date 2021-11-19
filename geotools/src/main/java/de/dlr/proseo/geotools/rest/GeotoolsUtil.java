package de.dlr.proseo.geotools.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.geotools.GeotoolsConfiguration;
import de.dlr.proseo.geotools.GeotoolsConfiguration.Shapefile;
import de.dlr.proseo.geotools.rest.model.RestPoint;
import de.dlr.proseo.geotools.rest.model.RestPolygon;

@Component
public class GeotoolsUtil {
	
	private static Logger logger = LoggerFactory.getLogger(GeotoolsUtil.class);
	
	/** Geotools configuration */
	@Autowired
	GeotoolsConfiguration geotoolsConfig;
	
	private Map<String, List<ShpFile>> shapeMap = null;
	
	private Boolean initialized = false;
	
	private void addShapeFile(ShpFile shp, String type) {
		if (shp != null) {
			List<ShpFile> shps = shapeMap.get(type);
			if (shps == null) {
				shapeMap.put(type, new ArrayList<ShpFile>());
			}
			shapeMap.get(type).add(shp);			
		} 
	}
	
	private void init() {
		if (logger.isTraceEnabled()) logger.trace(">>> init()");
		
		if (!initialized) {
			if (logger.isTraceEnabled()) logger.trace("... initializing shape files");
			
			if (shapeMap ==  null) {
				shapeMap = new HashMap<String, List<ShpFile>>();
			}
			
			if (geotoolsConfig.getShapefiles() != null) {
				for (Shapefile sf : geotoolsConfig.getShapefiles()) {
					ShpFile shpFile = new ShpFile();
					shpFile = shpFile.openFileAndCreate(sf.getPath(), sf.getType());
					
					if (logger.isTraceEnabled())
						try {
							logger.trace("... shape file {} has bounds:\n{}",
									shpFile.getFilename(), shpFile.getSource().getBounds());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					addShapeFile(shpFile, sf.getShapeType());
					logger.info("Shape file '{}' for type '{}' initialized", sf.getPath(), sf.getShapeType());
				}
			}
			initialized = true;
		}
	}
	
	public String getInfo() {
		init();
		return "info";
	}


	public Boolean isPointInside(Double latitude, Double longitude, String[] types) {
		if (logger.isTraceEnabled()) logger.trace(">>> isPointInside({}, {}, {})", latitude, longitude, types);
		
		init();
		// now we have the Polygon
		// iterate over the shape files
		if (types == null || types.length < 1) {
			types = shapeMap.keySet().toArray(new String[shapeMap.keySet().size()]);
		}
		for (String type : types) {
			if (shapeMap.get(type) != null) {
				for (ShpFile sf : shapeMap.get(type)) {
					if (isPointInside(latitude, longitude, sf)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public Boolean isPointInside(Double latitude, Double longitude, ShpFile shpFile) {
		if (logger.isTraceEnabled()) logger.trace(">>> isPointInside({}, {}, {})", latitude, longitude,
				(null == shpFile ? "null" : shpFile.getFilename()));
		
		init();
		Point point = shpFile.getGeometry().createPoint(new Coordinate(longitude, latitude));
		Filter pointInPolygon = shpFile.getFilter().contains(shpFile.getFilter().property("the_geom"), shpFile.getFilter().literal(point));
		try {
			SimpleFeatureCollection features = shpFile.getSource().getFeatures(pointInPolygon);
			if (features.size() > 0) {
				return true;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public Boolean isPolyInside(RestPolygon poly, String[] types) {
		if (logger.isTraceEnabled()) logger.trace(">>> isPolyInside({}, {})", poly, types);
		
		init();
		if (poly != null) {
			closePolygon(poly);
			List<Coordinate> coords = new ArrayList<Coordinate>();
			for (RestPoint p : poly.getPoints()) {
				coords.add(new Coordinate(p.getLon(), p.getLat()));
			}
			if (logger.isTraceEnabled()) logger.trace("... polygon converted to list of coordinates: {}", coords);
			// now we have the Polygon
			// iterate over the shape files
			if (types == null || types.length < 1) {
				types = shapeMap.keySet().toArray(new String[shapeMap.keySet().size()]);
				if (logger.isTraceEnabled()) logger.trace("... no type(s), iterating over all types: {}", Arrays.asList(types));
			}
			for (String type : types) {
				if (logger.isTraceEnabled()) logger.trace("... checking type: {}", type);
				if (shapeMap.get(type) == null || shapeMap.get(type).isEmpty()) {
					logger.error("No shape files found for type {}", type);
				} else {
					for (ShpFile sf : shapeMap.get(type)) {
						if (isPolyInside(coords, sf)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	private Boolean isPolyInside(List<Coordinate> coords, ShpFile shpFile) {
		if (logger.isTraceEnabled()) logger.trace(">>> isPolyInside({}, {})", coords, (null == shpFile ? "null" : shpFile.getFilename()));
		
		try {
			logger.trace("... shape file {} has bounds:\n{}",
					shpFile.getFilename(), shpFile.getSource().getBounds());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		// init();		// leave this out, it is certain to be executed by the calling method
		Polygon poly = shpFile.getGeometry()
				.createPolygon(coords.toArray(new Coordinate[coords.size()]));
		if (logger.isTraceEnabled()) logger.trace("... created polygon {}", poly);
		
		// Naming: polygonInPolygon?
		Filter pointInPolygon = shpFile.getFilter()
				.contains(shpFile.getFilter().property("the_geom"), shpFile.getFilter().literal(poly));

		try {
			SimpleFeatureCollection features = shpFile.getSource().getFeatures(pointInPolygon);
			if (logger.isTraceEnabled()) logger.trace("... found features {}", features.toArray());

			if (features.size() > 0) {
				return true;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	public Boolean isPolyOverlap(RestPolygon poly, String[] types) {
		logger.trace("isPolyOverlap({}, {})", poly, types);
		init();
		if (poly != null) {
			closePolygon(poly);
			List<Coordinate> coords = new ArrayList<Coordinate>();
			for (RestPoint p : poly.getPoints()) {
				coords.add(new Coordinate(p.getLon(), p.getLat()));
			}
			// now we have the Polygon
			// iterate over the shape files
			if (types == null || types.length < 1) {
				types = shapeMap.keySet().toArray(new String[shapeMap.keySet().size()]);
			}
			for (String type : types) {
				if (shapeMap.get(type) != null) {
					for (ShpFile sf : shapeMap.get(type)) {
						if (isPolyOverlap(coords, sf)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	private Boolean isPolyOverlap(List<Coordinate> coords, ShpFile shpFile) {
		init();
		Polygon poly = shpFile.getGeometry().createPolygon(coords.toArray(new Coordinate[coords.size()]));
		Filter pointInPolygon = shpFile.getFilter().overlaps(shpFile.getFilter().property("the_geom"), shpFile.getFilter().literal(poly));
		try {
			SimpleFeatureCollection features = shpFile.getSource().getFeatures(pointInPolygon);
			if (features.size() > 0) {
				return true;
			}
			pointInPolygon = shpFile.getFilter().contains(shpFile.getFilter().property("the_geom"), shpFile.getFilter().literal(poly));
			features = shpFile.getSource().getFeatures(pointInPolygon);
			if (features.size() > 0) {
				return true;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	
	private RestPolygon closePolygon(RestPolygon poly) {
		if (poly != null) {
			RestPoint p1 = poly.getPoints().get(0);
			RestPoint pn = poly.getPoints().get(poly.getPoints().size() - 1);
			if (!p1.getLat().equals(pn.getLat()) || !p1.getLon().equals(pn.getLon())) {
				RestPoint pl = new RestPoint();
				pl.setLat(p1.getLat());
				pl.setLon(p1.getLon());
				poly.getPoints().add(pl);
			}
		}
		return poly;
	}
}
