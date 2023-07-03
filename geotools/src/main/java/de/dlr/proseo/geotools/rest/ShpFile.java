/**
 * GeoUtils.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.geotools.rest;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.filter.FilterFactory2;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.GeotoolsMessage;

/**
 * Hold a shape file and initialized data like feature source, filter, geometry,
 * ...
 *
 * @author Melchinger
 *
 */
public class ShpFile {

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(ShpFile.class);

	/**
	 * An enumeration of different types of geographic file formats, i.e. SHP, KML,
	 * and GEOJSON
	 */
	static public enum GeoFileType {
		/** Shapefile (.shp) format. */
		SHP,

		/** Keyhole Markup Language (.kml) format. */
		KML,

		/** GeoJSON (.geojson) format. */
		GEOJSON
	}

	/**
	 * The name of the shape file
	 */
	private String filename;

	/**
	 * The type of the shape file (shp, kml, geojson)
	 */
	private GeoFileType type;

	/**
	 * The opened feature source, i.e. a source that has been opened and is ready
	 * for querying or accessing the features it contains. A geographic feature
	 * represents a spatial object, such as a point, line, or polygon, with
	 * associated attributes.
	 */
	private SimpleFeatureSource source;

	/**
	 * The filter factory used for filtering features in the shape file
	 */
	private FilterFactory2 filter;

	/**
	 * The geometry factory used for creating geometric objects
	 */
	private GeometryFactory geometry;

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @return the type
	 */
	public GeoFileType getType() {
		return type;
	}

	/**
	 * @return the source
	 */
	public SimpleFeatureSource getSource() {
		return source;
	}

	/**
	 * @return the filter
	 */
	public FilterFactory2 getFilter() {
		return filter;
	}

	/**
	 * @return the geometry factory
	 */
	public GeometryFactory getGeometry() {
		return geometry;
	}

	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(GeoFileType type) {
		this.type = type;
	}

	/**
	 * @param source the source to set
	 */
	public void setSource(SimpleFeatureSource source) {
		this.source = source;
	}

	/**
	 * @param filter the filter to set
	 */
	public void setFilter(FilterFactory2 filter) {
		this.filter = filter;
	}

	/**
	 * @param geometry the geometry factory to set
	 */
	public void setGeometry(GeometryFactory geometry) {
		this.geometry = geometry;
	}

	/**
	 * Open and initialize a shape file
	 *
	 * @param filename The file name
	 * @param type     The file type (currently only SHP supported)
	 *
	 * @return The initialized instance
	 * @throws UnsupportedOperationException if a valid, but not yet handled file
	 *                                       type was given
	 * @throws IllegalArgumentException      if an invalid file type was given
	 * @throws IOException                   if the shape file could not be opened
	 *                                       created successfully
	 * 
	 */
	public ShpFile openFileAndCreate(String filename, GeoFileType type)
			throws UnsupportedOperationException, IllegalArgumentException, IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> openFileAndCreate({}, {})", filename, type);

		// Set the filename and file type for the ShpFile instance
		this.filename = filename;
		this.type = type;

		// Create a File object using the provided filename
		File file = new File(filename);

		// Perform different actions based on the file type
		switch (type) {
		case SHP:
			try {
				// Create a ShapefileDataStoreFactory to later create a new data store
				ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();

				// Create a map with parameters for creating the data store
				Map<String, Serializable> params = new HashMap<>();
				params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
				params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
				params.put(ShapefileDataStoreFactory.ENABLE_SPATIAL_INDEX.key, Boolean.TRUE);
				params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, Boolean.TRUE);
				params.put(ShapefileDataStoreFactory.CACHE_MEMORY_MAPS.key, Boolean.TRUE);

				// Create a new ShapefileDataStore using the provided parameters
				ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);

				// Get the feature source from the shape file data store
				this.source = store.getFeatureSource();

				// Get the filter factory using the default hints from GeoTools
				this.filter = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

				// Get the geometry factory from JTSFactoryFinder
				this.geometry = JTSFactoryFinder.getGeometryFactory();

				// Return the initialized ShpFile instance
				return this;
			} catch (IOException e) {
				// IO Exception will be handled at a higher level
				throw e;
			}

		case KML:
			// TODO Implement action for KML file type
			throw new UnsupportedOperationException("Shape files of type KML are currently not handled.");

		case GEOJSON:
			// TODO Implement action for GeoJSON file type
			throw new UnsupportedOperationException("Shape files of type GEOJSON are currently not handled.");

		default:
			throw new IllegalArgumentException(logger.log(GeotoolsMessage.INVALID_FILE_TYPE, type));
		}
	}

}
