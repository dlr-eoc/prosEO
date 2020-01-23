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
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A provider for the Product entity data model
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProductEdmProvider extends CsdlAbstractEdmProvider {

	// Service Namespace
	public static final String NAMESPACE = "prosEO.PRIP";

	// EDM Container
	public static final String CONTAINER_NAME = "Container";
	public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

	// Entity Types Names
	public static final String ET_PRODUCT_NAME = "Product";
	public static final FullQualifiedName ET_PRODUCT_FQN = new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);

	// Entity Set Names
	public static final String ES_PRODUCTS_NAME = "Products";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductEdmProvider.class);

	@Override
	public CsdlEntityContainer getEntityContainer() throws ODataException {
		if (logger.isTraceEnabled()) logger.trace(">>> getEntityContainer()");
		
		// create EntitySets
		List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
		entitySets.add(getEntitySet(CONTAINER, ES_PRODUCTS_NAME));

		// create EntityContainer
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
	public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
		if (logger.isTraceEnabled()) logger.trace(">>> getEntityType({})", entityTypeName);
		
		// this method is called for one of the EntityTypes that are configured in the Schema
		if(entityTypeName.equals(ET_PRODUCT_FQN)){

			//create EntityType properties
			CsdlProperty id = new CsdlProperty().setName("Id").setType(EdmPrimitiveTypeKind.Guid.getFullQualifiedName());
			CsdlProperty name = new CsdlProperty().setName("Name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty contentType = new CsdlProperty().setName("ContentType").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
			CsdlProperty contentLength = new CsdlProperty().setName("ContentLength").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName());
			CsdlProperty creationDate = new CsdlProperty().setName("CreationDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
			CsdlProperty evictionDate = new CsdlProperty().setName("EvictionDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName());
			// TODO Add structured properties

			// create CsdlPropertyRef for Key element
			CsdlPropertyRef propertyRef = new CsdlPropertyRef();
			propertyRef.setName("Id");

			// configure EntityType
			CsdlEntityType entityType = new CsdlEntityType();
			entityType.setName(ET_PRODUCT_NAME);
			entityType.setProperties(Arrays.asList(id, name , contentType, contentLength, creationDate, evictionDate));
			entityType.setKey(Collections.singletonList(propertyRef));
			entityType.setHasStream(true);

			return entityType;
		}

		return null;	}

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
