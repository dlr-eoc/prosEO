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
import de.dlr.proseo.storagemgr.UniquePathsStorageTestUtils;
import de.dlr.proseo.storagemgr.UniqueStorageTestPaths;
import de.dlr.proseo.storagemgr.rest.model.RestJoborder;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

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
	private StorageTestUtils storageTestUtils;

	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();
	
	private UniqueStorageTestPaths uniquePaths;
	private UniquePathsStorageTestUtils uniqueUtils;

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
		
		setUniqueTestPaths(); 

		storageProvider.loadVersion2();
		storageProvider.setStorage(StorageType.POSIX);
	
		downloadRestJobOrder();
	}

	@Test
	public void testDownload_v1Posix() throws Exception {
		
		setUniqueTestPaths(); 
		
		storageProvider.loadVersion1();
		storageProvider.setStorage(StorageType.POSIX);
		
		downloadRestJobOrder();
	}
	
	private void setUniqueTestPaths() {
		
		uniquePaths = new UniqueStorageTestPaths(this, testName); 
		uniqueUtils = new UniquePathsStorageTestUtils(uniquePaths.getSourcePath(),
				uniquePaths.getStoragePath(), uniquePaths.getCachePath());
		
		storageProvider.setSourcePath(uniquePaths.getSourcePath());
		storageProvider.setStoragePath(uniquePaths.getStoragePath());
		storageProvider.setCachePath(uniquePaths.getCachePath());
	}

	private void downloadRestJobOrder() throws Exception {

		TestUtils.printMethodName(this, testName);
	
		String relativePath = "file.txt";

		String pathInfo = uniqueUtils.createSourceFile(relativePath);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING).param("pathInfo", pathInfo);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		
		uniquePaths.deleteUniqueTestDirectory();
	}
}
