package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

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
import de.dlr.proseo.storagemgr.UniqueStorageTestPaths;
import de.dlr.proseo.storagemgr.rest.model.RestProductFS;
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
public class ProductControllerImplTest_upload {

	@Autowired
	private MockMvc mockMvc;

	@Rule
	public TestName testName = new TestName();
	
 	@Autowired
	private StorageProvider storageProvider;

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/products";

	
	/**
	 * Register products/files/dirs from unstructered storage in prosEO-storage
	 * 
	 * POST /products RestProductFS
	 * 
	 * @return RestProductFS
	 */
	@Test
	public void testUpload_v1Posix() throws Exception {
		
		// StorageProvider storageProvider = new StorageProvider();
		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);
		
		createRestProductFS(storageProvider);
		
		assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}
	
	/**
	 * Register products/files/dirs from unstructered storage in prosEO-storage
	 * 
	 * POST /products RestProductFS
	 * 
	 * @return RestProductFS
	 */
	@Test
	public void testUpload_v2Posix() throws Exception {
		
		// StorageProvider storageProvider = new StorageProvider();
		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);
		
		createRestProductFS(storageProvider);
		
		assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}
	
	private void createRestProductFS(StorageProvider storageProvider) throws Exception {
		
		TestUtils.printMethodName(this, testName);
		UniqueStorageTestPaths uniquePaths = new UniqueStorageTestPaths(this, testName);

		RestProductFS restProductFS = populateRestProductFS(storageProvider);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(REQUEST_STRING)	
				.content(asJsonString(restProductFS)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().is(201)).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		
		uniquePaths.deleteUniqueTestDirectories();
	}

	private RestProductFS populateRestProductFS(StorageProvider storageProvider) {
		
		UniqueStorageTestPaths uniquePaths = new UniqueStorageTestPaths(this, testName);

		String productId = "123";
		String sourceStorageType = "POSIX";
		String relativePath1 = new PathConverter(uniquePaths.getUniqueTestFolder(), "folder1/file1.txt").getPath();
		String relativePath2 = new PathConverter(uniquePaths.getUniqueTestFolder(), "folder1/file2.txt").getPath();
		
		String absolutePath1 = storageProvider.getSourceFile(relativePath1).getFullPath();
		String absolutePath2 = storageProvider.getSourceFile(relativePath2).getFullPath();
		
		TestUtils.createFile(absolutePath1, "content");
		TestUtils.createFile(absolutePath2, "content");
	
		List<String> sourceFilePaths = new ArrayList<>();
		sourceFilePaths.add(absolutePath1);
		sourceFilePaths.add(absolutePath2);

		String targetStorageId = "234";
		String targetStorageType = "POSIX";
		String registeredFilePath = "/registeredPath";
		Boolean registered = false;
		Long registeredFilesCount = 3l;

		List<String> registeredFilesList = new ArrayList<>();
		registeredFilesList.add("/registered/file1.txt");
		registeredFilesList.add("/registered/file2.txt");

		Boolean deleted = false;
		String message = "message";

		return new RestProductFS(productId, sourceStorageType, sourceFilePaths, targetStorageId, targetStorageType,
				registeredFilePath, registered, registeredFilesCount, registeredFilesList, deleted, message);

	}
	
	
	private static String asJsonString(final Object obj) {
		try {
			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	
}
