/**
 * CadipMonitor.java
 *
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.api.cadipmon;

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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;

import org.apache.olingo.client.api.ODataClient;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntityRequest;
import org.apache.olingo.client.api.communication.request.retrieve.ODataEntitySetRequest;
import org.apache.olingo.client.api.communication.response.ODataRetrieveResponse;
import org.apache.olingo.client.api.domain.ClientEntity;
import org.apache.olingo.client.api.domain.ClientEntitySet;
import org.apache.olingo.client.api.uri.QueryOption;
import org.apache.olingo.client.core.ODataClientFactory;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
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
import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ApiMonitorMessage;
import de.dlr.proseo.logging.messages.GeneralMessage;
import de.dlr.proseo.logging.messages.OAuthMessage;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Monitor for CADU Interface Points (CADIP)
 *
 * For specification details see "CADU Interface Delivery Point Specification" (ESA-EOPG-EOPGC-IF-15, issue 1.1)
 *
 * @author Dr. Thomas Bassler
 */
@Component
@Scope("singleton")
public class CadipMonitor extends BaseMonitor {

	private static final String CHANNEL_PREFIX = "ch_";

	/** The path to the target CADU directory (for L0 processing) */
	private Path caduDirectoryPath;

	/** The L0 processor command (a single command taking the CADU directory as argument) */
	private String l0ProcessorCommand;

	/** Maximum number of parallel file download threads within a download session (default 1 = no parallel downloads) */
	private int maxFileDownloadThreads = 1;

	/** Interval in millliseconds to check for completed file downloads (default 2000 ms) */
	private int fileWaitInterval = 2000;

	/** Maximum number of wait cycles for file download completion checks (default 900 = total timeout of 30 min) */
	private int maxFileWaitCycles = 900;

	/** Interval in millliseconds to check for completed quality info requests (default 500 ms) */
	private int qualityRetryInterval = 2000;

	/** Maximum number of wait cycles for quality info completeness checks (default 300 = total timeout of 10 min) */
	private int maxQualityRetries = 300;

	/** Header marker for S3 redirects */
	private static final String S3_CREDENTIAL_PARAM = "Amz-Credential";

	/** Maximum number of session/file entries to retrieve in one request */
	private static final int MAX_ENTRY_COUNT = 1000;

	/** Date format for OData date/time-based requests */
	private static DateTimeFormatter odataDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");

	/** The last copy performance in MiB/s (static, because it may be read and written from different threads) */
	private static Double lastCopyPerformance = 0.0;

	/** Indicator for parallel copying processes */
	/* package */ Map<String, Boolean> copySuccess = new ConcurrentHashMap<>();

	/** Total data size per session */
	private Map<String, Long> sessionDataSizes = new ConcurrentHashMap<>();

	/** The CADIP Monitor configuration to use */
	@Autowired
	private CadipMonitorConfiguration config;

	/** A logger for this class */
	private static ProseoLogger logger = new ProseoLogger(CadipMonitor.class);

	/**
	 * Class describing a download session
	 */
	public static class TransferSession implements TransferObject {

		/** The satellite identifier */
		private String satelliteIdentifier;

		/** The session UUID */
		private String sessionUuid;

		/** The CADIP session identifier */
		private String sessionIdentifier;

		/** Number of channels in the downlink */
		private Integer numChannels;

		/** Station Unit ID */
		private String stationUnitId;

		/** Downlink orbit number */
		private Integer downlinkOrbit;

		/** Flags whether the session corresponds to a retransfer */
		private Boolean retransfer;

		/** Flag indicating completeness of session provisioning */
		private Boolean sessionComplete = false;

		/** Acquisition time of the first CADU in the first DSDB */
		private Instant downlinkStart;

		/** Acquisition time of the last CADU in the last DSDB */
		private Instant downlinkStop;

		/** Flag indicating quality of session provisioning */
		private Boolean deliveryPushOk;

		/** Total downlink size */
		private Long downlinkSize;

		/** Latest publication time of any CADU file */
		private Instant latestPublicationTime;

		/** Reference time for this session (publication timestamp) */
		private Instant referenceTime;

		/** Session quality information per channel (indexed by channel number, i. e. starting with 1!) */
		private TransferQualityInfo[] qualityInfos = null;

		/**
		 * Gets the session identifier
		 *
		 * @return the session identifier
		 */
		public String getSessionIdentifier() {
			return sessionIdentifier;
		}

		/**
		 * Sets the session identifier
		 *
		 * @param sessionIdentifier the session identifier to set
		 */
		public void setSessionIdentifier(String sessionIdentifier) {
			this.sessionIdentifier = sessionIdentifier;
		}

		/**
		 * Gets the satellite identifier
		 *
		 * @return the satellite identifier
		 */
		public String getSatelliteIdentifier() {
			return satelliteIdentifier;
		}

		/**
		 * Sets the satellite identifier
		 *
		 * @param satelliteIdentifier the satellite identifier to set
		 */
		public void setSatelliteIdentifier(String satelliteIdentifier) {
			this.satelliteIdentifier = satelliteIdentifier;
		}

		/**
		 * Gets the session UUID
		 *
		 * @return the session UUID
		 */
		public String getSessionUuid() {
			return sessionUuid;
		}

		/**
		 * Sets the session UUID
		 *
		 * @param sessionUuid the session UUID to set
		 */
		public void setSessionUuid(String sessionUuid) {
			this.sessionUuid = sessionUuid;
		}

		/**
		 * Gets the number of data channels in the session
		 *
		 * @return the number of channels
		 */
		public Integer getNumChannels() {
			return numChannels;
		}

		/**
		 * Sets the number of data channels in the session
		 *
		 * @param numChannels the number of channels to set
		 */
		public void setNumChannels(Integer numChannels) {
			this.numChannels = numChannels;
		}

		/**
		 * Gets the station unit ID
		 *
		 * @return the station unit ID
		 */
		public String getStationUnitId() {
			return stationUnitId;
		}

		/**
		 * Sets the station unit ID
		 *
		 * @param stationUnitId the station unit ID to set
		 */
		public void setStationUnitId(String stationUnitId) {
			this.stationUnitId = stationUnitId;
		}

		/**
		 * Gets the downlink orbit number
		 *
		 * @return the downlink orbit number
		 */
		public Integer getDownlinkOrbit() {
			return downlinkOrbit;
		}

		/**
		 * Sets the downlink orbit number
		 *
		 * @param downlinkOrbit the downlink orbit number to set
		 */
		public void setDownlinkOrbit(Integer downlinkOrbit) {
			this.downlinkOrbit = downlinkOrbit;
		}

		/**
		 * Gets the retransfer flag
		 *
		 * @return true, if this session corresponds to a retransfer, false otherwise
		 */
		public Boolean isRetransfer() {
			return retransfer;
		}

		/**
		 * Sets the retransfer flag
		 *
		 * @param retransfer set to true, if this session corresponds to a retransfer, false otherwise
		 */
		public void setRetransfer(Boolean retransfer) {
			this.retransfer = retransfer;
		}

		/**
		 * Indicates whether session provisioning is complete
		 *
		 * @return true, if session provisioning is complete, false otherwise
		 */
		public Boolean isSessionComplete() {
			return sessionComplete;
		}

		/**
		 * Sets the flag indicating completeness of session provisioning
		 *
		 * @param sessionComplete set to true, if session provisioning is complete, to false otherwise
		 */
		public void setSessionComplete(Boolean sessionComplete) {
			this.sessionComplete = sessionComplete;
		}

		/**
		 * Gets the acquisition time of the first CADU in the first DSDB
		 *
		 * @return the downlink start time
		 */
		public Instant getDownlinkStart() {
			return downlinkStart;
		}

		/**
		 * Sets the acquisition time of the first CADU in the first DSDB
		 *
		 * @param downlinkStart the downlink start time to set
		 */
		public void setDownlinkStart(Instant downlinkStart) {
			this.downlinkStart = downlinkStart;
		}

