/**
 * GeoUtils.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.geotools.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.geotools.GeotoolsConfiguration;
import de.dlr.proseo.geotools.GeotoolsConfiguration.Shapefile;
import de.dlr.proseo.geotools.rest.model.RestPoint;
import de.dlr.proseo.geotools.rest.model.RestPolygon;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeotoolsMessage;

/**
 * A utility class for working with geospatial data. It provides methods for
 * checking if a point or polygon is inside or overlaps with certain regions
 * defined by shape files (provided in application.yml).
 */
@Component
public class GeotoolsUtil {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(GeotoolsUtil.class);

	/** Geotools configuration */
	@Autowired
	GeotoolsConfiguration geotoolsConfig;

	/**
	 * A map holding the defined region types/names as keys and the ShpFile(s) for
	 * each region as values
	 */
	private Map<String, List<ShpFile>> shapeMap = null;

	/** Tracks whether the shape files are initialized */
	private Boolean initialized = false;

	/**
	 * Adds a shape file to the shapeMap for the given region type
	 *
	 * @param shp  the shape file to add
	 * @param type the type of the shape file (region name)
	 */
	private void addShapeFile(ShpFile shp, String type) {
		if (shp != null) {
			List<ShpFile> shps = shapeMap.get(type);
			if (shps == null) {
				shapeMap.put(type, new ArrayList<ShpFile>());
			}
			shapeMap.get(type).add(shp);
		}
	}

	/**
	 * Read and initialize the shape files at first call
	 *
	 * @throws IOExeption if a shape file could not be opened or created
	 */
	private void init() throws IOException {

		logger.trace(">>> init()");

		if (!initialized) {

			logger.trace("... initializing shape files");

			if (shapeMap == null) {
				shapeMap = new HashMap<>();
			}

			if (geotoolsConfig.getShapefiles() != null) {

				// If shape files are configured, open and initialize them

				for (Shapefile sf : geotoolsConfig.getShapefiles()) {
					ShpFile shpFile = new ShpFile();

					try {
						shpFile = shpFile.openFileAndCreate(sf.getPath(), sf.getType());
					} catch (UnsupportedOperationException e) {
						// if a valid, but not yet handled file type was given
						// to be handled at a higher level
						throw e;
					} catch (IllegalArgumentException e) {
						// if an invalid file type was given
						// to be handled at a higher level
						throw e;
					} catch (IOException e) {
						// if the shape file could not be opened or created successfully
						// to be handled at a higher level
						throw e;
					}

					try {
						logger.trace("... shape file {} has bounds:\n{}", shpFile.getFilename(), shpFile.getSource().getBounds());
					} catch (IOException e) {
						logger.log(GeotoolsMessage.BOUNDS_IO_SHAPE_FILE, shpFile.getFilename(), e);
					}

					addShapeFile(shpFile, sf.getShapeType());
					logger.log(GeotoolsMessage.SHAPE_FILE_INITIALIZED, sf.getPath(), sf.getShapeType());
				}
			}

			initialized = true;
		}
	}

	/**
	 * Retrieves information about the loaded shapes
	 *
	 * @return information about the loaded shapes
	 *
	 * @throws UnsupportedOperationException if a valid, but not yet handled file
	 *                                       type was given
	 * @throws IllegalArgumentException      if an invalid file type was given
	 * @throws IOException                   if the shape file could not be opened
	 *                                       created successfully
	 *
	 */
	public String getInfo() throws UnsupportedOperationException, IllegalArgumentException, IOException {
		StringBuilder info = new StringBuilder();

		// Ensure shape map is initialized
		try {
			init();
		} catch (UnsupportedOperationException e) {
			// if a valid, but not yet handled file type was given
			// to be handled at a higher level
			throw e;
		} catch (IllegalArgumentException e) {
			// if an invalid file type was given
			// to be handled at a higher level
			throw e;
		} catch (IOException e) {
			// if the shape file could not be opened or created successfully
			// to be handled at a higher level
			throw e;
		}

		shapeMap.forEach((String type, List<ShpFile> files) -> {
			info.append("Region " + type + " with files \n");

			files.forEach(file -> {
				info.append("- " + file.getFilename() + "\n");
			});

			info.append("\n");
		});

		return info.toString();
	}

