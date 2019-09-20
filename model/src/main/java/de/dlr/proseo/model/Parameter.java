/**
 * Parameter.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.io.Serializable;

import javax.persistence.Embeddable;

/**
 * This class allows to add mission-specific parameters to any persistent object. A parameter consists of a type
 * (of the enum ParameterType) and a value with a class that corresponds to the type.
 * 
 * @author Dr. Thomas Bassler
 */
@Embeddable
public class Parameter {
	
	/* Error messages */
	private static final String MSG_INVALID_PARAMETER_VALUE_FOR_TYPE = "Invalid parameter value %s for type %s";
	private static final String MSG_PARAMETER_CANNOT_BE_CONVERTED = "Parameter of type %s cannot be converted to %s";
	
	/** The type of the parameter */
	private ParameterType parameterType;
	
	/** The parameter value */
	private Serializable parameterValue;
	
	/**
	 *  Enumeration of valid parameter types for mission-specific parameters
	 */
	public enum ParameterType { STRING, BOOLEAN, INTEGER, DOUBLE };
	
	/**
	 * Initializer with type and value (values of type Short and Float are converted to Integer and Double, respectively)
	 * 
	 * @param parameterType the type of the new parameter
	 * @param parameterValue the value of the new parameter
	 * @return the initialized parameter for chaining
	 */
	public Parameter init(ParameterType parameterType, Serializable parameterValue) {
		this.parameterType = parameterType;
		switch(parameterType) {
		case STRING:
			if (!(parameterValue instanceof String)) {
				throw new IllegalArgumentException(String.format(MSG_INVALID_PARAMETER_VALUE_FOR_TYPE, parameterValue.toString(), parameterType.toString()));
			}
			this.parameterValue = parameterValue;
			break;
		case BOOLEAN:
			if (!(parameterValue instanceof Boolean)) {
				throw new IllegalArgumentException(String.format(MSG_INVALID_PARAMETER_VALUE_FOR_TYPE, parameterValue.toString(), parameterType.toString()));
			}
			this.parameterValue = parameterValue;
			break;
		case INTEGER:
			if (!(parameterValue instanceof Short) && !(parameterValue instanceof Integer)) {
				throw new IllegalArgumentException(String.format(MSG_INVALID_PARAMETER_VALUE_FOR_TYPE, parameterValue.toString(), parameterType.toString()));
			}
			if (parameterValue instanceof Short) {
				this.parameterValue = Integer.valueOf((Short) parameterValue);
			} else {
				this.parameterValue = parameterValue;
			}
			break;
		case DOUBLE:
			if (!(parameterValue instanceof Float) && !(parameterValue instanceof Double)) {
				throw new IllegalArgumentException(String.format(MSG_INVALID_PARAMETER_VALUE_FOR_TYPE, parameterValue.toString(), parameterType.toString()));
			}
			if (parameterValue instanceof Float) {
				this.parameterValue = Double.valueOf((Float) parameterValue);
			} else {
				this.parameterValue = parameterValue;
			}
			break;
		}
		
		return this;
	}
	
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
	public void setParameterType(ParameterType parameterType) {
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
			throw new ClassCastException(String.format(MSG_PARAMETER_CANNOT_BE_CONVERTED, parameterType.toString(), ParameterType.STRING.toString()));
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
			throw new ClassCastException(String.format(MSG_PARAMETER_CANNOT_BE_CONVERTED, parameterType.toString(), ParameterType.INTEGER.toString()));
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
			throw new ClassCastException(String.format(MSG_PARAMETER_CANNOT_BE_CONVERTED, parameterType.toString(), ParameterType.BOOLEAN.toString()));
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
			throw new ClassCastException(String.format(MSG_PARAMETER_CANNOT_BE_CONVERTED, parameterType.toString(), ParameterType.DOUBLE.toString()));
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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