/**
 * Parameter.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import de.dlr.proseo.model.enums.ParameterType;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import jakarta.persistence.Column;

/**
 * This class allows to add mission-specific parameters to any persistent object. A parameter consists of a type
 * (of the enum ParameterType) and a value with a class that corresponds to the type.
 * 
 * @author Dr. Thomas Bassler
 */
@Embeddable
public class Parameter {
	
	private static final int MAX_VARCHAR_LENGTH = 255;
	/* Error messages */
	private static final String MSG_INVALID_PARAMETER_VALUE_FOR_TYPE = "Invalid parameter value %s for type %s";
	private static final String MSG_PARAMETER_CANNOT_BE_CONVERTED = "Parameter of type %s cannot be converted to %s";
	
	/** The type of the parameter */
	@Enumerated(EnumType.STRING)
	@Basic(optional = false)
	private ParameterType parameterType;
	
	/** 
	 * The parameter value (unless it is a string of more than 255 characters, in which case the parameter value is stored
	 * in parameterClob). 
	 */
	private String parameterValue;
	
	/**
	 * A string parameter value of more than 255 characters (takes precedence over any value in the parameterValue attribute).
	 */
	@Column(columnDefinition="CLOB")
	private String parameterClob;
	
	/**
	 * Initializer with type and value (values of type Short and Float are converted to Integer and Double, respectively)
	 * 
	 * @param parameterType the type of the new parameter
	 * @param parameterValue the value of the new parameter
	 * @return the initialized parameter for chaining
	 * @throws IllegalArgumentException if parameter type and class of parameter value do not match
	 */
	public Parameter init(ParameterType parameterType, Serializable parameterValue) throws IllegalArgumentException {
		this.parameterType = parameterType;
		setParameterValue(parameterValue);
		
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
	public String getParameterValue() { 
		return (null == parameterClob ? parameterValue : parameterClob);
	}
	
	/**
	 * Sets the value of the parameter
	 * 
	 * @param parameterValue the value to set
	 * @throws IllegalArgumentException if parameter type and class of parameter value do not match
	 */
	public void setParameterValue(Serializable parameterValue) throws IllegalArgumentException {
		switch(parameterType) {
		case STRING:
			String parameterValueString = parameterValue.toString();
			if (MAX_VARCHAR_LENGTH < parameterValueString.length()) {
				this.parameterClob = parameterValueString;
				this.parameterValue = parameterValueString.substring(0, MAX_VARCHAR_LENGTH - 3) + "...";
			} else {
				this.parameterValue = parameterValueString;
			}
			break;
		case BOOLEAN:
			if (parameterValue instanceof Boolean) {
				this.parameterValue = parameterValue.toString();
				break;
			}
			if (parameterValue instanceof String) {
				// Boolean.parseBoolean() is too lenient for us!
				if ("true".equals(parameterValue.toString().toLowerCase())) {
					this.parameterValue = "true";
					break;
				} else if ("false".equals(parameterValue.toString().toLowerCase())) {
					this.parameterValue = "false";
					break;
				}
			}
			throw new IllegalArgumentException(String.format(MSG_INVALID_PARAMETER_VALUE_FOR_TYPE, parameterValue.toString(), parameterType.toString()));
		case INTEGER:
			if (parameterValue instanceof Short || parameterValue instanceof Integer) {
				this.parameterValue = parameterValue.toString();
				break;
			}
			if (parameterValue instanceof String) {
				try {
					this.parameterValue = "" + Integer.parseInt((String) parameterValue);
					break;
				} catch (NumberFormatException e) {
					// Handled below
				}
			}
			throw new IllegalArgumentException(String.format(MSG_INVALID_PARAMETER_VALUE_FOR_TYPE, parameterValue.toString(), 
					parameterType.toString()));
		case DOUBLE:
			if (parameterValue instanceof Float || parameterValue instanceof Double) {
				this.parameterValue = parameterValue.toString();
			}
			if (parameterValue instanceof String) {
				try {
					this.parameterValue = "" + Double.parseDouble((String) parameterValue);
					break;
				} catch (NumberFormatException e) {
					// Handled below
				}
			}
			throw new IllegalArgumentException(String.format(MSG_INVALID_PARAMETER_VALUE_FOR_TYPE, parameterValue.toString(), 
					parameterType.toString()));
		case INSTANT:
			if (parameterValue instanceof TemporalAccessor) {
				this.parameterValue = OrbitTimeFormatter.format((TemporalAccessor) parameterValue);
			}
			if (parameterValue instanceof String) {
				try {
					this.parameterValue = OrbitTimeFormatter.format(OrbitTimeFormatter.parseDateTime((String) parameterValue));
					break;
				} catch (DateTimeException e) {
					// Handled below
				}
			}
			throw new IllegalArgumentException(String.format(MSG_INVALID_PARAMETER_VALUE_FOR_TYPE, parameterValue.toString(), 
					parameterType.toString()));
		}
	}
	
	/**
	 * Gets the parameter value as String
	 * @return the parameter value
	 */
	public String getStringValue() throws ClassCastException {
		return getParameterValue();
	}
	
	/**
	 * Sets the value of the parameter to the given string and the type to ParameterType.STRING
	 * @param newValue the value to set (the type is implicit)
	 */
	public void setStringValue(String newValue) {
		parameterType = ParameterType.STRING;
		setParameterValue(newValue);
	}

	/**
	 * Gets the parameter value as Integer, if it has the appropriate type
	 * @return the parameter value
	 * @throws ClassCastException if the parameter is not of type ParameterType.INTEGER
	 */
	public Integer getIntegerValue() throws ClassCastException {
		if (ParameterType.INTEGER.equals(parameterType)) {
			return Integer.parseInt(parameterValue);
		} else {
			throw new ClassCastException(String.format(MSG_PARAMETER_CANNOT_BE_CONVERTED, parameterType.toString(),
					ParameterType.INTEGER.toString()));
		}
	}
	/**
	 * Sets the value of the parameter to the given integer and the type to ParameterType.INTEGER
	 * @param newValue the value to set (the type is implicit)
	 */
	public void setIntegerValue(Integer newValue) {
		parameterType = ParameterType.INTEGER;
		parameterValue = newValue.toString();
	}

	/**
	 * Gets the parameter value as Boolean, if it has the appropriate type
	 * @return the parameter value
	 * @throws ClassCastException if the parameter is not of type ParameterType.BOOLEAN
	 */
	public Boolean getBooleanValue() throws ClassCastException {
		if (ParameterType.BOOLEAN.equals(parameterType)) {
			return Boolean.parseBoolean(parameterValue);
		} else {
			throw new ClassCastException(String.format(MSG_PARAMETER_CANNOT_BE_CONVERTED, parameterType.toString(), 
					ParameterType.BOOLEAN.toString()));
		}
	}
	/**
	 * Sets the value of the parameter to the given boolean and the type to ParameterType.BOOLEAN
	 * @param newValue the value to set (the type is implicit)
	 */
	public void setBooleanValue(Boolean newValue) {
		parameterType = ParameterType.BOOLEAN;
		parameterValue = newValue.toString();
	}

	/**
	 * Gets the parameter value as Double, if it has the appropriate type
	 * @return the parameter value
	 * @throws ClassCastException if the parameter is not of type ParameterType.DOUBLE
	 */
	public Double getDoubleValue() throws ClassCastException {
		if (ParameterType.DOUBLE.equals(parameterType)) {
			return Double.parseDouble(parameterValue);
		} else {
			throw new ClassCastException(String.format(MSG_PARAMETER_CANNOT_BE_CONVERTED, parameterType.toString(), 
					ParameterType.DOUBLE.toString()));
		}
	}
	/**
	 * Sets the value of the parameter to the given double and the type to ParameterType.DOUBLE
	 * @param newValue the value to set (the type is implicit)
	 */
	public void setDoubleValue(Double newValue) {
		parameterType = ParameterType.DOUBLE;
		parameterValue = newValue.toString();
	}
	
	/**
	 * Gets the parameter value as Instant, if it has the appropriate type
	 * @return the parameter value
	 * @throws ClassCastException if the parameter is not of type ParameterType.INSTANT
	 */
	public Instant getInstantValue() throws ClassCastException {
		if (ParameterType.INSTANT.equals(parameterType)) {
			return Instant.from(OrbitTimeFormatter.parseDateTime(parameterValue));
		} else {
			throw new ClassCastException(String.format(MSG_PARAMETER_CANNOT_BE_CONVERTED, parameterType.toString(), 
					ParameterType.INSTANT.toString()));
		}
	}
	/**
	 * Sets the value of the parameter to the given instant and the type to ParameterType.INSTANT
	 * @param newValue the value to set (the type is implicit)
	 */
	public void setInstantValue(TemporalAccessor newValue) {
		parameterType = ParameterType.INSTANT;
		parameterValue = OrbitTimeFormatter.format(newValue);
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
		return Objects.equals(this.getParameterValue(), other.getParameterValue());
	}

	@Override
	public String toString() {
		return "Parameter [parameterType=" + parameterType + ", parameterValue=" + parameterValue + "]";
	}		
}