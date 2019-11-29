package de.dlr.proseo.ui.gui;

import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.dlr.proseo.model.rest.model.RestProcessorClass;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
public class processorService {
	private static Logger logger = LoggerFactory.getLogger(processorService.class);
	
	public Flux<RestProcessorClass> get(String mission, String processorName) {
		String uri ="https://proseo-registry.eoc.dlr.de/proseo/processor-mgr/v0.1/processorclasses";
		if(null != mission && null != processorName) {
			uri += "?mission=" + mission + "&processorName=" + processorName;
		} else if (null != processorName) {
			uri += "?processorName=" + processorName;
		} else if (null != mission) { 
			uri += "?mission=" + mission;
		} 
		logger.trace("URI " + uri);
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
				HttpClient.create().followRedirect((req, res) -> {
					logger.trace("response:{}", res.status());
					return HttpResponseStatus.FOUND.equals(res.status());
				})
			));
		ResponseSpec responseSpec = webclient.build().get().uri(uri).headers(headers -> headers.setBasicAuth("s5p-proseo", "sieb37.Schlaefer")).accept(MediaType.APPLICATION_JSON).retrieve();
		return responseSpec.bodyToFlux(RestProcessorClass.class);

	}
	public Mono<RestProcessorClass> post(String mission, String id, String processorClassName, String processorClassVersion, String[] processorClassProductClass) {
		String uri ="";
		MultiValueMap<String,String> map = new LinkedMultiValueMap<>();
		if(null != mission && null != id && null != processorClassName && null!= processorClassVersion && null != processorClassProductClass) {
		uri += "https://proseo-registry.eoc.dlr.de/proseo/processor-mgr/v0.1/processorclasses";
		logger.trace("URI " + uri);
		map.add("id", id);
		map.add("version", processorClassVersion);
		map.add("missionCode", mission);
		map.add("processorName", processorClassName);
		map.add("productClasses", processorClassProductClass.toString());
		}
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
                HttpClient.create().followRedirect((req, res) -> {
                    logger.trace("response:{}" + res);
                    return HttpResponseStatus.FOUND.equals(res.status());
                })
        ));
		   
		   
		return webclient.build().post().uri(uri).body(
				BodyInserters.fromFormData(map))
				.headers(headers -> headers.setBasicAuth("s5p-proseo", "sieb37.Schlaefer")).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(RestProcessorClass.class);

	}
	

}
