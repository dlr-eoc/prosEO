package de.dlr.proseo.monitor;

import java.util.List;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO MonitorServices component
 * 
 * @author Ernst Melchinger
 *
 */
@Configuration
@ConfigurationProperties(prefix="proseo.monitor")
@EntityScan(basePackages = "de.dlr.proseo.model")

public class MonitorConfiguration {

	/**
	 * List of Services to monitor
	 */
	private List<Service> services;
	/**
	 * List of docker machines
	 */
	private List<Docker> dockers;
	/**
	 * List of docker machines
	 */
	private List<Timeliness> timeliness;

	/**
	 * Wait time for connection
	 */
	private Long connectTimeout;
	/**
	 * Wait time for answer
	 */
	private Long readTimeout;
	/**
	 * Cycle wait time
	 */
	private Long cycle;
	/**
	 * Cycle wait time
	 */
	private Long serviceCycle;
	/**
	 * Cycle wait time
	 */
	private Long orderCycle;
	/**
	 * Cycle wait time
	 */
	private Long productCycle;
	/**
	 * Cycle wait time
	 */
	private Long rawdataCycle;
	/**
	 * Cycle wait time
	 */
	private Long kpi01TimelinessCycle;
	/**
	 * Cycle wait time
	 */
	private Long kpi02CompletenessCycle;
	
	/**
	 * Start creating aggregation at this time
	 */
	private String aggregationStart;
	
	 /**
	 * @return the timeliness
	 */
	public List<Timeliness> getTimeliness() {
		return timeliness;
	}
	/**
	 * @param timeliness the timeliness to set
	 */
	public void setTimeliness(List<Timeliness> timeliness) {
		this.timeliness = timeliness;
	}
	/**
	 * @return the kpi01TimelinessCycle
	 */
	public Long getKpi01TimelinessCycle() {
		return kpi01TimelinessCycle;
	}
	/**
	 * @return the kpi02CompletenessCycle
	 */
	public Long getKpi02CompletenessCycle() {
		return kpi02CompletenessCycle;
	}
	/**
	 * @param kpi01TimelinessCycle the kpi01TimelinessCycle to set
	 */
	public void setKpi01TimelinessCycle(Long kpi01TimelinessCycle) {
		this.kpi01TimelinessCycle = kpi01TimelinessCycle;
	}
	/**
	 * @param kpi02CompletenessCycle the kpi02CompletenessCycle to set
	 */
	public void setKpi02CompletenessCycle(Long kpi02CompletenessCycle) {
		this.kpi02CompletenessCycle = kpi02CompletenessCycle;
	}
	/**
	 * @return the rawdataCycle
	 */
	public Long getRawdataCycle() {
		return rawdataCycle;
	}
	/**
	 * @param rawdataCycle the rawdataCycle to set
	 */
	public void setRawdataCycle(Long rawdataCycle) {
		this.rawdataCycle = rawdataCycle;
	}
	/**
	 * @return the aggregationStart
	 */
	public String getAggregationStart() {
		return aggregationStart;
	}
	/**
	 * @param productAggregationStart the productAggregationStart to set
	 */
	public void setAggregationStart(String aggregationStart) {
		this.aggregationStart = aggregationStart;
	}
	/**
	 * @return the productCycle
	 */
	public Long getProductCycle() {
		return productCycle;
	}
	/**
	 * @param productCycle the productCycle to set
	 */
	public void setProductCycle(Long productCycle) {
		this.productCycle = productCycle;
	}
	/**
	 * @return the services
	 */
	public List<Service> getServices() {
		return services;
	}
	/**
	 * @return the dockers
	 */
	public List<Docker> getDockers() {
		return dockers;
	}
	/**
	 * @param services the services to set
	 */
	public void setServices(List<Service> services) {
		this.services = services;
	}
	/**
	 * @param dockers the dockers to set
	 */
	public void setDockers(List<Docker> dockers) {
		this.dockers = dockers;
	}
	
	/**
	 * @return the connectTimeout
	 */
	public Long getConnectTimeout() {
		return connectTimeout;
	}
	/**
	 * @return the readTimeout
	 */
	public Long getReadTimeout() {
		return readTimeout;
	}
	/**
	 * @return the cycle
	 */
	public Long getCycle() {
		return cycle;
	}
	/**
	 * @param connectTimeout the connectTimeout to set
	 */
	public void setConnectTimeout(Long connectTimeout) {
		this.connectTimeout = connectTimeout;
	}
	/**
	 * @param readTimeout the readTimeout to set
	 */
	public void setReadTimeout(Long readTimeout) {
		this.readTimeout = readTimeout;
	}
	/**
	 * @param cycle the cycle to set
	 */
	public void setCycle(Long cycle) {
		this.cycle = cycle;
	}

