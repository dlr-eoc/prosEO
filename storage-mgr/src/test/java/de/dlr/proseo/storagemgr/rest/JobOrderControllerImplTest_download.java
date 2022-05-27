package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.rest.model.RestJoborder;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Mock Mvc test for Product Controller
 * 
 * @author Denys Chaykovskiy
 * 
 */
/**
 * @throws Exception
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class JobOrderControllerImplTest_download {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TestUtils testUtils;

	@Autowired
	private StorageTestUtils storageTestUtils;

	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/joborders";

	/**
	 * Download prosEO Job Order File as Base64-encoded string
	 * 
	 * GET /joborders pathInfo="/.."
	 * 
	 * @return String
	 */

	@Test
	public void testDownload_v2Posix() throws Exception {

		storageProvider.loadVersion2();
		downloadRestJobOrder();
	}

	@Test
	public void testDownload_v1Posix() throws Exception {

		storageProvider.loadVersion1();
		downloadRestJobOrder();
	}

	private void downloadRestJobOrder() throws Exception {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();

		String relativePath = "file.txt";

		String pathInfo = storageTestUtils.createSourceFile(relativePath);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING).param("pathInfo", pathInfo);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
	}
	
	private static String asJsonString(final Object obj) {
		try {
			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}


}
