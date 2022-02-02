/**
 * AuxipMonitor.java
 * 
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.auxipmon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.uri.QueryOption;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.api.basemon.BaseMonitor;
import de.dlr.proseo.api.basemon.TransferObject;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

/**
 * Monitor for Auxiliary Data Interface Points (AUXIP)
 * 
 * For specification details see "Auxiliary Data Interface Delivery Point Specification" (ESA-EOPG-EOPGC-IF-10, issue 1.3)
 * 
 * @author Dr. Thomas Bassler
 */
@Component
@Scope("singleton")
public class AuxipMonitor extends BaseMonitor {
	
	/** Header marker for S3 redirects */
	private static final String S3_CREDENTIAL_PARAM = "Amz-Credential";

	/** Maximum number of product entries to retrieve in one request */
	private static final int MAX_PRODUCT_COUNT = 1000;
	
	/** The last copy performance in MiB/s (static, because it may be read and written from different threads) */
	private static Double lastCopyPerformance = 0.0;

	/** Indicator for parallel copying processes */
	/* package */ Map<String, Boolean> copySuccess = new ConcurrentHashMap<>();
	
	/** The AUXIP Monitor configuration to use */
	@Autowired
	private AuxipMonitorConfiguration config;
	
	/** Reference times per product type */
//	private Map<String, Instant> productTypeReferenceTimes = new HashMap<>();

	// Message IDs
	private static final int MSG_ID_TOKEN_REQUEST_FAILED = 5362;
	private static final int MSG_ID_TOKEN_RESPONSE_INVALID = 5363;
	private static final int MSG_ID_ACCESS_TOKEN_MISSING = 5364;
	private static final int MSG_ID_TOKEN_RESPONSE_EMPTY = 5365;
	private static final int MSG_ID_PRODUCT_UUID_MISSING = 5370;
	private static final int MSG_ID_PRODUCT_TRANSFER_COMPLETED = 5371;
	private static final int MSG_ID_TARGET_DIRECTORY_NOT_WRITABLE = 5372;
	private static final int MSG_ID_FILE_NOT_WRITABLE = 5373;
	private static final int MSG_ID_PRODUCT_FILENAME_MISSING = 5374;
	private static final int MSG_ID_PRODUCT_SIZE_MISSING = 5375;
	private static final int MSG_ID_PRODUCT_HASH_MISSING = 5376;
	private static final int MSG_ID_PRODUCT_VAL_START_MISSING = 5377;
	private static final int MSG_ID_PRODUCT_VAL_STOP_MISSING = 5378;
	private static final int MSG_ID_PRODUCT_PUBLICATION_MISSING = 5379;
	private static final int MSG_ID_PRODUCT_EVICTION_MISSING = 5380;
	private static final int MSG_ID_PRODUCT_EVICTED = 5381;
	private static final int MSG_ID_WAIT_INTERRUPTED = 5382;
	private static final int MSG_ID_ODATA_REQUEST_FAILED = 5383;
	private static final int MSG_ID_ODATA_RESPONSE_UNREADABLE = 5384;
	private static final int MSG_ID_ODATA_REQUEST_ABORTED = 5385;
	private static final int MSG_ID_EXCEPTION_THROWN = 5386;
	private static final int MSG_ID_PRODUCT_DOWNLOAD_FAILED = 5387;
	private static final int MSG_ID_RETRIEVAL_RESULT = 5388;
	
	/* Same as XBIP Monitor */
	private static final int MSG_ID_AVAILABLE_DOWNLOADS_FOUND = 5302;
	private static final int MSG_ID_TRANSFER_OBJECT_IS_NULL = 5303;
	private static final int MSG_ID_INVALID_TRANSFER_OBJECT_TYPE = 5304;
	/* package */ static final int MSG_ID_COPY_FILE_FAILED = 5307;
	private static final int MSG_ID_FOLLOW_ON_ACTION_STARTED = 5311;
	
