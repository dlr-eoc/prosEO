/**
 * OverlapControllerImpl.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.geotools.rest;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import de.dlr.proseo.geotools.rest.model.RestPoint;
import de.dlr.proseo.geotools.rest.model.RestPolygon;
import de.dlr.proseo.logging.http.HttpPrefix;
import de.dlr.proseo.logging.http.ProseoHttp;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeotoolsMessage;

/**
 * Controller to handle the overlap request, which checks whether a specified
 * polygon overlaps selected or all regions from application.yml.
 *
 * @author Ernst Melchinger
 *
 */
@Component
public class OverlapControllerImpl implements OverlapController {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(OverlapControllerImpl.class);

	/** HTTP service methods */
	private static ProseoHttp http = new ProseoHttp(logger, HttpPrefix.GEOTOOLS);

	/** The geotools utilities */
	@Autowired
	private GeotoolsUtil geotools;

	/**
	 * Checks whether an area, i.e. the Polygon defined by the array poly overlaps
	 * at least one of the provided regions. If none are given, all available region
	 * types will be considered.
	 *
	 * For longer arrays, using the overlapspoly() method with a RestPolygon is
	 * recommended, and for high lengths in fact necessary.
	 *
	 * @param poly String array containing latitude (uneven fields)/longitude
	 *             (uneven fields) pairs in Double format, which together describe
	 *             an area to be compared to a number of specified regions
	 * @param type String array used to determine which regions (specified in
	 *             application.yml) to check. If the array parameter is null or
	 *             empty, all available region types will be considered
	 * @return HttpStatus 200 and true if the polygon overlaps one or more of the
	 *         specified regions (all known regions if none were specified), false
	 *         otherwise OR HttpStatus 400 and an error message if the input was
	 *         invalid OR HttpStatus 500 and an error message if the input was valid
	 *         but the implementation is pending or a problem occurred while trying
	 *         to process a shape file
	 */
	@Override
	public ResponseEntity<Boolean> overlaps(String[] poly, String[] type) {

		// If a singular geographical point was defined:
		if (null != poly && poly.length == 2) {
			Double latitude = Double.valueOf(poly[0]);
			Double longitude = Double.valueOf(poly[1]);

			// Check whether the point defined by latitude and longitude contained
			// in at least one of the specified regions
			try {
				Boolean contains = geotools.isPointInside(latitude, longitude, type);
				return new ResponseEntity<>(contains, HttpStatus.OK);
			} catch (UnsupportedOperationException e) {
				return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IllegalArgumentException e) {
				return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
			} catch (IOException e) {
				return new ResponseEntity<>(http.errorHeaders(e.getClass().getName() + " / " + e.getMessage()),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		// If a geographical are was defined:
		else if (null != poly && poly.length > 2 && poly.length % 2 == 0) {

			// Convert coordinates into RestPolygon
			RestPolygon polygon = new RestPolygon();
			for (int i = 0; i < poly.length - 1; i = i + 2) {
				RestPoint p = new RestPoint();
				p.setLat(Double.valueOf(poly[i]));
				p.setLon(Double.valueOf(poly[i + 1]));
				polygon.getPoints().add(p);
			}

			// Check whether the region defined by the RestPolygon overlaps at
			// least one of the specified regions
			try {
				Boolean contains = geotools.isPolyOverlap(polygon, type);
				return new ResponseEntity<>(contains, HttpStatus.OK);
			} catch (UnsupportedOperationException e) {
				return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IllegalArgumentException e) {
				return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
			} catch (IOException e) {
				return new ResponseEntity<>(http.errorHeaders(e.getClass().getName() + " / " + e.getMessage()),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		// If no or an uneven number of latitude/longitude values were provided:
		return new ResponseEntity<>(http.errorHeaders(logger.log(GeotoolsMessage.INVALID_COORDINATES)), HttpStatus.BAD_REQUEST);
	}

	/**
	 * Checks whether an area defined by a RestPolygin overlaps at least one of the
	 * provided regions. If none are given, all available region types will be
	 * considered.
	 *
	 * @param restPolygon RestPolygon describing an area to be compared to a number of
	 *             specified regions
	 * @param type String array used to determine which regions (specified in
	 *             application.yml) to check. If the array parameter is null or
	 *             empty, all available region types will be considered
	 * @return HttpStatus 200 and true if the polygon overlaps one or more of the
	 *         specified regions (all known regions if none were specified), false
	 *         otherwise OR HttpStatus 400 and an error message if the input was
	 *         invalid OR HttpStatus 500 and an error message if the input was valid
	 *         but the implementation is pending or a problem occurred while trying
	 *         to process a shape file
	 */
	@Override
	public ResponseEntity<Boolean> overlapspoly(String[] type, RestPolygon restPolygon) {

		// If a singular geographical point was defined:
		if (null != restPolygon && restPolygon.getPoints().size() == 1) {

			// Check whether the point defined by latitude and longitude overlaps
			// at least one of the specified regions
			try {
				Boolean overlaps = geotools.isPointInside(restPolygon.getPoints().get(0).getLat(),
						restPolygon.getPoints().get(0).getLon(), type);
				return new ResponseEntity<>(overlaps, HttpStatus.OK);
			} catch (UnsupportedOperationException e) {
				return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IllegalArgumentException e) {
				return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
			} catch (IOException e) {
				return new ResponseEntity<>(http.errorHeaders(e.getClass().getName() + " / " + e.getMessage()),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		// If a geographical are was defined:
		else if (null != restPolygon && restPolygon.getPoints().size() > 1) {

			// Check whether the region defined by the RestPolygon overlaps at
			// least one of the specified regions
			try {
				Boolean overlaps = geotools.isPolyOverlap(restPolygon, type);
				return new ResponseEntity<>(overlaps, HttpStatus.OK);
			} catch (UnsupportedOperationException e) {
				return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
			} catch (IllegalArgumentException e) {
				return new ResponseEntity<>(http.errorHeaders(e.getMessage()), HttpStatus.BAD_REQUEST);
			} catch (IOException e) {
				return new ResponseEntity<>(http.errorHeaders(e.getClass().getName() + " / " + e.getMessage()),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		// If no polygon was given or the given polygon contains no points
		return new ResponseEntity<>(http.errorHeaders(logger.log(GeotoolsMessage.REST_POLYGON_MISSING)), HttpStatus.BAD_REQUEST);
	}
}
