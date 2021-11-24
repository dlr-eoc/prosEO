package de.dlr.proseo.geotools.rest;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.geotools.GeotoolsConfiguration;
import de.dlr.proseo.geotools.rest.model.RestPoint;
import de.dlr.proseo.geotools.rest.model.RestPolygon;

/**
 * Controller to handle the contains request
 * 
 * @author Melchinger
 *
 */
@Component
public class ContainControllerImpl implements ContainController {
	
	/**
	 * Logger of this class
	 */
	private static Logger logger = LoggerFactory.getLogger(InfoControllerImpl.class);

	/** Geotools configuration */
	@Autowired
	private GeotoolsConfiguration geotoolsConfig;

	/**
	 * The geotools utilities 
	 */
	@Autowired
	private GeotoolsUtil geotools;
	
	/**
	 * Checks wheather the Polygon defined by poly array is in the region named type
	 * 
	 * @param poly String array containing latitude, longitude pairs 
	 * @param type The region type
	 * @return true if poly is contained
	 */
	@Override
	public ResponseEntity<Boolean> contains(String[] poly, String[] type) {
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
			return new ResponseEntity<>(geotools.isPolyInside(polygon, type), HttpStatus.OK);
		}
		return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
	};

	/**
	 * Checks wheather the Polygon defined py restPolygon is in the region named type
	 * 
	 * @param type The region type
	 * @param restPolygon RestPolygon structure  
	 * @return true if restPolygon is contained
	 */
	@Override
    public ResponseEntity<Boolean> containspoly(String[] type,
        RestPolygon restPolygon) {
		if (restPolygon.getPoints().size() == 1) {
			return new ResponseEntity<>(geotools.isPointInside(
					restPolygon.getPoints().get(0).getLat(), 
					restPolygon.getPoints().get(0).getLon(), type), HttpStatus.OK);
		} else if (restPolygon.getPoints().size() > 1) {
			return new ResponseEntity<>(geotools.isPolyInside(restPolygon, type), HttpStatus.OK);
		}
		return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
    }
}
	