	// Message strings
	private static final String MSG_TRANSFER_OBJECT_IS_NULL = "(E%d) Transfer object is null - skipped";
	private static final String MSG_INVALID_TRANSFER_OBJECT_TYPE = "(E%d) Transfer object %s of invalid type found - skipped";
	/* package */ static final String MSG_COPY_FILE_FAILED = "(E%d) Copying of session data file %s failed (cause: %s)";
	private static final String MSG_TOKEN_REQUEST_FAILED = "(E%d) Token request to AUXIP URI %s failed";
	private static final String MSG_TOKEN_RESPONSE_INVALID = "(E%d) Token response %s from AUXIP URI %s invalid (cause: %s)";
	private static final String MSG_ACCESS_TOKEN_MISSING = "(E%d) Token response %s from AUXIP URI %s does not contain access token";
	private static final String MSG_TOKEN_RESPONSE_EMPTY = "(E%d) Token response %s from AUXIP URI %s is empty";
	private static final String MSG_PRODUCT_UUID_MISSING = "(E%d) Product list entry %s does not contain product UUID ('Id' element)";
	private static final String MSG_TARGET_DIRECTORY_NOT_WRITABLE = "(E%d) Target directory %s not writable";
	private static final String MSG_FILE_NOT_WRITABLE = "(E%d) Cannot write product file %s";
	private static final String MSG_PRODUCT_FILENAME_MISSING = "(E%d) Product list entry %s does not contain product filename ('Name' element)";
	private static final String MSG_PRODUCT_SIZE_MISSING = "(E%d) Product list entry %s does not contain product size ('ContentLength' element)";
	private static final String MSG_PRODUCT_HASH_MISSING = "(E%d) Product list entry %s does not contain product checksum ('Checksum/Value' element)";
	private static final String MSG_PRODUCT_VAL_START_MISSING = "(E%d) Product list entry %s does not contain product validity start ('ContentDate/Start' element)";
	private static final String MSG_PRODUCT_VAL_STOP_MISSING = "(E%d) Product list entry %s does not contain product validity end ('ContentDate/End' element)";
	private static final String MSG_PRODUCT_PUBLICATION_MISSING = "(E%d) Product list entry %s does not contain valid publication time ('PublicationDate' element)";
	private static final String MSG_PRODUCT_EVICTION_MISSING = "(E%d) Product list entry %s does not contain valid eviction time ('EvictionDate' element)";
	private static final String MSG_WAIT_INTERRUPTED = "(E%d) Wait for next chunk of product data interrupted";
	private static final String MSG_ODATA_REQUEST_FAILED = "(E%d) OData request for reference time %s failed with HTTP status code %d, message:\n%s\n";
	private static final String MSG_ODATA_RESPONSE_UNREADABLE = "(E%d) OData response not readable";
	private static final String MSG_ODATA_REQUEST_ABORTED = "(E%d) OData request for reference time %s aborted (cause: %s / %s)";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown in AUXIP monitor: ";
	private static final String MSG_PRODUCT_DOWNLOAD_FAILED = "(E%d) Download of product file %s failed (cause: %s)";

	private static final String MSG_PRODUCT_EVICTED = "(W%d) Product %s already evicted at %s – skipped";

	private static final String MSG_AVAILABLE_DOWNLOADS_FOUND = "(I%d) %d session entries found for download (unfiltered)";
	private static final String MSG_PRODUCT_TRANSFER_COMPLETED = "(I%d) Transfer for session %s completed";
	private static final String MSG_FOLLOW_ON_ACTION_STARTED = "(I%d) Follow-on action for session %s started with command %s";
	private static final String MSG_RETRIEVAL_RESULT = "(I%d) Retrieval request returned %d products out of %d available";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(AuxipMonitor.class);
	
	/**
	 * Class describing a download session
	 */
	public static class TransferProduct implements TransferObject {
		
		/** Product UUID */
		private String uuid;
		
		/** Product filename */
		private String name;
		
		/** Product size */
		private Long size;
		
		/** Product file MD5 hash */
		private String checksum;
		
		/** Product validity start */
		private Instant startTime;
		
		/** Product validity end */
		private Instant stopTime;
		
		/** Product publication time */
		private Instant publicationTime;
		
		/** Product eviction time */
		private Instant evictionTime;
		
		// Getter/setter pairs for attributes
		
		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Long getSize() {
			return size;
		}

		public void setSize(Long size) {
			this.size = size;
		}

		public String getChecksum() {
			return checksum;
		}

		public void setChecksum(String checksum) {
			this.checksum = checksum;
		}

		public Instant getStartTime() {
			return startTime;
		}

		public void setStartTime(Instant startTime) {
			this.startTime = startTime;
		}

		public Instant getStopTime() {
			return stopTime;
		}

		public void setStopTime(Instant stopTime) {
			this.stopTime = stopTime;
		}

		public Instant getPublicationTime() {
			return publicationTime;
		}

		public void setPublicationTime(Instant publicationTime) {
			this.publicationTime = publicationTime;
		}

		public Instant getEvictionTime() {
			return evictionTime;
		}

		public void setEvictionTime(Instant evictionTime) {
			this.evictionTime = evictionTime;
		}

		/**
		 * Gets the product UUID as identifier
		 * 
		 * @return identifier string
		 * 
		 * @see de.dlr.proseo.api.basemon.TransferObject#getIdentifier()
		 */
		@Override
		public String getIdentifier() {
			return uuid;
		}

		/**
		 * Gets the publication time as transfer object reference time
		 * 
		 * @return the reference time
		 * 
		 * @see de.dlr.proseo.api.basemon.TransferObject#getReferenceTime()
		 */
		@Override
		public Instant getReferenceTime() {
			return publicationTime;
		}
		
	}
	
