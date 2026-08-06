/**
 * ProductControllerImplTest_get.java
 * 
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

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
import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.utils.PathConverter;

/**
 * Mock Mvc test for Product Controller
 * 
 * @author Denys Chaykovskiy
 * 
 */
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ProductControllerImplTest_get {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BaseStorageTestUtils storageTestUtils;

	@Autowired
	private StorageProvider storageProvider;

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/products";

	/**
	 * Get products with given directory prefix
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 * @return products string[]
	 */
	@Test
	public void testGet_v2Posix(TestInfo testInfo) throws Exception {

		StorageType storageType = StorageType.POSIX;
		storageProvider.setDefaultStorage(storageType);

		getProductFiles(storageType, testInfo);

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertEquals(storageType, realStorageType, "Expected: SM POSIX, " + " Exists: " + realStorageType);
	}

	/**
	 * Get products with given directory prefix
	 * 
	 * GET /products storageType="POSIX"&prefix="/.."
	 * 
	 * @return products string[]
	 */
	@Test
	public void testGet_v2S3(TestInfo testInfo) throws Exception {

		StorageType storageType = StorageType.S3;
		storageProvider.setDefaultStorage(storageType);

		getProductFiles(storageType, testInfo);

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertEquals(storageType, realStorageType, "Expected: SM S3, " + " Exists: " + realStorageType);
	}

	/**
	 * GET Files, Storage -> List<String> getProductFiles(String storageType, String
	 * prefix)
	 *
	 * gets files from storage with prefix (folder) and returns absolute paths
	 * List<String>
	 * 
	 * INPUT
	 * 
	 * storageType: "S3" or "POSIX" prefix (folder): "somefolder/folder" or "" - all
	 * storage files
	 * 
	 * OUTPUT
	 * 
	 * List<String> absolute paths (if no storage type - union from all storages)
	 * 
	 * s3://<bucket>/<relativePath> // no storage path in s3
	 * /<storagePath>/<relativePath> // no bucket in posix currently
	 * 
	 */
	private void getProductFiles(StorageType storageType, TestInfo testInfo) throws Exception {

		TestUtils.printMethodName(this, testInfo);

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
			StorageFile targetFile = storageProvider.getStorageFileFromDefaultStorage(relativePath);
			storageProvider.getStorage().uploadFile(sourceFile, targetFile);
		}

		// show storage files
		BaseStorageTestUtils.printStorageFiles("Before http-call", storageProvider.getStorage());

		// HTTP Get files from storage
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("storageType", storageType.toString()).param("prefix", prefix);
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		// show results of http-download
		TestUtils.printMvcResult(REQUEST_STRING, mvcResult);

		// check real with expected absolute storage paths
		String json = mvcResult.getResponse().getContentAsString();
		List<?> realAbsoluteStoragePaths = new ObjectMapper().readValue(json, List.class);

		Storage storage = storageProvider.getStorage();
		for (int i = 0; i < realAbsoluteStoragePaths.size(); i++) {

			String realAbsoluteStoragePath = (String) realAbsoluteStoragePaths.get(i);

			String expectedAbsoluteStoragePath = storage.getAbsolutePath(relativePaths.get(i));
			expectedAbsoluteStoragePath = storage.addFSPrefix(expectedAbsoluteStoragePath);
			
			System.out.println("Real      " + realAbsoluteStoragePath);
			System.out.println("Expected: " + expectedAbsoluteStoragePath);
			
			realAbsoluteStoragePath = new PathConverter(realAbsoluteStoragePath).normalizeWindowsPath().getPath();

			assertEquals(expectedAbsoluteStoragePath, realAbsoluteStoragePath,
					"Real path: " + realAbsoluteStoragePath + " Expected  path: " + expectedAbsoluteStoragePath);
		}

		// delete storage files with prefix
		storageProvider.getStorage().delete(prefix);

		// show storage files after deletion
		BaseStorageTestUtils.printStorageFiles("After deletion", storageProvider.getStorage());
	}
}