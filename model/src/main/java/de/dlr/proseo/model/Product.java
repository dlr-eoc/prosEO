/**
 * Product.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.enums.ProductQuality;
import de.dlr.proseo.model.enums.ProductionType;

/**
 * Representation of a data product
 * 
 * @author Dr. Thomas Bassler
 *
 */

@Entity
@Table(indexes = { 
		@Index(unique = true, columnList = "uuid"),
		@Index(unique = false, columnList = "product_class_id, sensing_start_time"), 
		@Index(unique = false, columnList = "product_class_id, sensing_stop_time"), 
		@Index(unique = false, columnList = "product_class_id, generation_time"), 
		@Index(unique = false, columnList = "eviction_time"),
		@Index(unique = false, columnList = "publication_time"),
		@Index(unique = false, columnList = "enclosing_product_id")})
public class Product extends PersistentObject {
	
	private static final String MSG_FILENAME_TEMPLATE_NOT_FOUND = "Product filename template for mission not found";

	/**
	 * Universally unique product identifier (Production Interface Delivery Point Specification, sec. 3.1)
	 */
	@Column(nullable = false)
	private UUID uuid;
	
	/** Product class this products instantiates */
	@ManyToOne(fetch = FetchType.LAZY)
	private ProductClass productClass;
	
	/** One of the file classes defined for the mission (Ground Segment File Format Standard, sec. 4.1.2; optional) */
	private String fileClass;
	
	/** Processing modes as defined for the enclosing mission (only if generated by this mission) */
	private String mode;
	
	/** Indicator for the suitability of this product for general use */
	@Enumerated(EnumType.STRING)
	private ProductQuality productQuality = ProductQuality.NOMINAL;
	
	/** 
	 * Product (validity) start time as requested during order planning; used by Production Planner to prevent
	 * double processing of the same product. Initially same as sensingStartTime. 
	 */
	@Column(name = "requested_start_time", columnDefinition = "TIMESTAMP(6)")
	private Instant requestedStartTime;
	
	/**
	 * Product (validity) stop time as requested during order planning; used by Production Planner to prevent
	 * double processing of the same product. Initially same as sensingStopTime. 
	 */
	@Column(name = "requested_stop_time", columnDefinition = "TIMESTAMP(6)")
	private Instant requestedStopTime;
	
	/**
	 * Sensing start time; initially same as requestedStartTime, but may be updated after processing to reflect
	 * the actual product sensing times
	 */
	@Column(name = "sensing_start_time", columnDefinition = "TIMESTAMP(6)")
	private Instant sensingStartTime;
	
	/**
	 * Sensing stop time; initially same as requestedStopTime, but may be updated after processing to reflect
	 * the actual product sensing times
	 */
	@Column(name = "sensing_stop_time", columnDefinition = "TIMESTAMP(6)")
	private Instant sensingStopTime;
	
	/**
	 * The latest point in time, at which the input satellite raw data (e. g. CADU chunks) for this product or any of its 
	 * input products became available for processing. This timestamp is primarily used to control timeliness requirements
	 * in systematic processing.
	 */
	@Column(name = "raw_data_availability_time", columnDefinition = "TIMESTAMP(6)")
	private Instant rawDataAvailabilityTime;
	
	/** Product generation time */
	@Column(name = "generation_time", columnDefinition = "TIMESTAMP(6)")
	private Instant generationTime;
	
	/**
	 * Earliest time, at which a product file for this product was ingested; this timestamp will be preserved even after
	 * the deletion of all related product files
	 */
	@Column(name = "publication_time", columnDefinition = "TIMESTAMP(6)")
	private Instant publicationTime;
	
	/**
	 * The time from which on this product including its product files will be deleted from prosEO, unless other consistency
	 * constraints (e. g. existing processing orders) prevent it. Computed as the product generation time plus the product
	 * retention period from the generating processing order or from the mission. If the eviction time is not set and cannot
	 * be computed, then no automatic product deletion takes place.
	 */
	@Column(name = "eviction_time", columnDefinition = "TIMESTAMP(6)")
	private Instant evictionTime;
	
