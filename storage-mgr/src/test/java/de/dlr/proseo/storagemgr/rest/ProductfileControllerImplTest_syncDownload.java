package de.dlr.proseo.storagemgr.rest;

import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

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
import de.dlr.proseo.storagemgr.StorageProvider;
import de.dlr.proseo.storagemgr.BaseStorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.model.StorageFile;
import de.dlr.proseo.storagemgr.model.StorageType;
import de.dlr.proseo.storagemgr.rest.model.RestFileInfo;
import de.dlr.proseo.storagemgr.utils.FileUtils;
import de.dlr.proseo.storagemgr.utils.PathConverter;
import de.dlr.proseo.storagemgr.StreamInterceptor;




import java.util.ArrayList;


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
public class ProductfileControllerImplTest_syncDownload {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BaseStorageTestUtils storageTestUtils;

	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/productfiles";
	

	@Test
	public void testDownload_posix() throws Exception {

		StorageType storageType = StorageType.POSIX;
		storageProvider.setStorage(storageType);

		syncDownload("Posix");

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}
	
	@Test
	public void testDownload_S3() throws Exception {

		StorageType storageType = StorageType.S3;
		storageProvider.setStorage(storageType);

		syncDownload("S3");

		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	/**
	 * DOWNLOAD Storage -> Cache (getRestFileInfoByPathInfo)
	 *
	 * takes filename from path and productid from parameter, ignores the rest of
	 * the path
	 * 
	 * INPUT
	 * 
	 * absolutePath s3://<bucket>/<relativePath> // no storage path in s3
	 * /<storagePath>/<relativePath> // no bucket in posix currently
	 * 
	 * OUTPUT
	 * 
	 * Posix only (cache): /<cachePath>/<relativePath>
	 */
	private void syncDownload(String testID) throws Exception {

		TestUtils.printMethodName(this, testName);
			
		StreamInterceptor streamInterceptor = new StreamInterceptor(System.out); 
		    
		// create file in source
		// upload to storage
		// call http-download
		// check results (download in cache)

		String relativePath = "product/productFileDownload" + testID + ".txt";
		relativePath = new PathConverter(relativePath).getPath();

		// create file in source
		long fileSizeInBytes = 100L * 1024 * 1024; // 100 MB
		String absoluteSourcePath = storageTestUtils.createSourceFile(relativePath, fileSizeInBytes);

		// upload file to storage from source
		StorageFile sourceFile = storageProvider.getSourceFile(relativePath);
		StorageFile storageFile = storageProvider.getStorageFile(relativePath);
		storageProvider.getStorage().upload(sourceFile, storageFile);

		// show storage and cache files
		BaseStorageTestUtils.printStorageFiles("BEFORE http-call", storageProvider.getStorage());
		storageTestUtils.printCache();

		// concurrent threads download concurrently from storage to cache
		DownloadThread thread1 = new DownloadThread(relativePath);
		thread1.start();

		DownloadThread thread2 = new DownloadThread(relativePath);
		thread2.start();
	
		thread1.join();
        thread2.join();
        
        // not a concurrent thread uses downloaded file (in thread 1 or 2) from cache
		DownloadThread thread3 = new DownloadThread(relativePath);
		thread3.start();
		
        thread3.join();
            
        List<String> interceptedOutput = streamInterceptor.getOutput();
        streamInterceptor.restoreDefaultOutput(); 
        TestUtils.printList("CATCHED LOGS AND OUTPUT", interceptedOutput);      
        
		// delete files with empty folders

		String cacheFile = new PathConverter(storageProvider.getCachePath(), relativePath).getPath();

		if (new File(absoluteSourcePath).exists()) {
			new FileUtils(absoluteSourcePath).deleteFile(); // source
		}
		
		if (new File(cacheFile).exists()) {
			new FileUtils(cacheFile).deleteFile(); // cache
		}
		
		if (new File(storageFile.getFullPath()).exists()) {
			storageProvider.getStorage().deleteFile(storageFile); // in storage
		}
	}

	private class DownloadThread extends Thread {

		private String relativePath;

		public DownloadThread(String relativePath) {

			this.relativePath = relativePath;
		}

		public void run() {
			
			System.out.println("Thread name: " + Thread.currentThread().getName());

			// rest-download file from storage to cache
			String absoluteStoragePath = storageProvider.getStorage().getAbsolutePath(relativePath);
			System.out.println("Http-download call path (absolute storage path):" + absoluteStoragePath);

			MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get(REQUEST_STRING).param("pathInfo",
					absoluteStoragePath);
			MvcResult mvcResult;

			try {
				mvcResult = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

				// show results of http-download
				System.out.println("Thread name: " + Thread.currentThread().getName());
				TestUtils.printMvcResult(REQUEST_STRING, mvcResult);

				storageTestUtils.printCache();

				// check real with expected absolute cache path
				String expectedAbsoluteCachePath = new PathConverter(storageProvider.getCachePath(), relativePath)
						.getPath();

				String json = mvcResult.getResponse().getContentAsString();
				RestFileInfo result = new ObjectMapper().readValue(json, RestFileInfo.class);
				String realAbsoluteCachePath = result.getFilePath();

				System.out.println("Real cache path:     " + realAbsoluteCachePath);
				System.out.println("Expected cache path: " + expectedAbsoluteCachePath);

				realAbsoluteCachePath = new PathConverter(realAbsoluteCachePath).normalizeWindowsPath().getPath();
				assertTrue("Real cache path: " + realAbsoluteCachePath + " expected cache path: "
						+ expectedAbsoluteCachePath, realAbsoluteCachePath.equals(expectedAbsoluteCachePath));

				assertTrue("Downloaded file from storage to cache does not exist: " + realAbsoluteCachePath,
						new File(realAbsoluteCachePath).exists());

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
}
