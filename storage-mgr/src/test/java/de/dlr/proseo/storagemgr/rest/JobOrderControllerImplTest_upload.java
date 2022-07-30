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
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.dlr.proseo.storagemgr.StorageManager;
import de.dlr.proseo.storagemgr.StorageManagerConfiguration;
import de.dlr.proseo.storagemgr.StorageTestUtils;
import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.rest.model.RestJoborder;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;
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
public class JobOrderControllerImplTest_upload {

	@Autowired
	private MockMvc mockMvc;
	
 	@Autowired
	private StorageProvider storageProvider;
 	
	@Autowired
	private StorageManagerConfiguration cfg;
	
	@Rule
	public TestName testName = new TestName();

	private static final String REQUEST_STRING = "/proseo/storage-mgr/x/joborders";

	/**
	 * Upload prosEO Job Order File for later use in a job   // String -> StorageFile
	 * 
	 * POST /joborders RestJoborder
	 * 
	 * @return RestJoborder
	 */

	@Test
	public void testUpload_v2Posix() throws Exception {

		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);

		uploadRestJobOrder(storageType);
		
		assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	@Test
	public void testUpload_v1Posix() throws Exception {

		StorageType storageType = StorageType.POSIX; 
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);

		uploadRestJobOrder(storageType);
		
		assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM POSIX, " + " Exists: " + realStorageType, storageType == realStorageType);
	}
	
	@Test
	public void testUpload_v2S3() throws Exception {

		StorageType storageType = StorageType.S3; 
		storageProvider.loadVersion2();
		storageProvider.setStorage(storageType);

		uploadRestJobOrder(storageType);
		
		assertTrue("Expected: SM Version2, " + " Exists: 1", storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}

	@Test
	public void testUpload_v1S3() throws Exception {

		StorageType storageType = StorageType.S3; 
		storageProvider.loadVersion1();
		storageProvider.setStorage(storageType);

		uploadRestJobOrder(storageType);
		
		assertTrue("Expected: SM Version1, " + " Exists: 2", !storageProvider.isVersion2());
		StorageType realStorageType = storageProvider.getStorage().getStorageType();
		assertTrue("Expected: SM S3, " + " Exists: " + realStorageType, storageType == realStorageType);
	}
	
	/**
	 * Upload prosEO Job Order String to storage  // String -> StorageFile 
	 * 
	 * output name of file is a random-generated name
	 * 
	 * folder -> STORAGE/joborders/..
	 * 
	 * POST /joborders RestJoborder
	 * 
	 * @return RestJoborder
	 */	
	private void uploadRestJobOrder(StorageType storageType) throws Exception {
		
		TestUtils.printMethodName(this, testName);

		// create rest job object from string
		String base64 = "PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0iVVRGLTgiIHN0YW5kYWxvbmU9Im5vIj8+PElwZl9Kb2JfT3JkZXI+PElwZl9Db25mPjxQcm9jZXNzb3JfTmFtZT5QVE1MMjwvUHJvY2Vzc29yX05hbWU+PFZlcnNpb24+MC4xLjA8L1ZlcnNpb24+PFN0ZG91dF9Mb2dfTGV2ZWw+SU5GTzwvU3Rkb3V0X0xvZ19MZXZlbD48U3RkZXJyX0xvZ19MZXZlbD5JTkZPPC9TdGRlcnJfTG9nX0xldmVsPjxUZXN0PmZhbHNlPC9UZXN0PjxCcmVha3BvaW50X0VuYWJsZT5mYWxzZTwvQnJlYWtwb2ludF9FbmFibGU+PFByb2Nlc3NpbmdfU3RhdGlvbj5wcm9zRU8gVGVzdCBNaXNzaW9uIExlcmNoZW5ob2Y8L1Byb2Nlc3NpbmdfU3RhdGlvbj48U2Vuc2luZ19UaW1lPjxTdGFydD4yMDIwMDMyNV8xNDE4MjAwMDAwMDA8L1N0YXJ0PjxTdG9wPjIwMjAwMzI1XzE1MjgyMDAwMDAwMDwvU3RvcD48L1NlbnNpbmdfVGltZT48Q29uZmlnX0ZpbGVzPjxDb25mX0ZpbGVfTmFtZT4vdXNyL3NoYXJlL3NhbXBsZS1wcm9jZXNzb3IvY29uZi9wdG1fbDJfY29uZmlnLnhtbDwvQ29uZl9GaWxlX05hbWU+PC9Db25maWdfRmlsZXM+PER5bmFtaWNfUHJvY2Vzc2luZ19QYXJhbWV0ZXJzPjxQcm9jZXNzaW5nX1BhcmFtZXRlcj48TmFtZT5sb2dnaW5nLmR1bXBsb2c8L05hbWU+PFZhbHVlPm51bGw8L1ZhbHVlPjwvUHJvY2Vzc2luZ19QYXJhbWV0ZXI+PFByb2Nlc3NpbmdfUGFyYW1ldGVyPjxOYW1lPlRocmVhZHM8L05hbWU+PFZhbHVlPjEwPC9WYWx1ZT48L1Byb2Nlc3NpbmdfUGFyYW1ldGVyPjxQcm9jZXNzaW5nX1BhcmFtZXRlcj48TmFtZT5Qcm9jZXNzaW5nX01vZGU8L05hbWU+PFZhbHVlPk9GRkw8L1ZhbHVlPjwvUHJvY2Vzc2luZ19QYXJhbWV0ZXI+PFByb2Nlc3NpbmdfUGFyYW1ldGVyPjxOYW1lPmxvZ2dpbmcucm9vdDwvTmFtZT48VmFsdWU+bm90aWNlPC9WYWx1ZT48L1Byb2Nlc3NpbmdfUGFyYW1ldGVyPjwvRHluYW1pY19Qcm9jZXNzaW5nX1BhcmFtZXRlcnM+PC9JcGZfQ29uZj48TGlzdF9vZl9JcGZfUHJvY3MgY291bnQ9IjEiPjxJcGZfUHJvYz48VGFza19OYW1lPnB0bV9sMjwvVGFza19OYW1lPjxUYXNrX1ZlcnNpb24+MC4xLjA8L1Rhc2tfVmVyc2lvbj48TGlzdF9vZl9JbnB1dHMgY291bnQ9IjIiPjxJbnB1dD48RmlsZV9UeXBlPkwxQl9QQVJUMjwvRmlsZV9UeXBlPjxGaWxlX05hbWVfVHlwZT5QaHlzaWNhbDwvRmlsZV9OYW1lX1R5cGU+PExpc3Rfb2ZfRmlsZV9OYW1lcyBjb3VudD0iMCIvPjwvSW5wdXQ+PElucHV0PjxGaWxlX1R5cGU+TDFCX1BBUlQxPC9GaWxlX1R5cGU+PEZpbGVfTmFtZV9UeXBlPlBoeXNpY2FsPC9GaWxlX05hbWVfVHlwZT48TGlzdF9vZl9GaWxlX05hbWVzIGNvdW50PSIxIj48RmlsZV9OYW1lIEZTX1R5cGU9IlMzIj5zMzovL3Byb3Nlby1kYXRhLTAwMS8xODcxNDYvUFRNX09QRVJfTDFCX1BBUlQxXzIwMjAwMzI1VDE0MDMzMF8yMDIwMDMyNVQxNTQ0NDBfMDMwMDNfQ29wQ29sXzAuMS4wXzIwMjIwMzAxVDA4MzI0NS5uYzwvRmlsZV9OYW1lPjwvTGlzdF9vZl9GaWxlX05hbWVzPjwvSW5wdXQ+PC9MaXN0X29mX0lucHV0cz48TGlzdF9vZl9PdXRwdXRzIGNvdW50PSIxIj48T3V0cHV0IFByb2R1Y3RfSUQ9IjE5ODg1MyI+PEZpbGVfVHlwZT5QVE1fTDJBPC9GaWxlX1R5cGU+PEZpbGVfTmFtZV9UeXBlPlBoeXNpY2FsPC9GaWxlX05hbWVfVHlwZT48RmlsZV9OYW1lIEZTX1R5cGU9IlMzIj5QVE1fT1BFUl9QVE1fTDJBXzIwMjAwMzI1VDE0MTgyMF8yMDIwMDMyNVQxNTI4MjBfMDMwMDNfQ29wQ29sXzAuMS4wXzIwMjIwMzAyVDA5NDQxMi5uYzwvRmlsZV9OYW1lPjwvT3V0cHV0PjwvTGlzdF9vZl9PdXRwdXRzPjwvSXBmX1Byb2M+PC9MaXN0X29mX0lwZl9Qcm9jcz48L0lwZl9Kb2JfT3JkZXI+";
		Boolean uploaded = false;
		String fsType = storageType.toString();
		String message = "message";
		String pathInfo = ""; // output parameter, path of created job order, see below

		RestJoborder joborder = new RestJoborder(base64, uploaded, fsType, pathInfo, message);

		// upload rest job
		MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(REQUEST_STRING)
				.content(TestUtils.asJsonString(joborder)).contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
		MvcResult mvcResult = mockMvc.perform(request).andExpect(status().is(201)).andReturn();
		
		// String createdJobOrderPath = getJobOrderRelativePath(cfg.getJoborderPrefix());
		// StorageFile jobOrder = storageProvider.getStorageFile(createdJobOrderPath);
		// boolean jobOrderExists = storageProvider.getStorage().fileExists(jobOrder);
		// assertTrue("Job Order was not created in Storage", jobOrderExists);   // job order is a random name
		
		// show results of http-download
		TestUtils.printMvcResult(REQUEST_STRING, mvcResult); 
		
		// show path of created rest job
		String json = mvcResult.getResponse().getContentAsString();
		RestJoborder result = new ObjectMapper().readValue(json, RestJoborder.class);
		System.out.println("Created job order path: " + result.getPathInfo());
		
		// show storage files
		StorageTestUtils.printStorageFiles("After http-call", storageProvider.getStorage());

		// no delete of created order
	}
	
	// TODO: Delete it if no need for deleting job orders
	private void deleteJobOrders() {
		
		StorageFile jobOrder = storageProvider.getStorageFile(cfg.getJoborderPrefix());
		TestUtils.deleteDirectory(jobOrder.getFullPath());
	}
}
