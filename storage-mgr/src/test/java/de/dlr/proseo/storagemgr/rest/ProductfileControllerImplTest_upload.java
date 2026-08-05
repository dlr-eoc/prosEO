package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageProvider;
import de.dlr.proseo.storagemgr.BaseStorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;
import de.dlr.proseo.storagemgr.utils.FileUtils;
import de.dlr.proseo.storagemgr.utils.PathConverter;

/**
 * Mock Mvc test for Product Controller
 * 
 * @author Denys Chaykovskiy
 * 
 */
/**
 * @throws Exception
 */
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ProductfileControllerImplTest_upload {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BaseStorageTestUtils storageTestUtils;

	@Autowired
	private StorageProvider storageProvider;

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/productfiles";

	@Test
	public void testUpload_Posix(TestInfo testInfo) throws Exception {

		StorageType storageType = StorageType.POSIX;
		storageProvider.setDefaultStorage(storageType);

		upload(testInfo);

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertEquals(storageType, realStorageType, "Expected: SM POSIX, " + " Exists: " + realStorageType);
	}

	@Test
	public void testUpload_S3(TestInfo testInfo) throws Exception {

		StorageType storageType = StorageType.S3;
		storageProvider.setDefaultStorage(storageType);

		upload(testInfo);

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertEquals(storageType, realStorageType, "Expected: SM S3, " + " Exists: " + realStorageType);
	}

	/**
	 * UPLOAD (updateProductfiles)
	 * 
	 * NOT INTENDED FOR USE OUTSIDE UNIT TEST CASES!
	 * 
	 * absolute file -> storage (absoluteSourcePath/filename) takes filename from path and
	 * productid from parameter, ignores the rest of the path
	 * 
	 * INPUT
	 * 
	 * absolutePath /../filename.* (posix absolute file) productId 12345 (digits
	 * only) fileSize 123l (long)
	 * 
	 * OUTPUT
	 * 
	 * Posix: /<storagePath>/<productId>/<filename from input absolutPath>
	 * s3://<defaultBucket>/<productId>/<filename from input absolutPath>
	 * @param testInfo TODO
	 */
	private void upload(TestInfo testInfo) throws Exception {

		TestUtils.printMethodName(this, testInfo);

		String productId = "12345"; // only int type allowed
		String filename = "productFileUpload.txt";
		String relativePath = new PathConverter(productId, filename).getPath();

		// create file in source for upload
		String absoluteSourcePath = storageTestUtils.createSourceFile(relativePath);
		String fileSize = Long.toString(storageProvider.getSourceFileSize(relativePath));

		// rest-upload file from source to storage
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.put(REQUEST_STRING)
				.param("pathInfo", absoluteSourcePath).param("productId", productId).param("fileSize", fileSize);
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isCreated()).andReturn();

		// show results of http-upload
		TestUtils.printMvcResult(REQUEST_STRING, mvcResult); 

		// check real with expected storage path 
		String json = mvcResult.getResponse().getContentAsString();
		RestFileInfo result = new ObjectMapper().readValue(json, RestFileInfo.class);
		String realRelativeStoragePath = storageProvider.getRelativePath(result.getFilePath());
		System.out.println("Created job order path: " + realRelativeStoragePath);
		assertEquals(relativePath, realRelativeStoragePath, "Expected path: " + relativePath + " Exists: " + realRelativeStoragePath);

		// show storage files
		BaseStorageTestUtils.printStorageFiles("After http-call", storageProvider.getStorage());

		// delete files with empty folders
		new FileUtils(absoluteSourcePath).deleteFile(); // source
		storageProvider.getStorage().delete(realRelativeStoragePath); // storage
	}
}