	/**
	 * Checks whether the geographical point defined by latitude and longitude is
	 * inside of at least one of the provided regions
	 *
	 * @param latitude  a Double describing the latitude of the coordinate
	 * @param longitude a Double describing the longitude of the coordinate
	 * @param types     the types parameter is used to determine which region types
	 *                  to check if a given point is inside. If the types parameter
	 *                  is null or empty, it will consider all available region
	 *                  types stored in the shapeMap. Otherwise, it will only check
	 *                  the specified region types.
	 * @return true if the coordinate is contained in at least one of the provided
	 *         regions (or all known regions if none were provided)
	 * @throws UnsupportedOperationException if a valid, but not yet handled file
	 *                                       type was given
	 * @throws IllegalArgumentException      if an invalid file type was given
	 * @throws IOException                   if the shape file could not be opened
	 *                                       created successfully
	 *
	 */
	public Boolean isPointInside(Double latitude, Double longitude, String[] types)
			throws UnsupportedOperationException, IllegalArgumentException, IOException {

		logger.trace(">>> isPointInside({}, {}, {})", latitude, longitude, types);

		// Ensure shape map is initialized
		try {
			init();
		} catch (UnsupportedOperationException e) {
			// if a valid, but not yet handled file type was given
			// to be handled at a higher level
			throw e;
		} catch (IllegalArgumentException e) {
			// if an invalid file type was given
			// to be handled at a higher level
			throw e;
		} catch (IOException e) {
			// if the shape file could not be opened or created successfully
			// to be handled at a higher level
			throw e;
		}

		try {
			init();
		} catch (UnsupportedOperationException e) {
			// if a valid, but not yet handled file type was given
			// to be handled at a higher level
			throw e;
		} catch (IllegalArgumentException e) {
			// if an invalid file type was given
			// to be handled at a higher level
			throw e;
		} catch (IOException e) {
			// if the shape file could not be opened or created successfully
			// to be handled at a higher level
			throw e;
		}

		// If no regions were provided, check against all known regions
		if (types == null || types.length < 1) {
			types = shapeMap.keySet().toArray(new String[shapeMap.keySet().size()]);
		}

		// Check whether the coordinate is contained in at least one region
		for (String type : types) {

			// Ensure that valid types were provided
			if (shapeMap.get(type) == null || shapeMap.get(type).isEmpty()) {
				throw new IllegalArgumentException(
						logger.log(GeotoolsMessage.NO_SHAPE_FILES_FOUND, type, shapeMap.keySet().toString()));
			} else {
				for (ShpFile sf : shapeMap.get(type)) {
					if (isPointInside(latitude, longitude, sf)) {
						logger.log(GeotoolsMessage.POINT_INSIDE_AREAS, latitude, longitude, Arrays.asList(types));
						return true;
					}
				}
			}
		}

		logger.log(GeotoolsMessage.POINT_NOT_INSIDE_AREAS, latitude, longitude, Arrays.asList(types));
		return false;
	}

	/**
	 * Checks whether the geographical point defined by latitude and longitude is
	 * contained in the region described by the provided shpFile
	 *
	 * @param latitude  a Double describing the latitude of the coordinate
	 * @param longitude a Double describing the longitude of the coordinate
	 * @param shpFile   the shape file against which to compare the coordinate
	 * @return true if the geographical point defined by latitude and longitude is
	 *         contained in the region described by the provided shpFile
	 * @throws UnsupportedOperationException if a valid, but not yet handled file
	 *                                       type was given
	 * @throws IllegalArgumentException      if an invalid file type was given or
	 *                                       either input is missing
	 * @throws IOException                   if the shape file could not be opened
	 *                                       created successfully
	 *
	 */
	public Boolean isPointInside(Double latitude, Double longitude, ShpFile shpFile)
			throws UnsupportedOperationException, IllegalArgumentException, IOException {

		logger.trace(">>> isPointInside({}, {}, {})", latitude, longitude, (null == shpFile ? "null" : shpFile.getFilename()));

		// Ensure shape map is initialized
		try {
			init();
		} catch (UnsupportedOperationException e) {
			// if a valid, but not yet handled file type was given
			// to be handled at a higher level
			throw e;
		} catch (IllegalArgumentException e) {
			// if an invalid file type was given
			// to be handled at a higher level
			throw e;
		} catch (IOException e) {
			// if the shape file could not be opened or created successfully
			// to be handled at a higher level
			throw e;
		}

		// Check arguments
		if (null == latitude)
			throw new IllegalArgumentException(logger.log(GeotoolsMessage.ARGUMENT_MISSING, "latitude"));
		if (null == longitude)
			throw new IllegalArgumentException(logger.log(GeotoolsMessage.ARGUMENT_MISSING, "longitude"));
		if (null == shpFile)
			throw new IllegalArgumentException(logger.log(GeotoolsMessage.ARGUMENT_MISSING, "shape file"));

		// Create a Point object using the provided longitude and latitude coordinates
		// and a filter to check if the point is contained within the region described
		// by the shape file
		Point point = shpFile.getGeometry().createPoint(new Coordinate(longitude, latitude));
		Filter containFilter = shpFile.getFilter()
			.contains(shpFile.getFilter().property("the_geom"), shpFile.getFilter().literal(point));

		try {
			// Get the geographical features from the shape file that satisfy the filter. If
			// there are any features (points, lines, polygons, etc.) that satisfy it, the
			// given point is inside the region described by the shape file.
			SimpleFeatureCollection features = shpFile.getSource().getFeatures(containFilter);

			if (features.size() > 0) {
				return true;
			}
		} catch (IOException e) {
			// if the shape file could not be opened successfully
			// to be handled at a higher level
			throw e;
		}

		return false;
	}

