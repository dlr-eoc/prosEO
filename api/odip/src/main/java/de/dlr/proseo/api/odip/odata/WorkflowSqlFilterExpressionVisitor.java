/*
 * WorkflowSqlFilterExpressionVisitor.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.odip.odata;

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
import org.apache.olingo.commons.core.edm.primitivetype.EdmBoolean;
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
import de.dlr.proseo.api.odip.odata.AttributeLambdaExpressionVisitor.AttributeCondition;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.enums.OrderState;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Evaluation of OData expressions
 *  
 * @author Dr. Thomas Bassler
 */
public class WorkflowSqlFilterExpressionVisitor implements ExpressionVisitor<String> {
	
	/** Counter for parameters in WHERE clause */
	private int paramCount = 0;
	
	/** SQL command parts */
	private static final String SELECT_CLAUSE = "SELECT DISTINCT p.* ";
	private static final String SELECT_COUNT_CLAUSE = "SELECT count(DISTINCT p.*) ";
	private static final String FROM_CLAUSE = "FROM workflow p\n" +
			"JOIN workflow_option wo ON wo.workflow_id = p.id\n" +
			"LEFT OUTER JOIN product_class ipc ON p.input_product_class_id = ipc.id\n" +
			"LEFT OUTER JOIN product_class opc ON p.output_product_class_id = opc.id\n" +
			"LEFT OUTER JOIN configured_processor cp ON p.configured_processor_id = cp.id\n";
			// "LEFT OUTER JOIN workflow_option_value_range wovr ON wo.id = wovr.workflow_option_id\n";
	private static final String OPTION_JOIN_TEMPLATE = "LEFT OUTER JOIN workflow_option pp%d ON p.id = pp%d.workflow_id\n";
	private static final String WHERE_CLAUSE = "WHERE ";
	private static final String OPTION_WHERE_TEMPLATE = "(pp%d.name = '%s')";
	
	/** Mapping from OData member names to SQL schema names */
	private static Map<String, String> oDataToSqlMap = new HashMap<>();
	private static final String[][] ODATA_TO_SQL_MAPPING = {
			{ OdipEdmProvider.GENERIC_PROP_ID, "p.uuid" },
			{ OdipEdmProvider.GENERIC_PROP_NAME, "p.name" }, 
			{ CscAttributeName.WORKFLOW_VERSION.getValue(), "p.workflow_version" },
			{ CscAttributeName.OUTPUT_PRODUCT_CLASS.getValue(), "opc.product_type" },
			{ CscAttributeName.INPUT_PRODUCT_CLASS.getValue(), "ipc.product_type" },
			{ CscAttributeName.DESCRIPTION.getValue(), "p.description" }
	};
	
	private static final DateTimeFormatter sqlTimestampFormatter =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS Z").withZone(ZoneId.of("UTC"));

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(OrderSqlFilterExpressionVisitor.class);

	/* Initialize OData-to-SQL name mapping */
	{
		for (int i = 0; i < ODATA_TO_SQL_MAPPING.length; ++i) {
			oDataToSqlMap.put(ODATA_TO_SQL_MAPPING[i][0], ODATA_TO_SQL_MAPPING[i][1]);
		}
	}
	
	/**
	 * Get the applicable SQL command up to and including the 'WHERE' keyword (the remainder has been created by the
	 * visit* methods).
	 * 
	 * Make sure this OrderSqlFilterExpressionVisitor was subject to an "accept" call before calling this method!
	 * 
	 * @param countOnly create a command, which only counts the requested objects, but does not return them
	 * 
	 * @return a partial SQL command string
	 */
	
