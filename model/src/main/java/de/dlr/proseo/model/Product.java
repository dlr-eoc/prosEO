/**
 * Product.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.time.Instant;
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
import javax.persistence.Index;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.standard.SpelExpressionParser;

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
		@Index(unique = false, columnList = "product_class_id, generation_time") })
public class Product extends PersistentObject {
	
	private static final String MSG_FILENAME_TEMPLATE_NOT_FOUND = "Product filename template for mission not found";

	/**
	 * Universally unique product identifier (Production Interface Delivery Point Specification, sec. 3.1)
	 */
	@Column(nullable = false)
	private UUID uuid;
	
	/** Product class this products instantiates */
	@ManyToOne
	private ProductClass productClass;
	
	/** One of the file classes defined for the mission (Ground Segment FIle Format Standard, sec. 4.1.2) */
	private String fileClass;
	
	/** Processing modes as defined for the enclosing mission (only if generated by this mission) */
	private String mode;
	
	/** Indicator for the suitability of this product for general use */
	@Enumerated(EnumType.STRING)
	private ProductQuality productQuality = ProductQuality.NOMINAL;
	
	/** Sensing start time */
	@Column(name = "sensing_start_time", columnDefinition = "TIMESTAMP(6)")
	private Instant sensingStartTime;
	
	/** Sensing stop time */
	@Column(name = "sensing_stop_time", columnDefinition = "TIMESTAMP(6)")
	private Instant sensingStopTime;
	
	/** Product generation time */
	@Column(name = "generation_time", columnDefinition = "TIMESTAMP(6)")
	private Instant generationTime;
	
	/** Type of production process generating this product */
	@Enumerated(EnumType.STRING)
	private ProductionType productionType;
	
	/** Set of component products */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "enclosingProduct")
	private Set<Product> componentProducts = new HashSet<>();
	
	/** Product for which this product is a component */
	@ManyToOne
	private Product enclosingProduct;
	
	/**
	 * Orbit relationship of this product, if any
	 * 
	 * If a product is associated to an orbit, its validity period may still differ from the orbit times, but its validity end time 
	 * must not extend further than into the following orbit (e. g. a near-realtime time slice beginning in one orbit, but also 
	 * including some data from the subsequent orbit).
	 */
	@ManyToOne
	private Orbit orbit;
	
	/** Product files for this product */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "product")
	private Set<ProductFile> productFile = new HashSet<>();
	
	/** Product queries satisfied by this product */
	@ManyToMany(mappedBy = "satisfyingProducts")
	private Set<ProductQuery> satisfiedProductQueries = new HashSet<>();
	
	/** Job step that produced this product (if any) */
	@OneToOne
	private JobStep jobStep;
	
	/** Processor configuration used for processing this product */
	@ManyToOne
	private ConfiguredProcessor configuredProcessor;
	
	/**
	 * A collection of mission-specific parameters for this object
	 */
	@ElementCollection
	private Map<String, Parameter> parameters = new HashMap<>();
	
	/** The logger for this class */
	private static final Logger logger = LoggerFactory.getLogger(Product.class);
	
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
	 * sets the product quality indicator
	 * 
	 * @param productQuality the product quality to set
	 */
	public void setProductQuality(ProductQuality productQuality) {
		this.productQuality = productQuality;
	}
	/**
	 * Gets the sensing start time
	 * 
	 * @return the sensingStartTime
	 */
	public Instant getSensingStartTime() {
		return sensingStartTime;
	}
	/**
	 * Sets the sensing start time
	 * 
	 * @param sensingStartTime the sensingStartTime to set
	 */
	public void setSensingStartTime(Instant sensingStartTime) {
		this.sensingStartTime = sensingStartTime;
	}
	
	/**
	 * Gets the sensing stop time
	 * 
	 * @return the sensingStopTime
	 */
	public Instant getSensingStopTime() {
		return sensingStopTime;
	}
	
	/**
	 * Sets the sensing stop time
	 * 
	 * @param sensingStopTime the sensingStopTime to set
	 */
	public void setSensingStopTime(Instant sensingStopTime) {
		this.sensingStopTime = sensingStopTime;
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
			Expression exp = parser.parseExpression(expression);
			replacedExpressions.add((String) exp.getValue(this));
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
				+ ", sensingStartTime=" + sensingStartTime + ", sensingStopTime=" + sensingStopTime
				+ ", generationTime=" + generationTime
				+ ", mode=" + mode + ", fileClass=" + fileClass + ", productQuality=" + productQuality
				+ ", productionType=" + productionType + ", parameters=" + parameters + "]";
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(configuredProcessor, fileClass, mode, parameters, productClass,
				productQuality, productionType, sensingStartTime, sensingStopTime);
		return result;
	}
	/**
	 * Tests equality of products based on their attribute values. Returns true if either of the following alternatives holds:
	 * <ol>
	 *   <li>The database IDs are equal</li>
	 *   <li>The UUIDs are equal</li>
	 *   <li>All of the following attributes are equal:
	 *     <ul>
	 *       <li>Product class</li>
	 *       <li>Configured processor</li>
	 *       <li>Sensing start/stop times</li>
	 *       <li>Processing mode</li>
	 *       <li>File class</li>
	 *       <li>Product quality</li>
	 *       <li>Production type</li>
	 *       <li>All product parameters</li>
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
		if (this == obj)
			return true;
		if (super.equals(obj))
			return true;
		if (!(obj instanceof Product))
			return false;
		Product other = (Product) obj;
		if (uuid.equals(other.uuid))
			return true;
		return Objects.equals(configuredProcessor, other.configuredProcessor) && Objects.equals(fileClass, other.fileClass)
				&& Objects.equals(mode, other.mode)
				&& Objects.equals(parameters, other.parameters) && Objects.equals(productClass, other.productClass)
				&& productQuality == other.productQuality && productionType == other.productionType
				&& Objects.equals(sensingStartTime, other.sensingStartTime)
				&& Objects.equals(sensingStopTime, other.sensingStopTime);
	}

}
