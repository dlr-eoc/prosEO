/**
 * Metrics.java
 *
 * © 2024 Prophos Informatik GmbH
 */
package de.dlr.proseo.monitor.apimetrics;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.sql.Timestamp;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.model.Mission;
import de.dlr.proseo.model.ProductClass;
import de.dlr.proseo.model.enums.MetricType;
import de.dlr.proseo.model.enums.ProductionType;
import de.dlr.proseo.model.service.RepositoryService;
import de.dlr.proseo.model.util.ProseoUtil;
import de.dlr.proseo.monitor.MonitorConfiguration;
import de.dlr.proseo.model.ApiMetrics;

/**
 * This class contains the functions to calculate each metric
 *  
 * @author Ernst Melchinger
 *
 */
@Component
public class Metrics {

	/** Logger of this class */
	private static ProseoLogger logger = new ProseoLogger(Metrics.class);

	/** String constants */
	public static final String N_SIZE = "size";
	public static final String N_COUNT = "count";
	public static final String N_FAILED = "failed";
	public static final String N_COMPLETED = "completed";
	public static final String N_SENSING_TO_PUBLICATION = "SensingToPublication";
	public static final String N_ORIGIN_TO_PUBLICATION = "OriginToPublication";
	public static final String N_SUBMISSION_TO_COMPLETION = "SubmissionToCompletion";
	public static final String N_DOWNLOAD = "Download";
	public static final String N_DAILY = "Daily";
	public static final String N_MONTHLY = "Monthly";
	public static final String N_MIN = "min";
	public static final String N_MAX = "max";
	public static final String N_AVG = "avg";
	public static final String N_TIME = "time";

	/** The static monitor configuration */
	private MonitorConfiguration config;
	
	/** Transaction manager for transaction control */
	private PlatformTransactionManager txManager;
	
	/** JPA entity manager */
	private EntityManager em;

	
	/**
	 * Method to format a duration
	 * 
	 * @param duration The duration to format
	 * @return The formatted string
	 */
	private static String formatDuration(Duration duration) {
		return String.valueOf(duration.getSeconds());
	}
	
	
	/**
	 * Transform an object to a long value (if possible).
	 * 
	 * @param o The object to transform
	 * @return The long value contained in object, otherwise 0
	 */
	private static long toLong(Object o) {
		if (o instanceof BigDecimal) {
			return ((BigDecimal) o).longValue();
		} else if (o instanceof BigInteger) {
			return ((BigInteger) o).longValue();
		} else if (o instanceof Long) {
			return ((Long) o);
		}
		return 0;
	}
	
	
	/**
	 * The date time formatter used.
	 */
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd' 'HH:mm:ss.SSS")
		.withZone(ZoneId.of("UTC"));

	/**
	 * Constructor of a Metric instance
	 * 
	 * @param config The monitor configuration
	 * @param txManager The transaction manager
	 * @param em The JPA entity manager
	 */
	public Metrics(MonitorConfiguration config, PlatformTransactionManager txManager, EntityManager em) {
		this.config = config;
		this.txManager = txManager;
		this.em = em;
	}
	
	/**
	 * Get the name of the mission
	 * 
	 * @param mission The mission
	 * @return The name
	 */
	private String getPlatformShortName(Mission mission) {
		return mission.getName();
	}

	/**
	 * Get a string parameter of a product.
	 * 
	 * @param productId The product id
	 * @param key The parameter key
	 * @param defaultValue The default value if parameter don't exist
	 * @return The parameter or default value prefixed by "."
	 */
	private String getStringParameterWithPoint(Long productId, String key, String defaultValue) {
		String paramSql = "SELECT par.parameter_value FROM product_parameters par "
				+ " WHERE par.product_id = " + productId 
				+ " AND par.parameters_key = '" + key + "';";

		Query queryParam = em.createNativeQuery(paramSql);
		Object paramObj = null;
		try {
			paramObj = queryParam.getSingleResult();
		} catch (NoResultException ex) {
			// do nothing
		}
		String value = defaultValue;
		if (paramObj != null && paramObj instanceof String) {
			value = "." + (String)paramObj;
		}
		return value;
	}

	
	/**
	 * Update a count metric.
	 * 
	 * @param key The metric key
	 * @param suffix The key suffix (appended to the key with divider ".")
	 * @param map The map containing the value
	 * @param now The current date and time
	 */
	private void updateCountMetric(String key, String suffix, Map<String, Long> map, Instant now) {
		String name = key + "." + suffix;
		ApiMetrics metric = RepositoryService.getApiMetricsRepository().findLastEntryByName(name);
		if (metric == null) {
			metric = new ApiMetrics();
			metric.setName(name);
			metric.setMetrictype(MetricType.COUNTER);
		}
		metric.setCount(map.get(key));
		metric.setTimestamp(now);
		RepositoryService.getApiMetricsRepository().save(metric);
	}
	