	public String getSqlCommand(boolean countOnly) {
		if (logger.isTraceEnabled()) logger.trace(">>> getSqlCommand()");
		
		StringBuilder result = new StringBuilder(countOnly ? SELECT_COUNT_CLAUSE : SELECT_CLAUSE);
		result.append(FROM_CLAUSE);
		
		for (int i = 1; i <= paramCount; ++i) {
			result.append(String.format(OPTION_JOIN_TEMPLATE, i, i));
		}
		
		result.append(WHERE_CLAUSE);
		
		return result.toString();
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
			String propertyName = uriResourceProperty.getProperty().getName();
			if (logger.isTraceEnabled()) logger.trace("... found primitive property: " + propertyName);
			String mappedProperty = oDataToSqlMap.get(propertyName);
			if (logger.isTraceEnabled()) logger.trace("... mapped primitive property: " + mappedProperty);
			
			if (null == mappedProperty) {
				throw new ODataApplicationException("Invalid property name '" + propertyName, 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}

			if (logger.isTraceEnabled()) logger.trace("<<< visitMember()");
			return (null == mappedProperty ? "NOT FOUND" : mappedProperty);
		}
		
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
				if (logger.isTraceEnabled()) logger.trace("... translation of lambda function 'any': " + result);
				if (logger.isTraceEnabled()) logger.trace("<<< visitMember()");
				return result;
			default:
				throw new ODataApplicationException("Only primitive, complex, navigation and lambda 'any' URI resources and arrays thereof are allowed as sub-paths in filter expressions", 
						HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
			}
			
		}
		if (logger.isTraceEnabled()) logger.trace("... derived property: " + propertyName);
		String mappedProperty = oDataToSqlMap.get(propertyName.toString());
		if (logger.isTraceEnabled()) logger.trace("... mapped property: " + mappedProperty);

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
				|| literal.getType() instanceof EdmSingle || literal.getType() instanceof EdmDouble
				|| literal.getType() instanceof EdmBoolean) {
			result = literalAsString;
		} else {
			if (logger.isTraceEnabled()) logger.trace("... found literal of type: " + literal.getType().getName());
			throw new ODataApplicationException("Only Edm.Boolean, Edm.Byte, Edm.SByte, Edm.Int16, Edm.Int32, Edm.Int64, Edm.String, Edm.DateTimeOffset, Edm.Decimal, Edm.Single and Edm.Double literals are implemented", 
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
		// Arithmetic operators (with parentheses for complex expressions)
		case ADD:
			result = "(" + left + " + " + right + ")";
			break;
		case MUL:
			result = "(" + left + " * " + right + ")";
			break;
		case DIV:
			result = "(" + left + " / " + right + ")";
			break;
		case SUB:
			result = "(" + left + " - " + right + ")";
			break;
		// Comparison operators
		case EQ:
			if (right.contains(", ")) {
				result = "" + left + " IN (" + right + ")";
			} else {
				result = "" + left + " = " + right;
			}
			break;
		case NE:
			if (right.contains(", ")) {
				result = "" + left + " NOT IN (" + right + ")";
			} else {
				result = "" + left + " <> " + right;
			}
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
		// Boolean operators (with parentheses for complex expressions)
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

				result = valueParam1 + " LIKE '%" + valueParam2.replace("%", "\\%").replace("_", "\\_") + "%' ESCAPE '\\'";
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

				result = valueParam1 + " LIKE '" + valueParam2.replace("%", "\\%").replace("_", "\\_") + "%' ESCAPE '\\'";
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

				result = valueParam1 + " LIKE '%" + valueParam2.replace("%", "\\%").replace("_", "\\_") + "' ESCAPE '\\'";
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
			case OdipEdmProvider.EN_PRODUCTIONTYPE_NAME:
				result.append("'").append(ProductionType.get(enumValue).toString()).append("'");
				break;
			case OdipEdmProvider.EN_PRODUCTIONORDERSTATE_NAME:
				if (enumValue.equals(OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_QUEUED)) {
					result.append("'").append(OrderState.INITIAL.toString()).append("', ");
					result.append("'").append(OrderState.APPROVED.toString()).append("', ");
					result.append("'").append(OrderState.PLANNING.toString()).append("', ");
					result.append("'").append(OrderState.PLANNED.toString()).append("'");
				} else if (enumValue.equals(OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_IN_PROGRESS)) {
					result.append("'").append(OrderState.RELEASING.toString()).append("', ");
					result.append("'").append(OrderState.RELEASED.toString()).append("', ");
					result.append("'").append(OrderState.RUNNING.toString()).append("'");
				} else if (enumValue.equals(OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_COMPLETED)) {
					result.append("'").append(OrderState.COMPLETED.toString()).append("'");
				} else if (enumValue.equals(OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_FAILED)) {
					result.append("'").append(OrderState.FAILED.toString()).append("'");
				} else if (enumValue.equals(OdipEdmProvider.ENEN_PRODUCTIONORDERSTATE_CANCELLED)) {
					result.append("'").append(OrderState.SUSPENDING.toString()).append("'");
				}
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
		
		// Evaluate the lambda expression
		AttributeLambdaExpressionVisitor attVisitor = new AttributeLambdaExpressionVisitor(lambdaVariable);
		AttributeCondition attCondition = expression.accept(attVisitor);
		
		// Replace name by attribute or parameter
		if (logger.isTraceEnabled()) logger.trace("... got attribute condition: " + attCondition);
		String mappedProperty = oDataToSqlMap.get(attCondition.getName());
		if (logger.isTraceEnabled()) logger.trace("... mapped property: " + mappedProperty);
		
		String result = null;
		if (null == mappedProperty) {
			// Not an SQL column, check parameter
			if (null == CscAttributeName.get(attCondition.getName())) {
				throw new ODataApplicationException("Invalid attribute name '" + attCondition.getName() + "' found in Attribute lambda expression", 
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
			++paramCount;
			result = String.format(OPTION_WHERE_TEMPLATE,
					paramCount, attCondition.getName(), paramCount, attCondition.getOp(), attCondition.getValue());
		} else {
			attCondition.setName(mappedProperty);
			result = attCondition.toString();
		}
		
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
		if (logger.isTraceEnabled()) logger.trace(">>> visitBinaryOperator({}, {}, {})", operator, left, right);
		
		if (null == left || null == right || right.isEmpty()) {
			throw new ODataApplicationException("Cannot compare string '" + left + "' to string list '" + right + "'", 
					HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
		}
		
		StringBuilder result = new StringBuilder();
		if (BinaryOperatorKind.IN.equals(operator)) {
			result.append(left).append(" IN (").append(right.get(0));
			for (int i = 1; i < right.size(); ++i) {
				result.append(", ").append(right.get(i));
			}
			result.append(')');
		} else {
			throw new ODataApplicationException("Binary operator '" + operator + "' on lists is not implemented", 
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}

		if (logger.isTraceEnabled()) logger.trace("<<< visitBinaryOperator()");
		return result.toString();
	}
}
