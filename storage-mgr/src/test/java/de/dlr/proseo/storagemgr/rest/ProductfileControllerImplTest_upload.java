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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.version2.StorageProvider;

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
public class ProductfileControllerImplTest_upload {

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

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/productfiles";

	
	
	@Test
	public void testUpload_v1Posix() throws Exception {
		
		String relativePath = "testControllerFile.txt";
		
		storageProvider.loadDefaultPaths();
		storageProvider.loadVersion1();
		upload(relativePath);
	}
	
	@Test
	public void testUpload_v2Posix() throws Exception {
		
		String relativePath = "testControllerFile.txt";

		storageProvider.loadDefaultPaths();
		storageProvider.loadVersion2();
		upload(relativePath);
	}
	
	private void upload(String relativePath) throws Exception {
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();
		
		String productId = "123"; 	
		String absolutePath = storageTestUtils.createSourceFile(relativePath);
		String fileSize = Long.toString(storageProvider.getSourceFileSize(relativePath));
		
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.put(REQUEST_STRING)
				.param("pathInfo", absolutePath)
				.param("productId", productId)
				.param("fileSize", fileSize);
				
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isCreated()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		
		storageTestUtils.printPosixStorage();
		storageTestUtils.printVersion("FINISHED upload-Test");
		
		TestUtils.deleteStorageDirectories();
	}
}
