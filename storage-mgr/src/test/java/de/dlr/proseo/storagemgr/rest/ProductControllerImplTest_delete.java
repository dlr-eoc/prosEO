package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

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

		StorageProvider storageProvider = new StorageProvider();
		storageProvider.loadVersion1();
		storageProvider.setStorage(StorageType.POSIX);

		delete(storageProvider);
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

		StorageProvider storageProvider = new StorageProvider();
		storageProvider.loadVersion2();
		storageProvider.setStorage(StorageType.POSIX);

		delete(storageProvider);

	}

	private void delete(StorageProvider storageProvider) throws Exception {

		TestUtils.printMethodName(this, testName);
		UniqueStorageTestPaths uniquePaths = new UniqueStorageTestPaths(this, testName);

		// create unique source pathes
		String prefix = uniquePaths.getUniqueTestFolder();
		List<String> pathes = new ArrayList<>();
		pathes.add(new PathConverter(prefix, "file1.txt").getPath());
		pathes.add(new PathConverter(prefix, "file2.txt").getPath());
		pathes.add(new PathConverter(prefix, "dir/file3.txt").getPath());

		// create and upload source files
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
		
		uniquePaths.deleteUniqueTestDirectories();

	}
}