	/**
	 * Checks whether the geographical area defined by the RestPolygon is contained
	 * in one or more of the region types. All available region types will be
	 * checked if none were specified.
	 *
	 * @param poly  a RestPolygon
	 * @param types the types parameter is used to determine which region types to
	 *              check if a given point is inside. If the types parameter is null
	 *              or empty, it will consider all available region types stored in
	 *              the shapeMap. Otherwise, it will only check the specified region
	 *              types.
	 *
	 * @return true if poly is inside
	 *
	 * @throws UnsupportedOperationException if a valid, but not yet handled file
	 *                                       type was given
	 * @throws IllegalArgumentException      if an invalid file type was given
	 * @throws IOException                   if the shape file could not be opened
	 *                                       created successfully
	 */
	public Boolean isPolyInside(RestPolygon poly, String[] types)
			throws UnsupportedOperationException, IllegalArgumentException, IOException {

		logger.trace(">>> isPolyInside({}, {})", poly, types);

		// Ensure shape map is initialized
		try {
			init();
		} catch (UnsupportedOperationException e) {
			// if a valid, but not yet handled file type was given
			// to be handled at a higher level
			throw e;
		} catch (IllegalArgumentException e) {
			// if an invalid file type was given
			// to be handled at a higher level
			throw e;
		} catch (IOException e) {
			// if the shape file could not be opened or created successfully
			// to be handled at a higher level
			throw e;
		}

		if (poly != null) {
			closePolygon(poly);

			// Create an ArrayList storing the coordinates
			List<Coordinate> coords = new ArrayList<>();
			for (RestPoint p : poly.getPoints()) {
				coords.add(new Coordinate(p.getLon(), p.getLat()));
			}

			logger.trace("... polygon converted to list of coordinates: {}", coords);

			// If no regions were provided, use all known regions to compare against
			if (types == null || types.length < 1) {
				types = shapeMap.keySet().toArray(new String[shapeMap.keySet().size()]);

				logger.trace("... no type(s), iterating over all types: {}", Arrays.asList(types));
			}

			// Iterate over all provided or known regions and check whether the polygon is
			// contained within
			for (String type : types) {

				logger.trace("... checking type: {}", type);

				if (shapeMap.get(type) == null || shapeMap.get(type).isEmpty()) {
					throw new IllegalArgumentException(
							logger.log(GeotoolsMessage.NO_SHAPE_FILES_FOUND, type, shapeMap.keySet().toString()));
				} else {
					for (ShpFile sf : shapeMap.get(type)) {
						if (isPolyInside(coords, sf)) {
							logger.log(GeotoolsMessage.POLYGON_INSIDE_AREAS, poly, Arrays.asList(types));
							return true;
						}
					}
				}
			}
		}

		logger.log(GeotoolsMessage.POLYGON_NOT_INSIDE_AREAS, poly, Arrays.asList(types));
		return false;
	}

