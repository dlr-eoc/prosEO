package de.dlr.proseo.geotools.rest;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
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

public class ShpFile {
	
	static public enum GeoFileType {
		SHP, KML, GEOJSON
	}

	/**
	 * The file name
	 */
	private String filename;
	/**
	 * The file type (shp, kml, geojson)
	 */
	private GeoFileType type;
	/**
	 * The opended feature source
	 */
	private SimpleFeatureSource source;
	/**
	 * The filter factory
	 */
	private FilterFactory2 filter;
	/**
	 * The geometry
	 */
	private GeometryFactory geometry;
	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
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
	 * @return the geometry
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
	 * @param geometry the geometry to set
	 */
	public void setGeometry(GeometryFactory geometry) {
		this.geometry = geometry;
	}
	
	public ShpFile openFileAndCreate(String filename, GeoFileType type) {
		this.filename = filename;
		this.type = type;
		File file = new File(filename);
		switch (type) {
		case SHP: 
			try {
				ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
				Map<String, Serializable> params = new HashMap<>();
				params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
				params.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
				params.put(ShapefileDataStoreFactory.ENABLE_SPATIAL_INDEX.key, Boolean.TRUE);
				params.put(ShapefileDataStoreFactory.MEMORY_MAPPED.key, Boolean.TRUE);
				params.put(ShapefileDataStoreFactory.CACHE_MEMORY_MAPS.key, Boolean.TRUE);

				ShapefileDataStore store = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
				source = store.getFeatureSource();
			    filter = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
			    geometry = JTSFactoryFinder.getGeometryFactory();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}

			break;
		case KML: 

			break;
		case GEOJSON: 
		
			break;
		default:
			return null;
		}
		return this;
	}
	
}