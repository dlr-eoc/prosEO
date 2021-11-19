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
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.retrieve.EdmMetadataRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientComplexValue;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.domain.ClientPrimitiveValue;
import org.apache.olingo.client.api.domain.ClientProperty;
import org.apache.olingo.client.api.uri.QueryOption;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

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
	
	/** The L0 processor command (a single command taking the CADU directory as argument) */
//	private String l0ProcessorCommand;

	/** Maximum number of parallel file download threads within a download session (default 1 = no parallel downloads) */
//	private int maxFileDownloadThreads = 1;
	
	/** Interval in millliseconds to check for completed file downloads (default 500 ms) */
//	private int fileWaitInterval = 500;
	
	/** Maximum number of wait cycles for file download completion checks (default 3600 = total timeout of 30 min) */
//	private int maxFileWaitCycles = 3600;
	
	/** The last copy performance in MiB/s (static, because it may be read and written from different threads) */
	private static Double lastCopyPerformance = 0.0;

	/** Indicator for parallel copying processes */
	/* package */ Map<String, Boolean> copySuccess = new ConcurrentHashMap<>();
	
	/** The AUXIP Monitor configuration to use */
	@Autowired
	private AuxipMonitorConfiguration config;

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

	private static final String MSG_AVAILABLE_DOWNLOADS_FOUND = "(I%d) %d session entries found for download (unfiltered)";
	private static final String MSG_PRODUCT_TRANSFER_COMPLETED = "(I%d) Transfer for session %s completed";
	private static final String MSG_FOLLOW_ON_ACTION_STARTED = "(I%d) Follow-on action for session %s started with command %s";

	/** A logger for this class */
	private static Logger logger = LoggerFactory.getLogger(AuxipMonitorConfiguration.class);
	
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
//		this.setMaxFileDownloadThreads(config.getMaxFileDownloadThreads());
//		this.setFileWaitInterval(config.getFileWaitInterval());
//		this.setMaxFileWaitCycles(config.getMaxFileWaitCycles());
		
//		l0ProcessorCommand = config.getL0Command();
		
		logger.info("------  Starting AUXIP Monitor  ------");
		logger.info("AUXIP base URI . . . . . . : " + config.getAuxipBaseUri());
		logger.info("AUXIP context. . . . . . . : " + config.getAuxipContext());
		logger.info("Use token-based auth . . . : " + config.getAuxipUseToken());
		logger.info("Product types requested  . : " + config.getAuxipProductTypes());
		logger.info("Transfer history file  . . : " + this.getTransferHistoryFile());
		logger.info("AUXIP check interval   . . : " + this.getCheckInterval());
		logger.info("History truncation interval: " + this.getTruncateInterval());
		logger.info("History retention period . : " + this.getHistoryRetentionDuration());
		logger.info("Ingestor URI . . . . . . . : " + config.getIngestorUri());
//		logger.info("L0 processor command . . . : " + l0ProcessorCommand);
		logger.info("Max. transfer sessions . . : " + this.getMaxDownloadThreads());
		logger.info("Transfer session wait time : " + this.getTaskWaitInterval());
		logger.info("Max. session wait cycles . : " + this.getMaxWaitCycles());
//		logger.info("Max. file download threads : " + this.getMaxFileDownloadThreads());
//		logger.info("File download wait time  . : " + this.getFileWaitInterval());
//		logger.info("Max. file wait cycles  . . : " + this.getMaxFileWaitCycles());
		
	}
	
	/**
	 * Gets the maximum number of parallel file download threads within a download session
	 * 
	 * @return the maximum number of parallel file download threads
	 */
//	public int getMaxFileDownloadThreads() {
//		return maxFileDownloadThreads;
//	}
		
	/**
	 * Sets the maximum number of parallel file download threads within a download session
	 * 
	 * @param maxFileDownloadThreads the maximum number of parallel file download threads to set
	 */
//	public void setMaxFileDownloadThreads(int maxFileDownloadThreads) {
//		this.maxFileDownloadThreads = maxFileDownloadThreads;
//	}
	
	/**
	 * Gets the interval to check for completed file downloads
	 * 
	 * @return the check interval in millliseconds
	 */
