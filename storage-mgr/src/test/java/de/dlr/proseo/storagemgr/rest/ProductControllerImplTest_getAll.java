package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.Assert.assertTrue;
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
import de.dlr.proseo.storagemgr.StorageProvider;
import de.dlr.proseo.storagemgr.BaseStorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.StorageType;

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
public class ProductControllerImplTest_getAll {

	@Autowired
	private MockMvc mockMvc;

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
	public void testGet_posix() throws Exception {

		StorageType storageType = StorageType.POSIX;
		storageProvider.setStorage(storageType);

		getProductFiles(storageType);

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
	public void testGet_S3() throws Exception {

		StorageType storageType = StorageType.S3;
		storageProvider.setStorage(storageType);

		getProductFiles(storageType);

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	/**
	 * GET Files, Storage -> List<String> getProductFiles(String storageType, String
	 * prefix)
	 *
	 * gets all files from storage and returns absolute paths
	 * List<String>
	 * 
	 * INPUT
	 * 
	 * storageType: "S3" or "POSIX" prefix (folder): "" - all
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
	private void getProductFiles(StorageType storageType) throws Exception {

		TestUtils.printMethodName(this, testName);

		// show storage files
		BaseStorageTestUtils.printStorageFiles("Before http-call", storageProvider.getStorage());

		// HTTP Get files from storage
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING)
				.param("storageType", storageType.toString()).param("prefix", "");
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		// show results of http-get-call
		TestUtils.printMvcResult(REQUEST_STRING, mvcResult);
	}
}