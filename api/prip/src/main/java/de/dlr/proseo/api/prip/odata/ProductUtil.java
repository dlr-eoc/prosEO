/**
 * ProductUtil.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.interfaces.rest.model.RestParameter;
import de.dlr.proseo.interfaces.rest.model.RestProduct;
import de.dlr.proseo.interfaces.rest.model.RestProductFile;
import de.dlr.proseo.model.util.OrbitTimeFormatter;

/**
 * Utility class to convert product objects from prosEO REST API to PRIP (OData) REST API
 * 
 * Attributes are mission-specific for Sentinel-5P
 * TODO Find mission-independent way of transferring attributes
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class ProductUtil {

	private static final int DATE_TIME_OFFSET_LENGTH = 24;
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductUtil.class);
	
	/**
	 * Create a PRIP interface product from a prosEO interface product
	 * 
	 * @param restProduct the prosEO interface product to convert
	 * @return an OData entity object representing the prosEO interface product
	 * @throws IllegalArgumentException if any mandatory information is missing from the prosEO interface product 
	 * @throws URISyntaxException if a valid URI cannot be generated from any product UUID
	 */
	public static Entity toPripProduct(RestProduct restProduct) throws IllegalArgumentException, URISyntaxException {
		if (logger.isTraceEnabled()) logger.trace(">>> toPripProduct({})", restProduct.getId());
		
		Entity product = new Entity()
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, UUID.fromString(restProduct.getUuid())))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, restProduct.getProductClass()))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE,
						"application/octet-stream"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE,
						/* TODO restProductFile.getDownloadSize() */ 1234567))
				.addProperty(new Property(null, ProductEdmProvider.ET_PRODUCT_PROP_CREATION_DATE, ValueType.PRIMITIVE,
						OrbitTimeFormatter.parse(restProduct.getGenerationTime())))
				.addProperty(new Property(null, ProductEdmProvider.ET_PRODUCT_PROP_EVICTION_DATE, ValueType.PRIMITIVE,
						OrbitTimeFormatter.parse(restProduct.getGenerationTime())))
				.addProperty(new Property(null, ProductEdmProvider.ET_PRODUCT_PROP_PRODUCTION_TYPE, ValueType.ENUM,
						"NOMINAL".equals(restProduct.getProductQuality()) ? 
								ProductEdmProvider.EN_PRODUCTIONTYPE_ONDEMDEF : ProductEdmProvider.EN_PRODUCTIONTYPE_ONDEMNODEF));

		ComplexValue contentDate = new ComplexValue();
		contentDate.getValue().add(new Property(null, ProductEdmProvider.CT_TIMERANGE_PROP_START, ValueType.PRIMITIVE,
						OrbitTimeFormatter.parse(restProduct.getSensingStartTime())));
		contentDate.getValue().add(new Property(null, ProductEdmProvider.CT_TIMERANGE_PROP_END, ValueType.PRIMITIVE,
						OrbitTimeFormatter.parse(restProduct.getSensingStopTime())));
		product.addProperty(new Property(null, ProductEdmProvider.ET_PRODUCT_PROP_CONTENT_DATE, ValueType.COMPLEX, contentDate));

		List<Entity> attributes = new ArrayList<>();
		
		Entity beginningDateTime = new Entity();
		beginningDateTime.setType(ProductEdmProvider.ET_DATEATTRIBUTE_NAME);
		beginningDateTime
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "beginningDateTime"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE, DATE_TIME_OFFSET_LENGTH))
				.addProperty(new Property(null, ProductEdmProvider.ET_DATEATTRIBUTE_PROP_DATE_VALUE, ValueType.PRIMITIVE,
						OrbitTimeFormatter.parse(restProduct.getSensingStartTime())));
		attributes.add(beginningDateTime);
		
		Entity endingDateTime = new Entity();
		endingDateTime.setType(ProductEdmProvider.ET_DATEATTRIBUTE_NAME);
		endingDateTime
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "endingDateTime"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE, DATE_TIME_OFFSET_LENGTH))
				.addProperty(new Property(null, ProductEdmProvider.ET_DATEATTRIBUTE_PROP_DATE_VALUE, ValueType.PRIMITIVE,
						OrbitTimeFormatter.parse(restProduct.getSensingStopTime())));
		attributes.add(endingDateTime);
		
		Entity platformShortName = new Entity();
		platformShortName.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_NAME);
		platformShortName
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "platformShortName"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE,
						restProduct.getMissionCode().length()))
				.addProperty(new Property(null, ProductEdmProvider.ET_STRINGATTRIBUTE_PROP_STRING_VALUE, ValueType.PRIMITIVE,
						restProduct.getMissionCode()));
		attributes.add(platformShortName);
		
		// TODO Add instrument short name to Mission?
		Entity instrumentShortName = new Entity();
		instrumentShortName.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_NAME);
		instrumentShortName
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "platformShortName"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE, 0))
				.addProperty(new Property(null, ProductEdmProvider.ET_STRINGATTRIBUTE_PROP_STRING_VALUE, ValueType.PRIMITIVE, ""));
		attributes.add(instrumentShortName);
		
		Entity orbitNumber = new Entity();
		orbitNumber.setType(ProductEdmProvider.ET_INTEGERATTRIBUTE_NAME);
		orbitNumber
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "orbitNumber"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE, 0))
				.addProperty(new Property(null, ProductEdmProvider.ET_INTEGERATTRIBUTE_PROP_INTEGER_VALUE, ValueType.PRIMITIVE,
						null == restProduct.getOrbit() ? 0 : restProduct.getOrbit().getOrbitNumber()));
		attributes.add(orbitNumber);
		
		Entity processorName = new Entity();
		processorName.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_NAME);
		processorName
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "processorName"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE,
						null == restProduct.getConfiguredProcessor() ? 0 : restProduct.getConfiguredProcessor().getProcessorName().length()))
				.addProperty(new Property(null, ProductEdmProvider.ET_STRINGATTRIBUTE_PROP_STRING_VALUE, ValueType.PRIMITIVE,
						null == restProduct.getConfiguredProcessor() ? 0 : restProduct.getConfiguredProcessor().getProcessorName()));
		attributes.add(processorName);
		
		Entity processorVersion = new Entity();
		processorVersion.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_NAME);
		processorVersion
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "processorVersion"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE,
						null == restProduct.getConfiguredProcessor() ? 0 : restProduct.getConfiguredProcessor().getProcessorVersion().length()))
				.addProperty(new Property(null, ProductEdmProvider.ET_STRINGATTRIBUTE_PROP_STRING_VALUE, ValueType.PRIMITIVE,
						null == restProduct.getConfiguredProcessor() ? 0 : restProduct.getConfiguredProcessor().getProcessorVersion()));
		attributes.add(processorVersion);
		
		// TODO Add processing level to ProductClass?
		Entity processingLevel = new Entity();
		processingLevel.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_NAME);
		processingLevel
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "processingLevel"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE, 0))
				.addProperty(new Property(null, ProductEdmProvider.ET_STRINGATTRIBUTE_PROP_STRING_VALUE, ValueType.PRIMITIVE, ""));
		attributes.add(processingLevel);
		
		Entity processingMode = new Entity();
		processingMode.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_NAME);
		processingMode
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "processingMode"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE,
						restProduct.getMode().length()))
				.addProperty(new Property(null, ProductEdmProvider.ET_STRINGATTRIBUTE_PROP_STRING_VALUE, ValueType.PRIMITIVE,
						restProduct.getMode()));
		attributes.add(processingMode);
		
		Entity productClass = new Entity();
		productClass.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_NAME);
		productClass
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "productClass"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE,
						restProduct.getFileClass().length()))
				.addProperty(new Property(null, ProductEdmProvider.ET_STRINGATTRIBUTE_PROP_STRING_VALUE, ValueType.PRIMITIVE,
						restProduct.getFileClass()));
		attributes.add(productClass);
		
		Entity productType = new Entity();
		productType.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_NAME);
		productType
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "productType"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE,
						restProduct.getProductClass().length()))
				.addProperty(new Property(null, ProductEdmProvider.ET_STRINGATTRIBUTE_PROP_STRING_VALUE, ValueType.PRIMITIVE,
						restProduct.getProductClass()));
		attributes.add(productType);
		
		// Evaluate product parameters
		Map<String, RestParameter> parameterMap = new HashMap<>();
		for (RestParameter param: restProduct.getParameters()) parameterMap.put(param.getKey(), param);
		
		Entity revisionNumber = new Entity();
		revisionNumber.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_NAME);
		revisionNumber
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "revisionNumber"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE,
						null == parameterMap.get("revisionNumber") ? 0 : parameterMap.get("revisionNumber").toString().length()))
				.addProperty(new Property(null, ProductEdmProvider.ET_STRINGATTRIBUTE_PROP_STRING_VALUE, ValueType.PRIMITIVE,
						null == parameterMap.get("revisionNumber") ? 0 : parameterMap.get("revisionNumber").toString()));
		attributes.add(revisionNumber);
		
		// TODO To be confirmed by ESA (not in metadata mapping document)
		Entity copernicusCollection = new Entity();
		copernicusCollection.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_NAME);
		copernicusCollection
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, "copernicusCollection"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE, "text/plain"))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE,
						null == parameterMap.get("revisionNumber") ? 0 : parameterMap.get("copernicusCollection").toString().length()))
				.addProperty(new Property(null, ProductEdmProvider.ET_STRINGATTRIBUTE_PROP_STRING_VALUE, ValueType.PRIMITIVE,
						null == parameterMap.get("revisionNumber") ? 0 : parameterMap.get("copernicusCollection").toString()));
		attributes.add(copernicusCollection);
		
		// TODO Clarify with ESA whether all other product parameters (if available) shall be expanded, too
		
		// Set product key
		product.setId(new URI(ProductEdmProvider.ET_PRODUCT_NAME + "('" + restProduct.getUuid() + "')"));

		return product;
	}
}
