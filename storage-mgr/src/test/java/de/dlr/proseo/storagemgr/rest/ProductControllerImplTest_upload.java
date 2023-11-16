package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.Storage;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.rest.model.RestProductFS;
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
public class ProductControllerImplTest_upload {

	@Autowired
	private MockMvc mockMvc;

	@Rule
	public TestName testName = new TestName();
	
 	@Autowired
	private StorageProvider storageProvider;

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/products";
	
	/**
	 * Register products/files/dirs from unstructered storage in prosEO-storage
	 * 
	 * POST /products RestProductFS
	 * 
	 * @return RestProductFS
	 */
	@Test
	public void testUpload_posix() throws Exception {
		
		StorageType storageType = StorageType.POSIX; 
		storageProvider.setStorage(storageType);
		
		uploadRestProductFS();
		
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}
		
	/**
	 * Register products/files/dirs from unstructered storage in prosEO-storage
	 * 
	 * POST /products RestProductFS
	 * 
	 * @return RestProductFS
	 */
	@Test
	public void testUpload_S3() throws Exception {
		
		StorageType storageType = StorageType.S3; 
		storageProvider.setStorage(storageType);
		
		uploadRestProductFS();
		
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}
	
	/**
	 * LIST UPLOAD Source -> Storage (createRestProductFS)
	 *
	 * copies to productId folder list of files (sourceFilePaths)
	 * 
	 * target Path in storage -> <productId>/<filename from sourceFilePaths> 
	 * 
	 * ignores the rest of the source path, only productId from parameter and filename from source path
	 * 
	 * INPUT 
	 * 
	 * RestProductFS (list sourceFilePaths, )
	 * 
	 * sourceFiles - absolute paths posix and s3 (?)
	 * 
	 * absolutePath  	 
	 * s3://<bucket>/<relativePath>        // no storage path in s3
	 * /<storagePath>/<relativePath>       // no bucket in posix currently
	 * 
	 * OUTPUT 
	 * 
	 * RestProductFS
	 * 
	 * registered paths - uploaded storage paths
	 * 
	 * target Path in storage -> <productId>/<filename from sourceFilePaths> 
	 * 
	 * 
	 */
	private void uploadRestProductFS() throws Exception {
		
		TestUtils.printMethodName(this, testName);
		
		String productId = "123";
		
		// delete possible existing files from storage for clean test
		storageProvider.getStorage().delete(productId);
		
		// create relative files 
		List<String> relativePaths = new ArrayList<>(); 
		relativePaths.add("restProduct/restProductFile1.txt");
		relativePaths.add("restProduct/restProductFile2.txt");
	
		// populate restProduct parameter
		RestProductFS restProductFS = populateRestProductFS(productId, relativePaths);
		
		// show storage files
		StorageTestUtils.printStorageFiles("Before http-upload call", storageProvider.getStorage());

		// http-upload call
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(REQUEST_STRING)	
				.content(TestUtils.asJsonString(restProductFS)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().is(201)).andReturn();

		// show results of http-download
		TestUtils.printMvcResult(REQUEST_STRING, mvcResult); 
		
		// show storage files after http-upload
		StorageTestUtils.printStorageFiles("After http-upload call", storageProvider.getStorage());
		
		// check real with expected absolute storage paths 
		String json = mvcResult.getResponse().getContentAsString();
		RestProductFS result = new ObjectMapper().readValue(json, RestProductFS.class);
		List<String> realAbsoluteStoragePaths = result.getRegisteredFilesList();
		
		Storage storage = storageProvider.getStorage();
		for (int i = 0; i < realAbsoluteStoragePaths.size(); i++) {

			String realAbsoluteStoragePath = (String) realAbsoluteStoragePaths.get(i);
			
			String filename = new PathConverter(relativePaths.get(i)).getFileName();
			String relativeProductPath = new PathConverter(productId, filename).getPath();
			String expectedAbsoluteStoragePath	= storage.getStorageFile(relativeProductPath).getFullPath();

			System.out.println("Real      " + realAbsoluteStoragePath);
			System.out.println("Expected: " + expectedAbsoluteStoragePath);
			
			realAbsoluteStoragePath = new PathConverter(realAbsoluteStoragePath).normalizeWindowsPath().getPath();

			assertTrue("Real path: " + realAbsoluteStoragePath + " Expected  path: " + expectedAbsoluteStoragePath,
					realAbsoluteStoragePath.equals(expectedAbsoluteStoragePath));
		}
		
		// delete uploaded files from storage 
		storageProvider.getStorage().delete(productId);
	}

	private RestProductFS populateRestProductFS(String productId, List<String> relativePaths) {
		
		String sourceStorageType = "POSIX";
		
		List<String> sourceFilePaths = new ArrayList<>();
		for (String relativePath : relativePaths) {
			
			String absoluteSourcePath = storageProvider.getSourceFile(relativePath).getFullPath();
			TestUtils.createFile(absoluteSourcePath, "content");
			sourceFilePaths.add(absoluteSourcePath);
		}
	
		String targetStorageId = "234";
		String targetStorageType = storageProvider.getStorage().getStorageType().toString();
		String registeredFilePath = "/registeredPath";
		Boolean registered = false;
		Long registeredFilesCount = 3l;

		List<String> registeredFilesList = new ArrayList<>();
		registeredFilesList.add("/registered/registeredfile1.txt");
		registeredFilesList.add("/registered/registeredfile2.txt");

		Boolean deleted = false;
		String message = "message";

		return new RestProductFS(productId, sourceStorageType, sourceFilePaths, targetStorageId, targetStorageType,
				registeredFilePath, registered, registeredFilesCount, registeredFilesList, deleted, message);
	}
}
