/**
 * AttributeLambdaExpressionVisitor.java
 */
package de.dlr.proseo.api.odip.odata;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.edm.EdmEnumType;
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
import org.apache.olingo.server.api.uri.UriResourceLambdaVariable;
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
import de.dlr.proseo.logging.messages.OdipMessage;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Visitor class for parsing and evaluating expressions within Attribute lambda expressions in OData queries
 * 
 * @author Thomas Bassler
 */
public class AttributeLambdaExpressionVisitor implements ExpressionVisitor<AttributeCondition> {

	/** Lambda variable this visitor works on */
	private String lambdaVariable = null;

	private static final DateTimeFormatter sqlTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS Z")
		.withZone(ZoneId.of("UTC"));

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(AttributeLambdaExpressionVisitor.class);

	/**
	 * Structured representation of a selection condition for an attribute.
	 */
	public static class AttributeCondition {
		
		/** Attribute name */
		private String name;

		/** Conditional operator */
		private String op;

		/** Attribute value */
		private String value;

		/**
		 * Get the attribute name.
		 *
		 * @return The attribute name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Set the attribute name.
		 *
		 * @param name The attribute name
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * Get the conditional operator.
		 *
		 * @return The conditional operator
		 */
		public String getOp() {
			return op;
		}

		/**
		 * Set the conditional operator.
		 *
		 * @param op The conditional operator
		 */
		public void setOp(String op) {
			this.op = op;
		}

		/**
		 * Get the attribute value.
		 *
		 * @return The attribute value
		 */
		public String getValue() {
			return value;
		}

		/**
		 * Set the attribute value.
		 *
		 * @param value The attribute value
		 */
		public void setValue(String value) {
			this.value = value;
		}

		/**
		 * Returns a string representation of the attribute condition.
		 *
		 * @return The string representation
		 */
		@Override
		public String toString() {
			return String.format("%s %s %s", name, op, value);
		}
		
	}

	/**
	 * Constructor with the lambda variable to work upon
	 *
	 * @param lambdaVariable the lambda variable to use
	 */
	public AttributeLambdaExpressionVisitor(String lambdaVariable) {
		this.lambdaVariable = lambdaVariable;
	}