//	public int getFileWaitInterval() {
//		return fileWaitInterval;
//	}

	/**
	 * Sets the interval to check for completed file downloads
	 * 
	 * @param fileWaitInterval the check interval in millliseconds to set
	 */
//	public void setFileWaitInterval(int fileWaitInterval) {
//		this.fileWaitInterval = fileWaitInterval;
//	}

	/**
	 * Gets the maximum number of wait cycles for file download completion checks
	 * 
	 * @return the maximum number of wait cycles
	 */
//	public int getMaxFileWaitCycles() {
//		return maxFileWaitCycles;
//	}

	/**
	 * Sets the maximum number of wait cycles for file download completion checks
	 * 
	 * @param maxFileWaitCycles the maximum number of wait cycles to set
	 */
//	public void setMaxFileWaitCycles(int maxFileWaitCycles) {
//		this.maxFileWaitCycles = maxFileWaitCycles;
//	}

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
//		HttpClient httpClient = HttpClient.create().wiretap(true);
//		
//		WebClient webClient = WebClient.builder()
//				.clientConnector(new ReactorClientHttpConnector(httpClient))
//				.baseUrl(config.getAuxipBaseUri())
//				.build();
		
		WebClient webClient = WebClient.create(config.getAuxipBaseUri());
		RequestBodySpec request = webClient.post()
				.uri(config.getAuxipTokenUri())
				.accept(MediaType.APPLICATION_JSON);
		
		// Set username and password as query parameters
		MultiValueMap<String, String> queryVariables = new LinkedMultiValueMap<>();
		
		queryVariables.add("grant_type", "password");
		queryVariables.add("username", config.getAuxipUser());
		queryVariables.add("password", config.getAuxipPassword());
		
		// Add query parameters, if OpenID is required for login, otherwise prepare Basic Auth with username/password
		if (null == config.getAuxipClientId()) {
			request = request.header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encode(
					(config.getAuxipUser() + ":" + config.getAuxipPassword()).getBytes()));
		} else {
			queryVariables.add("scope", "openid");
			queryVariables.add("client_id", config.getAuxipClientId());
			queryVariables.add("client_secret", URLEncoder.encode(config.getAuxipClientSecret(), Charset.defaultCharset()));
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
		if (logger.isTraceEnabled()) logger.trace("... got token response '{}'", tokenResponse);
		
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
			if (logger.isTraceEnabled()) logger.trace("... found access token {}", accessToken);
			return (String) accessToken;
		}
	}

	/**
	 * Check for available products of the given product type published after the given reference time stamp
	 * 
	 * @param productType the product type to check for
	 * @param referenceTimeStamp the reference time stamp to check against
	 * @param bearerToken bearer token for authentication, if required (if not set, Basic Auth will be used)
	 * @return a list of product UUIDs available for download (may be empty)
	 */
	private List<TransferProduct> checkAvailableProducts(String productType, Instant referenceTimeStamp, String bearerToken) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkAvailableProducts({}, {}, <bearer token>)", productType, referenceTimeStamp);
		
		List<TransferProduct> result = new ArrayList<>();
		
		// Obtain OData metadata --> TODO Check whether this is actually needed??
		ODataClient oDataClient = ODataClientFactory.getClient();
		oDataClient.getConfiguration().setDefaultPubFormat(ContentType.APPLICATION_JSON);
		
		String oDataServiceRoot = config.getAuxipBaseUri() 
				+ "/" 
				+ (config.getAuxipContext().isBlank() ? "" : config.getAuxipContext() + "/") 
				+ "odata/v1";
		String authorizationHeader = config.getAuxipUseToken() ?
					"Bearer " + bearerToken : 
        			"Basic " + Base64.getEncoder().encode((config.getAuxipUser() + ":" + config.getAuxipPassword()).getBytes());
		
		if (logger.isTraceEnabled()) logger.trace("... requesting metadata document at URL '{}'", oDataServiceRoot);
		EdmMetadataRequest metaDataRequest = oDataClient.getRetrieveRequestFactory()
			.getMetadataRequest(oDataServiceRoot);
		metaDataRequest.addCustomHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
		Edm edm = metaDataRequest.execute().getBody();
		
		// Retrieve products
		if (logger.isTraceEnabled()) logger.trace("... requesting product list at URL '{}'", oDataServiceRoot);
		ODataEntitySetRequest<ClientEntitySet> request = oDataClient.getRetrieveRequestFactory()
		        .getEntitySetRequest(
		        		oDataClient.newURIBuilder(oDataServiceRoot)
		        			.appendEntitySetSegment("Products")
		        			.addQueryOption(QueryOption.FILTER, "startswith(Name,'" + productType + "')"
		        					+ " and PublicationDate gt " + referenceTimeStamp)
		        		//	.count()					// --> not implemented on PDGS-PRIP
		        		//	.top(1000)					// --> not allowed on PDGS-PRIP
		        		//	.orderBy("PublicationDate") // --> not allowed on PDGS-PRIP
		        			.build()
		        );
		request.addCustomHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
		ODataRetrieveResponse<ClientEntitySet> response = request.execute();
		ClientEntitySet entitySet = response.getBody();

		// Extract product metadata
		for (ClientEntity clientEntity: entitySet.getEntities()) {
			TransferProduct tp = extractTransferProduct(clientEntity);
			if (null != tp) {
				result.add(tp);
			}
		}
		

		// Create a request