	/**
	 * Update a count metric.
	 * 
	 * @param key The metric key
	 * @param value The new value
	 * @param now The current date and time
	 */
	private void updateCountMetric(String key, long value, Instant now) {
		String name = key;
		ApiMetrics metric = RepositoryService.getApiMetricsRepository().findLastEntryByName(name);
		if (metric == null) {
			metric = new ApiMetrics();
			metric.setName(name);
			metric.setMetrictype(MetricType.COUNTER);
		}
		metric.setCount(value);
		metric.setTimestamp(now);
		RepositoryService.getApiMetricsRepository().save(metric);
	}

	/**
	 * Update a gauge metric.
	 * 
	 * @param key The metric key
	 * @param value The new value
	 * @param now The current date and time
	 */
	private void updateGaugeMetric(String key, String value, Instant now) {
		String name = key;
		ApiMetrics metric = RepositoryService.getApiMetricsRepository().findLastEntryByName(name);
		if (metric == null) {
			metric = new ApiMetrics();
			metric.setName(name);
			metric.setMetrictype(MetricType.GAUGE);
		}
		metric.setGauge(value);
		metric.setTimestamp(now);
		RepositoryService.getApiMetricsRepository().save(metric);
	}

	/**
	 * Get the product classes to use for metrics. 
	 * Only classes without components are used. If a ProductTypeRegex is defined in configuration, 
	 * the type name has to match these regular expression too. 
	 * 
	 * @param transactionTemplate The transaction template 
	 * @param mission The mission of the product classes.
	 * @return A list of product classes
	 */
	private List<ProductClass> getProductClasses(TransactionTemplate transactionTemplate, Mission mission) {
	transactionTemplate.setReadOnly(true);
	return transactionTemplate.execute((status) -> {
		List<ProductClass> classes = RepositoryService.getProductClassRepository().findByMissionCode(mission.getCode());
		List<ProductClass> result = new ArrayList<ProductClass>();
		for (ProductClass pc : classes) {
			if (pc.getComponentClasses() != null || pc.getComponentClasses().isEmpty()) {
				if (config.getProductTypeRegex() == null 
						|| config.getProductTypeRegex().isBlank() 
						|| pc.getProductType().matches(config.getProductTypeRegex())) {
					result.add(pc);
				}
			}
		}
		return result;
	});
	}
	
