/**
 * ProductEdmProvider.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumMember;
import org.apache.olingo.commons.api.edm.provider.CsdlEnumType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.dlr.proseo.model.enums.ProductionType;

/**
 * A provider for the Product entity data model (as defined in Production Interface Delivery Point Specification,
 * ESA-EOPG-EOPGC-IF-3, issue 1.2, sec. 3.1)
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProductEdmProvider extends CsdlAbstractEdmProvider {

	// Service Namespace
	public static final String NAMESPACE = "OData.CSC"; // Copernicus Space Component

	// EDM Container
	public static final String CONTAINER_NAME = "Container";
	public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);
	
	// Generic properties for several types
	public static final String GENERIC_PROP_ID = "Id";
	public static final String GENERIC_PROP_NAME = "Name";
	public static final String GENERIC_PROP_CONTENT_TYPE = "ContentType";
	public static final String GENERIC_PROP_CONTENT_LENGTH = "ContentLength";
	public static final String GENERIC_PROP_VALUE = "Value";

	// Entity Types
	public static final String ET_PRODUCT_NAME = "Product";
	public static final FullQualifiedName ET_PRODUCT_FQN = new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);
	public static final String ET_PRODUCT_PROP_ORIGIN_DATE = "OriginDate";
	public static final String ET_PRODUCT_PROP_PUBLICATION_DATE = "PublicationDate";
	public static final String ET_PRODUCT_PROP_EVICTION_DATE = "EvictionDate";
	public static final String ET_PRODUCT_PROP_CHECKSUMS = "Checksums";
	public static final String ET_PRODUCT_PROP_CONTENT_DATE = "ContentDate";
	public static final String ET_PRODUCT_PROP_PRODUCTION_TYPE = "ProductionType";
	public static final String ET_PRODUCT_PROP_ATTRIBUTES = "Attributes";
	
	public static final String ET_ATTRIBUTES_NAME = ET_PRODUCT_PROP_ATTRIBUTES;
	public static final FullQualifiedName ET_ATTRIBUTES_FQN = new FullQualifiedName(NAMESPACE, ET_ATTRIBUTES_NAME);
	public static final String ET_ATTRIBUTES_PROP_VALUETYPE = "ValueType";
	
	public static final String ET_STRINGATTRIBUTE_NAME = "StringAttribute";
	public static final Object ET_STRINGATTRIBUTE_FQN = new FullQualifiedName(NAMESPACE, ET_STRINGATTRIBUTE_NAME);
	
	public static final String ET_DATEATTRIBUTE_NAME = "DateTimeOffsetAttribute";
	public static final Object ET_DATEATTRIBUTE_FQN = new FullQualifiedName(NAMESPACE, ET_DATEATTRIBUTE_NAME);
	
	public static final String ET_INTEGERATTRIBUTE_NAME = "IntegerAttribute";
	public static final Object ET_INTEGERATTRIBUTE_FQN = new FullQualifiedName(NAMESPACE, ET_INTEGERATTRIBUTE_NAME);

	public static final String ET_DOUBLEATTRIBUTE_NAME = "DoubleAttribute";
	public static final Object ET_DOUBLEATTRIBUTE_FQN = new FullQualifiedName(NAMESPACE, ET_DOUBLEATTRIBUTE_NAME);

	// Entity Sets
	public static final String ES_PRODUCTS_NAME = "Products";

	// Enumeration Types
	public static final String EN_PRODUCTIONTYPE_NAME = ET_PRODUCT_PROP_PRODUCTION_TYPE;
	public static final FullQualifiedName EN_PRODUCTIONTYPE_FQN = new FullQualifiedName(NAMESPACE, EN_PRODUCTIONTYPE_NAME);
	public static final String EN_PRODUCTIONTYPE_SYSTEMATIC = ProductionType.SYSTEMATIC.toString();
	public static final int EN_PRODUCTIONTYPE_SYSTEMATIC_VAL = 10;
	public static final String EN_PRODUCTIONTYPE_ONDEMDEF = ProductionType.ON_DEMAND_DEFAULT.toString();
	public static final int EN_PRODUCTIONTYPE_ONDEMDEF_VAL = 20;
	public static final String EN_PRODUCTIONTYPE_ONDEMNODEF = ProductionType.ON_DEMAND_NON_DEFAULT.toString();
	public static final int EN_PRODUCTIONTYPE_ONDEMNODEF_VAL = 30;

	// Complex Types
	public static final String CT_CHECKSUM_NAME = "Checksum";
	public static final FullQualifiedName CT_CHECKSUM_FQN = new FullQualifiedName(NAMESPACE, CT_CHECKSUM_NAME);
	public static final String CT_CHECKSUM_PROP_ALGORITHM = "Algorithm";
	public static final String CT_CHECKSUM_PROP_VALUE = "Value";
	public static final String CT_CHECKSUM_PROP_CHECKSUM_DATE = "ChecksumDate";

	public static final String CT_TIMERANGE_NAME = "TimeRange";
	public static final FullQualifiedName CT_TIMERANGE_FQN = new FullQualifiedName(NAMESPACE, CT_TIMERANGE_NAME);
	public static final String CT_TIMERANGE_PROP_START = "Start";
	public static final String CT_TIMERANGE_PROP_END = "End";
	
	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductEdmProvider.class);

	@Override
	public CsdlEntityContainer getEntityContainer() throws ODataException {
		if (logger.isTraceEnabled()) logger.trace(">>> getEntityContainer()");
		
		// Create EntitySets
		List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
		entitySets.add(getEntitySet(CONTAINER, ES_PRODUCTS_NAME));

		// Create EntityContainer
		CsdlEntityContainer entityContainer = new CsdlEntityContainer();
		entityContainer.setName(CONTAINER_NAME);
		entityContainer.setEntitySets(entitySets);

		return entityContainer;
	}

	@Override
	public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
		if (logger.isTraceEnabled()) logger.trace(">>> getEntityContainerInfo({})", entityContainerName);
		
		// This method is invoked when displaying the Service Document at e.g. http://localhost:8080/DemoService/DemoService.svc
		if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
			CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
			entityContainerInfo.setContainerName(CONTAINER);
			return entityContainerInfo;
		}

		return null;
	}

	@Override
	public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {
		if (logger.isTraceEnabled()) logger.trace(">>> getEntitySet({}, {})", entityContainer, entitySetName);
		
		if(entityContainer.equals(CONTAINER)){
			if(entitySetName.equals(ES_PRODUCTS_NAME)){
				CsdlEntitySet entitySet = new CsdlEntitySet();
				entitySet.setName(ES_PRODUCTS_NAME);
				entitySet.setType(ET_PRODUCT_FQN);

				return entitySet;
			}
		}

		return null;
	}
	
	@Override
	public CsdlEnumType	getEnumType(FullQualifiedName enumTypeName) {
		if (logger.isTraceEnabled()) logger.trace(">>> getEnumType({})", enumTypeName);
		
		// This method is called for one of the EnumTypes that are configured in the Schema
		if(enumTypeName.equals(EN_PRODUCTIONTYPE_FQN)){
			// Create enumeration values
			CsdlEnumMember systematic = new CsdlEnumMember().setName(EN_PRODUCTIONTYPE_SYSTEMATIC).setValue(String.valueOf(EN_PRODUCTIONTYPE_SYSTEMATIC_VAL));
			CsdlEnumMember onDemandDefault = new CsdlEnumMember().setName(EN_PRODUCTIONTYPE_ONDEMDEF).setValue(String.valueOf(EN_PRODUCTIONTYPE_ONDEMDEF_VAL));
			CsdlEnumMember onDemandNonDefault = new CsdlEnumMember().setName(EN_PRODUCTIONTYPE_ONDEMNODEF).setValue(String.valueOf(EN_PRODUCTIONTYPE_ONDEMNODEF_VAL));
			
			// Configure ProductionType enumeration type
			CsdlEnumType productionTypeType = new CsdlEnumType();
			productionTypeType.setName(EN_PRODUCTIONTYPE_NAME);
			productionTypeType.setMembers(Arrays.asList(systematic, onDemandDefault, onDemandNonDefault));

			return productionTypeType;
		}
		
		return null;
	}
	
	@Override
	public CsdlComplexType getComplexType(FullQualifiedName complexTypeName) {
		if (logger.isTraceEnabled()) logger.trace(">>> getComplexType({})", complexTypeName);
		
		// This method is called for one of the EnumTypes that are configured in the Schema
		if(complexTypeName.equals(CT_CHECKSUM_FQN)){
			
			// Create Checksum properties
			CsdlProperty algorithm = new CsdlProperty().setName(CT_CHECKSUM_PROP_ALGORITHM).setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty value = new CsdlProperty().setName(CT_CHECKSUM_PROP_VALUE).setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty checksumDate = new CsdlProperty().setName(CT_CHECKSUM_PROP_CHECKSUM_DATE).setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
			
			// Configure Checksum complex type
			CsdlComplexType checksumType = new CsdlComplexType();
			checksumType.setName(CT_CHECKSUM_NAME);
			checksumType.setProperties(Arrays.asList(algorithm, value, checksumDate));
			return checksumType;
		} else if(complexTypeName.equals(CT_TIMERANGE_FQN)){
			
			// Create TimeRange properties
			CsdlProperty start = new CsdlProperty().setName(CT_TIMERANGE_PROP_START).setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
			CsdlProperty end = new CsdlProperty().setName(CT_TIMERANGE_PROP_END).setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
			
			// Configure TimeRange complex type
			CsdlComplexType timeRangeType = new CsdlComplexType();
			timeRangeType.setName(CT_TIMERANGE_NAME);
			timeRangeType.setProperties(Arrays.asList(start, end));
			return timeRangeType;
		}
		
		return null;
	}

	@Override
	public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
		if (logger.isTraceEnabled()) logger.trace(">>> getEntityType({})", entityTypeName);
		
		// this method is called for one of the EntityTypes that are configured in the Schema
		if(entityTypeName.equals(ET_PRODUCT_FQN)){

			// Create Product properties
			CsdlProperty id = new CsdlProperty().setName(GENERIC_PROP_ID).setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());
			CsdlProperty name = new CsdlProperty().setName(GENERIC_PROP_NAME).setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty contentType = new CsdlProperty().setName(GENERIC_PROP_CONTENT_TYPE).setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty contentLength = new CsdlProperty().setName(GENERIC_PROP_CONTENT_LENGTH).setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
			CsdlProperty originDate = new CsdlProperty().setName(ET_PRODUCT_PROP_ORIGIN_DATE).setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
			CsdlProperty publicationDate = new CsdlProperty().setName(ET_PRODUCT_PROP_PUBLICATION_DATE).setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
			CsdlProperty evictionDate = new CsdlProperty().setName(ET_PRODUCT_PROP_EVICTION_DATE).setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());

			// Add structured properties
			CsdlProperty checksums = new CsdlProperty().setName(ET_PRODUCT_PROP_CHECKSUMS).setCollection(true).setType(CT_CHECKSUM_FQN);
			CsdlProperty contentDate = new CsdlProperty().setName(ET_PRODUCT_PROP_CONTENT_DATE).setType(CT_TIMERANGE_FQN);
			CsdlProperty productionType = new CsdlProperty().setName(ET_PRODUCT_PROP_PRODUCTION_TYPE).setType(EN_PRODUCTIONTYPE_FQN);
			
			// Add navigation properties
			CsdlNavigationProperty attributes = new CsdlNavigationProperty().setName(ET_PRODUCT_PROP_ATTRIBUTES)
				.setType(ET_ATTRIBUTES_FQN).setCollection(true).setPartner(ET_PRODUCT_NAME);

			// Create CsdlPropertyRef for Key element
			CsdlPropertyRef idRef = new CsdlPropertyRef();
			idRef.setName(GENERIC_PROP_ID);

			// Configure Product entity type
			CsdlEntityType productType = new CsdlEntityType();
			productType.setName(ET_PRODUCT_NAME);
			productType.setProperties(Arrays.asList(id, name , contentType, contentLength, originDate, publicationDate, evictionDate, checksums,
					contentDate, productionType));
			productType.setNavigationProperties(Arrays.asList(attributes));
			productType.setKey(Collections.singletonList(idRef));
			productType.setHasStream(true);

			return productType;
		} else if (entityTypeName.equals(ET_ATTRIBUTES_FQN)) {
			// Create Attribute properties
			CsdlProperty name = new CsdlProperty().setName(GENERIC_PROP_NAME).setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty valueType = new CsdlProperty().setName(ET_ATTRIBUTES_PROP_VALUETYPE).setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

			// Create CsdlPropertyRef for Key element
			CsdlPropertyRef idRef = new CsdlPropertyRef();
			idRef.setName(GENERIC_PROP_NAME);

			// Configure Attributes entity type
			CsdlEntityType attributesType = new CsdlEntityType();
			attributesType.setName(ET_ATTRIBUTES_NAME);
			attributesType.setProperties(Arrays.asList(name, valueType));
			attributesType.setKey(Collections.singletonList(idRef));
			
			return attributesType;
		} else if (entityTypeName.equals(ET_STRINGATTRIBUTE_FQN)) {
			// Create specific StringAttribute properties
			CsdlProperty stringValue = new CsdlProperty().setName(GENERIC_PROP_VALUE).setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			
			// Configure StringAttribute entity type
			CsdlEntityType stringAttributeType = new CsdlEntityType();
			stringAttributeType.setName(ET_STRINGATTRIBUTE_NAME);
			stringAttributeType.setBaseType(ET_ATTRIBUTES_FQN);
			stringAttributeType.setProperties(Arrays.asList(stringValue));
			
			return stringAttributeType;
		} else if (entityTypeName.equals(ET_DATEATTRIBUTE_FQN)) {
			// Create specific DateAttribute properties
			CsdlProperty dateValue = new CsdlProperty().setName(GENERIC_PROP_VALUE).setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
			
			// Configure DateAttribute entity type
			CsdlEntityType dateAttributeType = new CsdlEntityType();
			dateAttributeType.setName(ET_DATEATTRIBUTE_NAME);
			dateAttributeType.setBaseType(ET_ATTRIBUTES_FQN);
			dateAttributeType.setProperties(Arrays.asList(dateValue));
			
			return dateAttributeType;
		} else if (entityTypeName.equals(ET_INTEGERATTRIBUTE_FQN)) {
			// Create specific IntegerAttribute properties
			CsdlProperty integerValue = new CsdlProperty().setName(GENERIC_PROP_VALUE).setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
			
			// Configure IntegerAttribute entity type
			CsdlEntityType integerAttributeType = new CsdlEntityType();
			integerAttributeType.setName(ET_INTEGERATTRIBUTE_NAME);
			integerAttributeType.setBaseType(ET_ATTRIBUTES_FQN);
			integerAttributeType.setProperties(Arrays.asList(integerValue));
			
			return integerAttributeType;
		} else if (entityTypeName.equals(ET_DOUBLEATTRIBUTE_FQN)) {
			// Create specific DoubleAttribute properties
			CsdlProperty doubleValue = new CsdlProperty().setName(GENERIC_PROP_VALUE).setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
			
			// Configure DoubleAttribute entity type
			CsdlEntityType doubleAttributeType = new CsdlEntityType();
			doubleAttributeType.setName(ET_DOUBLEATTRIBUTE_NAME);
			doubleAttributeType.setBaseType(ET_ATTRIBUTES_FQN);
			doubleAttributeType.setProperties(Arrays.asList(doubleValue));
			
			return doubleAttributeType;
		}

		return null;
	}

	@Override
	public List<CsdlSchema> getSchemas() throws ODataException {
		if (logger.isTraceEnabled()) logger.trace(">>> getSchemas()");
		
		// create Schema
		CsdlSchema schema = new CsdlSchema();
		schema.setNamespace(NAMESPACE);

		// add EntityTypes
		List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
		entityTypes.add(getEntityType(ET_PRODUCT_FQN));
		schema.setEntityTypes(entityTypes);

		// add EntityContainer
		schema.setEntityContainer(getEntityContainer());

		// finally
		List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
		schemas.add(schema);

		return schemas;
	}

}
