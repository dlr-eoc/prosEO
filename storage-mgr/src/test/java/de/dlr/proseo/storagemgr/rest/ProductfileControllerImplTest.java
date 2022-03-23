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
public class ProductfileControllerImplTest {

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

	/**
	 * Retrieve file from Storage Manager into locally accessible file system // DOWNLOAD TEST
	 * 
	 * GET /productfiles pathInfo="/.."
	 * 
	 * @return RestFileInfo
	 */
	@Test
	public void testDownloadV1() throws Exception {
		
		String relativePath = "product/testControllerFile.txt";
		
		storageProvider.loadVersion1();
		download(relativePath);
	}
	
	@Test
	public void testDownloadManyV1() throws Exception {
		
		String relativePath = "product/";
		
		storageProvider.loadVersion1();
		// downloadMany(relativePath);
	}
	
	@Test
	public void testDownloadV2() throws Exception {
		
		String relativePath = "product/testControllerFile.txt";
		
		storageProvider.loadVersion2();
		download(relativePath);
	}
	
	@Test
	public void testUploadV1() throws Exception {
		
		String relativePath = "testControllerFile.txt";
		
		storageProvider.loadVersion1();
		upload(relativePath);
	}
	
	@Test
	public void testUploadV2() throws Exception {
		
		String relativePath = "testControllerFile.txt";

		storageProvider.loadVersion2();
		upload(relativePath);
	}
	
	
	/**
     *  Push file from local POSIX file system to Storage Manager
     *  
     *  POST /productfiles pathInfo="/.."&productId="123"&fileSize="234"  
     * 
     * @return
     *     RestFileInfo
     */
	/*
	@Test
	public void testCreateRestProductFS()  throws Exception {
		
	
	}
	*/
	
	public void upload(String relativePath) throws Exception {
		
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
	
	public void download(String relativePath) throws Exception {
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();
	
		String absolutePath = storageTestUtils.createSourceFile(relativePath);
		storageTestUtils.uploadToPosixStorage(relativePath);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("pathInfo", absolutePath);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		
		storageTestUtils.printCache();
		storageTestUtils.printVersion("FINISHED download-Test");

		
		TestUtils.deleteStorageDirectories();
	}
	
	public void downloadMany(String relativePath) throws Exception {
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();
	
		String relativePath1 = relativePath + "file1.txt";
		String relativePath2 = relativePath + "file2.txt";
		
		String absolutePath = storageProvider.getAbsoluteStoragePath(relativePath);
		
		String absolutePath1 = storageTestUtils.createSourceFile(relativePath1);
		String absolutePath2 = storageTestUtils.createSourceFile(relativePath2);
		
		storageTestUtils.uploadToPosixStorage(relativePath1);
		storageTestUtils.uploadToPosixStorage(relativePath2);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("pathInfo", absolutePath);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		
		storageTestUtils.printCache();
		storageTestUtils.printVersion("FINISHED download-Test");

		
		TestUtils.deleteStorageDirectories();
	}
}
