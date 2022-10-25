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
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import org.springframework.stereotype.Component;

import de.dlr.proseo.logging.logger.ProseoLogger;

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
	public static final String ET_PRODUCT_PROP_CHECKSUM = "Checksum";
	public static final String ET_PRODUCT_PROP_CONTENT_DATE = "ContentDate";
	public static final String ET_PRODUCT_PROP_PRODUCTION_TYPE = "ProductionType";
	public static final String ET_PRODUCT_PROP_FOOTPRINT = "Footprint";
	public static final String ET_PRODUCT_PROP_ATTRIBUTES = "Attributes";
	public static final String ET_PRODUCT_PROP_STRING_ATTRIBUTES = "StringAttributes";
	public static final String ET_PRODUCT_PROP_INT_ATTRIBUTES = "IntegerAttributes";
	public static final String ET_PRODUCT_PROP_DOUBLE_ATTRIBUTES = "DoubleAttributes";
	public static final String ET_PRODUCT_PROP_BOOL_ATTRIBUTES = "BooleanAttributes";
	public static final String ET_PRODUCT_PROP_DATE_ATTRIBUTES = "DateTimeOffsetAttributes";
	
	public static final String ET_ATTRIBUTE_NAME = "Attribute";
	public static final FullQualifiedName ET_ATTRIBUTE_FQN = new FullQualifiedName(NAMESPACE, ET_ATTRIBUTE_NAME);
	public static final String ET_ATTRIBUTE_PROP_VALUETYPE = "ValueType";
	
	public static final String ET_STRINGATTRIBUTE_NAME = "StringAttribute";
	public static final FullQualifiedName ET_STRINGATTRIBUTE_FQN = new FullQualifiedName(NAMESPACE, ET_STRINGATTRIBUTE_NAME);
	public static final String ET_STRINGATTRIBUTE_VALUETYPE = "String";
	
	public static final String ET_DATEATTRIBUTE_NAME = "DateTimeOffsetAttribute";
	public static final FullQualifiedName ET_DATEATTRIBUTE_FQN = new FullQualifiedName(NAMESPACE, ET_DATEATTRIBUTE_NAME);
	public static final String ET_DATEATTRIBUTE_VALUETYPE = "DateTimeOffset";
	
	public static final String ET_INTEGERATTRIBUTE_NAME = "IntegerAttribute";
	public static final FullQualifiedName ET_INTEGERATTRIBUTE_FQN = new FullQualifiedName(NAMESPACE, ET_INTEGERATTRIBUTE_NAME);
	public static final String ET_INTEGERATTRIBUTE_VALUETYPE = "Integer";

	public static final String ET_DOUBLEATTRIBUTE_NAME = "DoubleAttribute";
	public static final FullQualifiedName ET_DOUBLEATTRIBUTE_FQN = new FullQualifiedName(NAMESPACE, ET_DOUBLEATTRIBUTE_NAME);
	public static final String ET_DOUBLEATTRIBUTE_VALUETYPE = "Double";

	public static final String ET_BOOLEANATTRIBUTE_NAME = "BooleanAttribute";
	public static final FullQualifiedName ET_BOOLEANATTRIBUTE_FQN = new FullQualifiedName(NAMESPACE, ET_BOOLEANATTRIBUTE_NAME);
	public static final String ET_BOOLEANATTRIBUTE_VALUETYPE = "Boolean";

	// Entity Sets
	public static final String ES_PRODUCTS_NAME = "Products";
	public static final String ES_ATTRIBUTES_NAME = "Attributes";
	public static final String ES_STRINGATTRIBUTES_NAME = "StringAttributes";
	public static final String ES_INTEGERATTRIBUTES_NAME = "IntegerAttributes";
	public static final String ES_DOUBLEATTRIBUTES_NAME = "DoubleAttributes";
	public static final String ES_BOOLEANATTRIBUTES_NAME = "BooleanAttributes";
	public static final String ES_DATEATTRIBUTES_NAME = "DateTimeOffsetAttributes";

	// Enumeration Types
	public static final String EN_PRODUCTIONTYPE_NAME = "ProductionType";
	public static final FullQualifiedName EN_PRODUCTIONTYPE_FQN = new FullQualifiedName(NAMESPACE, EN_PRODUCTIONTYPE_NAME);
	public static final String EN_PRODUCTIONTYPE_SYSTEMATIC = "systematic_production";
	public static final int EN_PRODUCTIONTYPE_SYSTEMATIC_VAL = 0;
	public static final String EN_PRODUCTIONTYPE_ONDEMDEF = "on-demand default";
	public static final int EN_PRODUCTIONTYPE_ONDEMDEF_VAL = 1;
	public static final String EN_PRODUCTIONTYPE_ONDEMNODEF = "on-demand non-default";
	public static final int EN_PRODUCTIONTYPE_ONDEMNODEF_VAL = 2;

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
	private static ProseoLogger logger = new ProseoLogger(ProductEdmProvider.class);

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

		if (logger.isTraceEnabled()) logger.trace("<<< getEntityContainer()");
		return entityContainer;
	}

	@Override
	public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {
		if (logger.isTraceEnabled()) logger.trace(">>> getEntityContainerInfo({})", entityContainerName);
		
		// This method is invoked when displaying the Service Document at e.g. http://localhost:8080/DemoService/DemoService.svc
		if (entityContainerName == null || entityContainerName.equals(CONTAINER)) {
			CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
			entityContainerInfo.setContainerName(CONTAINER);

			if (logger.isTraceEnabled()) logger.trace("<<< getEntityContainerInfo({}, {})", entityContainerName);
			return entityContainerInfo;
		}

		if (logger.isTraceEnabled()) logger.trace("<<< getEntityContainerInfo({}, {}) --> null", entityContainerName);
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
				
			    CsdlNavigationPropertyBinding navAttributesBinding = new CsdlNavigationPropertyBinding();
			    navAttributesBinding.setPath(ET_ATTRIBUTE_NAME); 
			    navAttributesBinding.setTarget(ES_ATTRIBUTES_NAME); //target entitySet, where the nav prop points to

			    entitySet.setNavigationPropertyBindings(Arrays.asList(navAttributesBinding));
				
				if (logger.isTraceEnabled()) logger.trace("<<< getEntitySet({}, {})", entityContainer, entitySetName);
				return entitySet;
			} else if (entitySetName.equals(ES_ATTRIBUTES_NAME)) {
				CsdlEntitySet entitySet = new CsdlEntitySet();
				entitySet.setName(ES_ATTRIBUTES_NAME);
				entitySet.setType(ET_ATTRIBUTE_FQN);
				entitySet.setIncludeInServiceDocument(false);
				
				if (logger.isTraceEnabled()) logger.trace("<<< getEntitySet({}, {})", entityContainer, entitySetName);
				return entitySet;
			} else if (entitySetName.equals(ES_STRINGATTRIBUTES_NAME)) {
				CsdlEntitySet entitySet = new CsdlEntitySet();
				entitySet.setName(ES_STRINGATTRIBUTES_NAME);
				entitySet.setType(ET_STRINGATTRIBUTE_FQN);
				entitySet.setIncludeInServiceDocument(false);
				
				if (logger.isTraceEnabled()) logger.trace("<<< getEntitySet({}, {})", entityContainer, entitySetName);
				return entitySet;
			} else if (entitySetName.equals(ES_INTEGERATTRIBUTES_NAME)) {
				CsdlEntitySet entitySet = new CsdlEntitySet();
				entitySet.setName(ES_INTEGERATTRIBUTES_NAME);
				entitySet.setType(ET_INTEGERATTRIBUTE_FQN);
				entitySet.setIncludeInServiceDocument(false);
				
				if (logger.isTraceEnabled()) logger.trace("<<< getEntitySet({}, {})", entityContainer, entitySetName);
				return entitySet;
			} else if (entitySetName.equals(ES_DOUBLEATTRIBUTES_NAME)) {
				CsdlEntitySet entitySet = new CsdlEntitySet();
				entitySet.setName(ES_DOUBLEATTRIBUTES_NAME);
				entitySet.setType(ET_DOUBLEATTRIBUTE_FQN);
				entitySet.setIncludeInServiceDocument(false);
				
				if (logger.isTraceEnabled()) logger.trace("<<< getEntitySet({}, {})", entityContainer, entitySetName);
				return entitySet;
			} else if (entitySetName.equals(ES_BOOLEANATTRIBUTES_NAME)) {
				CsdlEntitySet entitySet = new CsdlEntitySet();
				entitySet.setName(ES_BOOLEANATTRIBUTES_NAME);
				entitySet.setType(ET_BOOLEANATTRIBUTE_FQN);
				entitySet.setIncludeInServiceDocument(false);
				
				if (logger.isTraceEnabled()) logger.trace("<<< getEntitySet({}, {})", entityContainer, entitySetName);
				return entitySet;
			} else if (entitySetName.equals(ES_DATEATTRIBUTES_NAME)) {
				CsdlEntitySet entitySet = new CsdlEntitySet();
				entitySet.setName(ES_DATEATTRIBUTES_NAME);
				entitySet.setType(ET_DATEATTRIBUTE_FQN);
				entitySet.setIncludeInServiceDocument(false);
				
				if (logger.isTraceEnabled()) logger.trace("<<< getEntitySet({}, {})", entityContainer, entitySetName);
				return entitySet;
			}
		}

		if (logger.isTraceEnabled()) logger.trace("<<< getEntitySet({}, {}) --> null", entityContainer, entitySetName);
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
			productionTypeType.setUnderlyingType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());

			if (logger.isTraceEnabled()) logger.trace("<<< getEnumType({})", enumTypeName);
			return productionTypeType;
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< getEnumType({}) --> null", enumTypeName);
		return null;
	}
	
	@Override
	public CsdlComplexType getComplexType(FullQualifiedName complexTypeName) {
		if (logger.isTraceEnabled()) logger.trace(">>> getComplexType({})", complexTypeName);
		
		// This method is called for one of the EnumTypes that are configured in the Schema
		if(complexTypeName.equals(CT_CHECKSUM_FQN)){
			
			// Create Checksum properties
			CsdlProperty algorithm = new CsdlProperty().setName(CT_CHECKSUM_PROP_ALGORITHM)
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()).setNullable(false);
			CsdlProperty value = new CsdlProperty().setName(CT_CHECKSUM_PROP_VALUE)
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()).setNullable(false);
			CsdlProperty checksumDate = new CsdlProperty().setName(CT_CHECKSUM_PROP_CHECKSUM_DATE)
					.setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
			
			// Configure Checksum complex type
			CsdlComplexType checksumType = new CsdlComplexType();
			checksumType.setName(CT_CHECKSUM_NAME);
			checksumType.setProperties(Arrays.asList(algorithm, value, checksumDate));

			if (logger.isTraceEnabled()) logger.trace("<<< getComplexType({})", complexTypeName);
			return checksumType;
		} else if(complexTypeName.equals(CT_TIMERANGE_FQN)){
			
			// Create TimeRange properties
			CsdlProperty start = new CsdlProperty().setName(CT_TIMERANGE_PROP_START)
					.setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()).setPrecision(6).setNullable(false);
			CsdlProperty end = new CsdlProperty().setName(CT_TIMERANGE_PROP_END)
					.setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()).setPrecision(6).setNullable(false);
			
			// Configure TimeRange complex type
			CsdlComplexType timeRangeType = new CsdlComplexType();
			timeRangeType.setName(CT_TIMERANGE_NAME);
			timeRangeType.setProperties(Arrays.asList(start, end));

			if (logger.isTraceEnabled()) logger.trace("<<< getComplexType({})", complexTypeName);
			return timeRangeType;
		}
		
		if (logger.isTraceEnabled()) logger.trace("<<< getComplexType({}) --> null", complexTypeName);
		return null;
	}

	@Override
	public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
		if (logger.isTraceEnabled()) logger.trace(">>> getEntityType({})", entityTypeName);
		
		// this method is called for one of the EntityTypes that are configured in the Schema
		if(entityTypeName.equals(ET_PRODUCT_FQN)){

			// Create Product properties
			CsdlProperty id = new CsdlProperty().setName(GENERIC_PROP_ID)
					.setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());
			CsdlProperty name = new CsdlProperty().setName(GENERIC_PROP_NAME)
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty contentType = new CsdlProperty().setName(GENERIC_PROP_CONTENT_TYPE)
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty contentLength = new CsdlProperty().setName(GENERIC_PROP_CONTENT_LENGTH)
					.setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
			CsdlProperty originDate = new CsdlProperty().setName(ET_PRODUCT_PROP_ORIGIN_DATE)
					.setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()).setPrecision(3);
			CsdlProperty publicationDate = new CsdlProperty().setName(ET_PRODUCT_PROP_PUBLICATION_DATE)
					.setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()).setPrecision(3);
			CsdlProperty evictionDate = new CsdlProperty().setName(ET_PRODUCT_PROP_EVICTION_DATE)
					.setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()).setPrecision(3);
			CsdlProperty footprint = new CsdlProperty().setName(ET_PRODUCT_PROP_FOOTPRINT)
					.setType(EdmPrimitiveTypeKind.GeographyPolygon.getFullQualifiedName());

			// Add structured properties
			CsdlProperty checksum = new CsdlProperty().setName(ET_PRODUCT_PROP_CHECKSUM).setCollection(true)
					.setType(CT_CHECKSUM_FQN);
			CsdlProperty contentDate = new CsdlProperty().setName(ET_PRODUCT_PROP_CONTENT_DATE)
					.setType(CT_TIMERANGE_FQN);
			CsdlProperty productionType = new CsdlProperty().setName(ET_PRODUCT_PROP_PRODUCTION_TYPE)
					.setType(EN_PRODUCTIONTYPE_FQN);
			
			// Add navigation properties
			CsdlNavigationProperty attributes = new CsdlNavigationProperty().setName(ET_PRODUCT_PROP_ATTRIBUTES)
					.setType(ET_ATTRIBUTE_FQN).setCollection(true);
			CsdlNavigationProperty stringAttributes = new CsdlNavigationProperty().setName(ET_PRODUCT_PROP_STRING_ATTRIBUTES)
					.setType(ET_STRINGATTRIBUTE_FQN).setCollection(true);
			CsdlNavigationProperty intAttributes = new CsdlNavigationProperty().setName(ET_PRODUCT_PROP_INT_ATTRIBUTES)
					.setType(ET_INTEGERATTRIBUTE_FQN).setCollection(true);
			CsdlNavigationProperty doubleAttributes = new CsdlNavigationProperty().setName(ET_PRODUCT_PROP_DOUBLE_ATTRIBUTES)
					.setType(ET_DOUBLEATTRIBUTE_FQN).setCollection(true);
			CsdlNavigationProperty boolAttributes = new CsdlNavigationProperty().setName(ET_PRODUCT_PROP_BOOL_ATTRIBUTES)
					.setType(ET_BOOLEANATTRIBUTE_FQN).setCollection(true);
			CsdlNavigationProperty dateAttributes = new CsdlNavigationProperty().setName(ET_PRODUCT_PROP_DATE_ATTRIBUTES)
					.setType(ET_DATEATTRIBUTE_FQN).setCollection(true);

			// Create CsdlPropertyRef for Key element
			CsdlPropertyRef idRef = new CsdlPropertyRef();
			idRef.setName(GENERIC_PROP_ID);

			// Configure Product entity type
			CsdlEntityType productType = new CsdlEntityType();
			productType.setName(ET_PRODUCT_NAME);
			productType.setProperties(Arrays.asList(id, name , contentType, contentLength, originDate, publicationDate, 
					evictionDate, footprint, checksum, contentDate, productionType));
			productType.setNavigationProperties(Arrays.asList(attributes, stringAttributes, intAttributes, doubleAttributes, 
					boolAttributes, dateAttributes));
			productType.setKey(Collections.singletonList(idRef));
			productType.setHasStream(true);

			if (logger.isTraceEnabled()) logger.trace("<<< getEntityType({})", entityTypeName);
			return productType;
		} else if (entityTypeName.equals(ET_ATTRIBUTE_FQN)) {
			// Create Attribute properties
			CsdlProperty name = new CsdlProperty().setName(GENERIC_PROP_NAME).setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty valueType = new CsdlProperty().setName(ET_ATTRIBUTE_PROP_VALUETYPE).setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

			// Create CsdlPropertyRef for Key element
			CsdlPropertyRef idRef = new CsdlPropertyRef();
			idRef.setName(GENERIC_PROP_NAME);

			// Configure Attributes entity type
			CsdlEntityType attributesType = new CsdlEntityType();
			attributesType.setName(ET_ATTRIBUTE_NAME);
			attributesType.setProperties(Arrays.asList(name, valueType));
			attributesType.setKey(Collections.singletonList(idRef));
			
			if (logger.isTraceEnabled()) logger.trace("<<< getEntityType({})", entityTypeName);
			return attributesType;
		} else if (entityTypeName.equals(ET_STRINGATTRIBUTE_FQN)) {
			// Create specific StringAttribute properties
			CsdlProperty stringValue = new CsdlProperty().setName(GENERIC_PROP_VALUE)
					.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			
			// Configure StringAttribute entity type
			CsdlEntityType stringAttributeType = new CsdlEntityType();
			stringAttributeType.setName(ET_STRINGATTRIBUTE_NAME);
			stringAttributeType.setBaseType(ET_ATTRIBUTE_FQN);
			stringAttributeType.setProperties(Arrays.asList(stringValue));
			
			if (logger.isTraceEnabled()) logger.trace("<<< getEntityType({})", entityTypeName);
			return stringAttributeType;
		} else if (entityTypeName.equals(ET_DATEATTRIBUTE_FQN)) {
			// Create specific DateAttribute properties
			CsdlProperty dateValue = new CsdlProperty().setName(GENERIC_PROP_VALUE)
					.setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
			
			// Configure DateAttribute entity type
			CsdlEntityType dateAttributeType = new CsdlEntityType();
			dateAttributeType.setName(ET_DATEATTRIBUTE_NAME);
			dateAttributeType.setBaseType(ET_ATTRIBUTE_FQN);
			dateAttributeType.setProperties(Arrays.asList(dateValue));
			
			if (logger.isTraceEnabled()) logger.trace("<<< getEntityType({})", entityTypeName);
			return dateAttributeType;
		} else if (entityTypeName.equals(ET_INTEGERATTRIBUTE_FQN)) {
			// Create specific IntegerAttribute properties
			CsdlProperty integerValue = new CsdlProperty().setName(GENERIC_PROP_VALUE)
					.setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
			
			// Configure IntegerAttribute entity type
			CsdlEntityType integerAttributeType = new CsdlEntityType();
			integerAttributeType.setName(ET_INTEGERATTRIBUTE_NAME);
			integerAttributeType.setBaseType(ET_ATTRIBUTE_FQN);
			integerAttributeType.setProperties(Arrays.asList(integerValue));
			
			if (logger.isTraceEnabled()) logger.trace("<<< getEntityType({})", entityTypeName);
			return integerAttributeType;
		} else if (entityTypeName.equals(ET_DOUBLEATTRIBUTE_FQN)) {
			// Create specific DoubleAttribute properties
			CsdlProperty doubleValue = new CsdlProperty().setName(GENERIC_PROP_VALUE)
					.setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());
			
			// Configure DoubleAttribute entity type
			CsdlEntityType doubleAttributeType = new CsdlEntityType();
			doubleAttributeType.setName(ET_DOUBLEATTRIBUTE_NAME);
			doubleAttributeType.setBaseType(ET_ATTRIBUTE_FQN);
			doubleAttributeType.setProperties(Arrays.asList(doubleValue));
			
			if (logger.isTraceEnabled()) logger.trace("<<< getEntityType({})", entityTypeName);
			return doubleAttributeType;
		} else if (entityTypeName.equals(ET_BOOLEANATTRIBUTE_FQN)) {
			// Create specific DoubleAttribute properties
			CsdlProperty booleanValue = new CsdlProperty().setName(GENERIC_PROP_VALUE)
					.setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName());
			
			// Configure BooleanAttribute entity type
			CsdlEntityType booleanAttributeType = new CsdlEntityType();
			booleanAttributeType.setName(ET_BOOLEANATTRIBUTE_NAME);
			booleanAttributeType.setBaseType(ET_ATTRIBUTE_FQN);
			booleanAttributeType.setProperties(Arrays.asList(booleanValue));
			
			if (logger.isTraceEnabled()) logger.trace("<<< getEntityType({})", entityTypeName);
			return booleanAttributeType;
		}

		if (logger.isTraceEnabled()) logger.trace("<<< getEntityType({}) --> null", entityTypeName);
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
		entityTypes.add(getEntityType(ET_ATTRIBUTE_FQN));
		entityTypes.add(getEntityType(ET_STRINGATTRIBUTE_FQN));
		entityTypes.add(getEntityType(ET_DATEATTRIBUTE_FQN));
		entityTypes.add(getEntityType(ET_INTEGERATTRIBUTE_FQN));
		schema.setEntityTypes(entityTypes);

		// add EntityContainer
		schema.setEntityContainer(getEntityContainer());
		
		// add ComplexTypes
		List<CsdlComplexType> complexTypes = new ArrayList<>();
		complexTypes.add(getComplexType(CT_CHECKSUM_FQN));
		complexTypes.add(getComplexType(CT_TIMERANGE_FQN));
		schema.setComplexTypes(complexTypes);
		
		// add EnumTypes
		List<CsdlEnumType> enumTypes = new ArrayList<>();
		enumTypes.add(getEnumType(EN_PRODUCTIONTYPE_FQN));
		schema.setEnumTypes(enumTypes);

		// finally
		List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
		schemas.add(schema);

		if (logger.isTraceEnabled()) logger.trace("<<< getSchemas()");
		return schemas;
	}

}
