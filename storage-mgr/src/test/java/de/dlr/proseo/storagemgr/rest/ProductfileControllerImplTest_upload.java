package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;
import de.dlr.proseo.storagemgr.version2.FileUtils;
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
public class ProductfileControllerImplTest_upload {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StorageTestUtils storageTestUtils;

	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/productfiles";

	@Test
	public void testUpload_v2Posix() throws Exception {

		StorageType storageType = StorageType.POSIX;
		storageProvider.setStorage(storageType);

		upload();

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	@Test
	public void testUpload_v2S3() throws Exception {

		StorageType storageType = StorageType.S3;
		storageProvider.setStorage(storageType);

		upload();

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	/**
	 * UPLOAD (updateProductfiles)
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
	 */
	private void upload() throws Exception {

		TestUtils.printMethodName(this, testName);

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
		assertTrue("Expected path: " + realRelativeStoragePath + " Exists: " + relativePath, relativePath.equals(realRelativeStoragePath));

		// show storage files
		StorageTestUtils.printStorageFiles("After http-call", storageProvider.getStorage());

		// delete files with empty folders
		new FileUtils(absoluteSourcePath).deleteFile(); // source
		storageProvider.getStorage().delete(realRelativeStoragePath); // storage
	}
}