	/**
	 * Visits the "Alias" expression in Attribute lambda expressions.
	 *
	 * @param aliasName The alias name.
	 * @return The AttributeCondition result.
	 * @throws ExpressionVisitException  If an error occurs during expression visiting.
	 * @throws ODataApplicationException If aliases are not implemented for Attribute lambda expressions.
	 */
	@Override
	public AttributeCondition visitAlias(String aliasName) throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Aliases are not implemented for Attribute lambda expressions",
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	/**
	 * Visits the "BinaryOperator" expression in Attribute lambda expressions.
	 *
	 * @param operator The binary operator kind.
	 * @param left     The left operand AttributeCondition.
	 * @param right    The right operand AttributeCondition.
	 * @return The AttributeCondition result.
	 * @throws ExpressionVisitException  If an error occurs during expression visiting.
	 * @throws ODataApplicationException If the operand types are invalid or if the operator is not allowed for Attribute lambda
	 *                                   expressions.
	 */
	@Override
	public AttributeCondition visitBinaryOperator(BinaryOperatorKind operator, AttributeCondition left, AttributeCondition right)
			throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled())
			logger.trace(">>> visitBinaryOperator({}, {}, {})", operator, left, right);

		// Evaluate operands
		if (!(left instanceof AttributeCondition && right instanceof AttributeCondition)) {
			String message = logger.log(OdipMessage.MSG_INVALID_OPERAND_TYPE);
			throw new ODataApplicationException(message, HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}

		AttributeCondition leftOperand = left;
		AttributeCondition rightOperand = right;
		AttributeCondition result = new AttributeCondition();

		// The code below works for expressions of the type "any(att:att/Name eq 'xxx' and att/OData.CSC.StringAttribute/Value eq
		// 'yyy')"
		// (where the sides may be exchanged)
		// TODO Extend for expressions like "any(Name eq 'xxx' and (Value eq 'yyy' or Value eq 'zzz'))"
		// or "any(Name eq 'xxx' and Value >= 123 and Value <= 456)"
		if (BinaryOperatorKind.AND.equals(operator)) {
			// Check which side contains the operator
			if (null == leftOperand.getOp()) {
				result.setName(leftOperand.getName().substring(1, leftOperand.getName().length() - 1));
				result.setOp(rightOperand.getOp());
				result.setValue(rightOperand.getValue());
			} else {
				result.setName(rightOperand.getName().substring(1, rightOperand.getName().length() - 1));
				result.setOp(leftOperand.getOp());
				result.setValue(leftOperand.getValue());
			}
		} else {
			// Evaluate a comparison operation
			if (OdipEdmProvider.GENERIC_PROP_NAME.equals(leftOperand.getName())) {
				if (!BinaryOperatorKind.EQ.equals(operator)) {
					String message = logger.log(OdipMessage.MSG_INVALID_OPERATOR_TYPE);
					throw new ODataApplicationException(message, HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
				}
				result.setName(rightOperand.getValue());
			} else if (OdipEdmProvider.GENERIC_PROP_VALUE.equals(leftOperand.getName())) {
				switch (operator) {
				case EQ:
					result.setOp("=");
					;
					break;
				case NE:
					result.setOp("<>");
					break;
				case GE:
					result.setOp(">=");
					break;
				case GT:
					result.setOp(">");
					break;
				case LE:
					result.setOp("<=");
					break;
				case LT:
					result.setOp("<");
					break;
				default:
					throw new ODataApplicationException(
							"Binary operation " + operator.name() + " is not allowed for Attribute lambda expressions",
							HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
				}
				result.setValue(rightOperand.getValue());
			} else if (OdipEdmProvider.GENERIC_PROP_NAME.equals(rightOperand.getName())) {
				if (!BinaryOperatorKind.EQ.equals(operator)) {
					String message = logger.log(OdipMessage.MSG_INVALID_OPERATOR_TYPE);
					throw new ODataApplicationException(message, HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
				}
				result.setName(leftOperand.getValue());
			} else if (OdipEdmProvider.GENERIC_PROP_VALUE.equals(rightOperand.getName())) {
				// Swap '>' and '<', because left and right sides will be swapped!
				switch (operator) {
				case EQ:
					result.setOp("=");
					;
					break;
				case NE:
					result.setOp("<>");
					break;
				case GE:
					result.setOp("<=");
					break;
				case GT:
					result.setOp("<");
					break;
				case LE:
					result.setOp(">=");
					break;
				case LT:
					result.setOp(">");
					break;
				default:
					throw new ODataApplicationException(
							"Binary operation " + operator.name() + " is not allowed for Attribute lambda expressions",
							HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
				}
				result.setValue(leftOperand.getValue());
			} else {
				String message = logger.log(OdipMessage.MSG_MISSING_OPERAND_NAME);

				throw new ODataApplicationException(message, HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
			}
		}
		if (logger.isTraceEnabled())
			logger.trace("... result: " + result);

		if (logger.isTraceEnabled())
			logger.trace("<<< visitBinaryOperator()");
		return result;
	}

	/**
	 * Visits the "BinaryOperator" expression with a list of right operands in Attribute lambda expressions.
	 *
	 * @param operator The binary operator kind.
	 * @param left     The left operand AttributeCondition.
	 * @param right    The list of right operands AttributeConditions.
	 * @return The AttributeCondition result.
	 * @throws ExpressionVisitException  If an error occurs during expression visiting.
	 * @throws ODataApplicationException If the left or right operands are null or empty, or if the operator is not supported for
	 *                                   Attribute lambda expressions.
	 */
	@Override
	public AttributeCondition visitBinaryOperator(BinaryOperatorKind operator, AttributeCondition left,
			List<AttributeCondition> right) throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled())
			logger.trace(">>> visitBinaryOperator({}, {}, {})", operator, left, right);

		if (null == left || null == right || right.isEmpty()) {
			throw new ODataApplicationException(
					"Cannot compare attribute condition '" + left + "' to list of attribute conditions '" + right + "'",
					HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
		}

		AttributeCondition result = new AttributeCondition();
		if (BinaryOperatorKind.IN.equals(operator)) {
			result.setOp("IN");
			result.setName("(" + left.toString() + ")");
			StringBuilder rightList = new StringBuilder("(").append(right.get(0).getValue());
			for (int i = 1; i < right.size(); ++i) {
				rightList.append(", ").append(right.get(i).getValue());
			}
			rightList.append(')');
			result.setValue(rightList.toString());
		} else {
			throw new ODataApplicationException(
					"Binary operator '" + operator + "' on lists in Attribute lambda expressions is not implemented",
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}

		if (logger.isTraceEnabled())
			logger.trace("<<< visitBinaryOperator()");
		return result;
	}

	/**
	 * Visits the "Enum" expression in Attribute lambda expressions.
	 *
	 * @param type       The EdmEnumType.
	 * @param enumValues The list of enum values.
	 * @return The AttributeCondition result.
	 * @throws ExpressionVisitException  If an error occurs during expression visiting.
	 * @throws ODataApplicationException If enums are not implemented for Attribute lambda expressions.
	 */
	@Override
	public AttributeCondition visitEnum(EdmEnumType type, List<String> enumValues)
			throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Enums are not implemented for Attribute lambda expressions",
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	/**
	 * Visits the "LambdaExpression" expression in Attribute lambda expressions.
	 *
	 * @param lambdaFunction The lambda function.
	 * @param lambdaVariable The lambda variable.
	 * @param expression     The lambda expression.
	 * @return The AttributeCondition result.
	 * @throws ExpressionVisitException  If an error occurs during expression visiting.
	 * @throws ODataApplicationException If lambda expressions are not implemented for Attribute lambda expressions.
	 */
	@Override
	public AttributeCondition visitLambdaExpression(String lambdaFunction, String lambdaVariable, Expression expression)
			throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Lambda expressions are not implemented for Attribute lambda expressions",
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	/**
	 * Visits the "LambdaReference" expression in Attribute lambda expressions.
	 *
	 * @param variableName The variable name.
	 * @return The AttributeCondition result.
	 * @throws ExpressionVisitException  If an error occurs during expression visiting.
	 * @throws ODataApplicationException If lambda references are not implemented for Attribute lambda expressions.
	 */
	@Override
	public AttributeCondition visitLambdaReference(String variableName) throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Lamdba references are not implemented for Attribute lambda expressions",
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	@Override
	public AttributeCondition visitLiteral(Literal literal) throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled())
			logger.trace(">>> visitLiteral({})", literal);

		// We can be sure, that the literal is a valid OData literal because the URI Parser checks
		// the lexicographical structure

		// String literals start and end with an single quotation mark
		String literalAsString = literal.getText();
		String literalResult = null;
		if (literal.getType() instanceof EdmString) {
			String stringLiteral = "";
			if (literal.getText().length() > 2) {
				// Remove OData quotes
				stringLiteral = literalAsString.substring(1, literalAsString.length() - 1);
			}
			// Add SQL string quotes
			literalResult = "'" + stringLiteral + "'";
		} else if (literal.getType() instanceof EdmDateTimeOffset) {
			// Try to convert the literal into an Java Instant
			try {
				Instant literalAsInstant = Instant.from(OrbitTimeFormatter.parse(literalAsString));
				literalResult = "'" + sqlTimestampFormatter.format(literalAsInstant) + "'";
			} catch (DateTimeParseException e) {
				throw new ODataApplicationException("Invalid date/time offset value " + literalAsString,
						HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
			}
		} else if (literal.getType() instanceof EdmByte || literal.getType() instanceof EdmSByte
				|| literal.getType() instanceof EdmInt16 || literal.getType() instanceof EdmInt32
				|| literal.getType() instanceof EdmInt64 || literal.getType() instanceof EdmDecimal
				|| literal.getType() instanceof EdmSingle || literal.getType() instanceof EdmDouble) {
			literalResult = literalAsString;
		} else {
			if (logger.isTraceEnabled())
				logger.trace("... found literal of type: " + literal.getType().getName());
			throw new ODataApplicationException(
					"Only Edm.Byte, Edm.SByte, Edm.Int16, Edm.Int32, Edm.Int64, Edm.String, Edm.DateTimeOffset, Edm.Decimal, Edm.Single and Edm.Double literals are implemented",
					HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}

		AttributeCondition result = new AttributeCondition();
		result.setValue(literalResult);

		if (logger.isTraceEnabled())
			logger.trace("<<< visitLiteral()");
		return result;
	}

	/**
	 * Visits the "Member" expression in Attribute lambda expressions.
	 *
	 * @param member The member.
	 * @return The AttributeCondition result.
	 * @throws ExpressionVisitException  If an error occurs during expression visiting.
	 * @throws ODataApplicationException If an unexpected URI structure, URI variable, or property name is encountered.
	 */
	@Override
	public AttributeCondition visitMember(Member member) throws ExpressionVisitException, ODataApplicationException {
		if (logger.isTraceEnabled())
			logger.trace(">>> visitMember({})", member);

		// Travel the property tree given in the URI resources
		Iterator<UriResource> uriResourceIter = member.getResourcePath().getUriResourceParts().iterator();

		UriResource uriResource = uriResourceIter.next(); // At least one element is guaranteed to exist, otherwise the parser would
															// not have led us here

		// We expect the first uriResource to be the lambda variable this visitor works on
		if (!(uriResource instanceof UriResourceLambdaVariable)) {
			String message = logger.log(OdipMessage.MSG_UNEXPECTED_URI, uriResource.getKind());
			throw new ODataApplicationException(message, HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}

		// Check that the correct lambda variable is referenced
		String uriVariableName = ((UriResourceLambdaVariable) uriResource).getVariableName();
		if (!uriVariableName.equals(lambdaVariable)) {
			String message = logger.log(OdipMessage.MSG_UNEXPECTED_URI_VAR, uriVariableName, lambdaVariable);
			throw new ODataApplicationException(message, HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}

		// Another path element containing either "Name" or "Value" must exist
		uriResource = uriResourceIter.next();

		if (!(uriResource instanceof UriResourcePrimitiveProperty)) {
			String message = logger.log(OdipMessage.MSG_UNEXPECTED_SUB_URI, uriResource.getKind());
			throw new ODataApplicationException(message, HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}
		String propertyName = ((UriResourcePrimitiveProperty) uriResource).getProperty().getName();

		if (!OdipEdmProvider.GENERIC_PROP_NAME.equals(propertyName) && !OdipEdmProvider.GENERIC_PROP_VALUE.equals(propertyName)) {
			String message = logger.log(OdipMessage.MSG_UNEXPECTED_PROPERTY, propertyName);
			throw new ODataApplicationException(message, HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
		}

		AttributeCondition result = new AttributeCondition();
		result.setName(propertyName);
		return result;
	}

	/**
	 * Visits the "MethodCall" expression in Attribute lambda expressions.
	 *
	 * @param methodCall The method kind.
	 * @param parameters The list of attribute conditions as parameters.
	 * @return The AttributeCondition result.
	 * @throws ExpressionVisitException  If an error occurs during expression visiting.
	 * @throws ODataApplicationException If method calls are not implemented for Attribute lambda expressions.
	 */
	@Override
	public AttributeCondition visitMethodCall(MethodKind methodCall, List<AttributeCondition> parameters)
			throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Method calls are not implemented for Attribute lambda expressions",
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	/**
	 * Visits the "TypeLiteral" expression in Attribute lambda expressions.
	 *
	 * @param type The EdmType.
	 * @return The AttributeCondition result.
	 * @throws ExpressionVisitException  If an error occurs during expression visiting.
	 * @throws ODataApplicationException If type literals are not implemented for Attribute lambda expressions.
	 */
	@Override
	public AttributeCondition visitTypeLiteral(EdmType type) throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Type literals are not implemented for Attribute lambda expressions",
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	/**
	 * Visits the "UnaryOperator" expression in Attribute lambda expressions.
	 *
	 * @param operator The unary operator kind.
	 * @param operand  The operand AttributeCondition.
	 * @return The AttributeCondition result.
	 * @throws ExpressionVisitException  If an error occurs during expression visiting.
	 * @throws ODataApplicationException If unary operators are not implemented for Attribute lambda expressions.
	 */
	@Override
	public AttributeCondition visitUnaryOperator(UnaryOperatorKind operator, AttributeCondition operand)
			throws ExpressionVisitException, ODataApplicationException {
		throw new ODataApplicationException("Unary operators are not implemented for Attribute lambda expressions",
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

}