	/**
	 * Initialize global parameters
	 */
	@PostConstruct
	private void init() {
		// Set parameters in base monitor
		this.setTransferHistoryFile(Paths.get(config.getAuxipHistoryPath()));
		this.setCheckInterval(config.getAuxipCheckInterval());
		this.setTruncateInterval(config.getAuxipTruncateInterval());
		this.setHistoryRetentionDuration(Duration.ofMillis(config.getAuxipHistoryRetention()));
		
		// Multi-threading control
		this.setMaxDownloadThreads(config.getMaxDownloadThreads());
		this.setTaskWaitInterval(config.getTaskWaitInterval());
		this.setMaxWaitCycles(config.getMaxWaitCycles());
		
		HttpURLConnection.setFollowRedirects(true);
		
		logger.info("------  Starting AUXIP Monitor  ------");
		logger.info("AUXIP base URI . . . . . . : " + config.getAuxipBaseUri());
		logger.info("AUXIP context. . . . . . . : " + config.getAuxipContext());
		logger.info("Use token-based auth . . . : " + config.getAuxipUseToken());
		logger.info("Product types requested  . : " + config.getAuxipProductTypes());
		logger.info("Transfer history file  . . : " + this.getTransferHistoryFile());
		logger.info("AUXIP check interval   . . : " + this.getCheckInterval());
		logger.info("Chunk retrieval interval . : " + config.getAuxipChunkInterval());
		logger.info("History truncation interval: " + this.getTruncateInterval());
		logger.info("History retention period . : " + this.getHistoryRetentionDuration());
		logger.info("Max. transfer sessions . . : " + this.getMaxDownloadThreads());
		logger.info("Transfer session wait time : " + this.getTaskWaitInterval());
		logger.info("Max. session wait cycles . : " + this.getMaxWaitCycles());
		
	}
	
	/**
	 * Gets the last copy performance for monitoring purposes
	 * 
	 * @return the last copy performance in MiB/s
	 */
	synchronized public Double getLastCopyPerformance() {
		return lastCopyPerformance;
	}

	/**
	 * Records the last copy performance for monitoring purposes
	 * 
	 * @param copyPerformance the copy performance in MiB/s
	 */
	synchronized /* package */ void setLastCopyPerformance(Double copyPerformance) {
		lastCopyPerformance = copyPerformance;
	}
	
	/**
	 * Request a bearer token from the AUXIP
	 * 
	 * @return the bearer token as received from AUXIP, or null, if the request failed
	 */
	private String getBearerToken() {
		if (logger.isTraceEnabled()) logger.trace(">>> getBearerToken()");
		
		// Create a request
		WebClient webClient = WebClient.create(config.getAuxipBaseUri());
		RequestBodySpec request = webClient.post()
				.uri(config.getAuxipTokenUri())
				.accept(MediaType.APPLICATION_JSON);
		
		// Set username and password as query parameters
		MultiValueMap<String, String> queryVariables = new LinkedMultiValueMap<>();
		
		queryVariables.add("grant_type", "password");
		queryVariables.add("username", config.getAuxipUser());
		queryVariables.add("password", config.getAuxipPassword());
		
		// Add client credentials, if OpenID is required for login, otherwise prepare Basic Auth with username/password
		if (null == config.getAuxipClientId()) {
			String base64Auth =  new String(Base64.getEncoder().encode((config.getAuxipUser() + ":" + config.getAuxipPassword()).getBytes()));
			request = request.header(HttpHeaders.AUTHORIZATION, "Basic " + base64Auth);
			logger.trace("... Auth: '{}'", base64Auth);
		} else {
			queryVariables.add("scope", "openid");
			if (config.getAuxipClientSendInBody()) {
				queryVariables.add("client_id", config.getAuxipClientId());
				queryVariables.add("client_secret",
						URLEncoder.encode(config.getAuxipClientSecret(), Charset.defaultCharset()));
			} else {
				String base64Auth =  new String(Base64.getEncoder()
						.encode((config.getAuxipClientId() + ":" + config.getAuxipClientSecret()).getBytes()));
				request = request.header(HttpHeaders.AUTHORIZATION, "Basic " + base64Auth);
				logger.trace("... Auth: '{}'", base64Auth);
			}
		}
		if (logger.isTraceEnabled()) logger.trace("... using query variables '{}'", queryVariables);
		
		// Perform token request
		String tokenResponse = request
			.body(BodyInserters.fromFormData(queryVariables))
			.retrieve()
			.bodyToMono(String.class)
			.block();
		if (null == tokenResponse) {
			logger.error(String.format(MSG_TOKEN_REQUEST_FAILED, MSG_ID_TOKEN_REQUEST_FAILED,
					config.getAuxipBaseUri() + "/" + config.getAuxipTokenUri()));
			return null;
		}
//		if (logger.isTraceEnabled()) logger.trace("... got token response '{}'", tokenResponse);
		
		// Analyse the result
		ObjectMapper om = new ObjectMapper();
		Map<?, ?> tokenResponseMap = null;
		try {
			tokenResponseMap = om.readValue(tokenResponse, Map.class);
		} catch (IOException e) {
			logger.error(String.format(MSG_TOKEN_RESPONSE_INVALID, MSG_ID_TOKEN_RESPONSE_INVALID,
					tokenResponse, config.getAuxipBaseUri() + "/" + config.getAuxipTokenUri(), e.getMessage()));
			return null;
		}
		if (null == tokenResponseMap || tokenResponseMap.isEmpty()) {
			logger.error(String.format(MSG_TOKEN_RESPONSE_EMPTY, MSG_ID_TOKEN_RESPONSE_EMPTY,
					tokenResponse, config.getAuxipBaseUri() + "/" + config.getAuxipTokenUri()));
			return null;
		}
		Object accessToken = tokenResponseMap.get("access_token");
		if (null == accessToken || ! (accessToken instanceof String)) {
			logger.error(String.format(MSG_ACCESS_TOKEN_MISSING, MSG_ID_ACCESS_TOKEN_MISSING,
					tokenResponse, config.getAuxipBaseUri() + "/" + config.getAuxipTokenUri()));
			return null;
		} else {
//			if (logger.isTraceEnabled()) logger.trace("... found access token {}", accessToken);
			return (String) accessToken;
		}
	}

