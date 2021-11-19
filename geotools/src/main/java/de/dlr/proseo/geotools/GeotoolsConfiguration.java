package de.dlr.proseo.geotools;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import de.dlr.proseo.geotools.rest.ShpFile;

/**
 * Configuration class for the prosEO MonitorServices component
 * 
 * @author Ernst Melchinger
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo.geotools")
public class GeotoolsConfiguration {

	private List<Shapefile> shapefiles;
	
	private String test;
	
	public List<Shapefile> getShapefiles() {
		return shapefiles;
	}

	public void setShapefiles(List<Shapefile> shapefiles) {
		this.shapefiles = shapefiles;
	}
	public String getTest() {
		return test;
	}

	public void setTest(String test) {
		this.test = test;
	}
	public static class Shapefile {
		/**
		 * The file type
		 */
		
		private ShpFile.GeoFileType type;
		/**
		 * The file path
		 */
		private String path;
		/**
		 * The cover type
		 */
		private String shapeType;
		/**
		 * @return the type
		 */
		public ShpFile.GeoFileType getType() {
			return type;
		}
		/**
		 * @return the path
		 */
		public String getPath() {
			return path;
		}
		/**
		 * @return the ShapeType
		 */
		public String getShapeType() {
			return shapeType;
		}
		/**
		 * @param type the type to set
		 */
		public void setType(ShpFile.GeoFileType type) {
			this.type = type;
		}
		/**
		 * @param path the path to set
		 */
		public void setPath(String path) {
			this.path = path;
		}
		/**
		 * @param cover the ShapeType to set
		 */
		public void setShapeType(String shapeType) {
			this.shapeType = shapeType;
		}
		
	}
}
