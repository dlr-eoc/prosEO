package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.core.type.TypeReference;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
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
import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
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
public class ProductControllerImplTest_get {

	@Autowired
	private MockMvc mockMvc;

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
	public void testGet_v2Posix() throws Exception {
		
		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);
		
		getProductFiles(storageType);
		
		assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	/**
	 * Get products with given directory prefix
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 * @return products string[]
	 */
	@Test
	public void testGet_v1Posix() throws Exception {

		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);
		
		getProductFiles(storageType);
		
		assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}
	
	/**
	 * Get products with given directory prefix
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 * @return products string[]
	 */
	@Test
	public void testGet_v2S3() throws Exception {
		
		StorageType storageType = StorageType.S3; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);
		
		getProductFiles(storageType);
		
		assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	/**
	 * Get products with given directory prefix
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 * @return products string[]
	 */
	@Test
	public void testGet_v1S3() throws Exception {

		StorageType storageType = StorageType.S3; 
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);
		
		getProductFiles(storageType);
		
		assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	private void getProductFiles(StorageType storageType) throws Exception {

		TestUtils.printMethodName(this, testName);

		// create source paths
		String prefix = "prodGetPrefix";
		List<String> relativePaths = new ArrayList<>();
		relativePaths.add(new PathConverter(prefix, "productGet1.txt").getPath());
		relativePaths.add(new PathConverter(prefix, "productGet2.txt").getPath());
		relativePaths.add(new PathConverter(prefix, "productGetDir/productGet3.txt").getPath());

		for (String relativePath : relativePaths) {
			
			// create file in source 
			storageTestUtils.createSourceFile(relativePath);
			
			// upload file to storage from source
			StorageFile sourceFile = storageProvider.getSourceFile(relativePath);
			StorageFile targetFile = storageProvider.getStorageFile(relativePath);
			storageProvider.getStorage().uploadFile(sourceFile, targetFile);
		}
		
		// show files in storage before http call
		List<String> storageFiles = storageProvider.getStorage().getFiles();	
		String stType = storageProvider.getStorage().getStorageType().toString();
		TestUtils.printList(stType + " Storage Files before HTTP call", storageFiles);

		// HTTP Get files from storage		
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("storageType", storageType.toString()).param("prefix", prefix);
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		// show results of http-upload
		System.out.println();
		System.out.println("HTTP Response");
		System.out.println("Request: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		System.out.println();
		
		// TODO: maybe convert List
		String json = mvcResult.getResponse().getContentAsString();
		// RestFileInfo result = new ObjectMapper().readValue(json, RestFileInfo.class);
		// String realCachePath = result.getFilePath();		
		
		//List<StringClass> actual = new ObjectMapper().readValue(json, new TypeReference<List<StringClass>>() {});
		
		// ObjectMapper mapper = new ObjectMapper();

		// this uses a TypeReference to inform Jackson about the Lists's generic type
		// List<PersonDto> actual = mapper.readValue(result.getResponse().getContentAsString(), new TypeReference<List<PersonDto>>() {});

		/*
		String listFormat = "%-20s %s";
		List<String> resultList = null; 
		System.out.println(String.format(listFormat, "Processor Name", "Version"));
		for (Object resultObject: (new ObjectMapper()).convertValue(resultList, List.class)) {
			if (resultObject instanceof Map) {
				Map<?, ?> resultMap = (Map<?, ?>) resultObject;
				System.out.println(String.format(listFormat, resultMap.get("processorName"), resultMap.get("processorVersion")));
			}
		*/
		
		// delete storage files with prefix
		storageProvider.getStorage().delete(prefix);
		
		// show files in storage after deletion
		storageFiles = storageProvider.getStorage().getFiles();	
		stType = storageProvider.getStorage().getStorageType().toString();
		TestUtils.printList(stType + " Storage Files after deletion", storageFiles);
	}
	
	
	private class StringClass {
		
		private String value;

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		} 
	}
}