/*
 * SqlFilterExpressionVisitor.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.olingo.commons.api.edm.EdmEnumType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.commons.core.edm.primitivetype.EdmByte;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDateTimeOffset;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDecimal;
import org.apache.olingo.commons.core.edm.primitivetype.EdmDouble;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt16;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt32;
import org.apache.olingo.commons.core.edm.primitivetype.EdmInt64;
import org.apache.olingo.commons.core.edm.primitivetype.EdmSByte;
import org.apache.olingo.commons.core.edm.primitivetype.EdmSingle;
import org.apache.olingo.commons.core.edm.primitivetype.EdmString;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceComplexProperty;
import org.apache.olingo.server.api.uri.UriResourceKind;
import org.apache.olingo.server.api.uri.UriResourceLambdaAny;
import org.apache.olingo.server.api.uri.UriResourceLambdaVariable;
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

import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Evaluation of OData expressions
 *  
 * @author Dr. Thomas Bassler
 */
public class SqlFilterExpressionVisitor implements ExpressionVisitor<String> {
	
	/** Mapping from OData member names to SQL schema names */
	private static Map<String, String> oDataToSqlMap = new HashMap<>();
	private static String[][] ODATA_TO_SQL_MAPPING = {
			{ ProductEdmProvider.GENERIC_PROP_ID, "p.uuid" },
			{ ProductEdmProvider.GENERIC_PROP_NAME, "ppf.product_file_name" },
			{ ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, null }, // not part of data model
			{ ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, "ppf.file_size" },
			{ ProductEdmProvider.ET_PRODUCT_PROP_PUBLICATION_DATE, "p.generation_time" }, // TODO to be updated to publication date attribute, when available
			{ ProductEdmProvider.ET_PRODUCT_PROP_CHECKSUMS, "ppf.checksum" },
			{ ProductEdmProvider.ET_PRODUCT_PROP_PRODUCTION_TYPE, "p.production_type" },
			{ ProductEdmProvider.ET_PRODUCT_PROP_CONTENT_DATE + "/" + ProductEdmProvider.CT_TIMERANGE_PROP_START, "p.sensing_start_time" },
			{ ProductEdmProvider.ET_PRODUCT_PROP_CONTENT_DATE + "/" + ProductEdmProvider.CT_TIMERANGE_PROP_END, "p.sensing_stop_time" },
			{ ProductEdmProvider.ET_PRODUCT_PROP_CHECKSUMS + "/" + ProductEdmProvider.CT_CHECKSUM_PROP_ALGORITHM, null },
			{ ProductEdmProvider.ET_PRODUCT_PROP_CHECKSUMS + "/" + ProductEdmProvider.CT_CHECKSUM_PROP_VALUE, "ppf.checksum" },
			{ ProductEdmProvider.ET_PRODUCT_PROP_CHECKSUMS + "/" + ProductEdmProvider.CT_CHECKSUM_PROP_CHECKSUM_DATE, "ppf.checksum_time" }
	};
	
	private static Map<String, String> oDataToPropertyMap = new HashMap<>();
	private static String[][] ODATA_TO_PROPERTY_MAPPING = {
	};

	private static final DateTimeFormatter sqlTimestampFormatter =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS Z").withZone(ZoneId.of("UTC"));

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(SqlFilterExpressionVisitor.class);

	/* Initialize OData-to-SQL name mapping */
	{
		for (int i = 0; i < ODATA_TO_SQL_MAPPING.length; ++i) {
			oDataToSqlMap.put(ODATA_TO_SQL_MAPPING[i][0], ODATA_TO_SQL_MAPPING[i][1]);
		}
		for (int i = 0; i < ODATA_TO_PROPERTY_MAPPING.length; ++i) {
			oDataToPropertyMap.put(ODATA_TO_PROPERTY_MAPPING[i][0], ODATA_TO_PROPERTY_MAPPING[i][1]);
		}
	}
	
	@Override
	public String visitMember(final Member member) throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitMember({})", member);
		
		// Travel the property tree given in the URI resources
		Iterator<UriResource> uriResourceIter = member.getResourcePath().getUriResourceParts().iterator();
		
		UriResource uriResource = uriResourceIter.next(); // At least one element is guaranteed to exist, otherwise the parser would not have led us here