	/**
	 * The download history for this product
	 */
	@ElementCollection
	private Set<DownloadHistory> downloadHistory = new HashSet<>();
	
	/** Type of production process generating this product */
	@Enumerated(EnumType.STRING)
	private ProductionType productionType;
	
	/** Set of component products */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "enclosingProduct")
	private Set<Product> componentProducts = new HashSet<>();
	
	/** Product for which this product is a component */
	@ManyToOne(fetch = FetchType.LAZY)
	private Product enclosingProduct;
	
	/**
	 * Orbit relationship of this product, if any
	 * 
	 * If a product is associated to an orbit, its validity period may still differ from the orbit times, but its validity end time 
	 * must not extend further than into the following orbit (e. g. a near-realtime time slice beginning in one orbit, but also 
	 * including some data from the subsequent orbit).
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	private Orbit orbit;
	
	/** Product files for this product */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "product")
	private Set<ProductFile> productFile = new HashSet<>();
	
	/** Product queries satisfied by this product */
	@ManyToMany(mappedBy = "satisfyingProducts")
	private Set<ProductQuery> satisfiedProductQueries = new HashSet<>();
	
	/** Job step that produced this product (if any) */
	@OneToOne(fetch = FetchType.LAZY)
	private JobStep jobStep;
	
	/** Processor configuration used for processing this product */
	@ManyToOne(fetch = FetchType.LAZY)
	private ConfiguredProcessor configuredProcessor;
	
	/**
	 * A collection of mission-specific parameters for this object
	 */
	@ElementCollection
	private Map<String, Parameter> parameters = new HashMap<>();
	
	/** The logger for this class */
	private static final ProseoLogger logger = new ProseoLogger(Product.class);
	
	/**
	 * Gets the universally unique product identifier
	 * 
	 * @return the UUID
	 */
	public UUID getUuid() {
		return uuid;
	}
	/**
	 * Sets the universally unique product identifier
	 * 
	 * @param uuid the UUID to set
	 */
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	/**
	 * Gets the product class
	 * 
	 * @return the productClass
	 */
	public ProductClass getProductClass() {
		return productClass;
	}
	/**
	 * Sets the product class
	 * 
	 * @param productClass the productClass to set
	 */
	public void setProductClass(ProductClass productClass) {
		this.productClass = productClass;
	}
	/**
	 * Gets the product file class
	 * 
	 * @return the file class
	 */
	public String getFileClass() {
		return fileClass;
	}
	/**
	 * Sets the product file class
	 * 
	 * @param fileClass the file class to set
	 */
	public void setFileClass(String fileClass) {
		this.fileClass = fileClass;
	}
	/**
	 * Gets the processing mode
	 * 
	 * @return the mode
	 */
	public String getMode() {
		return mode;
	}
	/**
	 * Sets the processing mode
	 * 
	 * @param mode the mode to set
	 */
	public void setMode(String mode) {
		this.mode = mode;
	}
	/**
	 * Gets the product quality indicator
	 * 
	 * @return the product quality
	 */
	public ProductQuality getProductQuality() {
		return productQuality;
	}
	/**
	 * Sets the product quality indicator
	 * 
	 * @param productQuality the product quality to set
	 */
	public void setProductQuality(ProductQuality productQuality) {
		this.productQuality = productQuality;
	}
	/**
	 * Gets the requested start time
	 * 
	 * @return the requested start time
	 */
	public Instant getRequestedStartTime() {
		return requestedStartTime;
	}
	/**
	 * Sets the requested start time
	 * 
	 * @param requestedStartTime the requested start time to set
	 */
	public void setRequestedStartTime(Instant requestedStartTime) {
		this.requestedStartTime = requestedStartTime;
	}
	/**
	 * Gets the requested stop time
	 * 
	 * @return the requested stop time
	 */
	public Instant getRequestedStopTime() {
		return requestedStopTime;
	}
	/**
	 * Sets the requested stop time
	 * 
	 * @param requestedStopTime the requested stop time to set
	 */
	public void setRequestedStopTime(Instant requestedStopTime) {
		this.requestedStopTime = requestedStopTime;
	}
	/**
	 * Gets the sensing start time
	 * 
	 * @return the sensing start time
	 */
	public Instant getSensingStartTime() {
		return sensingStartTime;
	}
	/**
	 * Sets the sensing start time
	 * 
	 * @param sensingStartTime the sensing start time to set
	 */
	public void setSensingStartTime(Instant sensingStartTime) {
		this.sensingStartTime = sensingStartTime;
	}
	
	/**
	 * Gets the sensing stop time
	 * 
	 * @return the sensing stop time
	 */
	public Instant getSensingStopTime() {
		return sensingStopTime;
	}
	
	/**
	 * Sets the sensing stop time
	 * 
	 * @param sensingStopTime the sensing stop time to set
	 */
	public void setSensingStopTime(Instant sensingStopTime) {
		this.sensingStopTime = sensingStopTime;
	}

	/**
	 * Gets the time of the availability of the satellite raw data
	 * 
	 * @return the raw data availability time
	 */
	public Instant getRawDataAvailabilityTime() {
		return rawDataAvailabilityTime;
	}
	
	/**
	 * Sets the time of the availability of the satellite raw data
	 * 
	 * @param rawDataAvailabilityTime the raw data availability time to set
	 */
	public void setRawDataAvailabilityTime(Instant rawDataAvailabilityTime) {
		this.rawDataAvailabilityTime = rawDataAvailabilityTime;
	}
	
	/**
	 * Gets the product generation time
	 * 
	 * @return the generationTime
	 */
	public Instant getGenerationTime() {
		return generationTime;
	}
	
	/**
	 * Sets the product generation time
	 * 
	 * @param generationTime the generationTime to set
	 */
	public void setGenerationTime(Instant generationTime) {
		this.generationTime = generationTime;
	}
	
	/**
	 * Gets the product publication (= ingestion) time
	 * 
	 * @return the publication time
	 */
	public Instant getPublicationTime() {
		return publicationTime;
	}
	
	/**
	 * Sets the product publication (= ingestion) time
	 * 
	 * @param publicationTime the publication time to set
	 */
	public void setPublicationTime(Instant publicationTime) {
		this.publicationTime = publicationTime;
	}
	
	/**
	 * Gets the product eviction time
	 * 
	 * @return the eviction time
	 */
	public Instant getEvictionTime() {
		return evictionTime;
	}
	
	/**
	 * Sets the product eviction time
	 * 
	 * @param evictionTime the eviction time to set
	 */
	public void setEvictionTime(Instant evictionTime) {
		this.evictionTime = evictionTime;
	}
	
	/**
	 * Gets the product download history
	 * 
	 * @return the download history
	 */
	public Set<DownloadHistory> getDownloadHistory() {
		return downloadHistory;
	}
	/**
	 * Sets the product download history
	 * 
	 * @param downloadHistory the download history to set
	 */
	public void setDownloadHistory(Set<DownloadHistory> downloadHistory) {
		this.downloadHistory = downloadHistory;
	}
	/**
	 * Gets the production type of the product
	 * 
	 * @return the production type
	 */
	public ProductionType getProductionType() {
		return productionType;
	}
	/**
	 * Sets the production type of the product
	 * 
	 * @param productionType the production type to set
	 */
	public void setProductionType(ProductionType productionType) {
		this.productionType = productionType;
	}
	/**
	 * Gets the sub-products of this product
	 * 
	 * @return the componentProducts
	 */
	public Set<Product> getComponentProducts() {
		return componentProducts;
	}
	
	/**
	 * Sets the sub-products of this product
	 * 
	 * @param componentProducts the componentProducts to set
	 */
	public void setComponentProducts(Set<Product> componentProducts) {
		this.componentProducts = componentProducts;
	}
	
	/**
	 * Gets the enclosing product of this product
	 * 
	 * @return the enclosingProduct
	 */
	public Product getEnclosingProduct() {
		return enclosingProduct;
	}
	
	/**
	 * Sets the enclosing product of this product
	 * 
	 * @param enclosingProduct the enclosingProduct to set
	 */
	public void setEnclosingProduct(Product enclosingProduct) {
		this.enclosingProduct = enclosingProduct;
	}
	
	/**
	 * Gets the orbit for this product
	 * 
	 * @return the orbit (or null, if this product is not associated to an orbit)
	 */
	public Orbit getOrbit() {
		return orbit;
	}
	
	/**
	 * Sets the orbit for this product
	 * 
	 * @param orbit the orbit to set (may be null)
	 */
	public void setOrbit(Orbit orbit) {
		this.orbit = orbit;
	}
	
	/**
	 * Gets the set of product files (at most one per existing processing facility)
	 * 
	 * @return the productFile
	 */
	public Set<ProductFile> getProductFile() {
		return productFile;
	}
	/**
	 * Sets the set of product files (at most one per existing processing facility)
	 * 
	 * @param productFile the productFile to set
	 */
	public void setProductFile(Set<ProductFile> productFile) {
		this.productFile = productFile;
	}
	/**
	 * Gets the set of satisfied product queries
	 * 
	 * @return the satisfiedProductQueries
	 */
	public Set<ProductQuery> getSatisfiedProductQueries() {
		return satisfiedProductQueries;
	}
	/**
	 * Sets the set of satisfied product queries
	 * 
	 * @param satisfiedProductQueries the satisfiedProductQueries to set
	 */
	public void setSatisfiedProductQueries(Set<ProductQuery> satisfiedProductQueries) {
		this.satisfiedProductQueries = satisfiedProductQueries;
	}
	/**
	 * Gets the job step that created this product
	 * 
	 * @return the jobStep
	 */
	public JobStep getJobStep() {
		return jobStep;
	}
	/**
	 * Sets the job step that created this product
	 * 
	 * @param jobStep the jobStep to set
	 */
	public void setJobStep(JobStep jobStep) {
		this.jobStep = jobStep;
	}
	/**
	 * Gets the configured processor that generated this product
	 * 
	 * @return the configuredProcessor
	 */
	public ConfiguredProcessor getConfiguredProcessor() {
		return configuredProcessor;
	}
	/**
	 * Sets the configured processor that generated this product
	 * 
	 * @param configuredProcessor the configuredProcessor to set
	 */
	public void setConfiguredProcessor(ConfiguredProcessor configuredProcessor) {
		this.configuredProcessor = configuredProcessor;
	}
	/**
	 * Gets the product parameters
	 * 
	 * @return the parameters
	 */
	public Map<String, Parameter> getParameters() {
		return parameters;
	}
	
	/**
	 * Sets the product parameters
	 * 
	 * @param parameters the parameters to set
	 */
	public void setParameters(Map<String, Parameter> parameters) {
		this.parameters = parameters;
	}
	
	/**
	 * Get a named String parameter
	 * 
	 * @param key the name of the String parameter
	 * @return the parameter value casted to String
	 * @throws ClassCastException if the named parameter is not of an appropriate type
	 */
	public String getStringParameter(String key) throws ClassCastException {
		return parameters.get(key).getStringValue();
	}

	/**
	 * Set the named String parameter to the given value
	 * 
	 * @param key the parameter name
	 * @param value the parameter value to set
	 */
	public void setStringParameter(String key, String value) {
		Parameter param = parameters.get(key);
		if (null == param) {
			param = new Parameter();
		}
		param.setStringValue(value);
		this.parameters.put(key, param);
	}

	/**
	 * Get a named Integer parameter
	 * 
	 * @param key the name of the Integer parameter
	 * @return the parameter value casted to Integer
	 * @throws ClassCastException if the named parameter is not of an appropriate type
	 */
	public Integer getIntegerParameter(String key) throws ClassCastException {
		return parameters.get(key).getIntegerValue();
	}

	/**
	 * Set the named Integer parameter to the given value
	 * 
	 * @param key the parameter name
	 * @param value the parameter value to set
	 */
	public void setIntegerParameter(String key, Integer value) {
		Parameter param = parameters.get(key);
		if (null == param) {
			param = new Parameter();
		}
		param.setIntegerValue(value);
		this.parameters.put(key, param);
	}

	/**
	 * Get a named Boolean parameter
	 * 
	 * @param key the name of the Boolean parameter
	 * @return the parameter value casted to Boolean
	 * @throws ClassCastException if the named parameter is not of an appropriate type
	 */
	public Boolean getBooleanParameter(String key) throws ClassCastException {
		return parameters.get(key).getBooleanValue();
	}

	/**
	 * Set the named Boolean parameter to the given value
	 * 
	 * @param key the parameter name
	 * @param value the parameter value to set
	 */
	public void setBooleanParameter(String key, Boolean value) {
		Parameter param = parameters.get(key);
		if (null == param) {
			param = new Parameter();
		}
		param.setBooleanValue(value);
		this.parameters.put(key, param);
	}

	/**
	 * Get a named Double parameter
	 * 
	 * @param key the name of the Double parameter
	 * @return the parameter value casted to Double
	 * @throws ClassCastException if the named parameter is not of an appropriate type
	 */
	public Double getDoubleParameter(String key) throws ClassCastException {
		return parameters.get(key).getDoubleValue();
	}

	/**
	 * Set the named Double parameter to the given value
	 * 
	 * @param key the parameter name
	 * @param value the parameter value to set
	 */
	public void setDoubleParameter(String key, Double value) {
		Parameter param = parameters.get(key);
		if (null == param) {
			param = new Parameter();
		}
		param.setDoubleValue(value);
		this.parameters.put(key, param);
	}
	
	/**
	 * Get a named Instant parameter
	 * 
	 * @param key the name of the Instant parameter
	 * @return the parameter value casted to Instant
	 * @throws ClassCastException if the named parameter is not of an appropriate type
	 */
	public Instant getInstantParameter(String key) throws ClassCastException {
		return parameters.get(key).getInstantValue();
	}

	/**
	 * Set the named Instant parameter to the given value
	 * 
	 * @param key the parameter name
	 * @param value the parameter value to set
	 */
	public void setInstantParameter(String key, TemporalAccessor value) {
		Parameter param = parameters.get(key);
		if (null == param) {
			param = new Parameter();
		}
		param.setInstantValue(value);
		this.parameters.put(key, param);
	}
	
	/**
	 * Generates a product filename according to the file name template given for the enclosing mission
	 * 
	 * @return a filename string
	 * @throws IllegalStateException if the filename template for the mission could not be found
	 * @throws ParseException if the filename template could not be parsed using Spring Expression Language
	 * @throws EvaluationException if the filename template could not be evaluated
	 */
	public String generateFilename() throws IllegalStateException, ParseException, EvaluationException {
		// Get the filename template from the product class or the mission
		String template = productClass.getProductFileTemplate();
		if (null == template) {
			throw new IllegalStateException(MSG_FILENAME_TEMPLATE_NOT_FOUND);
		}
		
		// Get the fixed parts of the template
		String[] fixedParts = template.split("\\$\\{[^{]*\\}", -1);
		if (logger.isDebugEnabled()) logger.debug("Found fixed template parts: " + Arrays.asList(fixedParts));
		
		// Get expressions
		int expressionStartPos = template.indexOf("${");
		List<String> expressions = new ArrayList<>();
		while (-1 != expressionStartPos) {
			int expressionEndPos = template.indexOf("}", expressionStartPos);
			expressions.add(template.substring(expressionStartPos + 2, expressionEndPos));
			expressionStartPos = template.indexOf("${", expressionStartPos + 1);
		}
		if (logger.isDebugEnabled()) logger.debug("Found expressions: " + expressions);
		
		// Replace expressions
		ExpressionParser parser = new SpelExpressionParser();
		List<String> replacedExpressions = new ArrayList<>();
		for (String expression: expressions) {
			try {
				Expression exp = parser.parseExpression(expression);
				replacedExpressions.add((String) exp.getValue(this));
			} catch (EvaluationException e) {
				throw new EvaluationException(e.getMessage() + "\n Parameter expression: " + expression);
			}
		}
		if (logger.isDebugEnabled()) logger.debug("Replaced expressions: " + replacedExpressions);
		
		// Create result string
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < fixedParts.length; ++i) {
			result.append(fixedParts[i]);
			if (i < replacedExpressions.size()) {
				result.append(replacedExpressions.get(i));
			}
		}
		if (logger.isDebugEnabled()) logger.debug("Replaced template: " + result);
		
		return result.toString();
	}
	
	@Override
	public String toString() {
		return "Product [productClass=" + (null == productClass ? "null" : productClass.getProductType()) 
				+ ", configuredProcessor=" + (null == configuredProcessor ? "null" : configuredProcessor.getIdentifier())
				+ ", requestedStartTime=" + requestedStartTime + ", requestedStopTime=" + requestedStopTime
				+ ", sensingStartTime=" + sensingStartTime + ", sensingStopTime=" + sensingStopTime
				+ ", generationTime=" + generationTime + ", publicationTime=" + publicationTime
				+ ", evictionTime=" + evictionTime
				+ ", mode=" + mode + ", fileClass=" + fileClass + ", productQuality=" + productQuality
				+ ", productionType=" + productionType + ", parameters=" + parameters + "]";
	}
	@Override
	public int hashCode() {
		return Objects.hash(productClass, sensingStartTime); // most distinguishing attributes
	}
	/**
	 * Tests equality of products based on their attribute values. Returns true if either of the following alternatives holds:
	 * <ol>
	 *   <li>The database IDs are equal</li>
	 *   <li>The UUIDs are equal or at least one of the UUIDs is null (i.e. they do not have different UUIDs)</li>
	 *   <li>All of the following attributes are equal:
	 *     <ul>
	 *       <li>Product class</li>
	 *       <li>Configured processor</li>
	 *       <li>Sensing start/stop times</li>
	 *       <li>Processing mode</li>
	 *       <li>File class</li>
	 *       <li>Product quality</li>
	 *       <li>Production type</li>
	 *       <li>All product parameters present in both products (additional parameters on either product will be ignored)</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 * Note that the generation time is not considered relevant for product equality, because two processings with the
	 * same set of attributes as listed above are expected to produce the same output.
	 * 
	 * @param obj the object to compare to
	 * @return true, if the two objects are equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		// Object identity
		if (this == obj)
			return true;
		
		// Same database object
		if (super.equals(obj))
			return true;
		
		if (!(obj instanceof Product))
			return false;
		Product other = (Product) obj;
		
		// Both UUIDs are set: Same UUIDs?
		if (null != uuid) {
			if (uuid.equals(other.getUuid())) {
				return true;
			}
			else if (null != other.getUuid()) {
				// both UUIDs set, and they are different
				return false;
			}
		}
		// At least one UUID is null, so compare attributes
		
		// Overlapping parameters are the same (mandatory, but not sufficient)
		for (String key: parameters.keySet()) {
			if (other.getParameters().containsKey(key) && !parameters.get(key).equals(other.getParameters().get(key))) {
				return false;
			}
		}
		
		// All other attributes mentioned above are the same (mandatory and sufficient)
		return Objects.equals(configuredProcessor, other.getConfiguredProcessor())
				&& Objects.equals(fileClass, other.getFileClass())
				&& Objects.equals(mode, other.getMode())
				&& Objects.equals(productClass, other.getProductClass())
				&& productQuality == other.getProductQuality()
				&& productionType == other.getProductionType()
				&& Objects.equals(sensingStartTime, other.getSensingStartTime())
				&& Objects.equals(sensingStopTime, other.getSensingStopTime());
	}

}