//		WebClient webClient = WebClient.create(config.getAuxipBaseUri());
//		String requestUri = (config.getAuxipContext().isBlank() ? "" : config.getAuxipContext() + "/") + "odata/v1/Products";
//		RequestBodySpec request = webClient.post()
//				.uri(requestUri)
//				.accept(MediaType.APPLICATION_JSON);
		
		// Set bearer token or Basic Auth header
//		if (config.getAuxipUseToken()) {
//			request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
//		} else {
//			request = request.header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encode(
//					(config.getAuxipUser() + ":" + config.getAuxipPassword()).getBytes()));
//		}
		
		// Set selection parameters
//		MultiValueMap<String, String> queryVariables = new LinkedMultiValueMap<>();
//		queryVariables.add("$filter", "startswith(Name,'S1B_OPER_" + productType + "')"
//				+ " and PublicationDate gt '" + referenceTimeStamp + "'");
//		queryVariables.add("$count", "true");
//		queryVariables.add("$top", "1000");
//		queryVariables.add("$orderby", "PublicationDate");

		// Perform product list request
//		String oDataResponse = request
//			.syncBody(queryVariables)
//			.retrieve()
//			.bodyToMono(String.class)
//			.block();
//		if (null == oDataResponse) {
//			logger.error(String.format(MSG_PRODUCT_REQUEST_FAILED, MSG_ID_PRODUCT_REQUEST_FAILED, requestUri));
//			return result;
//		}
		
		// Analyse the result
