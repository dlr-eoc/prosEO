/**
 * ProductEntityCollectionProcessor.java
 * 
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.prip.odata;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Retrieve product collections from the prosEO metadata database (via the Ingestor component) with additional information 
 * from the prosEO Storage Manager
 * 
 * @author Dr. Thomas Bassler
 *
 */
@Component
public class ProductEntityCollectionProcessor implements EntityCollectionProcessor {

	/* Message ID constants */
	private static final int MSG_ID_INVALID_ENTITY_TYPE = 5001;
	private static final int MSG_ID_URI_GENERATION_FAILED = 5002;

	/* Message string constants */
	private static final String MSG_INVALID_ENTITY_TYPE = "(E%d) Invalid entity type %s referenced in service request";
	private static final String MSG_URI_GENERATION_FAILED = "(E%d) URI generation from product UUID failed (cause: %s)";

	/** The cached OData factory object */
	private OData odata;
	/** The cached metadata of the OData service */
	private ServiceMetadata serviceMetadata;

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(ProductEntityCollectionProcessor.class);

	/**
	 * Create and log a formatted informational message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted info mesage
	 */
	private String logInfo(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.info(message);

		return message;
	}

	/**
	 * Create and log a formatted error message
	 * 
	 * @param messageFormat the message text with parameter placeholders in String.format() style
	 * @param messageId a (unique) message id
	 * @param messageParameters the message parameters (optional, depending on the message format)
	 * @return a formatted error message
	 */
	private String logError(String messageFormat, int messageId, Object... messageParameters) {
		// Prepend message ID to parameter list
		List<Object> messageParamList = new ArrayList<>(Arrays.asList(messageParameters));
		messageParamList.add(0, messageId);

		// Log the error message
		String message = String.format(messageFormat, messageParamList.toArray());
		logger.error(message);

		return message;
	}

	/**
	 * Initializes the processor for each HTTP request - response cycle
	 * (Copied from interface definition)
	 * 
	 * @param odata Olingo's root object, acting as a factory for various object types
	 * @param serviceMetadata metadata of the OData service like the EDM that have to be created before the OData request handling takes place
	 */
	@Override
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	/**
	 * Read the requested products from the prosEO kernel components
	 * (temporarily: hard coded examples)
	 * 
	 * @return a collection of entities representing products
	 * @throws URISyntaxException if a valid URI cannot be generated from any product UUID
	 */
	private EntityCollection queryProducts() throws URISyntaxException {
		EntityCollection productsCollection = new EntityCollection();
		List<Entity> productList = productsCollection.getEntities();

		// add some sample product entities
		final UUID id1 = UUID.fromString("dc3c0bd2-1586-4a41-8230-b7bd66de9a45");
		final Entity e1 = new Entity()
				.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, id1))
				.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebook Basic 15"))
				.addProperty(new Property(null, "ContentType", ValueType.PRIMITIVE,
						"application/octet-stream"));
		e1.setId(new URI(ProductEdmProvider.ET_PRODUCT_NAME + "('" + id1.toString() + "')"));
		productList.add(e1);

		final UUID id2 = UUID.fromString("2b5f5729-cfac-4407-8311-85140dcfc336");
		final Entity e2 = new Entity()
				.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, id2))
				.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "1UMTS PDA"))
				.addProperty(new Property(null, "ContentType", ValueType.PRIMITIVE,
						"application/json"));
		e2.setId(new URI(ProductEdmProvider.ET_PRODUCT_NAME + "('" + id2.toString() + "')"));
		productList.add(e2);

		final UUID id3 = UUID.fromString("70dfbf21-77db-40ad-972c-c9afa0faf626");
		final Entity e3 = new Entity()
				.addProperty(new Property(null, "Id", ValueType.PRIMITIVE, id3))
				.addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Ergo Screen"))
				.addProperty(new Property(null, "ContentType", ValueType.PRIMITIVE,
						"text/plain"));
		e3.setId(new URI(ProductEdmProvider.ET_PRODUCT_NAME + "('" + id3.toString() + "')"));
		productList.add(e3);

		return productsCollection;
	}

	/**
	 * Reads entities data from persistence and puts serialized content and status into the response.
	 * 
	 * @param request OData request object containing raw HTTP information
	 * @param response OData response object for collecting response data
	 * @param uriInfo information of a parsed OData URI
	 * @param responseFormat requested content type after content negotiation
	 * @throws ODataApplicationException if the service implementation encounters a failure
	 * @throws ODataLibraryException
	 */
	@Override
	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
		
		// 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); // in our example, the first segment is the EntitySet
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		// 2nd: fetch the data from backend for this requested EntitySetName
		// it has to be delivered as EntitySet object
		EntityCollection entitySet;
		if (edmEntitySet.getEntityType().getFullQualifiedName().equals(ProductEdmProvider.ET_PRODUCT_FQN)) {
			try {
				entitySet = queryProducts();
			} catch (URISyntaxException e) {
				String message = logError(MSG_URI_GENERATION_FAILED, MSG_ID_URI_GENERATION_FAILED, e.getMessage());
				response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
				response.setHeader("Warning", message);
				return;
			}
		} else {
			String message = logError(MSG_INVALID_ENTITY_TYPE, MSG_ID_INVALID_ENTITY_TYPE, edmEntitySet.getEntityType().getFullQualifiedName());
			response.setStatusCode(HttpStatusCode.BAD_REQUEST.getStatusCode());
			response.setHeader("Warning", message);
			return;
		}

		// 3rd: create a serializer based on the requested format (json)
		ODataSerializer serializer = odata.createSerializer(responseFormat);

		// 4th: Now serialize the content: transform from the EntitySet object to InputStream
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

		final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
		EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().id(id).contextURL(contextUrl).build();
		SerializerResult serializerResult = serializer.entityCollection(serviceMetadata, edmEntityType, entitySet, opts);
		InputStream serializedContent = serializerResult.getContent();

		// Finally: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}

}
