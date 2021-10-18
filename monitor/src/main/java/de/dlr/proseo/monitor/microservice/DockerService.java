package de.dlr.proseo.monitor.microservice;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.okhttp.OkDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient.Request;
import com.github.dockerjava.transport.DockerHttpClient.Response;

import de.dlr.proseo.model.util.MonServiceStates;
import de.dlr.proseo.monitor.MonitorConfiguration;

public class DockerService {

	/**
	 * The service name
	 */
	private String name;
	/**
	 * The machine ip service is running on
	 */
	private String ip;
	/**
	 * The HTTP port
	 */
	private Long port;
	/**
	 * The docker name
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
	 * @return the docker
	 */
	public String getApiVersion() {
		return apiVersion;
	}

	public DockerService(MonitorConfiguration.Docker docker) {
		this.name = docker.getName();
		this.ip = docker.getIp();
		this.port = docker.getPort();
		this.apiVersion = docker.getApiVersion();
	}
	
	public void check(MicroService ms, Monitor monitor) {
		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
				.withDockerHost("tcp://" + getIp() + ":" + getPort())
				.withDockerTlsVerify(false)
				.build();
		DockerHttpClient httpClient = new OkDockerHttpClient.Builder()
				.dockerHost(config.getDockerHost())
				.sslConfig(config.getSSLConfig())
				.build();
		Request request = Request.builder()
				.method(Request.Method.GET)
				.path("/containers/" + ms.getNameId() + "/json")
				.build();

		try (Response response = httpClient.execute(request)) {
			String x = IOUtils.toString(response.getBody(), StandardCharsets.UTF_8);
			ObjectMapper mapper = new ObjectMapper();
			Map<String, Object> map = mapper.readValue(x, Map.class);
			if (map != null) {
				if (map.get("State") != null) {
					if ((boolean) ((Map)(map.get("State"))).get("Running")) {
						if (ms.getHasActuator()) {
							// actuator does not answer "up", assume it is starting.
							ms.setState(MonServiceStates.STARTING_ID);
						} else {
							ms.setState(MonServiceStates.RUNNING_ID);
						}
					} else if ((boolean) ((Map)(map.get("State"))).get("Restarting")) {
						ms.setState(MonServiceStates.STARTING_ID);
					} else {
						// all other cases are "stopped"
						ms.setState(MonServiceStates.STOPPED_ID);
					}
				} else {
					ms.setState(MonServiceStates.STOPPED_ID);
				}
			} else {
				ms.setState(MonServiceStates.STOPPED_ID);				
			}
			ms.createEntry(monitor);
			return;
		} catch (Exception e) {
			// TODO no connect to docker or something went wrong, set state to stopped
			ms.setState(MonServiceStates.STOPPED_ID);
			ms.createEntry(monitor);
			return;
		}
	}
}
