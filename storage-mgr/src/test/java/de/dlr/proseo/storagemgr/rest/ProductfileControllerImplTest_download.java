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
import de.dlr.proseo.storagemgr.UniqueStorageTestPaths;
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.StorageType;

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
public class ProductfileControllerImplTest_download {

	@Autowired
	private MockMvc mockMvc;
	
	@Autowired
	private StorageTestUtils storageTestUtils;
	
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
	public void testDownload_v1Posix() throws Exception {
				
		StorageProvider storageProvider = new StorageProvider();
		storageProvider.loadVersion1();
		storageProvider.setStorage(StorageType.POSIX);

		download();
	}
	
	@Test
	public void testDownload_v1PosixMany() throws Exception {
		
		String relativePath = "product/";
		
		StorageProvider storageProvider = new StorageProvider();
		storageProvider.loadVersion1();
		storageProvider.setStorage(StorageType.POSIX);

		// downloadMany(relativePath);
	}
	
	@Test
	public void testDownload_v2Posix() throws Exception {
		
		
		StorageProvider storageProvider = new StorageProvider();
		storageProvider.loadVersion2();
		storageProvider.setStorage(StorageType.POSIX);

		download();
	}
	
	private void download() throws Exception {
		
		TestUtils.printMethodName(this, testName);
		UniqueStorageTestPaths uniquePaths = new UniqueStorageTestPaths(this, testName);
		
		String relativePath = "product/testControllerFile.txt";
		relativePath = new PathConverter(uniquePaths.getUniqueTestFolder(), relativePath).getPath();
	
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

		uniquePaths.deleteUniqueTestDirectories();		
	}
	
	private void downloadMany(StorageProvider storageProvider) throws Exception {
		
		// TODO: refactoring
		
		TestUtils.printMethodName(this, testName);
		UniqueStorageTestPaths uniquePaths = new UniqueStorageTestPaths(this, testName);
	
		String relativePath = "product/";
		String relativePath1 = relativePath + "file1.txt";
		String relativePath2 = relativePath + "file2.txt";
		
		relativePath1 = new PathConverter(uniquePaths.getUniqueTestFolder(), relativePath1).getPath();
		relativePath2 = new PathConverter(uniquePaths.getUniqueTestFolder(), relativePath2).getPath();

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

		uniquePaths.deleteUniqueTestDirectories();
	}
}