//		ObjectMapper om = new ObjectMapper();
//		Map<?, ?> oDataResult = null;
//		try {
//			oDataResult = om.readValue(oDataResponse, Map.class);
//		} catch (IOException e) {
//			logger.error(String.format(MSG_ODATA_RESPONSE_INVALID, MSG_ID_ODATA_RESPONSE_INVALID,
//					oDataResponse, requestUri, e.getMessage()));
//			return result;
//		}
//		if (null == oDataResult) {
//			logger.error(String.format(MSG_ODATA_RESULT_NULL, MSG_ID_ODATA_RESULT_NULL, requestUri));
//			return result;
//		}
//		
//		Object countObject = oDataResult.get("@odata.count");
//		int count = -1;
//		if (null == countObject || ! (countObject instanceof Number)) {
//			logger.warn(String.format(MSG_ODATA_COUNT_NULL, MSG_ID_ODATA_COUNT_NULL, requestUri));
//		}
//
//		Object productListObject = oDataResult.get("value");
//		if (null == productListObject || ! (productListObject instanceof List)) {
//			logger.error(String.format(MSG_PRODUCT_LIST_NULL, MSG_ID_PRODUCT_LIST_NULL, requestUri));
//			return result;
//		}
//		List<?> productList = (List<?>) productListObject;
//		
//		for (Object productObject: productList) {
//			if (! (productObject instanceof Map)) {
//				logger.error(String.format(MSG_PRODUCT_ENTRY_INVALID, MSG_ID_PRODUCT_ENTRY_INVALID,
//						productObject, requestUri));
//				return result;
//			}
//			TransferProduct tp = extractTransferProduct((Map<?, ?>) productObject);
//			if (null == tp) {
//				logger.error(String.format(MSG_PRODUCT_ENTRY_INVALID, MSG_ID_PRODUCT_ENTRY_INVALID,
//						productObject, requestUri));
//				return result;
//			}
//			
//			result.add(tp);
//		}
		
		return result;
	}

	/**
	 * @param productObject
	 * @return
	 */
	public TransferProduct extractTransferProduct(ClientEntity product) {
		if (logger.isTraceEnabled()) logger.trace(">>> extractTransferProduct({}, <bearer token>)", product);

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
			ClientProperty p = product.getProperty("ContentDate");
			if (logger.isTraceEnabled()) logger.trace("... p = {}", p);
			ClientComplexValue cv = p.getComplexValue();
			if (logger.isTraceEnabled()) logger.trace("... cv = {}", cv);
			ClientProperty p2 = cv.get("Start");
			if (logger.isTraceEnabled()) logger.trace("... p2 = {}", p2);
			ClientPrimitiveValue pv = p2.getPrimitiveValue();
			if (logger.isTraceEnabled()) logger.trace("... pv = {}", pv);
			String d = pv.toCastValue(String.class);
			if (logger.isTraceEnabled()) logger.trace("... d = {}", d);
			Instant i = Instant.parse(d);
			if (logger.isTraceEnabled()) logger.trace("... i = {}", i);
			tp.setStartTime(Instant.parse(product.getProperty("ContentDate").getComplexValue()
					.get("Start").getPrimitiveValue().toCastValue(String.class)));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.error(String.format(MSG_PRODUCT_VAL_START_MISSING, MSG_ID_PRODUCT_VAL_START_MISSING, product.toString()));
			return null;
		}
		if (logger.isTraceEnabled()) logger.trace("... start = {}", tp.getStartTime());
		
		try {
			tp.setStartTime(Instant.parse(product.getProperty("ContentDate").getComplexValue()
					.get("End").getPrimitiveValue().toCastValue(String.class)));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.error(String.format(MSG_PRODUCT_VAL_STOP_MISSING, MSG_ID_PRODUCT_VAL_STOP_MISSING, product.toString()));
			return null;
		}
		if (logger.isTraceEnabled()) logger.trace("... stop = {}", tp.getStopTime());
		
