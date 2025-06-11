/**
 * AuxipMonitor.java
 *
 * (C) 2021 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.auxipmon;

import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
//import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.dlr.proseo.api.basemon.BaseMonitor;
import de.dlr.proseo.api.basemon.TransferObject;
import de.dlr.proseo.basewrap.MD5Util;
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ApiMonitorMessage;
import de.dlr.proseo.logging.messages.OAuthMessage;
import de.dlr.proseo.model.util.OrbitTimeFormatter;
import de.dlr.proseo.model.util.ProseoUtil;

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

	/** Maximum number of product entries to retrieve in one request */
	private static final int MAX_PRODUCT_COUNT = 1000;

	/** The last copy performance in MiB/s (static, because it may be read and written from different threads) */
	private static Double lastCopyPerformance = 0.0;

	/** Indicator for parallel copying processes */
	/* package */ Map<String, Boolean> copySuccess = new ConcurrentHashMap<>();

	/** The AUXIP Monitor configuration to use */
	@Autowired
	private AuxipMonitorConfiguration config;

	private static final int MSG_ID_PRODUCT_TRANSFER_COMPLETED = 5371;
	private static final int MSG_ID_TARGET_DIRECTORY_NOT_WRITABLE = 5372;
	private static final int MSG_ID_FILE_NOT_WRITABLE = 5373;
	private static final int MSG_ID_PRODUCT_EVICTED = 5381;
	private static final int MSG_ID_WAIT_INTERRUPTED = 5382;
	private static final int MSG_ID_ODATA_REQUEST_FAILED = 5383;
	private static final int MSG_ID_ODATA_RESPONSE_UNREADABLE = 5384;
	private static final int MSG_ID_ODATA_REQUEST_ABORTED = 5385;
	private static final int MSG_ID_EXCEPTION_THROWN = 5386;
	private static final int MSG_ID_PRODUCT_DOWNLOAD_FAILED = 5387;
	private static final int MSG_ID_RETRIEVAL_RESULT = 5388;
	private static final int MSG_ID_FILE_SIZE_MISMATCH = 5389;
	private static final int MSG_ID_CHECKSUM_MISMATCH = 5390;
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
	private static final String MSG_TARGET_DIRECTORY_NOT_WRITABLE = "(E%d) Target directory %s not writable";
	private static final String MSG_FILE_NOT_WRITABLE = "(E%d) Cannot write product file %s";
	private static final String MSG_WAIT_INTERRUPTED = "(E%d) Wait for next chunk of product data interrupted";
	private static final String MSG_ODATA_REQUEST_FAILED = "(E%d) OData request for reference time %s failed with HTTP status code %d, message:\n%s\n";
	private static final String MSG_ODATA_RESPONSE_UNREADABLE = "(E%d) OData response not readable";
	private static final String MSG_ODATA_REQUEST_ABORTED = "(E%d) OData request for reference time %s aborted (cause: %s / %s)";
	private static final String MSG_EXCEPTION_THROWN = "(E%d) Exception thrown in AUXIP monitor: ";
	private static final String MSG_PRODUCT_DOWNLOAD_FAILED = "(E%d) Download of product file %s failed (cause: %s)";
	private static final String MSG_PRODUCT_DOWNLOAD_FAILED_AFTER_RETRIES = "(E%d) Download of product file %s failed after %s retries (cause: %s)";
	private static final String MSG_FILE_SIZE_MISMATCH = "(E%d) File size mismatch for product file %s (expected: %d Bytes, got %d Bytes)";
	private static final String MSG_CHECKSUM_MISMATCH = "(E%d) Checksum mismatch for product file %s (expected: %s, got %s)";
	private static final String MSG_PRODUCT_EVICTED = "(W%d) Product %s already evicted at %s – skipped";

	private static final String MSG_AVAILABLE_DOWNLOADS_FOUND = "(I%d) %d session entries found for download (unfiltered)";
	private static final String MSG_PRODUCT_TRANSFER_COMPLETED = "(I%d) Transfer completed: |%s|%s|%d|%s|%s|";
	private static final String MSG_FOLLOW_ON_ACTION_STARTED = "(I%d) Follow-on action for session %s started with command %s";
	private static final String MSG_RETRIEVAL_RESULT = "(I%d) Retrieval request returned %d products out of %d available";

	/** A oldLogger for this class */
	private static Logger oldLogger = LoggerFactory.getLogger(AuxipMonitor.class);
	private static ProseoLogger logger = new ProseoLogger(AuxipMonitor.class);

	// maximum number of retries to transfer a file
	private static final int MAX_RETRY = 5;
	// Wait interval in ms before retrying database operation
	public static final int AUXIP_WAIT = 1000;
	
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

		oldLogger.info("------  Starting AUXIP Monitor  ------");
		oldLogger.info("AUXIP base URI . . . . . . : " + config.getAuxipBaseUri());
		oldLogger.info("AUXIP context. . . . . . . : " + config.getAuxipContext());
		oldLogger.info("Use token-based auth . . . : " + config.getAuxipUseToken());
		oldLogger.info("Product types requested  . : " + config.getAuxipProductTypes());
		oldLogger.info("Transfer history file  . . : " + this.getTransferHistoryFile());
		oldLogger.info("AUXIP check interval   . . : " + this.getCheckInterval());
		oldLogger.info("Chunk retrieval interval . : " + config.getAuxipChunkInterval());
		oldLogger.info("History truncation interval: " + this.getTruncateInterval());
		oldLogger.info("History retention period . : " + this.getHistoryRetentionDuration());
		oldLogger.info("Max. transfer sessions . . : " + this.getMaxDownloadThreads());
		oldLogger.info("Transfer session wait time : " + this.getTaskWaitInterval());
		oldLogger.info("Max. session wait cycles . : " + this.getMaxWaitCycles());

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
		if (logger.isTraceEnabled())
			logger.trace(">>> getBearerToken()");

		// Create a request
		WebClient webClient = WebClient.create(config.getAuxipBaseUri());
		RequestBodySpec request = webClient.post().uri(config.getAuxipTokenUri()).accept(MediaType.APPLICATION_JSON);

		// Set username and password as query parameters
		MultiValueMap<String, String> queryVariables = new LinkedMultiValueMap<>();

		queryVariables.add("grant_type", "password");
		queryVariables.add("username", config.getAuxipUser());
		queryVariables.add("password", config.getAuxipPassword());

		// Add client credentials, if OpenID is required for login, otherwise prepare Basic Auth with username/password
		if (null == config.getAuxipClientId()) {
			String base64Auth = new String(
					Base64.getEncoder().encode((config.getAuxipUser() + ":" + config.getAuxipPassword()).getBytes()));
			request = request.header(HttpHeaders.AUTHORIZATION, "Basic " + base64Auth);
			logger.trace("... Auth: '{}'", base64Auth);
		} else {
			queryVariables.add("scope", "openid");
			if (config.getAuxipClientSendInBody()) {
				queryVariables.add("client_id", config.getAuxipClientId());
				queryVariables.add("client_secret", URLEncoder.encode(config.getAuxipClientSecret(), Charset.defaultCharset()));
			} else {
				String base64Auth = new String(
						Base64.getEncoder().encode((config.getAuxipClientId() + ":" + config.getAuxipClientSecret()).getBytes()));
				request = request.header(HttpHeaders.AUTHORIZATION, "Basic " + base64Auth);
				logger.trace("... Auth: '{}'", base64Auth);
			}
		}
		if (logger.isTraceEnabled())
			logger.trace("... using query variables '{}'", queryVariables);

		// Perform token request
		String tokenResponse = request.body(BodyInserters.fromFormData(queryVariables)).retrieve().bodyToMono(String.class).block();
		if (null == tokenResponse) {
			logger.log(OAuthMessage.TOKEN_REQUEST_FAILED, config.getAuxipBaseUri() + "/" + config.getAuxipTokenUri());
			return null;
		}