	/**
	 * Checks whether the geographical area defined by the coordinates is contained
	 * in the region described by the shape file.
	 *
	 * @param coords  a list of Coordinates forming a polygon
	 * @param shpFile a ShpFile forming a region to check if the polygon is inside
	 * @return true if poly is inside the region, false otherwise
	 *
	 * @throws UnsupportedOperationException if a valid, but not yet handled file
	 *                                       type was given
	 * @throws IllegalArgumentException      if no file or an invalid file type were
	 *                                       given
	 * @throws IOException                   if the shape file could not be opened
	 *                                       created successfully
	 *
	 */
	private Boolean isPolyInside(List<Coordinate> coords, ShpFile shpFile)
			throws UnsupportedOperationException, IllegalArgumentException, IOException {

		logger.trace(">>> isPolyInside({}, {})", coords, (null == shpFile ? "null" : shpFile.getFilename()));

		if (null == shpFile) {
			throw new IllegalArgumentException(logger.log(GeotoolsMessage.ARGUMENT_MISSING, "shpFile"));
		}

		logger.trace("... shape file {} has bounds:\n{}", shpFile.getFilename(), shpFile.getSource().getBounds());

		// Ensure shape map is initialized
		try {
			init();
		} catch (UnsupportedOperationException e) {
			// if a valid, but not yet handled file type was given
			// to be handled at a higher level
			throw e;
		} catch (IllegalArgumentException e) {
			// if an invalid file type was given
			// to be handled at a higher level
			throw e;
		} catch (IOException e) {
			// if the shape file could not be opened or created successfully
			// to be handled at a higher level
			throw e;
		}

		// TODO leave this out, it is certain to be executed by the calling method
		Polygon poly = shpFile.getGeometry().createPolygon(coords.toArray(new Coordinate[coords.size()]));

		logger.trace("... created polygon {}", poly);

		// Create a filter to check if the polygon is contained within the region
		// described by the shape file
		Filter containFilter = shpFile.getFilter()
			.contains(shpFile.getFilter().property("the_geom"), shpFile.getFilter().literal(poly));

		try {
			// Get the features from the shape file that satisfy the provided filter
			SimpleFeatureCollection features = shpFile.getSource().getFeatures(containFilter);

			logger.trace("... found contain features {}", features.toArray());

			if (features.size() > 0) {
				// If features were found, return true
				return true;
			} else {
				/*
				 * Even though the polygon might not be fully contained within the region
				 * described by the shape file, the union of the modified geometries (obtained
				 * from overlapping features) can still result in a geometry that matches the
				 * original polygon. This happens when the overlapping features cover parts of
				 * the polygon that are outside the region described by the shape file,
				 * resulting in a modified geometry that, when combined, restores the original
				 * shape of the polygon.
				 */

				// If no features were found, create a new filter for checking overlaps
				containFilter = shpFile.getFilter()
					.overlaps(shpFile.getFilter().property("the_geom"), shpFile.getFilter().literal(poly));

				// Get the features from the shape file that satisfy the overlap filter
				features = shpFile.getSource().getFeatures(containFilter);

				logger.trace("... found overlap features {}", features.toArray());

				if (features.size() > 0) {
					final WKTReader wktReader = new WKTReader(shpFile.getGeometry());

					SimpleFeature sf = null;

					Geometry geo = null;
					Geometry geo1 = null;
					Geometry geo2 = null;

					try (SimpleFeatureIterator it = features.features()) {
						while (it.hasNext()) {

							sf = it.next();

							try {
								// Read the geometry from the feature's attribute as a Well-Known Text (WKT)
								// representation
								geo = wktReader.read(sf.getAttribute("the_geom").toString());

								// Calculate the difference between the 'poly' and the read geometry
								geo1 = poly.difference(geo);

								// TODO There is no scenario where geo2 does not evaluate to null. Please check!
								if (geo2 == null) {
									geo2 = geo1;
								} else {
									// Union the difference geometries
									geo2 = geo1.union(geo2);
								}

							} catch (ParseException e) {
								// If there's an error parsing the geometry (not expected), print the stack
								// trace
								e.printStackTrace();
							}
						}
					}

					if (geo2.equalsTopo(poly)) {

						logger.trace("... union of overlap {} equals poly {}", geo2, poly);

						// Check if the union of the overlap geometries equals the 'poly'
						return true;
					}
				}
			}
		} catch (IOException e) {
			throw e;
		}

		logger.log(GeotoolsMessage.POLYGON_NOT_INSIDE_AREAS, poly, shpFile.getFilename());
		return false;
	}

