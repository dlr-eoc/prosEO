package de.dlr.proseo.geotools.rest;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.geotools.GeotoolsConfiguration;
import de.dlr.proseo.geotools.rest.model.RestPoint;
import de.dlr.proseo.geotools.rest.model.RestPolygon;

@Component
public class OverlapControllerImpl implements OverlapController {
	
	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(InfoControllerImpl.class);

	/** Geotools configuration */
	@Autowired
	private GeotoolsConfiguration geotoolsConfig;

	@Autowired
	private GeotoolsUtil geotools;

	/**
	 * Checks wheather the Polygon defined by poly array overlaps the region named type
	 * 
	 * @param poly String array containing latitude, longitude pairs 
	 * @param type The region type
	 * @return true if poly overlaps
	 */
	@Override
	public ResponseEntity<Boolean> overlaps(String[] poly, String[] type) {
		if (poly.length == 2) {
			Double latitude = Double.valueOf(poly[0]);
			Double longitude = Double.valueOf(poly[1]);
			return new ResponseEntity<>(geotools.isPointInside(latitude, longitude, type), HttpStatus.OK);
		} else if (poly.length > 2) {
			RestPolygon polygon = new RestPolygon();
			for (int i = 0; i < poly.length - 1; i = i + 2) {
				RestPoint p = new RestPoint();
				p.setLat(Double.valueOf(poly[i]));
				p.setLon(Double.valueOf(poly[i + 1]));
				polygon.getPoints().add(p);
			}
			return new ResponseEntity<>(geotools.isPolyOverlap(polygon, type), HttpStatus.OK);
		}
		return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
	};

	/**
	 * Checks wheather the Polygon defined py restPolygon overlaps the region named type
	 * 
	 * @param type The region type
	 * @param restPolygon RestPolygon structure  
	 * @return true if restPolygon overlaps
	 */
	@Override
    public ResponseEntity<Boolean> overlapspoly(String[] type,
            RestPolygon restPolygon) {
    		if (restPolygon.getPoints().size() == 1) {
    			return new ResponseEntity<>(geotools.isPointInside(
    					restPolygon.getPoints().get(0).getLat(), 
    					restPolygon.getPoints().get(0).getLon(), type), HttpStatus.OK);
    		} else if (restPolygon.getPoints().size() > 1) {
    			return new ResponseEntity<>(geotools.isPolyOverlap(restPolygon, type), HttpStatus.OK);
    		}
    		return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
}
	