	/**
	 * Check for available products published after the given reference time stamp; only a single request is made
	 * (except for paging) with all product types OR'ed in a single list
	 * 
	 * @param referenceTimeStamp the reference time stamp to check against
	 * @param bearerToken bearer token for authentication, if required (if not set, Basic Auth will be used)
	 * 
	 * @return a list of product UUIDs available for download (may be empty)
	 */
	private TransferControl checkAvailableProducts(Instant referenceTimeStamp, String bearerToken) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkAvailableProducts({}, <bearer token>)", referenceTimeStamp);
		
		TransferControl transferControl = new TransferControl();
		transferControl.referenceTime = referenceTimeStamp;
		
		// Prepare OData request
		ODataClient oDataClient = ODataClientFactory.getClient();
		oDataClient.getConfiguration().setDefaultPubFormat(ContentType.APPLICATION_JSON);
		
		String oDataServiceRoot = config.getAuxipBaseUri() 
				+ "/" 
				+ (config.getAuxipContext().isBlank() ? "" : config.getAuxipContext() + "/") 
				+ "odata/v1";
		String authorizationHeader = config.getAuxipUseToken() ?
					"Bearer " + bearerToken : 
        			"Basic " + Base64.getEncoder().encode((config.getAuxipUser() + ":" + config.getAuxipPassword()).getBytes());
		
		// Create query filter for all product types configured
		StringBuilder queryFilter = new StringBuilder("(false");
		for (String productType: config.getAuxipProductTypes()) {
			queryFilter.append(" or startswith(Name,'").append(productType).append("')");
		}
		queryFilter.append(") and PublicationDate gt ").append(referenceTimeStamp);
		
