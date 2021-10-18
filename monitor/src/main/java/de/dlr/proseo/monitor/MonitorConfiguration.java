package de.dlr.proseo.monitor;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for the prosEO Monitor component
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


}