		// Simple case: First property is primitive
		if (UriResourceKind.primitiveProperty.equals(uriResource.getKind())) {
			UriResourcePrimitiveProperty uriResourceProperty = (UriResourcePrimitiveProperty) uriResource;
			if (logger.isTraceEnabled()) logger.trace("... found primitive property: " + uriResourceProperty.getProperty().getName());
			String mappedProperty = oDataToSqlMap.get(uriResourceProperty.getProperty().getName());
			if (logger.isTraceEnabled()) logger.trace("... mapped primitive property: " + mappedProperty);

			if (logger.isTraceEnabled()) logger.trace("<<< visitMember()");
			return (null == mappedProperty ? "NOT FOUND" : mappedProperty);
		}
		
		// TODO From here: Analyse, what is actually happening, then rewrite for SQL "WHERE" clause
		
		// Get start entry
		StringBuilder propertyName = new StringBuilder();
		switch (uriResource.getKind()) {
		case complexProperty:
			EdmProperty edmProperty = ((UriResourceComplexProperty) uriResource).getProperty();
			if (logger.isTraceEnabled()) logger.trace("... found complex property: " + edmProperty.getName());
			propertyName.append(edmProperty.getName());
			break;
		case navigationProperty:
			EdmNavigationProperty edmNavProperty = ((UriResourceNavigation) uriResource).getProperty();
			if (logger.isTraceEnabled()) logger.trace("... found navigation property: " + edmNavProperty.getName());
			propertyName.append(edmNavProperty.getName());
			break;
		case lambdaVariable:
			UriResourceLambdaVariable lambdaVariable = (UriResourceLambdaVariable) uriResource;
			if (logger.isTraceEnabled()) logger.trace("... found lambda variable: " + lambdaVariable.getVariableName());
			propertyName.append(lambdaVariable.getVariableName());
			break;
		default:
			if (logger.isTraceEnabled()) logger.trace("... found unknown property of kind: " + uriResource.getKind());
			throw new ODataApplicationException("Only primitive, complex, navigation and lambda variable URI resources are implemented in filter expressions", 
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}
		
		// Loop through all remaining entries
		while (uriResourceIter.hasNext()) {
			uriResource = uriResourceIter.next();
			propertyName.append('/');
			if (logger.isTraceEnabled()) logger.trace("... next path property of kind: " + uriResource.getKind());
			
			// Extract the property name from the URI resource
			switch (uriResource.getKind()) {
			case primitiveProperty:
				propertyName.append(((UriResourcePrimitiveProperty) uriResource).getProperty().getName());
				break;
			case complexProperty:
				propertyName.append(((UriResourceComplexProperty) uriResource).getProperty().getName());
				break;
			case navigationProperty:
				propertyName.append(((UriResourceNavigation) uriResource).getProperty().getName());
				break;
			case lambdaAny:
				UriResourceLambdaAny lambdaResource = (UriResourceLambdaAny) uriResource;
				String result = visitLambdaExpression(lambdaResource.getSegmentValue(), lambdaResource.getLambdaVariable(), lambdaResource.getExpression());
				propertyName = new StringBuilder(result);
				break;
			default:
				throw new ODataApplicationException("Only primitive, complex, navigation and lambda 'any' URI resources and arrays thereof are allowed as sub-paths in filter expressions", 
						HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
			}
			
			// Find sub-property according to type of current property
//			switch (currentProperty.getValueType())	{
//			case COLLECTION_COMPLEX:
//			case COLLECTION_ENTITY:
//				// TODO Dubious - how to address the index of a collection? And the result probably is a ComplexType or an Entity, not a Property
//				List<?> propertyCollection = currentProperty.asCollection();
//				currentProperty = (Property) propertyCollection.get(Integer.parseInt(newPropertyName));
//				break;
//			case COMPLEX:
//				// Just to avoid NPEs
//				if (null == currentProperty.asComplex().getValue()) break;
//				for (Property property: currentProperty.asComplex().getValue()) {
//					if (property.getName().equals(newPropertyName)) {
//						currentProperty = property;
//						break;
//					}
//				}
//				break;
//			case ENTITY:
//				currentProperty = ((Entity) currentProperty.getValue()).getProperty(newPropertyName);
//				break;
//			default:
//				throw new ODataApplicationException("Only primitive, complex and navigation properties and arrays thereof are implemented in filter expressions", 
//						HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
//			}
			
		}
		if (logger.isTraceEnabled()) logger.trace("... derived property: " + propertyName);
		String mappedProperty = oDataToSqlMap.get(propertyName.toString());
		if (logger.isTraceEnabled()) logger.trace("... mapped property: " + propertyName);

		if (logger.isTraceEnabled()) logger.trace("<<< visitMember()");
		return (null == mappedProperty ? "NOT FOUND" : mappedProperty);
	}

	@Override
	public String visitLiteral(Literal literal) throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitLiteral({})", literal);
		
		// We can be sure, that the literal is a valid OData literal because the URI Parser checks 
		// the lexicographical structure

		// String literals start and end with an single quotation mark
		String literalAsString = literal.getText();
		String result = null;
		if(literal.getType() instanceof EdmString) {
			String stringLiteral = "";
			if(literal.getText().length() > 2) {
				// Remove OData quotes
				stringLiteral = literalAsString.substring(1, literalAsString.length() - 1);
			}
			// Add SQL string quotes
			result = "'" + stringLiteral + "'";
		} else if (literal.getType() instanceof EdmDateTimeOffset) {
			// Try to convert the literal into an Java Instant
			try {
				Instant literalAsInstant = Instant.from(OrbitTimeFormatter.parse(literalAsString));
				result = "'" + sqlTimestampFormatter.format(literalAsInstant) + "'";
			} catch(DateTimeParseException e) {
				throw new ODataApplicationException("Invalid date/time offset value " + literalAsString, 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
		} else if (literal.getType() instanceof EdmByte || literal.getType() instanceof EdmSByte
				|| literal.getType() instanceof EdmInt16 || literal.getType() instanceof EdmInt32
				|| literal.getType() instanceof EdmInt64 || literal.getType() instanceof EdmDecimal
				|| literal.getType() instanceof EdmSingle || literal.getType() instanceof EdmDouble) {
			result = literalAsString;
		} else {
			if (logger.isTraceEnabled()) logger.trace("... found literal of type: " + literal.getType().getName());
			throw new ODataApplicationException("Only Edm.Byte, Edm.SByte, Edm.Int16, Edm.Int32, Edm.Int64, Edm.String, Edm.DateTimeOffset, Edm.Decimal, Edm.Single and Edm.Double literals are implemented", 
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< visitLiteral()");
		return result;
	}

	@Override
	public String visitUnaryOperator(UnaryOperatorKind operator, String operand) 
			throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitUnaryOperator({}, {})", operator, operand);
		
		String result = null;
		switch (operator) {
		case NOT:
			result = "NOT " + operand;
			break;
		case MINUS:
			result = "-" + operand;
			break;
		default:
			throw new ODataApplicationException("Invalid type " + operator + " for unary operator", 
					HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< visitUnaryOperator()");
		return result;
	}

	@Override
	public String visitBinaryOperator(BinaryOperatorKind operator, String left, String right) 
			throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitBinaryOperator({}, {}, {})", operator, left, right);

		// Binary Operators are split up in three different kinds. Up to the kind of the operator it can be applied 
		// to different types
		//   - Arithmetic operations like add, minus, modulo, etc. are allowed on numeric types like Edm.Int32
		//   - Logical operations are allowed on numeric types and also Edm.String
		//   - Boolean operations like and, or are allowed on Edm.Boolean
		// A detailed explanation can be found in OData Version 4.0 Part 2: URL Conventions 
		
		String result = null;
		switch (operator) {
		// Arithmetic operators
		case ADD:
			result = "" + left + " + " + right;
			break;
		case MUL:
			result = "" + left + " * " + right;
			break;
		case DIV:
			result = "" + left + " / " + right;
			break;
		case SUB:
			result = "" + left + " - " + right;
			break;
		// Comparison operators
		case EQ:
			result = "" + left + " = " + right;
			break;
		case NE:
			result = "" + left + " <> " + right;
			break;
		case GE:
			result = "" + left + " >= " + right;
			break;
		case GT:
			result = "" + left + " > " + right;
			break;
		case LE:
			result = "" + left + " <= " + right;
			break;
		case LT:
			result = "" + left + " < " + right;
			break;
		// Boolean operators
		case AND:
			result = "(" + left + " AND " + right + ")";
			break;
		case OR:
			result = "(" + left + " OR " + right + ")";
			break;
		default:
			throw new ODataApplicationException("Binary operation " + operator.name() + " is not implemented", 
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< visitBinaryOperator()");
		return result;
	}

	/**
	 * Evaluation of a method call. Currently only the methods "contains", "startswith" and "endswith" are implemented
	 * 
	 * @param methodCall the method to execute
	 * @param parameters the parameters to pass to the method
	 * @return application return value of any type
	 * @throws ExpressionVisitException in no case (not used)
	 * @throws ODataApplicationException if the parameters are not applicable or insufficient for the requested method, 
	 *             or if an unimplemented method was requested
	 */
	@Override
	public String visitMethodCall(MethodKind methodCall, List<String> parameters) 
			throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitMethodCall({}, {})", methodCall, parameters);
		
		String result = null;
		switch (methodCall) {
		case CONTAINS:
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
				// Remove quotes added by visitLiteral
				valueParam2 = valueParam2.substring(1, valueParam2.length() - 1);

				result = valueParam1 + " LIKE '%" + valueParam2 + "%'";
			} else {
				throw new ODataApplicationException("contains() needs two parametres of type Edm.String", 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
			break;
		case STARTSWITH:
			// "startswith" gets two parameters, both have to be of type String
			if(parameters.get(0) instanceof String && parameters.get(1) instanceof String) {
				String valueParam1 = (String) parameters.get(0);
				String valueParam2 = (String) parameters.get(1);
				// Remove quotes added by visitLiteral
				valueParam2 = valueParam2.substring(1, valueParam2.length() - 1);

				result = valueParam1 + " LIKE '" + valueParam2 + "%'";
			} else {
				throw new ODataApplicationException("startswith() needs two parametres of type Edm.String", 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
			break;
		case ENDSWITH:
			// "endswith" gets two parameters, both have to be of type String
			if(parameters.get(0) instanceof String && parameters.get(1) instanceof String) {
				String valueParam1 = (String) parameters.get(0);
				String valueParam2 = (String) parameters.get(1);
				// Remove quotes added by visitLiteral
				valueParam2 = valueParam2.substring(1, valueParam2.length() - 1);

				result = valueParam1 + " LIKE '%" + valueParam2 + "'";
			} else {
				throw new ODataApplicationException("endswith() needs two parametres of type Edm.String", 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
			break;
		default:
			throw new ODataApplicationException("Method call " + methodCall + " not implemented", 
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< visitMethodCall()");
		return result;
	}

	@Override
	public String visitTypeLiteral(EdmType type) throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Type literals are not implemented", 
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	@Override
	public String visitAlias(String aliasName) throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Aliases are not implemented", 
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	@Override
	public String visitEnum(EdmEnumType type, List<String> enumValues) 
			throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitEnum({}, {})", type, enumValues);
		
		StringBuilder result = new StringBuilder();
		
		for (String enumValue: enumValues) {
			if (0 != result.length()) {
				result.append(", ");
			}
			switch (type.getName()) {
			case ProductEdmProvider.EN_PRODUCTIONTYPE_NAME:
				result.append("'").append(ProductionType.get(enumValue).toString()).append("'");
				break;
			default: 
				throw new ODataApplicationException("Enum conversion failed for enum value " + enumValue, 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
		}
		
		if (1 < enumValues.size()) {
			result.insert(0, '(').append(')');
		}

		if (logger.isTraceEnabled()) logger.trace("<<< visitEnum()");
		return result.toString();
	}

	@Override
	public String visitLambdaExpression(String lambdaFunction, String lambdaVariable, Expression expression) 
			throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled()) logger.trace(">>> visitLambdaExpression({}, {}, {})",
				lambdaFunction, lambdaVariable, expression);
		
		// TODO
		String result = expression.accept(this);
		
		if (logger.isTraceEnabled()) logger.trace("<<< visitLambdaExpression()");
		return result;
	}

	@Override
	public String visitLambdaReference(String variableName) 
			throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Lamdba references are not implemented", 
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	@Override
	public String visitBinaryOperator(BinaryOperatorKind operator, String left, List<String> right)
			throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Binary operators on lists are not implemented", 
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}
}
