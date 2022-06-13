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
	public void testDelete_v1Posix() throws Exception {

		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);

		delete(storageProvider);
		
		assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
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
	public void testDelete_v2Posix() throws Exception {

		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);

		delete(storageProvider);
		
		assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
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
	public void testDelete_v1S3() throws Exception {

		StorageType storageType = StorageType.S3; 
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);

		delete(storageProvider);
		
		assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	/**
	 * Delete/remove product by product path info from prosEO storage
	 * 
	 * DELETE /products pathInfo="/.."
	 * 
	 * @return RestProductFS
	 */
	@Test
	public void testDelete_v2S3() throws Exception {

		StorageType storageType = StorageType.S3; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);

		delete(storageProvider);
		
		assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}
	
	private void delete(StorageProvider storageProvider) throws Exception {

		TestUtils.printMethodName(this, testName);

		// create unique source paths
		String prefix = "product_delete";
		List<String> paths = new ArrayList<>();
		//paths.add(new PathConverter(prefix, "file1.txt").getPath());
		//paths.add(new PathConverter(prefix, "file2.txt").getPath());
		paths.add(new PathConverter(prefix, "dir/file3.txt").getPath());
		
		// create and upload source files
		for (String path : paths) {

			storageTestUtils.createSourceFile(path);
			storageProvider.getStorage().uploadSourceFile(path);
		}

		// check uploaded files
		int realCount = storageProvider.getStorage().getFiles(prefix).size();
		// assertTrue("After upload - Expected: 1, " + " Exists: " + realCount, realCount == 1);

		// HTTP delete call (prefix)
		// String pathInfo = storageProvider.getStorage().getStorageFile(prefix).getFullPath();
		
		String pathInfo = new PathConverter(storageProvider.getStorage().getBasePath(), prefix).addSlashAtEnd().getPath();
		
		TestUtils.printList("Storage files (prefix) before delete: ", storageProvider.getStorage().getFiles(prefix));
		System.out.println("Prefix: " + pathInfo);

		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.delete(REQUEST_STRING).param("pathInfo",
				pathInfo);
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		// check response after delete
		System.out.println("REQUEST: " + REQUEST_STRING);
		System.out.println("Status: " + mvcResult.getResponse().getStatus());
		System.out.println("Content: " + mvcResult.getResponse().getContentAsString());
		
		TestUtils.printList("Storage files (prefix) after delete: ", storageProvider.getStorage().getFiles(prefix));
	
		// check files after delete
		realCount = storageProvider.getStorage().getFiles(prefix).size();
		assertTrue("After delete - Expected: 0, " + " Exists: " + realCount, realCount == 0);
	}
}
