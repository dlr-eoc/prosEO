/**
 * PersistentObject.java
 * 
 * (C) 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.ElementCollection;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

/**
 * Abstract superclass of all persistent classes
 * 
 * @author Thomas Bassler
 */
@MappedSuperclass
@Inheritance(strategy=InheritanceType.JOINED)
abstract public class PersistentObject {
	/** Next object id for assignment */
	private static long nextId = System.currentTimeMillis(); // Seeded by the current time

	/**
	 * The persistent id of this object (an "assigned identifier" according to JPA).
	 */
	@Id
	private long id;
	
	/**
	 * A version identifier to track updates to the object (especially to detect concurrent update attempts).
	 */
	private int version;
	
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
	public class Parameter {
		/** The type of the parameter */
		private ParameterType type;
		/** The parameter value */
		private Object value;
		
		/**
		 * Gets the type of the parameter
		 * 
		 * @return the parameter type
		 */
		public ParameterType getType() {
			return type;
		}
		
		/**
		 * Sets the type of the parameter
		 * 
		 * @param type the type to set
		 */
		public void setType(ParameterType type) {
			this.type = type;
		}
		
		/**
		 * Gets the value of the parameter
		 * 
		 * @return the value
		 */
		public Object getValue() {
			return value;
		}
		
		/**
		 * Sets the value of the parameter
		 * 
		 * @param value the value to set
		 */
		public void setValue(Object value) {
			this.value = value;
		}
		
		/**
		 * Gets the parameter value as String, if it has the appropriate type
		 * @return the parameter value
		 * @throws ClassCastException if the parameter is not of type ParameterType.STRING
		 */
		public String getStringValue() throws ClassCastException {
			if (ParameterType.STRING.equals(type) && value instanceof String) {
				return (String) value;
			} else {
				throw new ClassCastException(String.format("Parameter of type %s cannot be converted to String", type.toString()));
			}
		}
		/**
		 * Sets the value of the parameter to the given string and the type to ParameterType.STRING
		 * @param newValue the value to set (the type is implicit)
		 */
		public void setStringValue(String newValue) {
			type = ParameterType.STRING;
			value = newValue;
		}

		/**
		 * Gets the parameter value as Integer, if it has the appropriate type
		 * @return the parameter value
		 * @throws ClassCastException if the parameter is not of type ParameterType.INTEGER
		 */
		public Integer getIntegerValue() throws ClassCastException {
			if (ParameterType.INTEGER.equals(type) && value instanceof Integer) {
				return (Integer) value;
			} else {
				throw new ClassCastException(String.format("Parameter of type %s cannot be converted to Integer", type.toString()));
			}
		}
		/**
		 * Sets the value of the parameter to the given integer and the type to ParameterType.INTEGER
		 * @param newValue the value to set (the type is implicit)
		 */
		public void setIntegerValue(Integer newValue) {
			type = ParameterType.INTEGER;
			value = newValue;
		}

		/**
		 * Gets the parameter value as Boolean, if it has the appropriate type
		 * @return the parameter value
		 * @throws ClassCastException if the parameter is not of type ParameterType.BOOLEAN
		 */
		public Boolean getBooleanValue() throws ClassCastException {
			if (ParameterType.BOOLEAN.equals(type) && value instanceof Boolean) {
				return (Boolean) value;
			} else {
				throw new ClassCastException(String.format("Parameter of type %s cannot be converted to Boolean", type.toString()));
			}
		}
		/**
		 * Sets the value of the parameter to the given boolean and the type to ParameterType.BOOLEAN
		 * @param newValue the value to set (the type is implicit)
		 */
		public void setBooleanValue(Boolean newValue) {
			type = ParameterType.BOOLEAN;
			value = newValue;
		}

		/**
		 * Gets the parameter value as Double, if it has the appropriate type
		 * @return the parameter value
		 * @throws ClassCastException if the parameter is not of type ParameterType.DOUBLE
		 */
		public Double getDoubleValue() throws ClassCastException {
			if (ParameterType.DOUBLE.equals(type) && value instanceof Double) {
				return (Double) value;
			} else {
				throw new ClassCastException(String.format("Parameter of type %s cannot be converted to Double", type.toString()));
			}
		}
		/**
		 * Sets the value of the parameter to the given double and the type to ParameterType.DOUBLE
		 * @param newValue the value to set (the type is implicit)
		 */
		public void setDoubleValue(Double newValue) {
			type = ParameterType.DOUBLE;
			value = newValue;
		}
		
		/**
		 * Gets the enclosing persistent object
		 * @return the enclosing object
		 */
		public PersistentObject getEnclosingObject() {
			return PersistentObject.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((type == null) ? 0 : getEnclosingObject().hashCode());
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			if (type != other.type)
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}		
	}
	
	/**
	 * Get the next available object id
	 * @return a unique object id
	 */
	private static synchronized long getNextId() {
		return ++nextId;
	}

	/**
	 * No-argument constructor that assigns the object id and initializes the version number
	 */
	public PersistentObject() {
		super();
		id = getNextId();
		version = 1;
	}

	/**
	 * Set the id of the persistent object.
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Gets the id of the persistent object
	 * @return the object id
	 */
	public long getId() {
		return this.id;
	}

	/**
	 * Gets the version of the persistent object
	 * @return the object version
	 */
	public int getVersion() {
		return version;
	}

	/**
	 * Increments the version of the persistent object
	 */
	public void incrementVersion() {
		this.version++;
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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

	/**
	 * Test equality of persistent objects based on their unique ID.
	 * 
	 * @param obj the object to compare this object to
	 * @return true, if obj is a persistent object and has the same ID, false otherwise
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof PersistentObject))
			return false;
		PersistentObject other = (PersistentObject) obj;
		return Objects.equals(id, other.id);
	}

}