//		if (logger.isTraceEnabled()) logger.trace("... got token response '{}'", tokenResponse);

		// Analyse the result
		ObjectMapper om = new ObjectMapper();
		Map<?, ?> tokenResponseMap = null;
		try {
			tokenResponseMap = om.readValue(tokenResponse, Map.class);
		} catch (IOException e) {
			logger.log(OAuthMessage.TOKEN_RESPONSE_INVALID, tokenResponse,
					config.getAuxipBaseUri() + "/" + config.getAuxipTokenUri(), e.getMessage());
			return null;
		}
		if (null == tokenResponseMap || tokenResponseMap.isEmpty()) {
			logger.log(OAuthMessage.TOKEN_RESPONSE_EMPTY, tokenResponse,
					config.getAuxipBaseUri() + "/" + config.getAuxipTokenUri());
			return null;
		}
		Object accessToken = tokenResponseMap.get("access_token");
		if (null == accessToken || !(accessToken instanceof String)) {
			logger.log(OAuthMessage.ACCESS_TOKEN_MISSING, tokenResponse,
					config.getAuxipBaseUri() + "/" + config.getAuxipTokenUri());
			return null;
		} else {
//			if (logger.isTraceEnabled()) logger.trace("... found access token {}", accessToken);
			return (String) accessToken;
		}
	}

	/**
	 * Check for available products published after the given reference time stamp; only a single request is made (except for
	 * paging) with all product types OR'ed in a single list
	 *
	 * @param referenceTimeStamp the reference time stamp to check against
	 * @param bearerToken        bearer token for authentication, if required (if not set, Basic Auth will be used)
	 *
	 * @return a list of product UUIDs available for download (may be empty)
	 */
	private TransferControl checkAvailableProducts(Instant referenceTimeStamp, String bearerToken) {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkAvailableProducts({}, <bearer token>)", referenceTimeStamp);

		TransferControl transferControl = new TransferControl();
		transferControl.referenceTime = referenceTimeStamp;

		// Prepare OData request
		ODataClient oDataClient = ODataClientFactory.getClient();
		oDataClient.getConfiguration().setDefaultPubFormat(ContentType.APPLICATION_JSON);

		String oDataServiceRoot = config.getAuxipBaseUri() + "/"
				+ (config.getAuxipContext().isBlank() ? "" : config.getAuxipContext() + "/") + "odata/v1";
		String authorizationHeader = config.getAuxipUseToken() ? "Bearer " + bearerToken
				: "Basic " + Base64.getEncoder().encodeToString((config.getAuxipUser() + ":" + config.getAuxipPassword()).getBytes());

		// Create query filter for all product types configured
		// Note: 'false' literal not implemented in some AUXIPs, therefore approach "false or ..." does not work
		StringBuilder queryFilter = new StringBuilder("(");
		boolean firstFilter = true;
		for (String productType : config.getAuxipProductTypes()) {
			if (firstFilter) {
				firstFilter = false;
			} else {
				queryFilter.append(" or ");
			}
			queryFilter.append("startswith(Name,'").append(productType).append("')");
		}
		queryFilter.append(") and PublicationDate gt ").append(referenceTimeStamp);

		// Retrieve products
		if (logger.isTraceEnabled())
			logger.trace("... requesting product list at URL '{}'", oDataServiceRoot);
		ODataEntitySetRequest<ClientEntitySet> request = oDataClient.getRetrieveRequestFactory()
			.getEntitySetRequest(oDataClient.newURIBuilder(oDataServiceRoot)
				.appendEntitySetSegment("Products")
				.addQueryOption(QueryOption.FILTER, queryFilter.toString())
				.addQueryOption(QueryOption.COUNT, "true")
				.top(MAX_PRODUCT_COUNT)
				.orderBy("PublicationDate asc")
				.build());
		request.addCustomHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
		if (logger.isTraceEnabled())
			logger.trace("... sending OData request '{}'", request.getURI());
		Future<ODataRetrieveResponse<ClientEntitySet>> futureResponse = request.asyncExecute();
		ODataRetrieveResponse<ClientEntitySet> response = null;
		try {
			response = futureResponse.get(30, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			logger.log(ApiMonitorMessage.ODATA_REQUEST_ABORTED, referenceTimeStamp, e1.getClass().getName(), e1.getMessage());
			return transferControl;
		}

		if (HttpStatus.OK.value() != response.getStatusCode()) {
			try {
				logger.log(ApiMonitorMessage.ODATA_REQUEST_FAILED, referenceTimeStamp, response.getStatusCode(),
						new String(response.getRawResponse().readAllBytes()));
			} catch (IOException e) {
				logger.log(ApiMonitorMessage.ODATA_RESPONSE_UNREADABLE);
			}
			return transferControl;
		}

		ClientEntitySet entitySet = response.getBody();
		logger.log(ApiMonitorMessage.RETRIEVAL_RESULT, entitySet.getEntities().size(), entitySet.getCount());

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
							oldLogger.warn(String.format(MSG_PRODUCT_EVICTED, MSG_ID_PRODUCT_EVICTED, tp.getName(),
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
				oldLogger.error(String.format(MSG_WAIT_INTERRUPTED, MSG_ID_WAIT_INTERRUPTED));
				return transferControl;
			}

			++cycleCount;
			if (logger.isTraceEnabled())
				logger.trace("... requesting next part product list at URL '{}', skipping {} entries", oDataServiceRoot,
						cycleCount * MAX_PRODUCT_COUNT);
			request = oDataClient.getRetrieveRequestFactory()
				.getEntitySetRequest(oDataClient.newURIBuilder(oDataServiceRoot)
					.appendEntitySetSegment("Products")
					.addQueryOption(QueryOption.FILTER, queryFilter.toString())
					.addQueryOption(QueryOption.COUNT, "true")
					.skip(cycleCount * MAX_PRODUCT_COUNT)
					.top(MAX_PRODUCT_COUNT)
					.orderBy("PublicationDate asc")
					.build());
			request.addCustomHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
			if (logger.isTraceEnabled())
				logger.trace("... sending OData request '{}'", request.getURI());
			futureResponse = request.asyncExecute();
			try {
				response = futureResponse.get(30, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e1) {
				oldLogger.error(String.format(MSG_ODATA_REQUEST_ABORTED, MSG_ID_ODATA_REQUEST_ABORTED, referenceTimeStamp,
						e1.getClass().getName(), e1.getMessage()));
				return transferControl;
			}

			if (HttpStatus.OK.value() != response.getStatusCode()) {
				try {
					oldLogger.error(String.format(MSG_ODATA_REQUEST_FAILED, MSG_ID_ODATA_REQUEST_FAILED, referenceTimeStamp,
							response.getStatusCode(), new String(response.getRawResponse().readAllBytes())));
				} catch (IOException e) {
					oldLogger.error(String.format(MSG_ODATA_RESPONSE_UNREADABLE, MSG_ID_ODATA_RESPONSE_UNREADABLE));
				}
				return transferControl;
			}

			entitySet = response.getBody();
			oldLogger.info(String.format(MSG_RETRIEVAL_RESULT, MSG_ID_RETRIEVAL_RESULT, entitySet.getEntities().size(),
					entitySet.getCount()));

		} while (!entitySet.getEntities().isEmpty());

		if (logger.isTraceEnabled())
			logger.trace("<<< checkAvailableProducts()");
		return transferControl;
	}

	/**
	 * @param productObject
	 * @return
	 */
	private TransferProduct extractTransferProduct(ClientEntity product) {
		if (logger.isTraceEnabled())
			logger.trace(">>> extractTransferProduct({})", (null == product ? "null" : product.getProperty("Name")));

		TransferProduct tp = new TransferProduct();

		try {
			tp.setUuid(product.getProperty("Id").getPrimitiveValue().toCastValue(String.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.PRODUCT_UUID_MISSING, product.toString());
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... uuid = {}", tp.getUuid());

		try {
			tp.setName(product.getProperty("Name").getPrimitiveValue().toCastValue(String.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.PRODUCT_FILENAME_MISSING, product.toString());
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... name = {}", tp.getName());

		try {
			tp.setSize(product.getProperty("ContentLength").getPrimitiveValue().toCastValue(Long.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.PRODUCT_SIZE_MISSING, product.toString());
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... size = {}", tp.getSize());

		tp.setChecksum(null);
		try {
			product.getProperty("Checksum").getCollectionValue().forEach(clientValue -> {
				try {
					if ("MD5".equals(clientValue.asComplex().get("Algorithm").getPrimitiveValue().toCastValue(String.class))) {
						tp.setChecksum(clientValue.asComplex().get("Value").getPrimitiveValue().toCastValue(String.class));
					}
				} catch (EdmPrimitiveTypeException e) {
					logger.log(ApiMonitorMessage.PRODUCT_HASH_MISSING, product.toString());
				}
			});
		} catch (NullPointerException e) {
			logger.log(ApiMonitorMessage.PRODUCT_HASH_MISSING, product.toString());
			return null;
		}
		if (null == tp.getChecksum()) {
			logger.log(ApiMonitorMessage.PRODUCT_HASH_MISSING, product.toString());
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... checksum = {}", tp.getChecksum());

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
			tp.setStartTime(Instant.from(OrbitTimeFormatter.parse(product.getProperty("ContentDate")
				.getComplexValue()
				.get("Start")
				.getPrimitiveValue()
				.toCastValue(String.class))));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			logger.log(ApiMonitorMessage.PRODUCT_VAL_START_MISSING, product.toString());
			return null;
		}
//		if (logger.isTraceEnabled()) logger.trace("... start = {}", tp.getStartTime());

		try {
			tp.setStopTime(Instant.from(OrbitTimeFormatter.parse(
					product.getProperty("ContentDate").getComplexValue().get("End").getPrimitiveValue().toCastValue(String.class))));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			logger.log(ApiMonitorMessage.PRODUCT_VAL_STOP_MISSING, product.toString());
			return null;
		}
//		if (logger.isTraceEnabled()) logger.trace("... stop = {}", tp.getStopTime());

		try {
			tp.setPublicationTime(
					Instant.from(OrbitTimeFormatter.parse(
							product.getProperty("PublicationDate")
							.getPrimitiveValue()
							.toCastValue(String.class))));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			logger.log(ApiMonitorMessage.PRODUCT_PUBLICATION_MISSING, product.toString());
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... publication = {}", tp.getPublicationTime());

		try {
			tp.setEvictionTime(Instant.from(OrbitTimeFormatter.parse(
					product.getProperty("EvictionDate")
					.getPrimitiveValue()
					.toCastValue(String.class))));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			logger.log(ApiMonitorMessage.PRODUCT_EVICTION_MISSING, product.toString());
			return null;
		}
//		if (logger.isTraceEnabled()) logger.trace("... eviction = {}", tp.getEvictionTime());

		return tp;
	}

	/**
	 * Check the configured AUXIP for new files of the configured product types, whose publication date is after the reference time
	 * stamp:
	 * <ol>
	 * <li>If token-based authentication is required, login to AUXIP and request token</li>
	 * <li>For all configured product types:
	 * <ol>
	 * <li>Retrieve all products of the given type with publication time after reference time stamp (authenticating with either
	 * Basic Auth or Bearer Token)</li>
	 * <li>Convert JSON product entries into transfer objects</li>
	 * </ol>
	 * </li>
	 * <li>Return the combined list of transfer objects</li>
	 * </ol>
	 *
	 * @param referenceTimeStamp the reference timestamp to apply for pickup point lookups
	 * @return a list of available transfer objects
	 */
	@Override
	protected TransferControl checkAvailableDownloads(Instant referenceTimeStamp) {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkAvailableDownloads({})", referenceTimeStamp);

		TransferControl transferControl = new TransferControl();

		try {
			// If token-based authentication is required, login to AUXIP and request token
			String bearerToken = null;
			if (config.getAuxipUseToken()) {
				if (logger.isTraceEnabled())
					logger.trace("... requesting token from AUXIP");
				bearerToken = getBearerToken();
				if (null == bearerToken) {
					// Already logged, return an empty list
					return transferControl;
				}
			}

			// Only a single request is made (not considering paging) with all
			// product types OR'ed in a single list
			transferControl = checkAvailableProducts(referenceTimeStamp, bearerToken);

			oldLogger.info(String.format(MSG_AVAILABLE_DOWNLOADS_FOUND, MSG_ID_AVAILABLE_DOWNLOADS_FOUND,
					transferControl.transferObjects.size()));
		} catch (Exception e) {
			oldLogger.error(String.format(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN), e);

		}

		return transferControl;
	}

	/**
	 * Transfer the products found to disk for later ingestion
	 */
	@Override
	protected boolean transferToTargetDir(TransferObject object) {
		if (logger.isTraceEnabled())
			logger.trace(">>> transferToTargetDir({})", null == object ? "null" : object.getIdentifier());

		if (null == object) {
			oldLogger.error(String.format(MSG_TRANSFER_OBJECT_IS_NULL, MSG_ID_TRANSFER_OBJECT_IS_NULL));
			return false;
		}

		if (object instanceof TransferProduct) {

			try {
				TransferProduct transferProduct = (TransferProduct) object;

				// Check target directory
				if (!Files.isWritable(Path.of(config.getAuxipDirectoryPath()))) {
					oldLogger.error(String.format(MSG_TARGET_DIRECTORY_NOT_WRITABLE, MSG_ID_TARGET_DIRECTORY_NOT_WRITABLE,
							config.getAuxipDirectoryPath()));
					return false;
				}

				// Create retrieval request
				String requestUri = config.getAuxipBaseUri() + "/" 
						+ (config.getAuxipContext().isBlank() ? "" : config.getAuxipContext() + "/") 
						+ "odata/v1/Products(" + transferProduct.getUuid() + ")" + "/$value";

				File productFile = new File(config.getAuxipDirectoryPath() + File.separator + transferProduct.getName());
				
				Instant copyStart = Instant.now();
				for (int i = 0; i < MAX_RETRY; i++) {
					try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
						logger.trace("... starting request for URL '{}'", requestUri);

						HttpGet httpGet = new HttpGet(requestUri);

						if (config.getAuxipUseToken()) {
							httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getBearerToken());
						} else {
							httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic "
									+ Base64.getEncoder().encodeToString((config.getAuxipUser() + ":" + config.getAuxipPassword()).getBytes()));
						}

						CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
						HttpEntity httpEntity = httpResponse.getEntity();

						if (httpEntity != null) {
							FileUtils.copyInputStreamToFile(httpEntity.getContent(), productFile);
						}

						httpResponse.close();
						break;
					} catch (FileNotFoundException e) {
						oldLogger.error(String.format(MSG_FILE_NOT_WRITABLE, MSG_ID_FILE_NOT_WRITABLE, productFile.toString()));
						return false;
					} catch (HttpResponseException e) {
						if ((i + 1) < MAX_RETRY) {
							oldLogger.error(String.format(MSG_PRODUCT_DOWNLOAD_FAILED, MSG_ID_PRODUCT_DOWNLOAD_FAILED,
									transferProduct.getName(), e.getMessage() + " / " + e.getReasonPhrase()));
							// retry
							ProseoUtil.randomWait(i, AUXIP_WAIT);
						} else {
							oldLogger.error(String.format(MSG_PRODUCT_DOWNLOAD_FAILED_AFTER_RETRIES, MAX_RETRY, MSG_ID_PRODUCT_DOWNLOAD_FAILED,
									transferProduct.getName(), e.getMessage() + " / " + e.getReasonPhrase()));
							return false;
						}
					} catch (Exception e) {
						if ((i + 1) < MAX_RETRY) {
							oldLogger.error(String.format(MSG_PRODUCT_DOWNLOAD_FAILED, MSG_ID_PRODUCT_DOWNLOAD_FAILED,
									transferProduct.getName(), e.getMessage()));
							// retry
							ProseoUtil.randomWait(i, AUXIP_WAIT);
						} else {
							oldLogger.error(String.format(MSG_PRODUCT_DOWNLOAD_FAILED_AFTER_RETRIES, MAX_RETRY, MSG_ID_PRODUCT_DOWNLOAD_FAILED,
									transferProduct.getName(), e.getClass().getName() + " / " + e.getMessage()), e);
							return false;
						}
					}
				}
				// Compare file size with value given by AUXIP
				Long productFileLength = productFile.length();
				if (!productFileLength.equals(transferProduct.getSize())) {
					oldLogger.error(String.format(MSG_FILE_SIZE_MISMATCH, MSG_ID_FILE_SIZE_MISMATCH,
							transferProduct.getIdentifier(), transferProduct.getSize(), productFileLength));
					return false;
				}

				// Record the performance for files of sufficient size
				Duration copyDuration = Duration.between(copyStart, Instant.now());
				Double copyPerformance = productFileLength / // Bytes
						(copyDuration.toNanos() / 1000000000.0) // seconds (with fraction)
						/ (1024 * 1024); // --> MiB/s

				if (config.getAuxipPerformanceMinSize() < productFileLength) {
					setLastCopyPerformance(copyPerformance);
				}

				// Compute checksum and compare with value given by AUXIP
				String md5Hash = MD5Util.md5Digest(productFile);
				if (!md5Hash.equalsIgnoreCase(transferProduct.checksum)) {
					oldLogger.error(String.format(MSG_CHECKSUM_MISMATCH, MSG_ID_CHECKSUM_MISMATCH, transferProduct.getIdentifier(),
							transferProduct.getChecksum(), md5Hash));
					return false;
				}

				// Log download with UUID, file name, size, checksum, publication date (request by ESA)
				oldLogger.info(String.format(MSG_PRODUCT_TRANSFER_COMPLETED, MSG_ID_PRODUCT_TRANSFER_COMPLETED,
						transferProduct.getIdentifier(), transferProduct.getName(), transferProduct.getSize(),
						transferProduct.getChecksum(), transferProduct.getPublicationTime().toString()));

				return true;
			} catch (Exception e) {
				oldLogger.error(String.format(MSG_EXCEPTION_THROWN, MSG_ID_EXCEPTION_THROWN), e);
				return false;
			}

		} else {
			oldLogger.error(
					String.format(MSG_INVALID_TRANSFER_OBJECT_TYPE, MSG_ID_INVALID_TRANSFER_OBJECT_TYPE, object.getIdentifier()));
			return false;
		}
	}

	/*
	 * (non-Javadoc) Trigger follow-on action (dummy implementation, to be overridden by subclass)
	 */
	@Override
	protected boolean triggerFollowOnAction(TransferObject transferObject) {
		if (logger.isTraceEnabled())
			logger.trace(">>> triggerFollowOnAction({})", null == transferObject ? "null" : transferObject.getIdentifier());

		if (null == transferObject) {
			oldLogger.error(String.format(MSG_TRANSFER_OBJECT_IS_NULL, MSG_ID_TRANSFER_OBJECT_IS_NULL));
			return false;
		}

		if (!(transferObject instanceof TransferProduct)) {
			oldLogger.error(String.format(MSG_INVALID_TRANSFER_OBJECT_TYPE, MSG_ID_INVALID_TRANSFER_OBJECT_TYPE,
					transferObject.getIdentifier()));
			return false;
		}

		TransferProduct transferProduct = (TransferProduct) transferObject;

		oldLogger.warn(String.format(MSG_FOLLOW_ON_ACTION_STARTED, MSG_ID_FOLLOW_ON_ACTION_STARTED, transferProduct.getIdentifier(),
				"NOT IMPLEMENTED"));

		return true;
	}

}