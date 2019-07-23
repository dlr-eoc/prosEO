/**
 * Product.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * Representation of a data product
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Entity
public class Product extends PersistentObject {
	
	/** Product class this products instantiates */
	@ManyToOne
	private ProductClass productClass;
	
	/** Sensing start time */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant sensingStartTime;
	
	/** Sensing stop time */
	@Column(columnDefinition = "TIMESTAMP(6)")
	private Instant sensingStopTime;
	
	/** Set of component products */
	@OneToMany(mappedBy = "enclosingProduct")
	private Set<Product> componentProducts;
	
	/** Product for which this product is a component */
	@ManyToOne
	private Product enclosingProduct;
	
	/** Orbit relationship of this product, if any */
	@ManyToOne
	private Orbit orbit;
	
	/** Product queries satisfied by this product */
	@ManyToMany
	private Set<ProductQuery> satisfiedProductQueries;
	
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
	
	/**
	 *  Enumeration of valid parameter types for mission-specific parameters
	 */
	public enum ParameterType { STRING, BOOLEAN, INTEGER, DOUBLE };
	
	/**
	 * This class allows to add mission-specific parameters to any persistent object. A parameter consists of a type
	 * (of the enum ParameterType) and a value with a class that corresponds to the type.
	 */
	@Embeddable
	public class Parameter {
		/** The type of the parameter */
		private ParameterType parameterType;
		/** The parameter value */
		private Serializable parameterValue;
		
		/**
		 * Gets the type of the parameter
		 * 
		 * @return the parameter type
		 */
		public ParameterType getParameterType() {
			return parameterType;
		}
		
		/**
		 * Sets the type of the parameter
		 * 
		 * @param parameterType the type to set
		 */
		public void seParametertType(ParameterType parameterType) {
			this.parameterType = parameterType;
		}
		
		/**
		 * Gets the value of the parameter
		 * 
		 * @return the parameter value
		 */
		public Serializable getParameterValue() {
			return parameterValue;
		}
		
		/**
		 * Sets the value of the parameter
		 * 
		 * @param parameterValue the value to set
		 */
		public void setParameterValue(Serializable parameterValue) {
			this.parameterValue = parameterValue;
		}
		
		/**
		 * Gets the parameter value as String, if it has the appropriate type
		 * @return the parameter value
		 * @throws ClassCastException if the parameter is not of type ParameterType.STRING
		 */
		public String getStringValue() throws ClassCastException {
			if (ParameterType.STRING.equals(parameterType) && parameterValue instanceof String) {
				return (String) parameterValue;
			} else {
				throw new ClassCastException(String.format("Parameter of type %s cannot be converted to String", parameterType.toString()));
			}
		}
		/**
		 * Sets the value of the parameter to the given string and the type to ParameterType.STRING
		 * @param newValue the value to set (the type is implicit)
		 */
		public void setStringValue(String newValue) {
			parameterType = ParameterType.STRING;
			parameterValue = newValue;
		}

		/**
		 * Gets the parameter value as Integer, if it has the appropriate type
		 * @return the parameter value
		 * @throws ClassCastException if the parameter is not of type ParameterType.INTEGER
		 */
		public Integer getIntegerValue() throws ClassCastException {
			if (ParameterType.INTEGER.equals(parameterType) && parameterValue instanceof Integer) {
				return (Integer) parameterValue;
			} else {
				throw new ClassCastException(String.format("Parameter of type %s cannot be converted to Integer", parameterType.toString()));
			}
		}
		/**
		 * Sets the value of the parameter to the given integer and the type to ParameterType.INTEGER
		 * @param newValue the value to set (the type is implicit)
		 */
		public void setIntegerValue(Integer newValue) {
			parameterType = ParameterType.INTEGER;
			parameterValue = newValue;
		}

		/**
		 * Gets the parameter value as Boolean, if it has the appropriate type
		 * @return the parameter value
		 * @throws ClassCastException if the parameter is not of type ParameterType.BOOLEAN
		 */
		public Boolean getBooleanValue() throws ClassCastException {
			if (ParameterType.BOOLEAN.equals(parameterType) && parameterValue instanceof Boolean) {
				return (Boolean) parameterValue;
			} else {
				throw new ClassCastException(String.format("Parameter of type %s cannot be converted to Boolean", parameterType.toString()));
			}
		}
		/**
		 * Sets the value of the parameter to the given boolean and the type to ParameterType.BOOLEAN
		 * @param newValue the value to set (the type is implicit)
		 */
		public void setBooleanValue(Boolean newValue) {
			parameterType = ParameterType.BOOLEAN;
			parameterValue = newValue;
		}

		/**
		 * Gets the parameter value as Double, if it has the appropriate type
		 * @return the parameter value
		 * @throws ClassCastException if the parameter is not of type ParameterType.DOUBLE
		 */
		public Double getDoubleValue() throws ClassCastException {
			if (ParameterType.DOUBLE.equals(parameterType) && parameterValue instanceof Double) {
				return (Double) parameterValue;
			} else {
				throw new ClassCastException(String.format("Parameter of type %s cannot be converted to Double", parameterType.toString()));
			}
		}
		/**
		 * Sets the value of the parameter to the given double and the type to ParameterType.DOUBLE
		 * @param newValue the value to set (the type is implicit)
		 */
		public void setDoubleValue(Double newValue) {
			parameterType = ParameterType.DOUBLE;
			parameterValue = newValue;
		}
		
		/**
		 * Gets the enclosing product
		 * @return the enclosing product
		 */
		public Product getEnclosingObject() {
			return Product.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((parameterType == null) ? 0 : getEnclosingObject().hashCode());
			result = prime * result + ((parameterType == null) ? 0 : parameterType.hashCode());
			result = prime * result + ((parameterValue == null) ? 0 : parameterValue.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof Parameter))
				return false;
			Parameter other = (Parameter) obj;
			if (!getEnclosingObject().equals(other.getEnclosingObject()))
				return false;
			if (parameterType != other.parameterType)
				return false;
			if (parameterValue == null) {
				if (other.parameterValue != null)
					return false;
			} else if (!parameterValue.equals(other.parameterValue))
				return false;
			return true;
		}		
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
	 * @return the parameter value casted to String
	 * @throws ClassCastException if the named parameter is not of an appropriate type
	 */
	public String getStringParameter(String key) throws ClassCastException {
		return parameters.get(key).getStringValue();
	}

	/**
	 * Set the named String parameter to the given value
	 * @param parameters the parameters to set
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
	 * @return the parameter value casted to Integer
	 * @throws ClassCastException if the named parameter is not of an appropriate type
	 */
	public Integer getIntegerParameter(String key) throws ClassCastException {
		return parameters.get(key).getIntegerValue();
	}

	/**
	 * Set the named Integer parameter to the given value
	 * @param parameters the parameters to set
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
	 * @return the parameter value casted to Boolean
	 * @throws ClassCastException if the named parameter is not of an appropriate type
	 */
	public Boolean getBooleanParameter(String key) throws ClassCastException {
		return parameters.get(key).getBooleanValue();
	}

	/**
	 * Set the named Boolean parameter to the given value
	 * @param parameters the parameters to set
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
	 * @return the parameter value casted to Double
	 * @throws ClassCastException if the named parameter is not of an appropriate type
	 */
	public Double getDoubleParameter(String key) throws ClassCastException {
		return parameters.get(key).getDoubleValue();
	}

	/**
	 * Set the named Double parameter to the given value
	 * @param parameters the parameters to set
	 */
	public void setDoubleParameter(String key, Double value) {
		Parameter param = parameters.get(key);
		if (null == param) {
			param = new Parameter();
		}
		param.setDoubleValue(value);
		this.parameters.put(key, param);
	}

}
