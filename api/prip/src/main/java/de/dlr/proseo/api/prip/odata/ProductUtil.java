/**
 * ProductUtil.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import static de.dlr.proseo.api.prip.odata.CscAttributeName.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.ComplexValue;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.Parameter;
import de.dlr.proseo.model.Product;
import de.dlr.proseo.model.ProductFile;
import de.dlr.proseo.model.enums.ProductionType;

/**
 * Utility class to convert product objects from prosEO database model to PRIP (OData) REST API
 * 
 * @author Dr. Thomas Bassler
 *
 */
public class ProductUtil {

	private static final String ERR_NO_PRODUCT_FILES_FOUND = "No product files found in product ";
	private static final Date START_OF_MISSION = Date.from(Instant.parse("1970-01-01T00:00:00.000Z"));
	private static final Date END_OF_MISSION = Date.from(Instant.parse("9999-12-31T23:59:59.999Z"));

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductUtil.class);
	
	/**
	 * Create a PRIP interface product from a prosEO interface product
	 * 
	 * @param modelProduct the prosEO model product to convert
	 * @return an OData entity object representing the prosEO interface product
	 * @throws IllegalArgumentException if any mandatory information is missing from the prosEO interface product 
	 * @throws URISyntaxException if a valid URI cannot be generated from any product UUID
	 */
	public static Entity toPripProduct(Product modelProduct) throws IllegalArgumentException, URISyntaxException {
		if (logger.isTraceEnabled()) logger.trace(">>> toPripProduct({})", modelProduct.getId());
		
		// Select a product file (we just take the first one, since they are assumed to be identical, even if stored on
		// different processing facilities)
		if (modelProduct.getProductFile().isEmpty()) {
			throw new IllegalArgumentException(ERR_NO_PRODUCT_FILES_FOUND + modelProduct.getId());
		}
		ProductFile modelProductFile = modelProduct.getProductFile().iterator().next();
		Mission modelMission = modelProduct.getProductClass().getMission();
		
		// Determine production type
		ProductionType modelProductionType = modelProduct.getProductionType();
		int productionType = ProductEdmProvider.EN_PRODUCTIONTYPE_SYSTEMATIC_VAL; // Default, also in case of no production type
		if (ProductionType.ON_DEMAND_DEFAULT.equals(modelProductionType)) {
			productionType = ProductEdmProvider.EN_PRODUCTIONTYPE_ONDEMDEF_VAL;
		} else if (ProductionType.ON_DEMAND_NON_DEFAULT.equals(modelProductionType)) {
			productionType = ProductEdmProvider.EN_PRODUCTIONTYPE_ONDEMNODEF_VAL;
		}
		
		// Create product entity
		Entity product = new Entity();
		product.setType(ProductEdmProvider.ET_PRODUCT_FQN.getFullQualifiedNameAsString());
		product.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_ID, ValueType.PRIMITIVE, modelProduct.getUuid()))
			.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, 
				(null == modelProductFile.getZipFileName() ? modelProductFile.getProductFileName() : modelProductFile.getZipFileName())))
			.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_TYPE, ValueType.PRIMITIVE,
				"application/octet-stream"))
			.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_CONTENT_LENGTH, ValueType.PRIMITIVE,
				(null == modelProductFile.getZipFileName() ? modelProductFile.getFileSize() : modelProductFile.getZipFileSize())))
			.addProperty(new Property(null, ProductEdmProvider.ET_PRODUCT_PROP_ORIGIN_DATE, ValueType.PRIMITIVE,
				(null == modelProduct.getRawDataAvailabilityTime() ? START_OF_MISSION : Date.from(modelProduct.getRawDataAvailabilityTime()))))
			.addProperty(new Property(null, ProductEdmProvider.ET_PRODUCT_PROP_PUBLICATION_DATE, ValueType.PRIMITIVE,
					(null == modelProduct.getPublicationTime() ? Date.from(modelProduct.getGenerationTime()) : 
						Date.from(modelProduct.getPublicationTime()))))
			.addProperty(new Property(null, ProductEdmProvider.ET_PRODUCT_PROP_EVICTION_DATE, ValueType.PRIMITIVE,
				(null == modelProduct.getEvictionTime() ? END_OF_MISSION : Date.from(modelProduct.getEvictionTime()))))
			.addProperty(new Property(null, ProductEdmProvider.ET_PRODUCT_PROP_PRODUCTION_TYPE, ValueType.ENUM, productionType));

		ComplexValue contentDate = new ComplexValue();
		contentDate.getValue().add(new Property(null, ProductEdmProvider.CT_TIMERANGE_PROP_START, ValueType.PRIMITIVE,
				Date.from(modelProduct.getSensingStartTime())));
		contentDate.getValue().add(new Property(null, ProductEdmProvider.CT_TIMERANGE_PROP_END, ValueType.PRIMITIVE,
				Date.from(modelProduct.getSensingStopTime())));
		product.addProperty(new Property(null, ProductEdmProvider.ET_PRODUCT_PROP_CONTENT_DATE, ValueType.COMPLEX, contentDate));
		
		// Fill checksum information from restProductFile
		List<ComplexValue> checksums = new ArrayList<>();
		ComplexValue checksum = new ComplexValue();
		checksum.getValue().add(new Property(null, ProductEdmProvider.CT_CHECKSUM_PROP_ALGORITHM, ValueType.PRIMITIVE, "MD5"));
		checksum.getValue().add(new Property(null, ProductEdmProvider.CT_CHECKSUM_PROP_VALUE, ValueType.PRIMITIVE,
			(null == modelProductFile.getZipFileName() ? modelProductFile.getChecksum() : modelProductFile.getZipChecksum())));
		checksum.getValue().add(new Property(null, ProductEdmProvider.CT_CHECKSUM_PROP_CHECKSUM_DATE, ValueType.PRIMITIVE,
			(null == modelProductFile.getZipFileName() ?
				Date.from(modelProductFile.getChecksumTime()) :
				Date.from(modelProductFile.getZipChecksumTime()))));
		checksums.add(checksum);
		product.addProperty(new Property(null, ProductEdmProvider.ET_PRODUCT_PROP_CHECKSUM, ValueType.COLLECTION_COMPLEX, checksums));

		// Create navigable collection of attributes
		EntityCollection attributes = new EntityCollection();
		attributes.setId(new URI("Product(" + modelProduct.getUuid() + ")/Attributes"));
		
		Entity beginningDateTime = new Entity();
		beginningDateTime.setType(ProductEdmProvider.ET_DATEATTRIBUTE_FQN.toString());
		beginningDateTime
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, BEGINNING_DATE_TIME.getValue()))
				.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
						ProductEdmProvider.ET_DATEATTRIBUTE_NAME))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE,
						Date.from(modelProduct.getSensingStartTime())));
		attributes.getEntities().add(beginningDateTime);
		
		Entity endingDateTime = new Entity();
		endingDateTime.setType(ProductEdmProvider.ET_DATEATTRIBUTE_FQN.toString());
		endingDateTime
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, ENDING_DATE_TIME.getValue()))
				.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
						ProductEdmProvider.ET_DATEATTRIBUTE_NAME))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE,
						Date.from(modelProduct.getSensingStopTime())));
		attributes.getEntities().add(endingDateTime);
		
		Entity processingDateTime = new Entity();
		processingDateTime.setType(ProductEdmProvider.ET_DATEATTRIBUTE_FQN.toString());
		processingDateTime
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, PROCESSING_DATE.getValue()))
				.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
						ProductEdmProvider.ET_DATEATTRIBUTE_NAME))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE,
						Date.from(modelProduct.getGenerationTime())));
		attributes.getEntities().add(processingDateTime);
		
		Entity platformShortName = new Entity();
		platformShortName.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_FQN.toString());
		platformShortName
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, PLATFORM_SHORT_NAME.getValue()))
				.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
						ProductEdmProvider.ET_STRINGATTRIBUTE_NAME))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE,
						modelMission.getName()));
		attributes.getEntities().add(platformShortName);
		
		if (null != modelProduct.getConfiguredProcessor()) {
			// Product was generated by this prosEO instance, so we can reasonably set the processing centre and the instrument short name
			Entity processingCentre = new Entity();
			processingCentre.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_FQN.toString());
			processingCentre
					.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE,
							PROCESSING_CENTER.getValue()))
					.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
							ProductEdmProvider.ET_STRINGATTRIBUTE_NAME))
					.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE,
							(null == modelMission.getProcessingCentre() ? "" : modelMission.getProcessingCentre())));
			attributes.getEntities().add(processingCentre);
			
			String instrument = "";
			if (!modelMission.getSpacecrafts().isEmpty()
					&& !modelMission.getSpacecrafts().iterator().next().getPayloads().isEmpty()) {
				instrument = modelMission.getSpacecrafts().iterator().next().getPayloads().get(0).getName();
			}
			Entity instrumentShortName = new Entity();
			instrumentShortName.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_FQN.toString());
			instrumentShortName
					.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE,
							INSTRUMENT_SHORT_NAME.getValue()))
					.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
							ProductEdmProvider.ET_STRINGATTRIBUTE_NAME))
					.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE, instrument));
			attributes.getEntities().add(instrumentShortName);
		}
		
		Entity orbitNumber = new Entity();
		orbitNumber.setType(ProductEdmProvider.ET_INTEGERATTRIBUTE_FQN.toString());
		orbitNumber
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, ORBIT_NUMBER.getValue()))
				.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
						ProductEdmProvider.ET_INTEGERATTRIBUTE_NAME))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE,
						null == modelProduct.getOrbit() ? 0 : modelProduct.getOrbit().getOrbitNumber()));
		attributes.getEntities().add(orbitNumber);
		
		Entity processorName = new Entity();
		processorName.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_FQN.toString());
		processorName
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, PROCESSOR_NAME.getValue()))
				.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
						ProductEdmProvider.ET_STRINGATTRIBUTE_NAME))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE,
						null == modelProduct.getConfiguredProcessor() ? 0 : 
							modelProduct.getConfiguredProcessor().getProcessor().getProcessorClass().getProcessorName()));
		attributes.getEntities().add(processorName);
		
		Entity processorVersion = new Entity();
		processorVersion.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_FQN.toString());
		processorVersion
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, PROCESSOR_VERSION.getValue()))
				.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
						ProductEdmProvider.ET_STRINGATTRIBUTE_NAME))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE,
						null == modelProduct.getConfiguredProcessor() ? 0 : 
							modelProduct.getConfiguredProcessor().getProcessor().getProcessorVersion()));
		attributes.getEntities().add(processorVersion);
		
		Entity processingLevel = new Entity();
		processingLevel.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_FQN.toString());
		processingLevel
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, PROCESSING_LEVEL.getValue()))
				.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
						ProductEdmProvider.ET_STRINGATTRIBUTE_NAME))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE, 
						null == modelProduct.getProductClass().getProcessingLevel() ? "" :
							modelProduct.getProductClass().getProcessingLevel()));
		attributes.getEntities().add(processingLevel);
		
		Entity processingMode = new Entity();
		processingMode.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_FQN.toString());
		processingMode
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, PROCESSING_MODE.getValue()))
				.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
						ProductEdmProvider.ET_STRINGATTRIBUTE_NAME))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE,
						null == modelProduct.getMode() ? 0 : modelProduct.getMode()));
		attributes.getEntities().add(processingMode);
		
		Entity productType = new Entity();
		productType.setType(ProductEdmProvider.ET_STRINGATTRIBUTE_FQN.toString());
		productType
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, PRODUCT_TYPE.getValue()))
				.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
						ProductEdmProvider.ET_STRINGATTRIBUTE_NAME))
				.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE,
						modelProduct.getProductClass().getProductType()));
		attributes.getEntities().add(productType);
		
		// Evaluate product parameters
		Map<String, Parameter> parameterMap = modelProduct.getParameters();
		
		for (Entry<String, Parameter> parameter: parameterMap.entrySet()) {
			FullQualifiedName entityType = null;
			Object entityValue = null;
			switch (parameter.getValue().getParameterType()) {
			case STRING:
				entityType = (FullQualifiedName) ProductEdmProvider.ET_STRINGATTRIBUTE_FQN;
				entityValue = parameter.getValue().getStringValue();
				break;
			case INTEGER:
				entityType = (FullQualifiedName) ProductEdmProvider.ET_INTEGERATTRIBUTE_FQN;
				entityValue = parameter.getValue().getIntegerValue();
				break;
			case DOUBLE:
				entityType = (FullQualifiedName) ProductEdmProvider.ET_DOUBLEATTRIBUTE_FQN;
				entityValue = parameter.getValue().getDoubleValue();
				break;
			case BOOLEAN:
				entityType = (FullQualifiedName) ProductEdmProvider.ET_STRINGATTRIBUTE_FQN;
				entityValue = parameter.getValue().getBooleanValue().toString();
				break;
			}
			Entity parameterEntity = new Entity();
			parameterEntity.setType(entityType.getFullQualifiedNameAsString());
			parameterEntity
					.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_NAME, ValueType.PRIMITIVE, parameter.getKey()))
					.addProperty(new Property(null, ProductEdmProvider.ET_ATTRIBUTE_PROP_VALUETYPE, ValueType.PRIMITIVE,
							entityType.getName()))
					.addProperty(new Property(null, ProductEdmProvider.GENERIC_PROP_VALUE, ValueType.PRIMITIVE, entityValue));
			attributes.getEntities().add(parameterEntity);
		}
		
		// Add the attributes collection to the product entity
		Link link = new Link();
		link.setTitle(ProductEdmProvider.ET_PRODUCT_PROP_ATTRIBUTES);
		link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
		link.setRel(Constants.NS_ASSOCIATION_LINK_REL + ProductEdmProvider.ET_PRODUCT_PROP_ATTRIBUTES);
		link.setInlineEntitySet(attributes);
		product.getNavigationLinks().add(link);
		
		// Set product key
		product.setId(new URI(ProductEdmProvider.ET_PRODUCT_NAME + "('" + modelProduct.getUuid() + "')"));

		return product;
	}
}
