package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.version2.PathConverter;
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
public class ProductControllerImplTest_delete {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StorageTestUtils storageTestUtils;

	@Rule
	public TestName testName = new TestName();

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
	public void testDelete_posix() throws Exception {

		StorageType storageType = StorageType.POSIX;
		storageProvider.setStorage(storageType);

		delete(storageProvider);

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	/**
	 * Delete/remove product by product path info from prosEO storage
	 * 
	 * DELETE /products pathInfo="/.."
	 * 
	 * @return RestProductFS
	 */
	@Test
	public void testDelete_S3() throws Exception {

		StorageType storageType = StorageType.S3;
		storageProvider.setStorage(storageType);

		delete(storageProvider);

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	private void delete(StorageProvider storageProvider) throws Exception {

		TestUtils.printMethodName(this, testName);

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
		StorageTestUtils.printStorageFiles("Before http-call", storageProvider.getStorage());

		// show storage files with prefix before http-delete-call
		StorageTestUtils.printStorageFilesWithPrefix("Before http-call", storageProvider.getStorage(), prefix);

		// check count of uploaded prefix storage files
		int realStorageFileCount = storageProvider.getStorage().getRelativeFiles(prefix).size();
		int expectedStorageFileCount = relativePaths.size();
		assertTrue("After upload - Expected:" + expectedStorageFileCount + " Exists: " + realStorageFileCount,
				realStorageFileCount == expectedStorageFileCount);

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
		StorageTestUtils.printStorageFilesWithPrefix("After http-call", storageProvider.getStorage(), prefix);

		// check files after delete (expected: 0)
		realStorageFileCount = storageProvider.getStorage().getRelativeFiles(prefix).size();
		expectedStorageFileCount = 0;
		assertTrue("After upload - Expected:" + expectedStorageFileCount + " Exists: " + realStorageFileCount,
				realStorageFileCount == expectedStorageFileCount);
	}
}