		/**
		 * Gets the acquisition time of the last CADU in the last DSDB
		 *
		 * @return the downlink stop time
		 */
		public Instant getDownlinkStop() {
			return downlinkStop;
		}

		/**
		 * Sets the acquisition time of the last CADU in the last DSDB
		 *
		 * @param downlinkStop the downlink stop time to set
		 */
		public void setDownlinkStop(Instant downlinkStop) {
			this.downlinkStop = downlinkStop;
		}

		/**
		 * Indicates whether session provisioning was successful
		 *
		 * @return true, if session provisioning was successful, false otherwise
		 */
		public Boolean getDeliveryPushOk() {
			return deliveryPushOk;
		}

		/**
		 * Sets the flag indicating success of session provisioning
		 *
		 * @param deliveryPushOk set to true, if session provisioning was successful, to false otherwise
		 */
		public void setDeliveryPushOk(Boolean deliveryPushOk) {
			this.deliveryPushOk = deliveryPushOk;
		}

		/**
		 * Gets the total downlink size
		 *
		 * @return the downlink size in bytes
		 */
		public Long getDownlinkSize() {
			return downlinkSize;
		}

		/**
		 * Sets the total downlink size
		 *
		 * @param downlinkSize the downlink size in bytes to set
		 */
		public void setDownlinkSize(Long downlinkSize) {
			this.downlinkSize = downlinkSize;
		}

		/**
		 * Gets the latest publication time of any CADU file
		 *
		 * @return the latest CADU publication time
		 */
		public Instant getLatestPublicationTime() {
			return latestPublicationTime;
		}

		/**
		 * Sets the latest publication time of any CADU file
		 *
		 * @param latestPublicationTime the latest CADU publication time to set
		 */
		public void setLatestPublicationTime(Instant latestPublicationTime) {
			this.latestPublicationTime = latestPublicationTime;
		}

		/**
		 * Gets the array of quality information data per channel
		 *
		 * @return an array of quality information objects, indexed by channel number
		 */
		public TransferQualityInfo[] getQualityInfos() {
			return qualityInfos;
		}

		/**
		 * Sets the array of quality information data per channel
		 *
		 * @param qualityInfos the array of quality information objects to set
		 */
		public void setQualityInfos(TransferQualityInfo[] qualityInfos) {
			this.qualityInfos = qualityInfos;
		}

		/**
		 * Gets the session reference time
		 *
		 * @return the reference time
		 */
		@Override
		public Instant getReferenceTime() {
			return referenceTime;
		}

		/**
		 * Sets the session reference time
		 *
		 * @param referenceTime the reference time to set
		 */
		public void setReferenceTime(Instant referenceTime) {
			this.referenceTime = referenceTime;
		}

		/**
		 * Gets the combined transfer object identifier: satellite|station unit|session ID
		 *
		 * @see de.dlr.proseo.api.basemon.TransferObject#getIdentifier()
		 */
		@Override
		public String getIdentifier() {
			return sessionIdentifier;
		}

	}

	/**
	 * Class describing a single CADU (DSDB) file
	 */
	public static class TransferFile {

		/** File UUID */
		private String uuid;

		/** File name */
		private String filename;

		/** Channel the file belongs to */
		private Integer channel;

		/** DSDB block number */
		private Integer blockNumber;

		/** Flag indicating whether this is the final file of the session */
		private Boolean finalBlock;

		/** File publication timestamp */
		private Instant publicationTime;

		/** Expected file eviction time */
		private Instant evictionTime;

		/** File size in bytes */
		private Integer fileSize;

		/** Flag indicating retransferred file */
		private Boolean retransfer;

		/**
		 * Gets the file UUID
		 *
		 * @return the UUID
		 */
		public String getUuid() {
			return uuid;
		}

		/**
		 * Sets the file UUID
		 *
		 * @param uuid the UUID to set
		 */
		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		/**
		 * Gets the file name
		 *
		 * @return the file name
		 */
		public String getFilename() {
			return filename;
		}

		/**
		 * Sets the file name
		 *
		 * @param filename the file name to set
		 */
		public void setFilename(String filename) {
			this.filename = filename;
		}

		/**
		 * Gets the channel the file belongs to
		 *
		 * @return the channel number
		 */
		public Integer getChannel() {
			return channel;
		}

		/**
		 * Sets the channel the file belongs to
		 *
		 * @param channel the channel number to set
		 */
		public void setChannel(Integer channel) {
			this.channel = channel;
		}

		/**
		 * Gets the DSDB block number
		 *
		 * @return the block number
		 */
		public Integer getBlockNumber() {
			return blockNumber;
		}

		/**
		 * Sets the DSDB block number
		 *
		 * @param blockNumber the block number to set
		 */
		public void setBlockNumber(Integer blockNumber) {
			this.blockNumber = blockNumber;
		}

		/**
		 * Indicates whether this is the final file of the session
		 *
		 * @return true, if this is the final file, false otherwise
		 */
		public boolean isFinalBlock() {
			// Has been found to sometimes be null, although as per ICD this is not allowed, so we play it safe here
			return Objects.equals(finalBlock, Boolean.TRUE);
		}

		/**
		 * Sets the flag indicating whether this is the final file of the session
		 *
		 * @param finalBlock set to true, if this is the final file, to false otherwise
		 */
		public void setFinalBlock(Boolean finalBlock) {
			this.finalBlock = finalBlock;
		}

		/**
		 * Gets the file publication timestamp
		 *
		 * @return the publication time
		 */
		public Instant getPublicationTime() {
			return publicationTime;
		}

		/**
		 * Sets the file publication timestamp
		 *
		 * @param publicationTime the publication time to set
		 */
		public void setPublicationTime(Instant publicationTime) {
			this.publicationTime = publicationTime;
		}

		/**
		 * Gets the expected file eviction time
		 *
		 * @return the eviction time
		 */
		public Instant getEvictionTime() {
			return evictionTime;
		}

		/**
		 * Sets the expected file eviction time
		 *
		 * @param evictionTime the eviction time to set
		 */
		public void setEvictionTime(Instant evictionTime) {
			this.evictionTime = evictionTime;
		}

		/**
		 * Gets the file size
		 *
		 * @return the file size in bytes
		 */
		public Integer getFileSize() {
			return fileSize;
		}

		/**
		 * Sets the file size
		 *
		 * @param fileSize the file size in bytes to set
		 */
		public void setFileSize(Integer fileSize) {
			this.fileSize = fileSize;
		}

		/**
		 * Indicates whether this file was retransferred
		 *
		 * @return true, if the file was retransferred, false otherwise
		 */
		public Boolean getRetransfer() {
			return retransfer;
		}

		/**
		 * Sets the flag indicating a retransferred file
		 *
		 * @param retransfer set to true, if the file was retransferred, to false otherwise
		 */
		public void setRetransfer(Boolean retransfer) {
			this.retransfer = retransfer;
		}

	}

	/**
	 * Class holding session quality data (available as soon as final CADU file has been written by acquisition station)
	 */
	public static class TransferQualityInfo {

		/** Downlink channel */
		private Integer channel;

		/** Downlink session ID */
		private String sessionId;

		/** Actual Date/Time start of the transferring of the first CADU chunk for the corresponding channel */
		private Instant deliveryStart;

		/** Actual Date/Time stop of the transferring of the last CADU chunk for the corresponding channel */
		private Instant deliveryStop;

		/** Total number of transferred chunks (CADU files) for the corresponding channel */
		private Integer totalChunks;

		/** Overall volume size in bytes of the acquisition session for the corresponding channel */
		private Long totalVolume;

		/**
		 * Gets the channel number
		 *
		 * @return the channel number
		 */
		public Integer getChannel() {
			return channel;
		}

		/**
		 * Sets the channel number
		 *
		 * @param channel the channel number to set
		 */
		public void setChannel(Integer channel) {
			this.channel = channel;
		}

		/**
		 * Gets the session ID
		 *
		 * @return the session ID
		 */
		public String getSessionId() {
			return sessionId;
		}

