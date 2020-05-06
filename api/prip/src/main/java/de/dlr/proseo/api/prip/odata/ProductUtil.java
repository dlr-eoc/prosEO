/**
 * ProductUtil.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.interfaces.rest.model.RestProduct;

/**
 * Utility class to convert product objects from prosEO REST API to PRIP (OData) REST API
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class ProductUtil {

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductUtil.class);

	/**
	 * Create a PRIP interface product from a prosEO interface product
	 * 
	 * @param restProduct the prosEO interface product to convert
	 * @return an OData entity object representing the prosEO interface product
	 * @throws URISyntaxException if a valid URI cannot be generated from any product UUID
	 */
	public static Entity toPripProduct(RestProduct restProduct) throws URISyntaxException {
		if (logger.isTraceEnabled()) logger.trace(">>> toPripProduct({})", restProduct.getId());
		
		Entity product = new Entity()
				.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, UUID.fromString(restProduct.getUuid())))
				.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, restProduct.getProductClass()))
				.addProperty(new Property(null, "ContentType", ValueType.PRIMITIVE,
						"application/octet-stream"));
		// TODO Add remaining properties
		product.setId(new URI(ProductEdmProvider.ET_PRODUCT_NAME + "('" + restProduct.getUuid() + "')"));

		return product;
	}
}