		// Retrieve products
		if (logger.isTraceEnabled()) logger.trace("... requesting product list at URL '{}'", oDataServiceRoot);
		ODataEntitySetRequest<ClientEntitySet> request = oDataClient.getRetrieveRequestFactory()
		        .getEntitySetRequest(
		        		oDataClient.newURIBuilder(oDataServiceRoot)
		        			.appendEntitySetSegment("Products")
		        			.addQueryOption(QueryOption.FILTER, queryFilter.toString())
		        			.addQueryOption(QueryOption.COUNT, "true")
		        			.top(MAX_PRODUCT_COUNT)
		        			.orderBy("PublicationDate asc")
		        			.build()
		        );
		request.addCustomHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
		if (logger.isTraceEnabled()) logger.trace("... sending OData request '{}'", request.getURI());
		Future<ODataRetrieveResponse<ClientEntitySet>> futureResponse = request.asyncExecute();
		ODataRetrieveResponse<ClientEntitySet> response = null;
		try {
			response = futureResponse.get(30, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			logger.error(String.format(MSG_ODATA_REQUEST_ABORTED, MSG_ID_ODATA_REQUEST_ABORTED, 
					referenceTimeStamp, e1.getClass().getName(), e1.getMessage()));
			return transferControl;
		}
		
		if (HttpStatus.OK.value() != response.getStatusCode()) {
			try {
				logger.error(String.format(MSG_ODATA_REQUEST_FAILED, MSG_ID_ODATA_REQUEST_FAILED,
					referenceTimeStamp, response.getStatusCode(), new String(response.getRawResponse().readAllBytes())));
			} catch (IOException e) {
				logger.error(String.format(MSG_ODATA_RESPONSE_UNREADABLE, MSG_ID_ODATA_RESPONSE_UNREADABLE));
			}
			return transferControl;
		}
		
		ClientEntitySet entitySet = response.getBody();
		logger.info(String.format(MSG_RETRIEVAL_RESULT, MSG_ID_RETRIEVAL_RESULT, entitySet.getEntities().size(), entitySet.getCount()));
		
		// No products found, next search starts from current date and time
		if (entitySet.getEntities().isEmpty()) {
			transferControl.referenceTime = Instant.now();
			return transferControl;
		}

		int cycleCount = 0;
		do {
			// Extract product metadata
			for (ClientEntity clientEntity : entitySet.getEntities()) {
				TransferProduct tp = extractTransferProduct(clientEntity);
				if (null != tp) {
					if (transferControl.referenceTime.isBefore(tp.getPublicationTime())) {
						transferControl.referenceTime = tp.getPublicationTime();
					}
					if (!referenceTimeStamp.isAfter(tp.getPublicationTime())) {
						if (Instant.now().isAfter(tp.getEvictionTime())) {
							logger.warn(String.format(MSG_PRODUCT_EVICTED, MSG_ID_PRODUCT_EVICTED, tp.getName(),
									tp.getEvictionTime().toString()));
						} else {
							transferControl.transferObjects.add(tp);
						}
					}
				}
			}
			
			// Get next chunk of data, if any
			if (MAX_PRODUCT_COUNT > entitySet.getEntities().size()) {
				if (logger.isTraceEnabled())
					logger.trace("... {} product entries received, not expecting any more", entitySet.getEntities().size());
				break;
			}
			
			if (logger.isTraceEnabled())
				logger.trace("... waiting {} s before requesting next chunk of data", config.getAuxipChunkInterval() / 1000);
			try {
				Thread.sleep(config.getAuxipChunkInterval());
			} catch (InterruptedException e) {
				logger.error(String.format(MSG_WAIT_INTERRUPTED, MSG_ID_WAIT_INTERRUPTED));
				return transferControl;
			}
			
			++cycleCount;
			if (logger.isTraceEnabled()) logger.trace("... requesting next part product list at URL '{}', skipping {} entries",
					oDataServiceRoot, cycleCount * MAX_PRODUCT_COUNT);
			request = oDataClient.getRetrieveRequestFactory()
			        .getEntitySetRequest(
			        		oDataClient.newURIBuilder(oDataServiceRoot)
			        			.appendEntitySetSegment("Products")
			        			.addQueryOption(QueryOption.FILTER, queryFilter.toString())
			        			.addQueryOption(QueryOption.COUNT, "true")
			        			.skip(cycleCount * MAX_PRODUCT_COUNT)
			        			.top(MAX_PRODUCT_COUNT)
			        			.orderBy("PublicationDate asc")
			        			.build()
			        );
			request.addCustomHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
			if (logger.isTraceEnabled()) logger.trace("... sending OData request '{}'", request.getURI());
			futureResponse = request.asyncExecute();
			try {
				response = futureResponse.get(30, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e1) {
				logger.error(String.format(MSG_ODATA_REQUEST_ABORTED, MSG_ID_ODATA_REQUEST_ABORTED, 
						referenceTimeStamp, e1.getClass().getName(), e1.getMessage()));
				return transferControl;
			}
			
			if (HttpStatus.OK.value() != response.getStatusCode()) {
				try {
					logger.error(String.format(MSG_ODATA_REQUEST_FAILED, MSG_ID_ODATA_REQUEST_FAILED,
						referenceTimeStamp, response.getStatusCode(), new String(response.getRawResponse().readAllBytes())));
				} catch (IOException e) {
					logger.error(String.format(MSG_ODATA_RESPONSE_UNREADABLE, MSG_ID_ODATA_RESPONSE_UNREADABLE));
				}
				return transferControl;
			}
			
			entitySet = response.getBody();
			logger.info(String.format(MSG_RETRIEVAL_RESULT, MSG_ID_RETRIEVAL_RESULT, entitySet.getEntities().size(), entitySet.getCount()));
			
		} while (!entitySet.getEntities().isEmpty());
		
		if (logger.isTraceEnabled()) logger.trace("<<< checkAvailableProducts()");
		return transferControl;
	}

	/**
	 * @param productObject
	 * @return
	 */
	private TransferProduct extractTransferProduct(ClientEntity product) {
		if (logger.isTraceEnabled()) logger.trace(">>> extractTransferProduct({})", 
				(null == product ? "null" : product.getProperty("Name")));

		TransferProduct tp = new TransferProduct();
		
		try {
			tp.setUuid(product.getProperty("Id").getPrimitiveValue().toCastValue(String.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.error(String.format(MSG_PRODUCT_UUID_MISSING, MSG_ID_PRODUCT_UUID_MISSING, product.toString()));
			return null;
		}
		if (logger.isTraceEnabled()) logger.trace("... uuid = {}", tp.getUuid());
		
		try {
			tp.setName(product.getProperty("Name").getPrimitiveValue().toCastValue(String.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.error(String.format(MSG_PRODUCT_FILENAME_MISSING, MSG_ID_PRODUCT_FILENAME_MISSING, product.toString()));
			return null;
		}
		if (logger.isTraceEnabled()) logger.trace("... name = {}", tp.getName());
		
		try {
			tp.setSize(product.getProperty("ContentLength").getPrimitiveValue().toCastValue(Long.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.error(String.format(MSG_PRODUCT_SIZE_MISSING, MSG_ID_PRODUCT_SIZE_MISSING, product.toString()));
			return null;
		}
		if (logger.isTraceEnabled()) logger.trace("... size = {}", tp.getSize());
		
		tp.setChecksum(null);
		try {
			product.getProperty("Checksum").getCollectionValue().forEach(clientValue -> {
				try {
					if ("MD5".equals(clientValue.asComplex().get("Algorithm").getPrimitiveValue().toCastValue(String.class))) {
						tp.setChecksum(clientValue.asComplex().get("Value").getPrimitiveValue().toCastValue(String.class));
					}
				} catch (EdmPrimitiveTypeException e) {
					logger.error(String.format(MSG_PRODUCT_HASH_MISSING, MSG_ID_PRODUCT_HASH_MISSING, product.toString()));
				}
			});
		} catch (NullPointerException e) {
			logger.error(String.format(MSG_PRODUCT_HASH_MISSING, MSG_ID_PRODUCT_HASH_MISSING, product.toString()));
			return null;
		}
		if (null == tp.getChecksum()) {
			logger.error(String.format(MSG_PRODUCT_HASH_MISSING, MSG_ID_PRODUCT_HASH_MISSING, product.toString()));
			return null;
		}
		if (logger.isTraceEnabled()) logger.trace("... checksum = {}", tp.getChecksum());
		
		try {
//			ClientProperty p = product.getProperty("ContentDate");
//			if (logger.isTraceEnabled()) logger.trace("... p = {}", p);
//			ClientComplexValue cv = p.getComplexValue();
//			if (logger.isTraceEnabled()) logger.trace("... cv = {}", cv);
//			ClientProperty p2 = cv.get("Start");
//			if (logger.isTraceEnabled()) logger.trace("... p2 = {}", p2);
//			ClientPrimitiveValue pv = p2.getPrimitiveValue();
//			if (logger.isTraceEnabled()) logger.trace("... pv = {}", pv);
//			String d = pv.toCastValue(String.class);
//			if (logger.isTraceEnabled()) logger.trace("... d = {}", d);
//			Instant i = Instant.parse(d);
//			if (logger.isTraceEnabled()) logger.trace("... i = {}", i);
			tp.setStartTime(Instant.parse(product.getProperty("ContentDate").getComplexValue()
					.get("Start").getPrimitiveValue().toCastValue(String.class)));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			logger.error(String.format(MSG_PRODUCT_VAL_START_MISSING, MSG_ID_PRODUCT_VAL_START_MISSING, product.toString()));
			return null;
		}
//		if (logger.isTraceEnabled()) logger.trace("... start = {}", tp.getStartTime());
		
		try {
			tp.setStopTime(Instant.parse(product.getProperty("ContentDate").getComplexValue()
					.get("End").getPrimitiveValue().toCastValue(String.class)));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			logger.error(String.format(MSG_PRODUCT_VAL_STOP_MISSING, MSG_ID_PRODUCT_VAL_STOP_MISSING, product.toString()));
			return null;
		}
//		if (logger.isTraceEnabled()) logger.trace("... stop = {}", tp.getStopTime());
		
		try {
			tp.setPublicationTime(Instant.parse(
					product.getProperty("PublicationDate").getPrimitiveValue().toCastValue(String.class)));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			logger.error(String.format(MSG_PRODUCT_PUBLICATION_MISSING, MSG_ID_PRODUCT_PUBLICATION_MISSING, product.toString()));
			return null;
		}
		if (logger.isTraceEnabled()) logger.trace("... publication = {}", tp.getPublicationTime());
		
		try {
			tp.setEvictionTime(Instant.parse(
					product.getProperty("EvictionDate").getPrimitiveValue().toCastValue(String.class)));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			logger.error(String.format(MSG_PRODUCT_EVICTION_MISSING, MSG_ID_PRODUCT_EVICTION_MISSING, product.toString()));
			return null;
		}
//		if (logger.isTraceEnabled()) logger.trace("... eviction = {}", tp.getEvictionTime());
		
		return tp;
	}

	/**
	 * Check the configured AUXIP for new files of the configured product types, whose publication date is after the
	 * reference time stamp:
	 * <ol>
	 *   <li>If token-based authentication is required, login to AUXIP and request token</li>
	 *   <li>For all configured product types:
	 *     <ol>
	 *       <li>Retrieve all products of the given type with publication time after reference time stamp (authenticating with
	 *           either Basic Auth or Bearer Token)</li>
	 *       <li>Convert JSON product entries into transfer objects</li>
	 *     </ol>
	 *   </li>
	 *   <li>Return the combined list of transfer objects</li>
	 * </ol>
	 * 
	 * @param referenceTimeStamp the reference timestamp to apply for pickup point lookups
	 * @return a list of available transfer objects
	 */
	@Override
	protected TransferControl checkAvailableDownloads(Instant referenceTimeStamp) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkAvailableDownloads({})", referenceTimeStamp);

		TransferControl transferControl = new TransferControl();
//		transferControl.referenceTime = referenceTimeStamp;
		
		try {
			// If token-based authentication is required, login to AUXIP and request token
			String bearerToken = null;
			if (config.getAuxipUseToken()) {
				if (logger.isTraceEnabled()) logger.trace("... requesting token from AUXIP");
				bearerToken = getBearerToken();
				if (null == bearerToken) {
					// Already logged, return an empty list
					return transferControl;
				}
			}
			
			// Loop over all configured product types
			
//			for (String productType: config.getAuxipProductTypes()) {
//				if (null == productTypeReferenceTimes.get(productType)) {
//					productTypeReferenceTimes.put(productType, referenceTimeStamp);
//				}
//				Instant productTypeReferenceTime = productTypeReferenceTimes.get(productType);
//				
//				if (logger.isTraceEnabled()) logger.trace("... checking for products of type {}", productType);
//				TransferControl productTypeTransferControl = checkAvailableProducts(productType, productTypeReferenceTime, bearerToken);
//				if (logger.isTraceEnabled()) logger.trace("... found {} products", productTypeTransferControl.transferObjects.size());
//
//				if (productTypeReferenceTime.isBefore(productTypeTransferControl.referenceTime)) {
//					productTypeReferenceTimes.put(productType, productTypeTransferControl.referenceTime);
//				}
//				if (transferControl.referenceTime.isBefore(productTypeTransferControl.referenceTime)) {
//					transferControl.referenceTime = productTypeTransferControl.referenceTime;
//				}
//				transferControl.transferObjects.addAll(productTypeTransferControl.transferObjects);
//			}
			
			// Only a single request is made (not considering paging) with all
			// product types OR'ed in a single list
			transferControl = checkAvailableProducts(referenceTimeStamp, bearerToken);
			
			logger.info(String.format(MSG_AVAILABLE_DOWNLOADS_FOUND, MSG_ID_AVAILABLE_DOWNLOADS_FOUND, transferControl.transferObjects.size()));
		} catch (Exception e) {
			logger.error(String.format(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN), e);;
		}
		
		return transferControl;
	}

	/**
	 * Transfer the products found to disk for later ingestion
	 */
	@Override
	protected boolean transferToTargetDir(TransferObject object) {
		if (logger.isTraceEnabled()) logger.trace(">>> transferToTargetDir({})", null == object ? "null" : object.getIdentifier());
		
		if (null == object) {
			logger.error(String.format(MSG_TRANSFER_OBJECT_IS_NULL, MSG_ID_TRANSFER_OBJECT_IS_NULL));
			return false;
		}
		
		if (object instanceof TransferProduct) {
			
			try {
				TransferProduct transferProduct = (TransferProduct) object;
				
				// Check target directory
				if (!Files.isWritable(Path.of(config.getAuxipDirectoryPath()))) {
					logger.error(String.format(MSG_TARGET_DIRECTORY_NOT_WRITABLE, MSG_ID_TARGET_DIRECTORY_NOT_WRITABLE,
							config.getAuxipDirectoryPath()));
					return false;
				}
				
				// Create retrieval request
				String requestUri = (config.getAuxipContext().isBlank() ? "" : config.getAuxipContext() + "/") + "odata/v1/"
						+ "Products("	+ transferProduct.getUuid() + ")"
						+ "/$value";
				
				HttpClient httpClient = HttpClient.create()
						.headers(httpHeaders -> {
							if (config.getAuxipUseToken()) {
								httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + getBearerToken());
							} else {
								httpHeaders.add(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(
										(config.getAuxipUser() + ":" + config.getAuxipPassword()).getBytes()));
							}
						})
						.followRedirect((request, response) -> {
							if (logger.isTraceEnabled()) logger.trace("... checking redirect for response status code {}", response.status());
							switch (response.status().code()) {
							case 301:
							case 302:
							case 307:
							case 308:
								String redirectLocation = response.responseHeaders().get(HttpHeaders.LOCATION);
								if (null == redirectLocation) {
									return false;
								}
								// Prevent sending authorization header, if target is S3 and credentials are already given in URL
								if (redirectLocation.contains(S3_CREDENTIAL_PARAM)) {
									if (logger.isTraceEnabled()) logger.trace(
											"... redirect credentials given in location header, removing authorization header from request");
									request.requestHeaders().remove(HttpHeaders.AUTHORIZATION);
								}
								return true;
							default:
								return false;
							}
						}, null)
						.secure()
						.doAfterResponse((response, connection) -> {
							if (logger.isTraceEnabled()) {
								logger.trace("... response code: {}", response.status());
								logger.trace("... response redirections: {}", Arrays.asList(response.redirectedFrom()));
								logger.trace("... response headers: {}", response.responseHeaders());
							}
						})
//						.wiretap(logger.isDebugEnabled())
						;

				WebClient webClient = WebClient.builder()
						.baseUrl(config.getAuxipBaseUri())
						.clientConnector(new ReactorClientHttpConnector(httpClient))
						.build();
				
				// Retrieve and store product file
				Instant copyStart = Instant.now();
				String productFileName = config.getAuxipDirectoryPath() + File.separator + transferProduct.getName();

				logger.trace("... starting request for URL '{}'", requestUri);
				
				try (FileOutputStream fileOutputStream = new FileOutputStream(productFileName);) {
					
					Flux<DataBuffer> dataBuffer = webClient
							.get()
							.uri(requestUri)
				            .accept(MediaType.APPLICATION_OCTET_STREAM)
							.retrieve()
							.bodyToFlux(DataBuffer.class);

					DataBufferUtils.write(dataBuffer, fileOutputStream).blockLast(Duration.ofSeconds(600));
				} catch (FileNotFoundException e) {
					logger.error(String.format(MSG_FILE_NOT_WRITABLE, MSG_ID_FILE_NOT_WRITABLE, productFileName));
					return false;
				} catch (WebClientResponseException e) {
					logger.error(String.format(MSG_PRODUCT_DOWNLOAD_FAILED, MSG_ID_PRODUCT_DOWNLOAD_FAILED, 
							transferProduct.getName(), e.getMessage() + " / " + e.getResponseBodyAsString()));
					return false;
				} catch (Exception e) {
					logger.error(String.format(MSG_PRODUCT_DOWNLOAD_FAILED, MSG_ID_PRODUCT_DOWNLOAD_FAILED, 
							transferProduct.getName(), e.getMessage()));
					return false;
				}

				Duration copyDuration = Duration.between(copyStart, Instant.now());
				Double copyPerformance = new File(productFileName).length() / // Bytes
						(copyDuration.toNanos() / 1000000000.0) // seconds (with fraction)
						/ (1024 * 1024); // --> MiB/s
				
				// Record the performance for files of sufficient size
				if (config.getAuxipPerformanceMinSize() < new File(productFileName).length()) {
					setLastCopyPerformance(copyPerformance);
				}
				
				// TODO Compute checksum and compare with value given by AUXIP
				// TODO Log download with UUID, file name, size, checksum, publication date (request by ESA)
				
				logger.info(String.format(MSG_PRODUCT_TRANSFER_COMPLETED, MSG_ID_PRODUCT_TRANSFER_COMPLETED, transferProduct.getIdentifier()));
				
				return true;
			} catch (Exception e) {
				logger.error(String.format(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN), e);
				return false;
			}

		} else {
			logger.error(String.format(MSG_INVALID_TRANSFER_OBJECT_TYPE, MSG_ID_INVALID_TRANSFER_OBJECT_TYPE, object.getIdentifier()));
			return false;
		}
	}

	/* (non-Javadoc)
	 * Trigger follow-on action (dummy implementation, to be overridden by subclass)
	 */
	@Override
	protected boolean triggerFollowOnAction(TransferObject transferObject) {
		if (logger.isTraceEnabled()) logger.trace(">>> triggerFollowOnAction({})",
				null == transferObject ? "null" : transferObject.getIdentifier());

		if (null == transferObject) {
			logger.error(String.format(MSG_TRANSFER_OBJECT_IS_NULL, MSG_ID_TRANSFER_OBJECT_IS_NULL));
			return false;
		}
		
		if (! (transferObject instanceof TransferProduct)) {
			logger.error(String.format(MSG_INVALID_TRANSFER_OBJECT_TYPE, MSG_ID_INVALID_TRANSFER_OBJECT_TYPE, transferObject.getIdentifier()));
			return false;
		}

		TransferProduct transferProduct = (TransferProduct) transferObject;
		
		logger.warn(String.format(MSG_FOLLOW_ON_ACTION_STARTED, MSG_ID_FOLLOW_ON_ACTION_STARTED,
				transferProduct.getIdentifier(), "NOT IMPLEMENTED"));

		return true;
	}

}