		/**
		 * Sets the session ID
		 *
		 * @param sessionId the session ID to set
		 */
		public void setSessionId(String sessionId) {
			this.sessionId = sessionId;
		}

		/**
		 * Gets the transfer start timestamp
		 *
		 * @return the delivery start time
		 */
		public Instant getDeliveryStart() {
			return deliveryStart;
		}

		/**
		 * sets the transfer start timestamp
		 *
		 * @param deliveryStart the delivery start time to set
		 */
		public void setDeliveryStart(Instant deliveryStart) {
			this.deliveryStart = deliveryStart;
		}

		/**
		 * Gets the transfer stop timestamp
		 *
		 * @return the delivery stop time
		 */
		public Instant getDeliveryStop() {
			return deliveryStop;
		}

		/**
		 * Sets the transfer stop timestamp
		 *
		 * @param deliveryStop the delivery stop time to set
		 */
		public void setDeliveryStop(Instant deliveryStop) {
			this.deliveryStop = deliveryStop;
		}

		/**
		 * Gets the total number of CADU files in this channel
		 *
		 * @return the number of chunks
		 */
		public Integer getTotalChunks() {
			return totalChunks;
		}

		/**
		 * Sets the total number of CADU files in this channel
		 *
		 * @param totalChunks the number of chunks to set
		 */
		public void setTotalChunks(Integer totalChunks) {
			this.totalChunks = totalChunks;
		}

		/**
		 * Gets the total data volume in this channel
		 *
		 * @return the total data volume
		 */
		public Long getTotalVolume() {
			return totalVolume;
		}

		/**
		 * Sets the total data volume in this channel
		 *
		 * @param totalVolume the total data volume to set
		 */
		public void setTotalVolume(Long totalVolume) {
			this.totalVolume = totalVolume;
		}

	}

	/**
	 * Gets the maximum number of parallel file download threads within a download session
	 *
	 * @return the maximum number of parallel file download threads
	 */
	public int getMaxFileDownloadThreads() {
		return maxFileDownloadThreads;
	}

	/**
	 * Sets the maximum number of parallel file download threads within a download session
	 *
	 * @param maxFileDownloadThreads the maximum number of parallel file download threads to set
	 */
	public void setMaxFileDownloadThreads(int maxFileDownloadThreads) {
		this.maxFileDownloadThreads = maxFileDownloadThreads;
	}

	/**
	 * Gets the interval to check for completed file downloads
	 *
	 * @return the check interval in millliseconds
	 */
	public int getFileWaitInterval() {
		return fileWaitInterval;
	}

	/**
	 * Sets the interval to check for completed file downloads
	 *
	 * @param fileWaitInterval the check interval in millliseconds to set
	 */
	public void setFileWaitInterval(int fileWaitInterval) {
		this.fileWaitInterval = fileWaitInterval;
	}

	/**
	 * Gets the maximum number of wait cycles for file download completion checks
	 *
	 * @return the maximum number of wait cycles
	 */
	public int getMaxFileWaitCycles() {
		return maxFileWaitCycles;
	}

	/**
	 * Sets the maximum number of wait cycles for file download completion checks
	 *
	 * @param maxFileWaitCycles the maximum number of wait cycles to set
	 */
	public void setMaxFileWaitCycles(int maxFileWaitCycles) {
		this.maxFileWaitCycles = maxFileWaitCycles;
	}