//		Object productUuid = product.get("Id");
//		if (null == productUuid || ! (productUuid instanceof String)) {
//			logger.error(String.format(MSG_PRODUCT_UUID_MISSING, MSG_ID_PRODUCT_UUID_MISSING, product));
//			return null;
//		}
//		tp.setUuid((String) productUuid);
//		
//		Object productFileName = product.get("Name");
//		if (null == productFileName || ! (productFileName instanceof String)) {
//			logger.error(String.format(MSG_PRODUCT_FILENAME_MISSING, MSG_ID_PRODUCT_FILENAME_MISSING, product));
//			return null;
//		}
//		tp.setName((String) productFileName);
//		
//		Object productSize = product.get("ContentLength");
//		if (null == productSize || ! (productSize instanceof Number)) {
//			logger.error(String.format(MSG_PRODUCT_SIZE_MISSING, MSG_ID_PRODUCT_SIZE_MISSING, product));
//			return null;
//		}
//		tp.setSize(((Number) productSize).longValue());
//		
//		Object productChecksums = product.get("Checksums");
//		if (null == productChecksums || ! (productChecksums instanceof List) || ((List<?>) productChecksums).isEmpty()) {
//			logger.error(String.format(MSG_PRODUCT_CHECKSUMS_MISSING, MSG_ID_PRODUCT_CHECKSUMS_MISSING, product));
//			return null;
//		}
//		String productHash = null;
//		for (Object productChecksum: (List<?>) productChecksums) {
//			if (! (productChecksum instanceof Map)) {
//				logger.error(String.format(MSG_PRODUCT_CHECKSUM_INVALID, MSG_ID_PRODUCT_CHECKSUM_INVALID, product));
//				continue;
//			}
//			if ("MD5".equals(((Map<?, ?>) productChecksum).get("Algorithm"))) {
//				Object productHashObject = ((Map<?, ?>) productChecksum).get("Value");
//				if (null == productHashObject || ! (productHashObject instanceof String)) {
//					logger.error(String.format(MSG_PRODUCT_HASH_MISSING, MSG_ID_PRODUCT_HASH_MISSING, product));
//					return null;
//				}
//				productHash = (String) productHashObject;
//			}
//		}
//		if (null == productHash) {
//			logger.error(String.format(MSG_PRODUCT_HASH_MISSING, MSG_ID_PRODUCT_HASH_MISSING, product));
//			return null;
//		}
//		tp.setChecksum(productHash);
//		
//		Object productContentDate = product.get("ContentDate");
//		if (null == productContentDate || ! (productContentDate instanceof Map) || ((Map<?, ?>) productContentDate).isEmpty()) {
//			logger.error(String.format(MSG_PRODUCT_CONTENT_DATE_MISSING, MSG_ID_PRODUCT_CONTENT_DATE_MISSING, product));
//			return null;
//		}
//		Object productValidityStart = 
		
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
	protected List<TransferObject> checkAvailableDownloads(Instant referenceTimeStamp) {
		if (logger.isTraceEnabled()) logger.trace(">>> checkAvailableDownloads({})", referenceTimeStamp);

		List<TransferObject> objectsToTransfer = new ArrayList<>();
		
		// If token-based authentication is required, login to AUXIP and request token
		String bearerToken = null;
		if (config.getAuxipUseToken()) {
			if (logger.isTraceEnabled()) logger.trace("... requesting token from AUXIP");
			bearerToken = getBearerToken();
			if (null == bearerToken) {
				// Already logged, return an empty list
				return objectsToTransfer;
			}
		}
		
		// Loop over all configured product types
		for (String productType: config.getAuxipProductTypes()) {
			if (logger.isTraceEnabled()) logger.trace("... checking for products of type {}", productType);
			List<TransferProduct> transferProducts = checkAvailableProducts(productType, referenceTimeStamp, bearerToken);
			if (logger.isTraceEnabled()) logger.trace("... found {} products", transferProducts.size());

			objectsToTransfer.addAll(transferProducts);
		}
		
		logger.info(String.format(MSG_AVAILABLE_DOWNLOADS_FOUND, MSG_ID_AVAILABLE_DOWNLOADS_FOUND, objectsToTransfer.size()));
		
		return objectsToTransfer;
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
			
			TransferProduct transferProduct = (TransferProduct) object;
			
			// Check target directory
			if (!Files.isWritable(Path.of(config.getAuxipDirectoryPath()))) {
				logger.error(String.format(MSG_TARGET_DIRECTORY_NOT_WRITABLE, MSG_ID_TARGET_DIRECTORY_NOT_WRITABLE,
						config.getAuxipDirectoryPath()));
				return false;
			}
			
			// Create retrieval request
			WebClient webClient = WebClient.create(config.getAuxipBaseUri());
			String requestUri = (config.getAuxipContext().isBlank() ? "" : config.getAuxipContext() + "/") + "odata/v1/Products("
					+ transferProduct.getUuid() + ")/$value";
			RequestHeadersSpec<?> request = webClient.get()
					.uri(requestUri)
					.accept(MediaType.APPLICATION_OCTET_STREAM);
			
			// Set bearer token or Basic Auth header
			if (config.getAuxipUseToken()) {
				if (logger.isTraceEnabled()) logger.trace("... requesting token from AUXIP for download");
				request = request.header(HttpHeaders.AUTHORIZATION, "Bearer " + getBearerToken());
			} else {
				request = request.header(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encode(
						(config.getAuxipUser() + ":" + config.getAuxipPassword()).getBytes()));
			}
			
			// Retrieve and store product file
			Instant copyStart = Instant.now();
			String productFileName = config.getAuxipDirectoryPath() + File.separator + transferProduct.getName();

			Flux<DataBuffer> productFlux = request.retrieve().bodyToFlux(DataBuffer.class);
			try {
				DataBufferUtils.write(productFlux, new FileOutputStream(productFileName)).blockLast();
			} catch (FileNotFoundException e) {
				logger.error(String.format(MSG_FILE_NOT_WRITABLE, MSG_ID_FILE_NOT_WRITABLE, productFileName));
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
			
			logger.info(String.format(MSG_PRODUCT_TRANSFER_COMPLETED, MSG_ID_PRODUCT_TRANSFER_COMPLETED, transferProduct.getIdentifier()));
			
			return true;

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