	/*
	 * Cumulative volume of <productType> by <productionType> of mission platform
	 * (<platformShortName>.<platformSerialIdentifier>) produced in Bytes
	 * 
	 * One entry for the complete time period 
	 */
	public void producedBytesAndCountForType() {
		if (!config.getProducedBytesAndCountForType()) return;
		if (logger.isTraceEnabled())
			logger.trace(">>> producedBytesAndCountForType()");
		// Iterate over productTypes, productionTypes, platformShortNames and platformSerialIdentifier
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

			transactionTemplate.setReadOnly(true);
			List<Mission> missions = transactionTemplate.execute((status) -> {
				return RepositoryService.getMissionRepository().findAll();
			});
			for (Mission mission : missions) {
				for (ProductionType productionType : ProductionType.values()) {
					
					for (ProductClass productClass : getProductClasses(transactionTemplate, mission)) {
						// only for processed products
						if (productClass.getProcessingLevel() != null) {
							String baseName = productionType.toString() + "." + productClass.getProductType();

							// Find the entry
							transactionTemplate.setReadOnly(false);
							for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
								try {
									Map<String, Long> sizeMap = new HashMap<String, Long>();
									Map<String, Long> countMap = new HashMap<String, Long>();
									transactionTemplate.execute((status) -> {
										// calculate metric
										// TODO optimize, at the moment always complete calculation
										// find product files	
										String sqlQuery;
										sqlQuery = "SELECT p.id, f.file_size FROM product p "
												+ " JOIN product_file f ON f.product_id = p.id "
												+ " WHERE p.product_class_id = " + productClass.getId() 
												+ " AND p.production_type = '" + productionType.name() + "';";	

										Query query = em.createNativeQuery(sqlQuery);
										Object oList = query.getResultList();
										if (oList instanceof ArrayList) {
											@SuppressWarnings("unchecked")
											ArrayList<Object> list = (ArrayList<Object>)oList;
											for (Object ele : list) {
												if (ele instanceof Object[]) {
													Object[] arrayEle = (Object[])ele;
													if (arrayEle.length == 2) {
														long pid = toLong(arrayEle[0]);
														long size = toLong(arrayEle[1]);
														// look for platform info in product parameters
														String shortName = getStringParameterWithPoint(pid, "platformShortName", "." + getPlatformShortName(mission));
														String identifier = getStringParameterWithPoint(pid, "platformSerialIdentifier", "");
														
														String entryName = baseName + shortName + identifier;
														if (sizeMap.containsKey(entryName)) {
															sizeMap.replace(entryName, sizeMap.get(entryName) + size);
															countMap.replace(entryName, countMap.get(entryName) + 1);
														} else {
															sizeMap.put(entryName, size);
															countMap.put(entryName, (long) 1);
														}
													}
												}
											}
										}
										Instant now = Instant.now();
										for (String key : sizeMap.keySet()) {
											updateCountMetric(key, N_SIZE, sizeMap, now);
											updateCountMetric(key, N_COUNT, countMap, now);
										}
										return null;
									});
									break;

								} catch (CannotAcquireLockException e) {
									if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

									if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
										ProseoUtil.dbWait();
									} else {
										if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
										throw e;
									}
								}	
							}
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Cumulative volume of data produced by <productionType> for mission platform 
	 * (<platformShortName>.<platformSerialIdentifier>) in Bytes
	 * 
	 * One entry for the complete time period 
	 */
	public void producedBytesAndCount() {
		if (!config.getProducedBytesAndCount()) return;
		if (logger.isTraceEnabled())
			logger.trace(">>> producedBytesAndCount()");
		// Iterate over productTypes, productionTypes, platformShortNames and platformSerialIdentifier
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

			transactionTemplate.setReadOnly(true);
			List<Mission> missions = transactionTemplate.execute((status) -> {
				return RepositoryService.getMissionRepository().findAll();
			});
			for (Mission mission : missions) {
				for (ProductionType productionType : ProductionType.values()) {
					String baseName = productionType.toString();
					// Find the entry
					transactionTemplate.setReadOnly(false);
					for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
						try {
							Map<String, Long> sizeMap = new HashMap<String, Long>();
							Map<String, Long> countMap = new HashMap<String, Long>();
							Map<String, Long> compMap = new HashMap<String, Long>();
							transactionTemplate.execute((status) -> {
								// calculate metric
								// TODO optimize, at the moment always complete calculation
								// find product files	
								String sqlQuery;
								sqlQuery = "SELECT p.id, f.file_size FROM product p" 
										+ " JOIN product_file f ON f.product_id = p.id " 
										+ " JOIN product_class pc ON p.product_class_id = pc.id "
										+ " WHERE pc.processing_level IS NOT NULL"
										+ " AND p.production_type = '" + productionType.name() + "';";	
								Query query = em.createNativeQuery(sqlQuery);
								Object sizeObjList = query.getResultList();

								if (sizeObjList instanceof ArrayList) {
									@SuppressWarnings("unchecked")
									ArrayList<Object> list = (ArrayList<Object>)sizeObjList;
									for (Object ele : list) {
										if (ele instanceof Object[]) {
											Object[] arrayEle = (Object[])ele;
											if (arrayEle.length == 2) {
												long pid = toLong(arrayEle[0]);
												long size = toLong(arrayEle[1]);
												// look for platform info in product parameters
												String shortName = getStringParameterWithPoint(pid, "platformShortName", "." + getPlatformShortName(mission));
												String identifier = getStringParameterWithPoint(pid, "platformSerialIdentifier", "");
												
												String entryName = baseName + shortName + identifier;
												if (sizeMap.containsKey(entryName)) {
													sizeMap.replace(entryName, sizeMap.get(entryName) + size);
													countMap.replace(entryName, countMap.get(entryName) + 1);
												} else {
													sizeMap.put(entryName, size);
													countMap.put(entryName, (long) 1);
												}
											}
										}
									}
								}
								sqlQuery = "SELECT p.id FROM product p" 
										+ " JOIN product_file f ON f.product_id = p.id " 
										+ " JOIN product_class pc ON p.product_class_id = pc.id "
										+ " WHERE pc.processing_level IS NOT NULL"
										+ " AND p.generation_time IS NOT NULL"
										+ " AND p.production_type = '" + productionType.name() + "';";	
								Query queryComp = em.createNativeQuery(sqlQuery);
								Object compObjList = queryComp.getResultList();

								if (compObjList instanceof ArrayList) {
									@SuppressWarnings("unchecked")
									ArrayList<Object> list = (ArrayList<Object>)sizeObjList;
									for (Object ele : list) {
										if (ele instanceof Object[]) {
											Object[] arrayEle = (Object[])ele;
											if (arrayEle.length == 2) {
												long pid = toLong(arrayEle[0]);
												// look for platform info in product parameters
												String shortName = getStringParameterWithPoint(pid, "platformShortName", "." + getPlatformShortName(mission));
												String identifier = getStringParameterWithPoint(pid, "platformSerialIdentifier", "");
												
												String entryName = baseName + shortName + identifier;
												if (sizeMap.containsKey(entryName)) {
													compMap.replace(entryName, countMap.get(entryName) + 1);
												} else {
													compMap.put(entryName, (long) 1);
												}
											}
										}
									}
								}
								

								Instant now = Instant.now();
								for (String key : sizeMap.keySet()) {
									updateCountMetric(key, N_SIZE, sizeMap, now);
								}
								for (String key : compMap.keySet()) {
									updateCountMetric(key, N_COMPLETED, compMap, now);
								}
								return null;
							});
							break;

						} catch (CannotAcquireLockException e) {
							if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

							if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
								ProseoUtil.dbWait();
							} else {
								if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
								throw e;
							}
						}	
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Cumulative size of <productType> by <productionType> of mission platform 
	 * (<platformShortName>.<platformSerialIdentifier>) downloaded (by <ServiceAlias>) in Bytes
	 * 
	 * One entry for the complete time period 
	 */
	public void downloadSize() {
		if (!config.getDownload()) return;
		if (logger.isTraceEnabled())
			logger.trace(">>> downloadSize()");
		// Iterate over productTypes, productionTypes, platformShortNames and platformSerialIdentifier
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

			transactionTemplate.setReadOnly(true);
			List<Mission> missions = transactionTemplate.execute((status) -> {
				return RepositoryService.getMissionRepository().findAll();
			});
			for (Mission mission : missions) {
				for (ProductClass productClass : getProductClasses(transactionTemplate, mission)) {
					// only for processed products
					if (productClass.getProcessingLevel() != null) {
						String nameSize = N_DOWNLOAD + "." + productClass.getProductType() + "."
								+ getPlatformShortName(mission) + "." + N_SIZE;
						String nameCount = N_DOWNLOAD + "." + productClass.getProductType() + "."
								+ getPlatformShortName(mission) + "." + N_COUNT;
						// Find the entry
						transactionTemplate.setReadOnly(false);
						for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
							try {
								transactionTemplate.execute((status) -> {
									ApiMetrics metricSize = RepositoryService.getApiMetricsRepository().findLastEntryByName(nameSize);
									if (metricSize == null) {
										metricSize = new ApiMetrics();
										metricSize.setName(nameSize);
										metricSize.setMetrictype(MetricType.COUNTER);
									}
									ApiMetrics metricCount = RepositoryService.getApiMetricsRepository().findLastEntryByName(nameCount);
									if (metricCount == null) {
										metricCount = new ApiMetrics();
										metricCount.setName(nameCount);
										metricCount.setMetrictype(MetricType.COUNTER);
									}
									// calculate metric
									// TODO optimize, at the moment always complete calculation
									// find product files	
									String sqlQuery;
									sqlQuery = "SELECT SUM(pdh.product_file_size), COUNT(pdh) FROM product_download_history pdh "
											+ " JOIN product p ON pdh.product_id = p.id "
											+ " WHERE p.product_class_id = " + productClass.getId() + ";";	

									Query query = em.createNativeQuery(sqlQuery);
									Object o = query.getSingleResult();
									long size = (long)0;
									long count = (long)0;
									if (o instanceof Object[]) {
										Object[] array = (Object[])o;
										if (array.length == 2) {
											size = toLong(array[0]);
											count = toLong(array[1]);
										}
									}
									Instant now = Instant.now();
									if (count > 0) {
										updateCountMetric(nameSize, size, now);
										updateCountMetric(nameCount, count, now);
									}
									return null;
								});
								break;

							} catch (CannotAcquireLockException e) {
								if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

								if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
									ProseoUtil.dbWait();
								} else {
									if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
									throw e;
								}
							}	
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Daily/monthly minimum, maximum and average time difference in seconds between sensing time (ContentDate/End) and PRIP PublicationDate of <productType> 
	 * of mission platform (<platformShortName>.<platformSerialIdentifier>) (sliding window of 24 hours/1 month). 
	 * This is only applicable to the Systematic Production.
	 * 
	 * @param period The period to retrieve
	 */
	public void sensingToPublication(Duration period) {
		if (!config.getSensingToPublication()) return;
		if (logger.isTraceEnabled())
			logger.trace(">>> sensingToPublication({})", period.toString());
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

			transactionTemplate.setReadOnly(true);
			List<Mission> missions = transactionTemplate.execute((status) -> {
				return RepositoryService.getMissionRepository().findAll();
			});
			for (Mission mission : missions) {
				for (ProductClass productClass : getProductClasses(transactionTemplate, mission)) {
					// only for processed products
					if (productClass.getProcessingLevel() != null) {
						String periodString = N_MONTHLY;
						if (period.equals(Duration.ofDays(1))) {
							periodString = N_DAILY;
						}
						String nameMinBase = N_SENSING_TO_PUBLICATION + "." + periodString + "." + N_MIN + "." + N_TIME 
								+ "." + productClass.getProductType();
						String nameMaxBase = N_SENSING_TO_PUBLICATION + "." + periodString + "." + N_MAX + "." + N_TIME 
								+ "." + productClass.getProductType();
						String nameAvgBase = N_SENSING_TO_PUBLICATION + "." + periodString + "." + N_AVG + "." + N_TIME 
								+ "." + productClass.getProductType();
						// Find the entry
						transactionTemplate.setReadOnly(false);
						for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
							try {
								Map<String, Duration> minMap = new HashMap<String, Duration>();
								Map<String, Duration> maxMap = new HashMap<String, Duration>();
								Map<String, Duration> sumMap = new HashMap<String, Duration>();
								Map<String, Duration> avgMap = new HashMap<String, Duration>();
								Map<String, Long> countMap = new HashMap<String, Long>();
								transactionTemplate.execute((status) -> {
									// calculate metric
									// TODO optimize, at the moment always complete calculation
									// find product files
									Instant now = Instant.now();
									Instant pubTime = now.minus(period);
									String sqlQuery;
									sqlQuery = "SELECT p.id, p.publication_time, p.sensing_stop_time FROM product p "
													+ " WHERE p.product_class_id = " + productClass.getId()
													+ " AND p.publication_time IS NOT NULL"
													+ " AND p.publication_time > '" + dateTimeFormatter.format(pubTime) + "';";	

									Query query = em.createNativeQuery(sqlQuery);
									Object oList = query.getResultList();
									Duration min = null;
									Duration max = null;
									if (oList instanceof ArrayList) {
										@SuppressWarnings("unchecked")
										ArrayList<Object> list = (ArrayList<Object>)oList;
										for (Object ele : list) {
											if (ele instanceof Object[]) {
												Object[] arrayEle = (Object[])ele;
												if (arrayEle.length == 3) {
													// Tuple with publication_time and sensing_stop_time
													long pid = toLong(arrayEle[0]);
													String shortName = getStringParameterWithPoint(pid, "platformShortName", "." + getPlatformShortName(mission));
													String identifier = getStringParameterWithPoint(pid, "platformSerialIdentifier", "");
													String nameMin = nameMinBase + shortName + identifier;
													String nameMax = nameMaxBase + shortName + identifier;
													String nameAvg = nameAvgBase + shortName + identifier; // also used for sum and count
													if (sumMap.get(nameAvg) == null) {
														sumMap.put(nameAvg, Duration.ofMillis(0));
													}
													if (countMap.get(nameAvg) == null) {
														countMap.put(nameAvg, (long)0);
													}
													Duration diff = Duration.between((((Timestamp)arrayEle[2]).toInstant()), 
															(((Timestamp)arrayEle[1]).toInstant()));
													sumMap.replace(nameAvg, sumMap.get(nameAvg).plus(diff));
													countMap.replace(nameAvg, countMap.get(nameAvg) + 1);
													if (minMap.get(nameMin) == null) {
														minMap.put(nameMin, diff);
													} else {
														min = minMap.get(nameMin);
														if (min.getSeconds() > diff.getSeconds()) {
															minMap.replace(nameMin, diff);
														} else if (min.getSeconds() == diff.getSeconds()) {
															 if (min.getNano() > diff.getNano()) {
																minMap.replace(nameMin, diff);
															}
														}
													}							
													if (maxMap.get(nameMax) == null) {
														maxMap.put(nameMax, diff);
													} else {
														max = maxMap.get(nameMax);
														if (max.getSeconds() < diff.getSeconds()) {
															maxMap.replace(nameMax, diff);
														} else if (max.getSeconds() == diff.getSeconds()) {
															 if (max.getNano() < diff.getNano()) {
																maxMap.replace(nameMax, diff);
															}
														}
													}													
												}
											}
										}
									}
									for (String key : sumMap.keySet()) {
										if (countMap.get(key) > 0) {
											avgMap.put(key, sumMap.get(key).dividedBy(countMap.get(key)));
											updateGaugeMetric(key, formatDuration(avgMap.get(key)), now);
										}
									}
									for (String key : minMap.keySet()) {
										if (minMap.get(key) != null) {
											updateGaugeMetric(key, formatDuration(minMap.get(key)), now);
										}
									}
									for (String key : maxMap.keySet()) {
										if (maxMap.get(key) != null) {
											updateGaugeMetric(key, formatDuration(maxMap.get(key)), now);
										}
									}
									return null;
								});
								break;

							} catch (CannotAcquireLockException e) {
								if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

								if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
									ProseoUtil.dbWait();
								} else {
									if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
									throw e;
								}
							}	
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Daily/monthly minimum, maximum and average time difference in seconds between OriginDate and PRIP PublicationDate of <productType> 
	 * of mission platform (<platformShortName>.<platformSerialIdentifier>) (sliding window of 24 hours/1 month). 
	 * This is only applicable to the Systematic Production.
	 * 
	 * @param period The period to retrieve
	 */
	public void originToPublication(Duration period) {
		if (!config.getOriginToPublication()) return;
		if (logger.isTraceEnabled())
			logger.trace(">>> originToPublication({})", period.toString());
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

			transactionTemplate.setReadOnly(true);
			List<Mission> missions = transactionTemplate.execute((status) -> {
				return RepositoryService.getMissionRepository().findAll();
			});
			for (Mission mission : missions) {
				for (ProductClass productClass : getProductClasses(transactionTemplate, mission)) {
					// only for processed products
					if (productClass.getProcessingLevel() != null) {
						String periodString = N_MONTHLY;
						if (period.equals(Duration.ofDays(1))) {
							periodString = N_DAILY;
						}
						String nameMinBase = N_ORIGIN_TO_PUBLICATION + "." + periodString + "." + N_MIN + "." + N_TIME 
								+ "." + productClass.getProductType();
						String nameMaxBase = N_ORIGIN_TO_PUBLICATION + "." + periodString + "." + N_MAX + "." + N_TIME 
								+ "." + productClass.getProductType();
						String nameAvgBase = N_ORIGIN_TO_PUBLICATION + "." + periodString + "." + N_AVG + "." + N_TIME 
								+ "." + productClass.getProductType();
						
						// Find the entry
						transactionTemplate.setReadOnly(false);
						for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
							try {
								transactionTemplate.execute((status) -> {
									Map<String, Duration> minMap = new HashMap<String, Duration>();
									Map<String, Duration> maxMap = new HashMap<String, Duration>();
									Map<String, Duration> sumMap = new HashMap<String, Duration>();
									Map<String, Duration> avgMap = new HashMap<String, Duration>();
									Map<String, Long> countMap = new HashMap<String, Long>();
									// calculate metric
									// TODO optimize, at the moment always complete calculation
									// find product files
									Instant now = Instant.now();
									Instant pubTime = now.minus(period);
									String sqlQuery;
									sqlQuery = "SELECT p.id, p.publication_time, p.raw_data_availability_time FROM product p "
											+ " WHERE p.product_class_id = " + productClass.getId()
											+ " AND p.publication_time IS NOT NULL"
											+ " AND p.raw_data_availability_time IS NOT NULL"
											+ " AND p.publication_time > '" + dateTimeFormatter.format(pubTime) + "';";	

									Query query = em.createNativeQuery(sqlQuery);
									Object oList = query.getResultList();
									Duration min = null;
									Duration max = null;
									if (oList instanceof ArrayList) {
										@SuppressWarnings("unchecked")
										ArrayList<Object> list = (ArrayList<Object>)oList;
										for (Object ele : list) {
											if (ele instanceof Object[]) {
												Object[] arrayEle = (Object[])ele;
												if (arrayEle.length == 3) {
													long pid = toLong(arrayEle[0]);
													String shortName = getStringParameterWithPoint(pid, "platformShortName", "." + getPlatformShortName(mission));
													String identifier = getStringParameterWithPoint(pid, "platformSerialIdentifier", "");
													String nameMin = nameMinBase + shortName + identifier;
													String nameMax = nameMaxBase + shortName + identifier;
													String nameAvg = nameAvgBase + shortName + identifier; // also used for sum and count
													if (sumMap.get(nameAvg) == null) {
														sumMap.put(nameAvg, Duration.ofMillis(0));
													}
													if (countMap.get(nameAvg) == null) {
														countMap.put(nameAvg, (long)0);
													}
													Duration diff = Duration.between((((Timestamp)arrayEle[2]).toInstant()), 
															(((Timestamp)arrayEle[1]).toInstant()));
													sumMap.replace(nameAvg, sumMap.get(nameAvg).plus(diff));
													countMap.replace(nameAvg, countMap.get(nameAvg) + 1);
													if (minMap.get(nameMin) == null) {
														minMap.put(nameMin, diff);
													} else {
														min = minMap.get(nameMin);
														if (min.getSeconds() > diff.getSeconds()) {
															minMap.replace(nameMin, diff);
														} else if (min.getSeconds() == diff.getSeconds()) {
															 if (min.getNano() > diff.getNano()) {
																minMap.replace(nameMin, diff);
															}
														}
													}							
													if (maxMap.get(nameMax) == null) {
														maxMap.put(nameMax, diff);
													} else {
														max = maxMap.get(nameMax);
														if (max.getSeconds() < diff.getSeconds()) {
															maxMap.replace(nameMax, diff);
														} else if (max.getSeconds() == diff.getSeconds()) {
															 if (max.getNano() < diff.getNano()) {
																maxMap.replace(nameMax, diff);
															}
														}
													}													
												}
											}
										}
									}
									for (String key : sumMap.keySet()) {
										if (countMap.get(key) > 0) {
											avgMap.put(key, sumMap.get(key).dividedBy(countMap.get(key)));
											updateGaugeMetric(key, formatDuration(avgMap.get(key)), now);
										}
									}
									for (String key : minMap.keySet()) {
										if (minMap.get(key) != null) {
											updateGaugeMetric(key, formatDuration(minMap.get(key)), now);
										}
									}
									for (String key : maxMap.keySet()) {
										if (maxMap.get(key) != null) {
											updateGaugeMetric(key, formatDuration(maxMap.get(key)), now);
										}
									}
									return null;
								});
								break;

							} catch (CannotAcquireLockException e) {
								if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

								if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
									ProseoUtil.dbWait();
								} else {
									if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
									throw e;
								}
							}	
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Minimum, maximum and average time from ProductionOrder SubmissionDate to CompletedDate on the 
	 * On-Demand PRIP (sliding window of 24 hours/1 month)
	 * 
	 * @param period The period to retrieve
	 */
	public void submissionToCompletedOrder(Duration period) {
		if (!config.getSubmisionToCompletion()) return;
		if (logger.isTraceEnabled())
			logger.trace(">>> submissionToCompletedOrder({})", period.toString());
		try {
			TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

			transactionTemplate.setReadOnly(true);
			List<Mission> missions = transactionTemplate.execute((status) -> {
				return RepositoryService.getMissionRepository().findAll();
			});
			for (Mission mission : missions) {
				for (ProductClass productClass : getProductClasses(transactionTemplate, mission)) {
					// only for processed products
					if (productClass.getProcessingLevel() != null) {
						String periodString = N_MONTHLY;
						if (period.equals(Duration.ofDays(1))) {
							periodString = N_DAILY;
						}
						String nameMin = N_SUBMISSION_TO_COMPLETION + "." + periodString + "." + N_MIN + "." + N_TIME;
						String nameMax = N_SUBMISSION_TO_COMPLETION + "." + periodString + "." + N_MAX + "." + N_TIME;
						String nameAvg = N_SUBMISSION_TO_COMPLETION + "." + periodString + "." + N_AVG + "." + N_TIME;
						// Find the entry
						transactionTemplate.setReadOnly(false);
						for (int i = 0; i < ProseoUtil.DB_MAX_RETRY; i++) {
							try {
								transactionTemplate.execute((status) -> {
									// calculate metric
									// TODO optimize, at the moment always complete calculation
									// find product files
									Instant now = Instant.now();
									String sqlQuery;
									sqlQuery = "SELECT o.actual_completion_time, o.submission_time FROM processing_order o "
											+ " WHERE o.actual_completion_time IS NOT NULL "
											+ " AND o.submission_time IS NOT NULL"
											+ " AND o.order_source = 'ODIP';";	

									Query query = em.createNativeQuery(sqlQuery);
									Object oList = query.getResultList();
									long count = (long)0;
									Duration sum = Duration.ofMillis(0);
									Duration min = null;
									Duration max = null;
									Duration avg = Duration.ofMillis(0);;
									if (oList instanceof ArrayList) {
										@SuppressWarnings("unchecked")
										ArrayList<Object> list = (ArrayList<Object>)oList;
										for (Object ele : list) {
											if (ele instanceof Object[]) {
												Object[] arrayEle = (Object[])ele;
												if (arrayEle.length == 2) {
													// Tuple with publication_time and sensing_stop_time
													count++;
													Duration diff = Duration.between((((Timestamp)arrayEle[1]).toInstant()), 
															(((Timestamp)arrayEle[0]).toInstant()));
													sum = sum.plus(diff);
													if (min == null) {
														min = diff;
													} else {
														if (min.getSeconds() > diff.getSeconds()) {
															min = diff;
														} else if (min.getSeconds() == diff.getSeconds()) {
															 if (min.getNano() > diff.getNano()) {
																min = diff;
															}
														}
													}							
													if (max == null) {
														max = diff;
													} else {
														if (max.getSeconds() < diff.getSeconds()) {
															max = diff;
														} else if (max.getSeconds() == diff.getSeconds()) {
															 if (max.getNano() < diff.getNano()) {
																 max = diff;
															}
														}
													}													
												}
											}
										}
									}
									if (count > 0) {
										avg = sum.dividedBy(count);
									} else {
										min = Duration.ofMillis(0);
										max = Duration.ofMillis(0);
									}
									updateGaugeMetric(nameMin, formatDuration(min), now);
									updateGaugeMetric(nameMax, formatDuration(max), now);
									updateGaugeMetric(nameAvg, formatDuration(avg), now);
									return null;
								});
								break;

							} catch (CannotAcquireLockException e) {
								if (logger.isDebugEnabled()) logger.debug("... database concurrency issue detected: ", e);

								if ((i + 1) < ProseoUtil.DB_MAX_RETRY) {
									ProseoUtil.dbWait();
								} else {
									if (logger.isDebugEnabled()) logger.debug("... failing after {} attempts!", ProseoUtil.DB_MAX_RETRY);
									throw e;
								}
							}	
						}
					}
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