	/**
	 * Initialize global parameters
	 */
	@PostConstruct
	private void init() {
		
		if (logger.isTraceEnabled())
			logger.trace("... init() called for object {} of class {}", this.toString(), this.getClass().getName());
		
		// Set parameters in base monitor
		this.setTransferHistoryFile(Paths.get(config.getCadipHistoryPath()));
		this.setCheckInterval(config.getCadipCheckInterval());
		this.setTruncateInterval(config.getCadipTruncateInterval());
		this.setHistoryRetentionDuration(Duration.ofMillis(config.getCadipHistoryRetention()));

		// Multi-threading control
		this.setMaxDownloadThreads(config.getMaxDownloadThreads());
		this.setTaskWaitInterval(config.getTaskWaitInterval());
		this.setMaxWaitCycles(config.getMaxWaitCycles());
		this.setMaxFileDownloadThreads(config.getMaxFileDownloadThreads());
		this.setFileWaitInterval(config.getFileWaitInterval());
		this.setMaxFileWaitCycles(config.getMaxFileWaitCycles());

		caduDirectoryPath = Path.of(config.getL0CaduDirectoryPath());

		HttpURLConnection.setFollowRedirects(true);

		logger.log(ApiMonitorMessage.CADIP_START_MESSAGE, config.getCadipBaseUri(), config.getCadipContext(),
				config.getCadipUseToken(), config.getCadipSatellite(), getTransferHistoryFile(), getCheckInterval(),
				config.getCadipSessionInterval(), config.getCadipRetrievalTimeout(), getTruncateInterval(),
				getHistoryRetentionDuration(), caduDirectoryPath, l0ProcessorCommand, getMaxDownloadThreads(),
				getTaskWaitInterval(), getMaxWaitCycles(), getMaxFileDownloadThreads(), getFileWaitInterval(),
				getMaxFileWaitCycles());

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
	 * Thread-safe method to calculate total session download size
	 *
	 * @param sessionId the identifier of the session to calculate the size for
	 * @param caduSize the size of the CADU chunk to add to the session download size
	 */
	synchronized private void addToSessionDataSize(String sessionId, long caduSize) {
		if (null == sessionDataSizes.get(sessionId)) {
			sessionDataSizes.put(sessionId, caduSize);
		} else {
			sessionDataSizes.put(sessionId, sessionDataSizes.get(sessionId) + caduSize);
		}
	}

	/**
	 * Kill all open subtasks (e. g. in case of error)
	 * 
	 * @param downloadTasks the list of tasks to kill
	 */
	private void killTasks(List<Thread> downloadTasks) {
		if (logger.isTraceEnabled()) logger.trace(">>> killTasks(Thread[{}])", (null == downloadTasks ? "null" : downloadTasks.size()));
		
		for (Thread task: downloadTasks) {
			task.interrupt();
		}
		
	}

	/**
	 * Request a bearer token from the CADIP
	 *
	 * @return the bearer token as received from CADIP, or null, if the request failed
	 */
	private String getBearerToken() {
		if (logger.isTraceEnabled())
			logger.trace(">>> getBearerToken()");

		// Create a request
		WebClient webClient = WebClient.create();
		RequestBodySpec request = webClient.post()
			.uri(config.getCadipTokenUri())
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.accept(MediaType.APPLICATION_JSON);
		if (logger.isTraceEnabled())
			logger.trace("... created request for URL '{}'", config.getCadipTokenUri());

		// Prepare Authentication header, if requested
		if (null != config.getCadipUser()) {
			String base64Auth = new String(
					Base64.getEncoder().encode((config.getCadipUser() + ":" + config.getCadipPassword()).getBytes()));
			request = request.header(HttpHeaders.AUTHORIZATION, "Basic " + base64Auth);
			if (logger.isTraceEnabled())
				logger.trace("... Auth: '{}'", base64Auth);
		}

		// Set grant type, username and password as query parameters
		MultiValueMap<String, String> queryVariables = new LinkedMultiValueMap<>();

		queryVariables.add("grant_type", config.getCadipTokenGrantType());
		queryVariables.add("username", config.getCadipTokenUser());
		queryVariables.add("password", config.getCadipTokenPassword());

		// Add client credentials, if requested
		if (null != config.getCadipClientId()) {
			queryVariables.add("client_id", config.getCadipClientId());
			queryVariables.add("client_secret", URLEncoder.encode(config.getCadipClientSecret(), Charset.defaultCharset()));
		}
		// Add (OpenID) scope, if requested
		if (null != config.getCadipTokenScope()) {
			queryVariables.add("scope", config.getCadipTokenScope());
		}
		if (logger.isTraceEnabled())
			logger.trace("... using query variables '{}'", queryVariables);

		// Perform token request
		String tokenResponse = request.body(BodyInserters.fromFormData(queryVariables)).retrieve().bodyToMono(String.class).block();
		if (null == tokenResponse) {
			logger.log(OAuthMessage.TOKEN_REQUEST_FAILED, config.getCadipTokenUri());
			return null;
		}
//		if (logger.isTraceEnabled()) logger.trace("... got token response '{}'", tokenResponse);

		// Analyse the result
		ObjectMapper om = new ObjectMapper();
		Map<?, ?> tokenResponseMap = null;
		try {
			tokenResponseMap = om.readValue(tokenResponse, Map.class);
		} catch (IOException e) {
			logger.log(OAuthMessage.TOKEN_RESPONSE_INVALID, tokenResponse, config.getCadipTokenUri(), e.getMessage());
			return null;
		}
		if (null == tokenResponseMap || tokenResponseMap.isEmpty()) {
			logger.log(OAuthMessage.TOKEN_RESPONSE_EMPTY, tokenResponse, config.getCadipTokenUri());
			return null;
		}
		Object accessToken = tokenResponseMap.get("access_token");
		if (null == accessToken || !(accessToken instanceof String)) {
			logger.log(OAuthMessage.ACCESS_TOKEN_MISSING, tokenResponse, config.getCadipTokenUri());
			return null;
		} else {
//			if (logger.isTraceEnabled()) logger.trace("... found access token {}", accessToken);
			return (String) accessToken;
		}
	}

	/**
	 * Check for available sessions published after the given reference time stamp; only a single request is made (except for
	 * paging)
	 *
	 * @param referenceTimeStamp the reference time stamp to check against
	 * @param maxEntries the maximum number of entries to retrieve
	 * @return a transfer control object containing a list of sessions available for download (may be empty)
	 */
	private TransferControl checkAvailableSessions(Instant referenceTimeStamp, Integer maxEntries) {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkAvailableProducts({})", referenceTimeStamp);

		TransferControl transferControl = new TransferControl();
		transferControl.referenceTime = referenceTimeStamp;

		// Prepare OData request
		ODataClient oDataClient = ODataClientFactory.getClient();
		oDataClient.getConfiguration().setDefaultPubFormat(ContentType.APPLICATION_JSON);

		String oDataServiceRoot = config.getCadipBaseUri() + "/"
				+ (config.getCadipContext().isBlank() ? "" : config.getCadipContext() + "/") + "odata/v1";
		String authorizationHeader = config.getCadipUseToken() ? "Bearer " + getBearerToken()
				: "Basic " + Base64.getEncoder().encode((config.getCadipUser() + ":" + config.getCadipPassword()).getBytes());

		// Create query filter
		StringBuilder queryFilter = new StringBuilder("Satellite eq '");
		queryFilter.append(config.getCadipSatellite()).append("'");
		if (config.isCadipRetransfer()) {
			// In the retransfer case we do not restrict the publication date
			queryFilter.append(" and Retransfer eq true");
		} else {
			queryFilter.append(" and Retransfer eq false")
				.append(" and PublicationDate gt ")
				.append(odataDateFormat.format(referenceTimeStamp.atZone(ZoneId.of("Z"))));
		}

		// Retrieve downlink sessions
		if (logger.isTraceEnabled())
			logger.trace("... requesting session list at URL '{}'", oDataServiceRoot);
		ODataEntitySetRequest<ClientEntitySet> request = oDataClient.getRetrieveRequestFactory()
			.getEntitySetRequest(oDataClient.newURIBuilder(oDataServiceRoot)
				.appendEntitySetSegment("Sessions")
				.addQueryOption(QueryOption.FILTER, queryFilter.toString())
				.addQueryOption(QueryOption.COUNT, "true")
				.top(null == maxEntries || 1 > maxEntries ? MAX_ENTRY_COUNT : maxEntries)
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
		logger.log(ApiMonitorMessage.SESSION_RETRIEVAL_RESULT, entitySet.getEntities().size(), entitySet.getCount());

		// No sessions found, next search starts from current date and time (except for retransfers)
		if (entitySet.getEntities().isEmpty()) {
			if (!config.isCadipRetransfer()) {
				transferControl.referenceTime = Instant.now();
			}
			
			if (logger.isTraceEnabled())
				logger.trace("<<< checkAvailableSessions()");
			return transferControl;
		}

		// Extract session metadata
		for (ClientEntity clientEntity : entitySet.getEntities()) {
			TransferSession ts = extractTransferSession(clientEntity);
			if (null != ts) {
				if (transferControl.referenceTime.isBefore(ts.getReferenceTime())) {
					transferControl.referenceTime = ts.getReferenceTime();
				}
				if (!referenceTimeStamp.isAfter(ts.getReferenceTime()) || config.isCadipRetransfer()) {
					transferControl.transferObjects.add(ts);
				}
			}
		}

		if (logger.isTraceEnabled())
			logger.trace("<<< checkAvailableSessions()");
		return transferControl;
	}

	/**
	 * Extract downlink session metadata from an OData "Sessions" response
	 *
	 * @param session the response to evaluate
	 * @return a session metadata object or null, if the extraction failed
	 */
	private TransferSession extractTransferSession(ClientEntity session) {
		if (logger.isTraceEnabled())
			logger.trace(">>> extractTransferSession({})", (null == session ? "null" : session.getProperty("SessionId")));

		TransferSession ts = new TransferSession();

		try {
			ts.setSessionUuid(session.getProperty("Id").getPrimitiveValue().toCastValue(String.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.SESSION_ELEMENT_MISSING, session.toString(), "Id");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... session UUID = {}", ts.getSessionUuid());

		try {
			ts.setSessionIdentifier(session.getProperty("SessionId").getPrimitiveValue().toCastValue(String.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.SESSION_ELEMENT_MISSING, session.toString(), "SessionId");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... session ID = {}", ts.getSessionIdentifier());

		try {
			ts.setNumChannels(session.getProperty("NumChannels").getPrimitiveValue().toCastValue(Integer.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.SESSION_ELEMENT_MISSING, session.toString(), "NumChannels");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... number of channels = {}", ts.getNumChannels());

		try {
			ts.setReferenceTime(
					Instant.parse(session.getProperty("PublicationDate").getPrimitiveValue().toCastValue(String.class)));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			logger.log(ApiMonitorMessage.SESSION_ELEMENT_MISSING, session.toString(), "PublicationDate");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... publication = {}", ts.getReferenceTime());

		try {
			ts.setSatelliteIdentifier(session.getProperty("Satellite").getPrimitiveValue().toCastValue(String.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.SESSION_ELEMENT_MISSING, session.toString(), "Satellite");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... satellite ID = {}", ts.getSatelliteIdentifier());

		try {
			ts.setStationUnitId(session.getProperty("StationUnitId").getPrimitiveValue().toCastValue(String.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.SESSION_ELEMENT_MISSING, session.toString(), "StationUnitId");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... station unit ID = {}", ts.getStationUnitId());

		try {
			ts.setDownlinkOrbit(session.getProperty("DownlinkOrbit").getPrimitiveValue().toCastValue(Integer.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.SESSION_ELEMENT_MISSING, session.toString(), "DownlinkOrbit");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... downlink orbit = {}", ts.getDownlinkOrbit());

		try {
			ts.setRetransfer(session.getProperty("Retransfer").getPrimitiveValue().toCastValue(Boolean.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			// Ignore, may not yet be set
		}
		if (logger.isTraceEnabled())
			logger.trace("... retransfer = {}", ts.isRetransfer());

		try {
			ts.setDownlinkStart(Instant.parse(session.getProperty("DownlinkStart").getPrimitiveValue().toCastValue(String.class)));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			// Ignore, may not yet be set
		}
		if (logger.isTraceEnabled())
			logger.trace("... downlink start = {}", ts.getDownlinkStart());

		try {
			ts.setDownlinkStop(Instant.parse(session.getProperty("DownlinkStop").getPrimitiveValue().toCastValue(String.class)));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			// Ignore, may not yet be set
		}
		if (logger.isTraceEnabled())
			logger.trace("... downlink stop = {}", ts.getDownlinkStop());

		try {
			ts.setDeliveryPushOk(session.getProperty("DeliveryPushOK").getPrimitiveValue().toCastValue(Boolean.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			// Ignore, may not yet be set
		}
		if (logger.isTraceEnabled())
			logger.trace("... delivery push OK = {}", ts.getDeliveryPushOk());

		ts.setLatestPublicationTime(ts.getReferenceTime());
		ts.setQualityInfos(new TransferQualityInfo[ts.getNumChannels()]);

		return ts;
	}

	/**
	 * Check the configured CADIP for new downlink sessions, whose publication date is after the reference time stamp:
	 * <ol>
	 * <li>Retrieve all downlink sessions with publication time after reference time stamp (authenticating with either Basic Auth or
	 * Bearer Token)</li>
	 * <li>Convert JSON session entries into transfer objects</li>
	 * <li>Return a list of all transfer objects</li>
	 * </ol>
	 *
	 * @param referenceTimeStamp the reference timestamp to apply for pickup point lookups
	 * @return a transfer control object containing a list of available transfer objects
	 */
	@Override
	protected TransferControl checkAvailableDownloads(Instant referenceTimeStamp) {
		if (logger.isTraceEnabled())
			logger.trace(">>> checkAvailableDownloads({})", referenceTimeStamp);

		TransferControl transferControl = new TransferControl();

		try {

			// Retrieve all available sessions
			transferControl = checkAvailableSessions(referenceTimeStamp, MAX_ENTRY_COUNT);

			logger.log(ApiMonitorMessage.AVAILABLE_DOWNLOADS_FOUND, transferControl.transferObjects.size());
		} catch (Exception e) {
			logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass().getName() + " / " + e.getMessage());
			if (logger.isDebugEnabled())
				logger.debug("... stack trace: ", e);
		}

		if (logger.isTraceEnabled())
			logger.trace("... Returning transferControl with reference time {}", transferControl.referenceTime);

		return transferControl;
	}

	/**
	 * Extract CADU (DSDB) file metadata from an OData "Files" response
	 *
	 * @param file the response to evaluate
	 * @return a CADU file metadata object or null, if the extraction failed
	 */
	private TransferFile extractTransferFile(ClientEntity file) {
		if (logger.isTraceEnabled())
			logger.trace(">>> extractTransferFile({})", (null == file ? "null" : file.getProperty("Name")));

		TransferFile tf = new TransferFile();

		try {
			tf.setUuid(file.getProperty("Id").getPrimitiveValue().toCastValue(String.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.FILE_ELEMENT_MISSING, file.toString(), "Id");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... file UUID = {}", tf.getUuid());

		try {
			tf.setFilename(file.getProperty("Name").getPrimitiveValue().toCastValue(String.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.FILE_ELEMENT_MISSING, file.toString(), "Name");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... file name = {}", tf.getFilename());

		try {
			tf.setChannel(file.getProperty("Channel").getPrimitiveValue().toCastValue(Integer.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.FILE_ELEMENT_MISSING, file.toString(), "Channel");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... channel = {}", tf.getChannel());

		try {
			tf.setBlockNumber(file.getProperty("BlockNumber").getPrimitiveValue().toCastValue(Integer.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.FILE_ELEMENT_MISSING, file.toString(), "BlockNumber");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... block number = {}", tf.getBlockNumber());

		try {
			tf.setFinalBlock(file.getProperty("FinalBlock").getPrimitiveValue().toCastValue(Boolean.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.FILE_ELEMENT_MISSING, file.toString(), "FinalBlock");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... final block = {}", tf.isFinalBlock());

		try {
			tf.setPublicationTime(Instant.parse(file.getProperty("PublicationDate").getPrimitiveValue().toCastValue(String.class)));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			logger.log(ApiMonitorMessage.FILE_ELEMENT_MISSING, file.toString(), "PublicationDate");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... publication = {}", tf.getPublicationTime());

		try {
			tf.setEvictionTime(Instant.parse(file.getProperty("EvictionDate").getPrimitiveValue().toCastValue(String.class)));
		} catch (EdmPrimitiveTypeException | NullPointerException | DateTimeParseException e) {
			logger.log(ApiMonitorMessage.FILE_ELEMENT_MISSING, file.toString(), "EvictionDate");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... eviction = {}", tf.getEvictionTime());

		try {
			tf.setFileSize(file.getProperty("Size").getPrimitiveValue().toCastValue(Integer.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.FILE_ELEMENT_MISSING, file.toString(), "Size");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... file size = {}", tf.getFileSize());

		try {
			tf.setRetransfer(file.getProperty("Retransfer").getPrimitiveValue().toCastValue(Boolean.class));
		} catch (EdmPrimitiveTypeException | NullPointerException e) {
			logger.log(ApiMonitorMessage.FILE_ELEMENT_MISSING, file.toString(), "Retransfer");
			return null;
		}
		if (logger.isTraceEnabled())
			logger.trace("... retransfer = {}", tf.getRetransfer());

		return tf;
	}

	/**
	 * Retrieve the CADU (DSDB) files for a given session
	 *
	 * @param transferSession the session to check
	 * @return a list of available CADU files (may be empty)
	 * @throws IOException if a failure occurs during the retrieval
	 */
	private List<TransferFile> retrieveSessionFiles(TransferSession transferSession) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> retrieveSessionFiles({})", null == transferSession ? "null" : transferSession.getIdentifier());

		// Prepare OData request
		ODataClient oDataClient = ODataClientFactory.getClient();
		oDataClient.getConfiguration().setDefaultPubFormat(ContentType.APPLICATION_JSON);

		String oDataServiceRoot = config.getCadipBaseUri() + "/"
				+ (config.getCadipContext().isBlank() ? "" : config.getCadipContext() + "/") + "odata/v1";
		String authorizationHeader = config.getCadipUseToken() ? "Bearer " + getBearerToken()
				: "Basic " + Base64.getEncoder().encode((config.getCadipUser() + ":" + config.getCadipPassword()).getBytes());

		// Create query filter
		StringBuilder queryFilter = new StringBuilder("SessionId eq '");
		queryFilter.append(transferSession.getSessionIdentifier()).append("'");
		queryFilter.append(" and Retransfer eq ").append(config.isCadipRetransfer());

		// Retrieve session files
		if (logger.isTraceEnabled())
			logger.trace("... requesting session file list at URL '{}'", oDataServiceRoot);
		ODataEntitySetRequest<ClientEntitySet> request = oDataClient.getRetrieveRequestFactory()
			.getEntitySetRequest(oDataClient.newURIBuilder(oDataServiceRoot)
				.appendEntitySetSegment("Files")
				.addQueryOption(QueryOption.FILTER, queryFilter.toString())
				.addQueryOption(QueryOption.COUNT, "true")
				.top(MAX_ENTRY_COUNT)
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
			throw new IOException(logger.log(ApiMonitorMessage.ODATA_REQUEST_ABORTED, transferSession.getIdentifier(),
					e1.getClass().getName(), e1.getMessage()));
		}

		if (HttpStatus.OK.value() != response.getStatusCode()) {
			String message = null;
			try {
				message = logger.log(ApiMonitorMessage.ODATA_SESSION_REQ_FAILED, transferSession.getIdentifier(),
						response.getStatusCode(), new String(response.getRawResponse().readAllBytes()));
			} catch (IOException e) {
				message = logger.log(ApiMonitorMessage.ODATA_RESPONSE_UNREADABLE);
			}
			throw new IOException(message);
		}

		ClientEntitySet entitySet = response.getBody();
		logger.log(ApiMonitorMessage.SESSION_RETRIEVAL_RESULT, entitySet.getEntities().size(), entitySet.getCount());

		// Extract transfer file metadata
		List<TransferFile> result = new ArrayList<>();

		for (ClientEntity clientEntity : entitySet.getEntities()) {
			TransferFile tf = extractTransferFile(clientEntity);
			if (null == tf) {
				// Already logged
				throw new IOException();
			}
			result.add(tf);

			if (tf.getPublicationTime().isAfter(transferSession.getLatestPublicationTime())) {
				transferSession.setLatestPublicationTime(tf.getPublicationTime());
			}
		}

		return result;
	}

	/**
	 * Download a single CADU (DSDB) file from the CADIP
	 *
	 * @param transferFile   metadata for the file to download
	 * @param caduFile file path in the session directory to download to
	 * @throws IOException if any failure occurs during the download
	 */
	private void downloadCaduFile(TransferFile transferFile, File caduFile) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadCaduFile({}, {})", null == transferFile ? "null" : transferFile.getFilename(),
					caduFile);

		// Create retrieval request
		String requestUri = (config.getCadipContext().isBlank() ? "" : config.getCadipContext() + "/") + "odata/v1/" + "Files("
				+ transferFile.getUuid() + ")" + "/$value";

		final StringBuilder bearerToken = new StringBuilder();

		HttpClient httpClient = HttpClient.create().secure().headers(httpHeaders -> {
			if (config.getCadipUseToken()) {
				httpHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + (bearerToken.append(getBearerToken()).toString()));
			} else {
				httpHeaders.add(HttpHeaders.AUTHORIZATION, "Basic "
						+ Base64.getEncoder().encodeToString((config.getCadipUser() + ":" + config.getCadipPassword()).getBytes()));
			}
		}).followRedirect((request, response) -> {
			if (logger.isTraceEnabled())
				logger.trace("... checking redirect for response status code {}", response.status());
			switch (response.status().code()) {
			case 301:
			case 302:
			case 307:
			case 308:
				if (null == response.responseHeaders().get(HttpHeaders.LOCATION)) {
					return false;
				}
				return true;
			default:
				return false;
			}
		}, redirectRequest -> {
			// Send authorization header, except if target is S3 (credential token already given in URL)
			if (!redirectRequest.uri().contains(S3_CREDENTIAL_PARAM)) {
				if (logger.isTraceEnabled())
					logger.trace("... adding redirect credentials to request header");
				if (config.getCadipUseToken()) {
					redirectRequest.requestHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken.toString());
				} else {
					redirectRequest.requestHeaders()
						.add(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder()
							.encodeToString((config.getCadipUser() + ":" + config.getCadipPassword()).getBytes()));
				}
			}
		}).doAfterResponseSuccess((response, connection) -> {
			if (logger.isTraceEnabled()) {
				logger.trace("... response code: {}", response.status());
				logger.trace("... response redirections: {}", Arrays.asList(response.redirectedFrom()));
				logger.trace("... response headers: {}", response.responseHeaders());
			}
		})
		// .wiretap(logger.isDebugEnabled())
		;

		WebClient webClient = WebClient.builder()
			.baseUrl(config.getCadipBaseUri())
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.build();

		// Retrieve and store product file
		Instant downloadStart = Instant.now();

		if (logger.isTraceEnabled())
			logger.trace("... starting request for URL '{}'", requestUri);

		try (FileOutputStream fileOutputStream = new FileOutputStream(caduFile)) {

			Mono<byte[]> dataBuffer = webClient.get()
				.uri(requestUri)
				.accept(MediaType.APPLICATION_OCTET_STREAM)
				.retrieve()
				.bodyToMono(byte[].class);

			if (logger.isTraceEnabled())
				logger.trace("... after webClient...bodyToMono()");

			byte[] buffer = dataBuffer.block();

			if (logger.isTraceEnabled())
				logger.trace("... got buffer of size {}", buffer.length);

			fileOutputStream.write(buffer);

			if (logger.isTraceEnabled())
				logger.trace("... buffer written to file {}", caduFile);

		} catch (FileNotFoundException e) {
			throw new IOException(logger.log(ApiMonitorMessage.FILE_NOT_WRITABLE, caduFile.toString(),
					e.getClass().getName() + " / " + e.getMessage()));
		} catch (WebClientResponseException e) {
			throw new IOException(logger.log(ApiMonitorMessage.FILE_DOWNLOAD_FAILED, caduFile.toString(),
					e.getMessage() + " / " + e.getResponseBodyAsString()));
		} catch (Exception e) {
			throw new IOException(logger.log(ApiMonitorMessage.FILE_DOWNLOAD_FAILED, caduFile.toString(),
					e.getClass().getName() + " / " + e.getMessage()));
		}

		// Compare file size with value given by CADIP
		Long caduFileLength = caduFile.length();
		if (!caduFileLength.equals(transferFile.getFileSize().longValue())) {
			throw new IOException(logger.log(ApiMonitorMessage.FILE_SIZE_MISMATCH, caduFile.toString(), transferFile.getFileSize(),
					caduFileLength));
		}

		// Record the performance for files of sufficient size
		if (config.getCadipPerformanceMinSize() < caduFileLength) {
			Duration downloadDuration = Duration.between(downloadStart, Instant.now());
			Double copyPerformance = caduFileLength / // Bytes
					(downloadDuration.toNanos() / 1000000000.0) // seconds (with fraction)
					/ (1024 * 1024); // --> MiB/s

			setLastCopyPerformance(copyPerformance);
		}

	}

	/**
	 * Extract session quality data from an OData "Sessions" response
	 *
	 * @param transferSession the transfer session to update
	 * @param qualityInfo     the quality info data to extract
	 */
	private void extractQualityInfo(TransferSession transferSession, Map<?, ?> qualityInfo) {
		if (logger.isTraceEnabled())
			logger.trace(">>> extractQualityInfo({}, {})", null == transferSession ? "null" : transferSession.getIdentifier(),
					qualityInfo);

		// Identify the channel number
		Integer channel = ((Number) qualityInfo.get("Channel")).intValue();

		// Update quality info for extracted channel
		TransferQualityInfo tqi = new TransferQualityInfo();
		tqi.setChannel(channel);
		tqi.setDeliveryStart(Instant.parse((String) qualityInfo.get("DeliveryStart")));
		tqi.setDeliveryStop(Instant.parse((String) qualityInfo.get("DeliveryStop")));
		tqi.setSessionId((String) qualityInfo.get("SessionId"));
		tqi.setTotalChunks(((Number) qualityInfo.get("TotalChunks")).intValue());
		tqi.setTotalVolume(((Number) qualityInfo.get("TotalVolume")).longValue());

		transferSession.getQualityInfos()[channel - 1] = tqi;
		transferSession.setDownlinkSize(transferSession.getDownlinkSize() + tqi.getTotalVolume());
	}

	/**
	 * Retrieve the quality information for all channels of the given transfer session
	 *
	 * @param transferSession the transfer session to use and update
	 * @throws IOException if an error occurs during request execution
	 */
	private void retrieveQualityInfo(TransferSession transferSession) throws IOException {
		if (logger.isTraceEnabled())
			logger.trace(">>> retrieveQualityInfo({})", null == transferSession ? "null" : transferSession.getIdentifier());

		// Prepare OData request
		ODataClient oDataClient = ODataClientFactory.getClient();
		oDataClient.getConfiguration().setDefaultPubFormat(ContentType.APPLICATION_JSON);

		String oDataServiceRoot = config.getCadipBaseUri() + "/"
				+ (config.getCadipContext().isBlank() ? "" : config.getCadipContext() + "/") + "odata/v1";
		String authorizationHeader = config.getCadipUseToken() ? "Bearer " + getBearerToken()
				: "Basic " + Base64.getEncoder().encode((config.getCadipUser() + ":" + config.getCadipPassword()).getBytes());

		// Retrieve session files
		if (logger.isTraceEnabled())
			logger.trace("... requesting session file list at URL '{}'", oDataServiceRoot);
		ODataEntityRequest<ClientEntity> request = oDataClient.getRetrieveRequestFactory()
			.getEntityRequest(oDataClient.newURIBuilder(oDataServiceRoot)
				.appendEntitySetSegment("Sessions(" + transferSession.getSessionUuid() + ")")
				.expand("QualityInfo")
				.build());
		request.addCustomHeader(HttpHeaders.AUTHORIZATION, authorizationHeader);
		if (logger.isTraceEnabled())
			logger.trace("... sending OData request '{}'", request.getURI());
		Future<ODataRetrieveResponse<ClientEntity>> futureResponse = request.asyncExecute();
		ODataRetrieveResponse<ClientEntity> response = null;
		try {
			response = futureResponse.get(30, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e1) {
			throw new IOException(logger.log(ApiMonitorMessage.ODATA_REQUEST_ABORTED, transferSession.getIdentifier(),
					e1.getClass().getName(), e1.getMessage()));
		}

		if (HttpStatus.OK.value() != response.getStatusCode()) {
			String message = null;
			try {
				message = logger.log(ApiMonitorMessage.ODATA_SESSION_REQ_FAILED, transferSession.getIdentifier(),
						response.getStatusCode(), new String(response.getRawResponse().readAllBytes()));
			} catch (IOException e) {
				message = logger.log(ApiMonitorMessage.ODATA_RESPONSE_UNREADABLE);
				if (logger.isTraceEnabled())
					logger.trace("... stack trace:", e);
			}
			throw new IOException(message);
		}

		ClientEntity session = response.getBody();

		// Update session data with downlink information
		TransferSession newTs = extractTransferSession(session);
		transferSession.setDownlinkStart(newTs.getDownlinkStart());
		transferSession.setDownlinkStop(newTs.getDownlinkStop());
		transferSession.setDeliveryPushOk(newTs.getDeliveryPushOk());

		// Extract quality info metadata
		Collection<?> qualityInfos = session.getProperty("QualityInfo").getValue().asCollection().asJavaCollection();

		transferSession.setDownlinkSize(0L);
		for (Object qualityInfoObject : qualityInfos) {
			if (qualityInfoObject instanceof Map) {
				Map<?, ?> qualityInfo = (Map<?, ?>) qualityInfoObject;
				extractQualityInfo(transferSession, qualityInfo);
			} else {
				logger.debug("qualityInfoObject is not a Map, but a {}", qualityInfoObject.getClass().getName());
			}
		}

	}

	/**
	 * Transfer the session found and its CADU files to the configured CADU target directory for L0 processing
	 *
	 * <ul>
	 * <li>Create empty "done" list</li>
	 * <li>Create session and channel directories</li>
	 * <li>WHILE session is not complete
	 * <ul>
	 * <li>Retrieve file list for session</li>
	 * <li>FOR EACH CADU file in file list and not in "done" list DO IN PARALLEL (up to quota)
	 * <ul>
	 * <li>Download CADU file (check size)</li>
	 * <li>IF CADU file is marked as "final block", set session as completed</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * </li>
	 * <li>Check session quality information</li>
	 * </ul>
	 */
	@Override
	protected boolean transferToTargetDir(TransferObject object) {
		if (logger.isTraceEnabled())
			logger.trace(">>> transferToTargetDir({})", null == object ? "null" : object.getIdentifier());

		if (null == object) {
			logger.log(ApiMonitorMessage.TRANSFER_OBJECT_IS_NULL);
			return false;
		}

		if (object instanceof TransferSession) {

			List<Thread> downloadTasks = new ArrayList<>();
			
			try {
				TransferSession transferSession = (TransferSession) object;

				// Optimistically we assume success (actually: it's an AND condition)
				copySuccess.put(transferSession.getIdentifier(), true);
				
				// Create session and channel directories
				if (!Files.isWritable(caduDirectoryPath)) {
					logger.log(ApiMonitorMessage.TARGET_DIR_NOT_WRITABLE, caduDirectoryPath);
					return false;
				}

				final String sessionDirName = "DCS_" + transferSession.getStationUnitId() + "_"
						+ transferSession.getSessionIdentifier() + "_dat";
				for (int i = 1; i <= transferSession.getNumChannels(); ++i) {
					try {
						Files.createDirectories(caduDirectoryPath.resolve(Path.of(sessionDirName, CHANNEL_PREFIX + i)));
					} catch (Exception e) {
						logger.log(ApiMonitorMessage.CANNOT_CREATE_TARGET_DIR, caduDirectoryPath);
					}
				}

				// Repeat downloads until session is complete or timeout reached
				final Semaphore semaphore = new Semaphore(maxFileDownloadThreads, true);
				if (logger.isDebugEnabled()) logger.debug("... file download semaphore {} created", semaphore);
				
				// Prevent parallel downloads of the same file
				final Set<File> filesInDownload = new HashSet<>();

				Instant downloadTimeout = Instant.now().plusMillis(config.getCadipRetrievalTimeout());

				while (!transferSession.isSessionComplete() && Instant.now().isBefore(downloadTimeout)) {

					// Retrieve file list for session
					List<TransferFile> transferFiles;
					try {
						transferFiles = retrieveSessionFiles(transferSession);
					} catch (IOException e) {
						// Already logged
						killTasks(downloadTasks);
						return false;
					}

					// Fail if the file list is empty
					if (transferFiles.isEmpty()) {
						logger.log(ApiMonitorMessage.FILE_LIST_EMPTY, transferSession.getSessionIdentifier());
						transferSession.setSessionComplete(true);
						break;
					}

					// Download files in parallel, unless in "done" list
					for (TransferFile transferFile : transferFiles) {
						// If CADU file is marked as "final block", set session as completed
						// (check before skipping, may change over time for any given CADU file)
						if (transferFile.isFinalBlock()) {
							transferSession.setSessionComplete(true);
						}

						// Skip files, which have already been downloaded (but count to session download size) or are being downloaded
						final File caduFile = caduDirectoryPath
								.resolve(Path.of(sessionDirName, CHANNEL_PREFIX + transferFile.getChannel(), transferFile.getFilename()))
								.toFile();
						if (filesInDownload.contains(caduFile) 
								|| caduFile.exists() && caduFile.length() == transferFile.getFileSize().longValue()) {
							if (logger.isTraceEnabled())
								logger.trace("... skipping file {} - already (being) downloaded!", transferFile.getFilename());
							if (!filesInDownload.contains(caduFile)) {
								// File was downloaded in earlier download session
								addToSessionDataSize(transferSession.getIdentifier(), transferFile.getFileSize());
								filesInDownload.add(caduFile); // Prevent counting again
							}
							continue;
						}
						filesInDownload.add(caduFile);
						
						// Fail if at least one file has been evicted
						if (Instant.now().isAfter(transferFile.getEvictionTime())) {
							logger.log(ApiMonitorMessage.FILE_EVICTED, transferFile.getFilename());
							transferSession.setSessionComplete(true);
							break;
						}

						// Prepare the download task
						Thread downloadTask = new Thread() {

							@Override
							public void run() {
								// Check whether parallel execution is allowed
								try {
									semaphore.acquire();
									if (logger.isDebugEnabled())
										logger.debug("... file download semaphore {} acquired, {} permits remaining",
												semaphore, semaphore.availablePermits());
								} catch (InterruptedException e) {
									logger.log(ApiMonitorMessage.ABORTING_TASK, e.toString());
									return;
								}

								// Download CADU file (including size check)
								try {
									// Try up to three times with five seconds delay in between
									int maxRetry = 3;
									for (int i = 0; i < maxRetry; i++) {
										try {
											downloadCaduFile(transferFile, caduFile);
											break;
										} catch (Exception e) {
											// Already logged
											if (i < maxRetry - 1) {
												if (logger.isTraceEnabled())
													logger.trace("... download attempt {} of {} failed, waiting 5 s", i + 1,
															maxRetry);
												try {
													Thread.sleep(5000);
												} catch (InterruptedException e1) {
													logger.log(ApiMonitorMessage.ABORTING_TASK, e1.toString());
													throw e;
												}
											} else {
												if (logger.isTraceEnabled())
													logger.trace("... download attempt {} of {} failed", i + 1, maxRetry);
												throw e;
											}
										}
									}

									// Calculate total download size
									addToSessionDataSize(transferSession.getIdentifier(), transferFile.getFileSize());

									// Log download with UUID, file name, size, publication date
									logger.log(ApiMonitorMessage.FILE_TRANSFER_COMPLETED, transferSession.getIdentifier(),
											transferFile.getFilename(), transferFile.getFileSize(),
											transferFile.getPublicationTime().toString());

								} catch (IOException e) {
									// Already logged
									copySuccess.put(transferSession.getIdentifier(), false);
								} finally {
									// Release parallel thread
									semaphore.release();
									if (logger.isDebugEnabled())
										logger.debug("... file download semaphore {} released, {} permits now available",
												semaphore, semaphore.availablePermits());
								}
							}
						};
						downloadTasks.add(downloadTask);

						// Start the download task asynchronically
						downloadTask.start();

					}

					if (!transferSession.isSessionComplete()) {
						// Wait a little before asking for more files
						try {
							if (logger.isTraceEnabled())
								logger.trace("... sleeping for {} ms", config.getCadipSessionInterval());
							Thread.sleep(config.getCadipSessionInterval());
						} catch (InterruptedException e) {
							logger.log(ApiMonitorMessage.DOWNLOAD_INTERRUPTED, transferSession.getSessionIdentifier());
							copySuccess.put(transferSession.getIdentifier(), false);
							break;
						}
					}
				}

				// Check session quality information (esp. no. of chunks and total volume/transfer data size) with retries
				int qualityRetries = 0;
				transferSession.setDownlinkSize(0L);
				while (0L == transferSession.getDownlinkSize() && qualityRetries < maxQualityRetries) {
					++qualityRetries;
					
					retrieveQualityInfo(transferSession);
					
					if (0L == transferSession.getDownlinkSize()) {
						try {
							if (logger.isTraceEnabled())
								logger.trace("... waiting {} ms before next check of quality info", qualityRetryInterval);
							Thread.sleep(qualityRetryInterval);
						} catch (InterruptedException e) {
							logger.log(ApiMonitorMessage.QUALITY_WAIT_INTERRUPTED, transferSession.getSessionIdentifier());
							return false;
						}
					}
				}
				if (0L == transferSession.getDownlinkSize()) {
					logger.log(ApiMonitorMessage.QUALITY_WAIT_TIMEOUT, (maxQualityRetries * qualityRetryInterval) / 1000,
							transferSession.getSessionIdentifier());
				}

				// Start parallel processing action
				boolean parallelActionSuccess = triggerParallelAction(transferSession);
				if (!parallelActionSuccess) {
					// We assume error already logged
					copySuccess.put(transferSession.getIdentifier(), false);
				}

				// Wait for all download subtasks
				if (logger.isTraceEnabled())
					logger.trace("... waiting for all file downloads to complete");
				int k = 0; // Timeout loop is cumulative for all subtasks
				for (Thread downloadTask : downloadTasks) {
					while (downloadTask.isAlive() && k < maxFileWaitCycles) {
						try {
							Thread.sleep(fileWaitInterval);
						} catch (InterruptedException e) {
							logger.log(ApiMonitorMessage.DOWNLOAD_INTERRUPTED, transferSession.getSessionIdentifier());
							copySuccess.put(transferSession.getIdentifier(), false);
							break;
						}
						++k;
					}
					if (k == maxFileWaitCycles && downloadTask.isAlive()) {
						// Timeout reached --> kill download and report error
						logger.log(ApiMonitorMessage.DOWNLOAD_TIMEOUT, (maxFileWaitCycles * fileWaitInterval) / 1000,
								transferSession.getSessionIdentifier());
						downloadTask.interrupt();
						copySuccess.put(transferSession.getIdentifier(), false);
					}
				}

				if (logger.isDebugEnabled()) logger.debug("... file download semaphore status: {}", semaphore);
				
				// Check the total session data size
				if (!transferSession.getDownlinkSize().equals(sessionDataSizes.get(transferSession.getIdentifier()))) {
					logger.log(ApiMonitorMessage.DATA_SIZE_MISMATCH, transferSession.getIdentifier(),
							transferSession.getDownlinkSize(), sessionDataSizes.get(transferSession.getIdentifier()));
					copySuccess.put(transferSession.getIdentifier(), false);
				} else {
					if (logger.isTraceEnabled())
						logger.trace("... total session data size is as expected: " + transferSession.getDownlinkSize());
				}
				transferSession.setDownlinkSize(sessionDataSizes.get(transferSession.getIdentifier()));
				sessionDataSizes.remove(transferSession.getIdentifier());

				// Check whether any copy action failed
				Boolean myCopySuccess = copySuccess.get(transferSession.getIdentifier());
				copySuccess.remove(transferSession.getIdentifier());

				if (logger.isDebugEnabled()) logger.debug("... file download semaphore {} about to be deleted", semaphore);
				
				// Check for timeout
				if (transferSession.isSessionComplete()) {
					logger.log(ApiMonitorMessage.SESSION_TRANSFER_COMPLETED, transferSession.getIdentifier(),
							(myCopySuccess ? "SUCCESS" : "FAILURE"));
					return myCopySuccess;
				} else {
					logger.log(ApiMonitorMessage.SESSION_DOWNLOAD_TIMEOUT, config.getCadipRetrievalTimeout() / 1000,
							transferSession.getIdentifier());
					return false;
				}

			} catch (Exception e) {
				logger.log(GeneralMessage.EXCEPTION_ENCOUNTERED, e.getClass().getName() + " / " + e.getMessage());
				killTasks(downloadTasks);
				return false;
			}

		} else {
			logger.log(ApiMonitorMessage.INVALID_TRANSFER_OBJECT_TYPE, object.getIdentifier());
			return false;
		}
	}

	/**
	 * Trigger any necessary parallel action on the transfer session (e. g. L0 processing)
	 *
	 * @param transferSession the transfer session to start the action on
	 * @return true, if starting the action succeeded (not necessarily the action itself), false otherwise
	 */
	protected boolean triggerParallelAction(TransferSession transferSession) {
		if (logger.isTraceEnabled())
			logger.trace(">>> triggerParallelAction({})", null == transferSession ? "null" : transferSession.getIdentifier());

		if (null == transferSession) {
			logger.log(ApiMonitorMessage.TRANSFER_OBJECT_IS_NULL);
			return false;
		}

		logger.log(ApiMonitorMessage.PARALLEL_ACTION_STARTED, transferSession.getIdentifier(), "NOT IMPLEMENTED");

		return true;
	}

	/*
	 * (non-Javadoc) Trigger follow-on action (dummy implementation, to be overridden by subclass)
	 */
	@Override
	protected boolean triggerFollowOnAction(TransferObject transferObject) {
		if (logger.isTraceEnabled())
			logger.trace(">>> triggerFollowOnAction({})", null == transferObject ? "null" : transferObject.getIdentifier());

		if (null == transferObject) {
			logger.log(ApiMonitorMessage.TRANSFER_OBJECT_IS_NULL);
			return false;
		}

		if (!(transferObject instanceof TransferSession)) {
			logger.log(ApiMonitorMessage.INVALID_TRANSFER_OBJECT_TYPE, transferObject.getIdentifier());
			return false;
		}

		TransferSession transferSession = (TransferSession) transferObject;

		logger.log(ApiMonitorMessage.FOLLOW_ON_ACTION_STARTED, transferSession.getIdentifier(), "NOT IMPLEMENTED");

		return true;
	}

	/**
	 * Check the CADIP status (by querying all available sessions)
	 * 
	 * @return true, if the CADIP is reachable and has session data, false otherwise
	 */
	public boolean checkStatus() {
		if (logger.isTraceEnabled()) logger.trace(">>> checkStatus()");
		
		boolean isCadipOk = false;
		
		try {
			TransferControl tc = checkAvailableSessions(Instant.ofEpochSecond(0), 1 /* just test the interface */);
			isCadipOk = !tc.transferObjects.isEmpty();
		} catch (Exception e) {
			// Never mind
		}
		
		return isCadipOk;
	}
}