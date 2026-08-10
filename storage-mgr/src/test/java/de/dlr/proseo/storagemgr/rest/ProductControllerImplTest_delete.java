/**
 * ProductControllerImplTest_delete.java
 *
 * (C) 2022 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.storagemgr.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import de.dlr.proseo.storagemgr.BaseStorageTestUtils;
import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageProvider;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.StorageType;
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
public class ProductControllerImplTest_delete {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BaseStorageTestUtils storageTestUtils;

	@Autowired
	private StorageProvider storageProvider;

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/products";

	/**
	 * Delete/remove product by product path info from prosEO storage
	 *
	 * DELETE /products pathInfo="/.."
	 *
	 * @return RestProductFS
	 */
	@Test
	public void testDelete_posix(TestInfo testInfo) throws Exception {

		StorageType storageType = StorageType.POSIX;
		storageProvider.setDefaultStorage(storageType);

		delete(storageProvider, testInfo);

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertEquals(storageType, realStorageType, "Expected: SM POSIX, " + " Exists: " + realStorageType);
	}

	/**
	 * Delete/remove product by product path info from prosEO storage
	 *
	 * DELETE /products pathInfo="/.."
	 *
	 * @return RestProductFS
	 */
	@Test
	public void testDelete_S3(TestInfo testInfo) throws Exception {

		StorageType storageType = StorageType.S3;
		storageProvider.setDefaultStorage(storageType);

		delete(storageProvider, testInfo);

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertEquals(storageType, realStorageType, "Expected: SM S3, " + " Exists: " + realStorageType);
	}

	private void delete(StorageProvider storageProvider, TestInfo testInfo) throws Exception {

		TestUtils.printMethodName(this, testInfo);

		// create unique source paths
		String prefix = "product_delete";
		List<String> relativePaths = new ArrayList<>();
		relativePaths.add(new PathConverter(prefix, "deletefile1.txt").getPath());
		// relativePaths.add(new PathConverter(prefix, "deletefile2.txt").getPath());
		// relativePaths.add(new PathConverter(prefix, "deletefiledir/file3.txt").getPath());

		// delete possible existing files with prefix before test
		storageProvider.getStorage().delete(prefix);

		// create and upload source files
		for (String relativePath : relativePaths) {

			storageTestUtils.createSourceFile(relativePath);
			storageProvider.getStorage().uploadSourceFile(relativePath);
		}

		// show storage files before http-delete-call
		BaseStorageTestUtils.printStorageFiles("Before http-call", storageProvider.getStorage());

		// show storage files with prefix before http-delete-call
		BaseStorageTestUtils.printStorageFilesWithPrefix("Before http-call", storageProvider.getStorage(), prefix);

		// check count of uploaded prefix storage files
		int realStorageFileCount = storageProvider.getStorage().getRelativeFiles(prefix).size();
		int expectedStorageFileCount = relativePaths.size();
		assertEquals(expectedStorageFileCount, realStorageFileCount,
				"After upload - Expected:" + expectedStorageFileCount + " Exists: " + realStorageFileCount);

		// absolute prefix path to delete
		String pathInfo = new PathConverter(storageProvider.getStorage().getAbsolutePath(prefix)).addSlashAtEnd()
				.getPath();
		System.out.println("HTTP PathInfo: " + pathInfo);

		// HTTP delete call (prefix)
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.delete(REQUEST_STRING).param("pathInfo",
				pathInfo);
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		// show results of http-download
		TestUtils.printMvcResult(REQUEST_STRING, mvcResult);

		// show storage files with prefix before http-delete-call
		BaseStorageTestUtils.printStorageFilesWithPrefix("After http-call", storageProvider.getStorage(), prefix);

		// check files after delete (expected: 0)
		realStorageFileCount = storageProvider.getStorage().getRelativeFiles(prefix).size();
		expectedStorageFileCount = 0;
		assertEquals(expectedStorageFileCount, realStorageFileCount,
				"After upload - Expected:" + expectedStorageFileCount + " Exists: " + realStorageFileCount);
	}
}
