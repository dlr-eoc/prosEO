package de.dlr.proseo.ui.gui;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

import de.dlr.proseo.ui.gui.service.ProcessorService;

import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

public class processorClassGUITest {
	@Autowired
	private ProcessorService processorService;
	@Test
	public void testGetProcessorClassName() {
		Builder webclient = WebClient.builder().clientConnector(new ReactorClientHttpConnector(
                HttpClient.create().followRedirect((req, res) -> {
                    return HttpResponseStatus.FOUND.equals(res.status());
                })
        ));
//		Flux<String> response = processorService.get("PTM","PTML1B");
//		List<String> list = new ArrayList<>();
//		response.subscribe(value -> list.add(value));
//		Assert.assertEquals(list.toString(), "[\n" + 
//				"  {\n" + 
//				"    \"id\": 16,\n" + 
//				"    \"version\": 1,\n" + 
//				"    \"missionCode\": \"PTM\",\n" + 
//				"    \"processorName\": \"PTML1B\",\n" + 
//				"    \"productClasses\": [\n" + 
//				"      \"L1B\"\n" + 
//				"    ]\n" + 
//				"  }\n" + 
//				"]");
	}

	@Test
	public void testPostProcessorClassName() {
//		fail("Not yet implemented");
	}

}
