/*
 * FilterExpression.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 * 
 * based on code from the Apache Olingo project
 */
package de.dlr.proseo.api.prip.odata;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDateTimeOffset;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDouble;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt32;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.expression.BinaryOperatorKind;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitor;
import org.apache.olingo.server.api.uri.queryoption.expression.Literal;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;
import org.apache.olingo.server.api.uri.queryoption.expression.MethodKind;
import org.apache.olingo.server.api.uri.queryoption.expression.UnaryOperatorKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterExpressionVisitor implements ExpressionVisitor<Object> {

	private Entity currentEntity;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductEdmProvider.class);

	public FilterExpressionVisitor(Entity currentEntity) {
		this.currentEntity = currentEntity;
	}

	@Override
	public Object visitMember(final Member member) throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitMember({})", member);
		
		// Travel the property tree given in the URI resources
		Iterator<UriResource> uriResourceIter = member.getResourcePath().getUriResourceParts().iterator();
		
		UriResource uriResource = uriResourceIter.next(); // At least one element is guaranteed to exist, otherwise the parser would not have led us here

		// Simple case: First property is primitive
		if (UriResourceKind.primitiveProperty.equals(uriResource.getKind())) {
			UriResourcePrimitiveProperty uriResourceProperty = (UriResourcePrimitiveProperty) uriResource;
			return currentEntity.getProperty(uriResourceProperty.getProperty().getName()).getValue();
		}
		
		// Get start entry
		Property currentProperty = null;
		if (UriResourceKind.complexProperty.equals(uriResource.getKind())) {
			currentProperty = currentEntity.getProperty(((UriResourceComplexProperty) uriResource).getProperty().getName());
		} else if (UriResourceKind.navigationProperty.equals(uriResource.getKind())) {
			currentProperty = currentEntity.getProperty(((UriResourceNavigation) uriResource).getProperty().getName());
		} else {
			throw new ODataApplicationException("Only primitive, complex and navigation properties are implemented in filter expressions", 
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}
		
		// Loop through all remaining entries
		while (uriResourceIter.hasNext()) {
			uriResource = uriResourceIter.next();
			
			// Extract the property name from the URI resource
			String newPropertyName = null;
			switch (uriResource.getKind()) {
			case primitiveProperty:
				newPropertyName = ((UriResourcePrimitiveProperty) uriResource).getProperty().getName();
				break;
			case complexProperty:
				newPropertyName = ((UriResourceComplexProperty) uriResource).getProperty().getName();
				break;
			case navigationProperty:
				newPropertyName = ((UriResourceNavigation) uriResource).getProperty().getName();
				break;
			default:
				throw new ODataApplicationException("Only primitive, complex and navigation properties and arrays thereof are implemented in filter expressions", 
						HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
			}
			
			// Find sub-property according to type of current property
			switch (currentProperty.getValueType())	{
			case COLLECTION_COMPLEX:
			case COLLECTION_ENTITY:
				// TODO Dubious - how to address the index of a collection? And the result probably is a ComplexType or an Entity, not a Property
				List<?> propertyCollection = currentProperty.asCollection();
				currentProperty = (Property) propertyCollection.get(Integer.parseInt(newPropertyName));
				break;
			case COMPLEX:
				for (Property property: currentProperty.asComplex().getValue()) {
					if (property.getName().equals(newPropertyName)) {
						currentProperty = property;
						break;
					}
				}
				break;
			case ENTITY:
				currentProperty = ((Entity) currentProperty.getValue()).getProperty(newPropertyName);
				break;
			default:
				throw new ODataApplicationException("Only primitive, complex and navigation properties and arrays thereof are implemented in filter expressions", 
						HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
			}
			
		}

		return currentProperty.getValue();
	}

	@Override
	public Object visitLiteral(Literal literal) throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitLiteral({})", literal);
		
		// We can be sure, that the literal is a valid OData literal because the URI Parser checks 
		// the lexicographical structure

		// String literals start and end with an single quotation mark
		String literalAsString = literal.getText();
		if(literal.getType() instanceof EdmString) {
			String stringLiteral = "";
			if(literal.getText().length() > 2) {
				// Remove quotes
				stringLiteral = literalAsString.substring(1, literalAsString.length() - 1);
			}
			return stringLiteral;
		} else if (literal.getType() instanceof EdmDateTimeOffset) {
			// Try to convert the literal into an Java Instant
			try {
				return Date.from(Instant.parse(literalAsString));
			} catch(NumberFormatException e) {
				throw new ODataApplicationException("Invalid date/time offset value " + literalAsString, 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
		} else if (literal.getType() instanceof EdmInt32) {
			// Try to convert the literal into an Java Integer
			try {
				return Integer.parseInt(literalAsString);
			} catch(NumberFormatException e) {
				throw new ODataApplicationException("Invalid integer value " + literalAsString, 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
		} else if (literal.getType() instanceof EdmDouble) {
			// Try to convert the literal into an Java Double
			try {
				return Double.parseDouble(literalAsString);
			} catch(NumberFormatException e) {
				throw new ODataApplicationException("Invalid double value " + literalAsString, 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
		} else {
			throw new ODataApplicationException("Only Edm.Int32, Edm.String, Edm.DateTimeOffset and Edm.Double literals are implemented", 
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}
	}

	@Override
	public Object visitUnaryOperator(UnaryOperatorKind operator, Object operand) 
			throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitUnaryOperator({}, {})", operator, operand);
		
		// OData allows two different unary operators. We have to take care, that the type of the operand fits to
		// operand

		if(operator == UnaryOperatorKind.NOT && operand instanceof Boolean) {
			// 1.) boolean negation 
			return !(Boolean) operand;
		} else if(operator == UnaryOperatorKind.MINUS && operand instanceof Integer){
			// 2.) arithmetic minus
			return -(Integer) operand;
		}

		// Operation not processed, throw an exception
		throw new ODataApplicationException("Invalid type for unary operator", 
				HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
	}

	@Override
	public Object visitBinaryOperator(BinaryOperatorKind operator, Object left, Object right) 
			throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitBinaryOperator({}, {}, {})", operator, left, right);

		// Binary Operators are split up in three different kinds. Up to the kind of the operator it can be applied 
		// to different types
		//   - Arithmetic operations like add, minus, modulo, etc. are allowed on numeric types like Edm.Int32
		//   - Logical operations are allowed on numeric types and also Edm.String
		//   - Boolean operations like and, or are allowed on Edm.Boolean
		// A detailed explanation can be found in OData Version 4.0 Part 2: URL Conventions 

		if (operator == BinaryOperatorKind.ADD
				|| operator == BinaryOperatorKind.MOD
				|| operator == BinaryOperatorKind.MUL
				|| operator == BinaryOperatorKind.DIV
				|| operator == BinaryOperatorKind.SUB) {
			return evaluateArithmeticOperation(operator, left, right);
		} else if (operator == BinaryOperatorKind.EQ
				|| operator == BinaryOperatorKind.NE
				|| operator == BinaryOperatorKind.GE
				|| operator == BinaryOperatorKind.GT
				|| operator == BinaryOperatorKind.LE
				|| operator == BinaryOperatorKind.LT) {
			return evaluateComparisonOperation(operator, left, right);
		} else if (operator == BinaryOperatorKind.AND
				|| operator == BinaryOperatorKind.OR) {
			return evaluateBooleanOperation(operator, left, right);
		} else {
			throw new ODataApplicationException("Binary operation " + operator.name() + " is not implemented", 
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}
	}

	private Object evaluateBooleanOperation(BinaryOperatorKind operator, Object left, Object right)
			throws ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> evaluateBooleanOperation({}, {}, {})", operator, left, right);

		// First check that both operands are of type Boolean
		if(left instanceof Boolean && right instanceof Boolean) {
			Boolean valueLeft = (Boolean) left;
			Boolean valueRight = (Boolean) right;

			// Than calculate the result value
			if(operator == BinaryOperatorKind.AND) {
				return valueLeft && valueRight;
			} else {
				// OR
				return valueLeft || valueRight;
			}
		} else {
			throw new ODataApplicationException("Boolean operations need two boolean operands", 
					HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
		}
	}

	private Object evaluateComparisonOperation(BinaryOperatorKind operator, Object left, Object right) 
			throws ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> evaluateComparisonOperation({}, {}, {})", operator, left, right);

		// All types in our tutorial supports all logical operations, but we have to make sure that the types are equals
		if(left.getClass().equals(right.getClass()) && left instanceof Comparable) {
			// Luckily all used types String, Boolean and also Integer support the interface Comparable
			@SuppressWarnings({ "rawtypes", "unchecked" })
			int result = ((Comparable) left).compareTo((Comparable) right);

			if (operator == BinaryOperatorKind.EQ) {
				return result == 0;
			} else if (operator == BinaryOperatorKind.NE) {
				return result != 0;
			} else if (operator == BinaryOperatorKind.GE) {
				return result >= 0;
			} else if (operator == BinaryOperatorKind.GT) {
				return result > 0;
			} else if (operator == BinaryOperatorKind.LE) {
				return result <= 0;
			} else {
				// BinaryOperatorKind.LT
				return result < 0;
			}

		} else {
			throw new ODataApplicationException("Comparision needs two equal types", 
					HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
		}
	}

	private Object evaluateArithmeticOperation(BinaryOperatorKind operator, Object left, 
			Object right) throws ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> evaluateArithmeticOperation({}, {}, {})", operator, left, right);

		// First check if the type of both operands is numerical
		if(left instanceof Number && right instanceof Number) {
			String leftString = left.toString();
			String rightString = right.toString();
			
			if (leftString.contains(".") || leftString.contains(",") || rightString.contains(".") || rightString.contains(",")) {
				// Do floating point arithmetic
				Double valueLeft = Double.valueOf(leftString);
				Double valueRight = Double.valueOf(rightString);

				// Than calculate the result value
				if(operator == BinaryOperatorKind.ADD) {
					return valueLeft + valueRight;
				} else if(operator == BinaryOperatorKind.SUB) {
					return valueLeft - valueRight;
				} else if(operator == BinaryOperatorKind.MUL) {
					return valueLeft * valueRight;
				} else if(operator == BinaryOperatorKind.DIV) {
					return valueLeft / valueRight;
				} else {
					// BinaryOperatorKind.MOD
					return valueLeft % valueRight;
				}
			} else {
				// Do integer arithmetic
				Long valueLeft = Long.valueOf(leftString);
				Long valueRight = Long.valueOf(rightString);

				// Than calculate the result value
				if(operator == BinaryOperatorKind.ADD) {
					return valueLeft + valueRight;
				} else if(operator == BinaryOperatorKind.SUB) {
					return valueLeft - valueRight;
				} else if(operator == BinaryOperatorKind.MUL) {
					return valueLeft * valueRight;
				} else if(operator == BinaryOperatorKind.DIV) {
					return valueLeft / valueRight;
				} else {
					// BinaryOperatorKind.MOD
					return valueLeft % valueRight;
				}
			}
		} else {
			throw new ODataApplicationException("Arithmetic operations needs two numeric operands", 
					HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
		}
	}

	@Override
	public Object visitMethodCall(MethodKind methodCall, List<Object> parameters) 
			throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitMethodCall({}, {})", methodCall, parameters);

		// To keep this tutorial small and simple, we implement only one method call
		if(MethodKind.CONTAINS == methodCall) {
			// "Contains" gets two parameters, both have to be of type String
			// e.g. /Products?$filter=contains(Description, '1024 MB')
			// 
			// First the method visistMember is called, which returns the current String value of the property.
			// After that the method visitLiteral is called with the string literal '1024 MB',
			// which returns a String
			//
			// Both String values are passed to visitMethodCall.
			if(parameters.get(0) instanceof String && parameters.get(1) instanceof String) {
				String valueParam1 = (String) parameters.get(0);
				String valueParam2 = (String) parameters.get(1);

				return valueParam1.contains(valueParam2);
			} else {
				throw new ODataApplicationException("contains() needs two parametres of type Edm.String", 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
		} else if (MethodKind.STARTSWITH == methodCall) {
			// "startswith" gets two parameters, both have to be of type String
			if(parameters.get(0) instanceof String && parameters.get(1) instanceof String) {
				String valueParam1 = (String) parameters.get(0);
				String valueParam2 = (String) parameters.get(1);

				return valueParam1.startsWith(valueParam2);
			} else {
				throw new ODataApplicationException("startswith() needs two parametres of type Edm.String", 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
		} else if (MethodKind.ENDSWITH == methodCall) {
			// "endswith" gets two parameters, both have to be of type String
			if(parameters.get(0) instanceof String && parameters.get(1) instanceof String) {
				String valueParam1 = (String) parameters.get(0);
				String valueParam2 = (String) parameters.get(1);

				return valueParam1.endsWith(valueParam2);
			} else {
				throw new ODataApplicationException("endswith() needs two parametres of type Edm.String", 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
		} else {
			throw new ODataApplicationException("Method call " + methodCall + " not implemented", 
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}
	}

	@Override
	public Object visitTypeLiteral(EdmType type) throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Type literals are not implemented", 
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	@Override
	public Object visitAlias(String aliasName) throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Aliases are not implemented", 
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	@Override
	public Object visitEnum(EdmEnumType type, List<String> enumValues) 
			throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitEnum({}, {})", type, enumValues);

		// Since no useful specification of this interface is given, the assumption is to 
		// - return a list of Integer values containing the internal representations of the enum strings given, or
		// - a single integer, if the size of the incoming list is 1
		if (1 == enumValues.size()) {
			try {
				return type.valueOfString(enumValues.get(0), false, null, null, null, false, Integer.class);
			} catch (EdmPrimitiveTypeException e) {
				throw new ODataApplicationException("Enum conversion failed for enum value " + enumValues.get(0) + ", cause: " + e.getMessage(), 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
		}
		
		List<Integer> enumInts = new ArrayList<>();
		for (String enumValue: enumValues) {
			try {
				enumInts.add(type.valueOfString(enumValue, false, null, null, null, false, Integer.class));
			} catch (EdmPrimitiveTypeException e) {
				throw new ODataApplicationException("Enum conversion failed for enum value " + enumValue + ", cause: " + e.getMessage(), 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
		}
		
		return enumInts;
	}

	@Override
	public Object visitLambdaExpression(String lambdaFunction, String lambdaVariable, Expression expression) 
			throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Lamdba expressions are not implemented", 
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	@Override
	public Object visitLambdaReference(String variableName) 
			throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Lamdba references are not implemented", 
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	@Override
	public Object visitBinaryOperator(BinaryOperatorKind operator, Object left, List<Object> right)
			throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Binary operators on lists are not implemented", 
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}
}