	/**
	 * Check whether the geographical area defined by the RestPolygon overlaps one
	 * or more of the region types. All available region types will be checked if
	 * none were specified.
	 *
	 * @param poly  a RestPolygon
	 * @param types the types parameter is used to determine which region types to
	 *              check if a given point is inside. If the types parameter is null
	 *              or empty, it will consider all available region types stored in
	 *              the shapeMap. Otherwise, it will only check the specified region
	 *              types.
	 * @return true if poly overlaps one or more of the regions, false otherwise
	 * @throws UnsupportedOperationException if a valid, but not yet handled file
	 *                                       type was given
	 * @throws IllegalArgumentException      if an invalid file type was given
	 * @throws IOException                   if the shape file could not be opened
	 *                                       created successfully
	 *
	 */
	public Boolean isPolyOverlap(RestPolygon poly, String[] types)
			throws UnsupportedOperationException, IllegalArgumentException, IOException {
		logger.trace(">>> isPolyOverlap({}, {})", poly, types);

		// Ensure shape map is initialized
		try {
			init();
		} catch (UnsupportedOperationException e) {
			// if a valid, but not yet handled file type was given
			// to be handled at a higher level
			throw e;
		} catch (IllegalArgumentException e) {
			// if an invalid file type was given
			// to be handled at a higher level
			throw e;
		} catch (IOException e) {
			// if the shape file could not be opened or created successfully
			// to be handled at a higher level
			throw e;
		}

		if (poly != null) {
			closePolygon(poly);

			// Create a new ArrayList to store the coordinates
			List<Coordinate> coords = new ArrayList<>();
			for (RestPoint p : poly.getPoints()) {
				coords.add(new Coordinate(p.getLon(), p.getLat()));
			}

			// If no regions were provided, consider all known regions
			if (types == null || types.length < 1) {
				types = shapeMap.keySet().toArray(new String[shapeMap.keySet().size()]);
			}

			// For all types and all shape files, check for overlaps with the polygon
			for (String type : types) {
				if (shapeMap.get(type) == null || shapeMap.get(type).isEmpty()) {
					throw new IllegalArgumentException(
							logger.log(GeotoolsMessage.NO_SHAPE_FILES_FOUND, type, shapeMap.keySet().toString()));
				} else {

					for (ShpFile sf : shapeMap.get(type)) {
						if (isPolyOverlap(coords, sf)) {
							logger.log(GeotoolsMessage.POLYGON_OVERLAPS, poly, Arrays.asList(types));
							return true;
						}
					}
				}
			}
		}

		logger.log(GeotoolsMessage.POLYGON_NO_OVERLAP, poly, Arrays.asList(types));
		return false;
	}

	/**
	 * Check whether the geographical area defined by the coordinates overlaps the
	 * region defined by the shape file.
	 *
	 * @param coords  a list of Coordinates forming a polygon
	 * @param shpFile a ShpFile forming a region to check if the polygon is inside
	 * @return true if the polygon overlaps the region, false otherwise
	 * @throws UnsupportedOperationException if a valid, but not yet handled file
	 *                                       type was given
	 * @throws IllegalArgumentException      if an invalid file type was given
	 * @throws IOException                   if the shape file could not be opened
	 *                                       created successfully
	 */
	private Boolean isPolyOverlap(List<Coordinate> coords, ShpFile shpFile)
			throws UnsupportedOperationException, IllegalArgumentException, IOException {
		logger.trace(">>> isPolyOverlap({}, {})", coords.toString(), shpFile);

		// Ensure shape map is initialized
		try {
			init();
		} catch (UnsupportedOperationException e) {
			// if a valid, but not yet handled file type was given
			// to be handled at a higher level
			throw e;
		} catch (IllegalArgumentException e) {
			// if an invalid file type was given
			// to be handled at a higher level
			throw e;
		} catch (IOException e) {
			// if the shape file could not be opened or created successfully
			// to be handled at a higher level
			throw e;
		}

		// Create a polygon from the provided coordinates
		Polygon poly = shpFile.getGeometry().createPolygon(coords.toArray(new Coordinate[coords.size()]));

		// Create a filter checking for overlaps with the region defined by the shape
		// file
		Filter overlapFilter = shpFile.getFilter()
			.overlaps(shpFile.getFilter().property("the_geom"), shpFile.getFilter().literal(poly));

		try {
			// Check if at least one feature overlaps the polygon
			SimpleFeatureCollection features = shpFile.getSource().getFeatures(overlapFilter);

			if (features.size() > 0) {
				return true;
			}

			// If not, check whether the polygon is fully contained
			// TODO Remove. It should be logically impossible that there are no overlaps but
			// the entire polygon is contained.
			Filter containFilter = shpFile.getFilter()
				.contains(shpFile.getFilter().property("the_geom"), shpFile.getFilter().literal(poly));

			features = shpFile.getSource().getFeatures(containFilter);

			if (features.size() > 0) {
				return true;
			}
		} catch (IOException e) {
			throw e;
		}

		return false;
	}

	/**
	 * Close the polygon if it is open, that is, the first and the last point of the
	 * polygon have different coordinates
	 *
	 * @param poly a RestPolygon
	 * @return the closed RestPolyon
	 */
	private RestPolygon closePolygon(RestPolygon poly) {
		if (poly != null) {
			// Check if the first point and the last point of the polygon have different
			// coordinates. If so, add the first point as the new last point.

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