	/**
	 * @return the serviceCycle
	 */
	public Long getServiceCycle() {
		return serviceCycle;
	}
	/**
	 * @return the orderCycle
	 */
	public Long getOrderCycle() {
		return orderCycle;
	}
	/**
	 * @param serviceCycle the serviceCycle to set
	 */
	public void setServiceCycle(Long serviceCycle) {
		this.serviceCycle = serviceCycle;
	}
	/**
	 * @param orderCycle the orderCycle to set
	 */
	public void setOrderCycle(Long orderCycle) {
		this.orderCycle = orderCycle;
	}


	public static class Service {
		/**
		 * The service caption
		 */
		private String name;
		/**
		 * The service name
		 */
		private String nameId;
			/**
			 * The machine url service is running on
			 */
			private String url;
			/**
			 * The docker name
			 */
			private String docker;
			/**
			 * Service is a prosEO service (spring boot application)
			 */
			private Boolean isProseo;
			/**
			 * Service is a spring boot application wiht actuator
			 */
			private Boolean hasActuator;

			/**
			 * @return the hasActuator
			 */
			public Boolean getHasActuator() {
				return hasActuator;
			}
			/**
			 * @param hasActuator the hasActuator to set
			 */
			public void setHasActuator(Boolean hasActuator) {
				this.hasActuator = hasActuator;
			}
			/**
			 * @return the name
			 */
			public String getName() {
				return name;
			}
			/**
			 * @return the name
			 */
			public String getNameId() {
				return nameId;
			}
			/**
			 * @return the url
			 */
			public String getUrl() {
				return url;
			}
			/**
			 * @param name the name to set
			 */
			public void setName(String name) {
				this.name = name;
			}
			/**
			 * @param name the name to set
			 */
			public void setNameId(String nameId) {
				this.nameId = nameId;
			}
			/**
			 * @param url the url to set
			 */
			public void setUrl(String url) {
				this.url = url;
			}
			/**
			 * @return the docker
			 */
			public String getDocker() {
				return docker;
			}
			/**
			 * @param docker the docker to set
			 */
			public void setDocker(String docker) {
				this.docker = docker;
			}
			/**
			 * @return
			 */
			public Boolean getIsProseo() {
				return isProseo;
			}
			/**
			 * @param isProseo
			 */
			public void setIsProseo(Boolean isProseo) {
				this.isProseo = isProseo;
			}
			
	 }
	 public static class Docker {
			/**
			 * The docker name
			 */
			private String name;
			/**
			 * The machine ip docker is running on
			 */
			private String ip;
			/**
			 * The HTTP port
			 */
			private Long port;
			/**
			 * The docker API apiVersion
			 */
			private String apiVersion;
			
			/**
			 * @return the name
			 */
			public String getName() {
				return name;
			}

			/**
			 * @return the ip
			 */
			public String getIp() {
				return ip;
			}

			/**
			 * @return the port
			 */
			public Long getPort() {
				return port;
			}
			
			/**
			 * @return the apiVersion
			 */
			public String getApiVersion() {
				return apiVersion;
			}

			/**
			 * @param name the name to set
			 */
			public void setName(String name) {
				this.name = name;
			}

			/**
			 * @param ip the ip to set
			 */
			public void setIp(String ip) {
				this.ip = ip;
			}

			/**
			 * @param port the port to set
			 */
			public void setPort(Long port) {
				this.port = port;
			}

			/**
			 * @param apiVersion the apiVersion to set
			 */
			public void setApiVersion(String apiVersion) {
				this.apiVersion = apiVersion;
			}
		 
	 }
	 public static class Timeliness {
		 /**
		  * The timeliness mode 
		  */
		 private String mode;
		 /**
		  * The timeliness value in minutes
		  */
		 private Long minutes;
		/**
		 * @return the mode
		 */
		public String getMode() {
			return mode;
		}
		/**
		 * @return the minutes
		 */
		public Long getMinutes() {
			return minutes;
		}
		/**
		 * @param mode the mode to set
		 */
		public void setMode(String mode) {
			this.mode = mode;
		}
		/**
		 * @param minutes the minutes to set
		 */
		public void setMinutes(Long minutes) {
			this.minutes = minutes;
		}

	 }


}
