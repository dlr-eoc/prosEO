package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import de.dlr.proseo.storagemgr.rest.model.RestProductFS;
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
public class ProductControllerImplTest {

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

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/products";

	/**
	 * Get products with given directory prefix
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 * @return products string[]
	 */
	@Test
	public void testGetProductFilesV2() throws Exception {

		storageProvider.loadVersion2();
		getProductFiles();
	}

	/**
	 * Get products with given directory prefix
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 * @return products string[]
	 */
	@Test
	public void testGetProductFilesV1() throws Exception {

		storageProvider.loadVersion1();
		getProductFiles();
	}

	private void getProductFiles() throws Exception {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();

		String storageType = "POSIX";
		String prefix = "files/";

		List<String> pathes = new ArrayList<>();
		pathes.add(prefix + "file1.txt");
		pathes.add(prefix + "file2.txt");
		pathes.add(prefix + "dir/file3.txt");

		for (String path : pathes) {

			storageTestUtils.createSourceFile(path);
			storageTestUtils.uploadToPosixStorage(path);
		}

		storageTestUtils.printPosixStorage();

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("storageType", storageType).param("prefix", prefix);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
	}
	
	/**
	 * Register products/files/dirs from unstructered storage in prosEO-storage
	 * 
	 * POST /products RestProductFS
	 * 
	 * @return RestProductFS
	 */
	@Test
	public void testCreateRestProductFSV1() throws Exception {
		
		storageProvider.loadVersion1();
		createRestProductFS();
	}
	
	/**
	 * Register products/files/dirs from unstructered storage in prosEO-storage
	 * 
	 * POST /products RestProductFS
	 * 
	 * @return RestProductFS
	 */
	@Test
	public void testCreateRestProductFSV2() throws Exception {
		
		storageProvider.loadVersion2();
		createRestProductFS();
	}
	
	private void createRestProductFS() throws Exception {
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();

		RestProductFS restProductFS = populateRestProductFS();

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(REQUEST_STRING)	
				.content(asJsonString(restProductFS)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().is(201)).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
	}
	
	public static String asJsonString(final Object obj) {
		try {
			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private RestProductFS populateRestProductFS() {

		String productId = "123";
		String sourceStorageType = "POSIX";
		String relativePath1 = "/folder1/file1.txt";
		String relativePath2 = "/folder1/file2.txt";
		
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

	/**
	 * Delete/remove product by product path info from prosEO storage
	 * 
	 * DELETE /products pathInfo="/.."
	 * 
	 * @return RestProductFS
	 */
	@Test
	public void testDeleteV1() throws Exception {

		storageProvider.loadVersion1();
		delete();
	}



	/**
	 * Delete/remove product by product path info from prosEO storage
	 * 
	 * DELETE /products pathInfo="/.."
	 * 
	 * @return RestProductFS
	 */
	@Test
	public void testDeleteV2() throws Exception {
		
		storageProvider.loadVersion2();
		delete();

	}

	private void delete() throws Exception {
		
		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyStorageDirectories();

		String storageType = "POSIX";
		String prefix = "files/";

		List<String> pathes = new ArrayList<>();
		pathes.add(prefix + "file1.txt");
		pathes.add(prefix + "file2.txt");
		pathes.add(prefix + "dir/file3.txt");

		for (String path : pathes) {

			storageTestUtils.createSourceFile(path);
			storageTestUtils.uploadToPosixStorage(path);
		}

		storageTestUtils.printPosixStorage();
		

		String pathInfo = storageProvider.getStorage().getStorageFile(prefix).getFullPath();

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.delete(REQUEST_STRING).param("pathInfo",
				pathInfo);

		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());

	}
}
