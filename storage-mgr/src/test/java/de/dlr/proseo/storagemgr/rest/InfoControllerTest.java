package de.dlr.proseo.storagemgr.rest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import de.dlr.proseo.storagemgr.StorageManager;

/**
 * Mock Mvc test for Info Controller
 * 
 * @author Denys Chaykovskiy
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class InfoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void shouldReturnDefaultMessage() throws Exception {
		
		String requestString =  "/proseo/storage-mgr/x/info";
						
		MvcResult mvcResult = mockMvc.perform(get(requestString))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("s3Region")))
				.andReturn(); 
		
		System.out.println("Status: " + mvcResult.getResponse().getStatus() );
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString() );
	}
}
