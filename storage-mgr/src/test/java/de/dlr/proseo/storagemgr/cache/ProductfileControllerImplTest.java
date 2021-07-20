package de.dlr.proseo.storagemgr.cache;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import de.dlr.proseo.storagemgr.StorageManager;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = StorageManager.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestEntityManager
public class ProductfileControllerImplTest {

	@Autowired
	private TestUtils testUtils;

	@Rule
	public TestName testName = new TestName();

	@Autowired
	private FileCache fileCache;

	String testPath;

	@PostConstruct
	private void init() {
		testPath = testUtils.getTestPath();
	}

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	public void testGetRestFileInfoByPathInfo() {

		TestUtils.printMethodName(this, testName);
		TestUtils.createEmptyTestDirectory();
		fileCache.clear();

		String str = restTemplate.getForObject("http://localhost:" + port + "/proseo/storage-mgr/x/info", String.class);

		System.out.println();
		System.out.println(str);
		System.out.println();

		String file1 = "file1.txt";
		String file2 = "file2.txt";

		String sourcePath = TestUtils.getInstance().getSourceTestPath();
		String sourcePath1 = sourcePath + "/" + file1;
		String sourcePath2 = sourcePath + "/" + file2;

		String destPath = TestUtils.getInstance().getTestPath();
		String destPath1 = Paths.get(destPath + "/" + file1 + "/" + file1).toString();
		String destPath2 = Paths.get(destPath + "/" + file2 + "/" + file2).toString();

		TestUtils.createFile(sourcePath1, "");
		TestUtils.createFile(sourcePath2, "");

		assertTrue("File1 exists already: " + destPath1, !new File(destPath1).exists());
		assertTrue("File2 exists already: " + destPath2, !new File(destPath2).exists());

		// file1 copied to dest and in cache, file2 - not copied and not in cache

		str = restTemplate.getForObject(
				"http://localhost:" + port + "/proseo/storage-mgr/x/productfiles?pathInfo=" + sourcePath1,
				String.class);

		assertTrue("File1 does not exist: " + destPath1, new File(destPath1).exists());
		assertTrue("File2 exists already: " + destPath2, !new File(destPath2).exists());

		assertTrue("Cache does not have 1 element. Cache Size: " + fileCache.size(), fileCache.size() == 1);

		// file1 and file2 copied to dest and in cache

		str = restTemplate.getForObject(
				"http://localhost:" + port + "/proseo/storage-mgr/x/productfiles?pathInfo=" + sourcePath2,
				String.class);

		assertTrue("File1 does not exist: " + destPath1, new File(destPath1).exists());
		assertTrue("File2 does not exist: " + destPath2, new File(destPath2).exists());

		assertTrue("Cache does not have 2 element. Cache Size: " + fileCache.size(), fileCache.size() == 2);

		// file1 and file2 copied to dest and in cache

		str = restTemplate.getForObject(
				"http://localhost:" + port + "/proseo/storage-mgr/x/productfiles?pathInfo=" + sourcePath2,
				String.class);

		assertTrue("File1 does not exist: " + destPath1, new File(destPath1).exists());
		assertTrue("File2 does not exist: " + destPath2, new File(destPath2).exists());

		assertTrue("Cache does not have 2 element. Cache Size: " + fileCache.size(), fileCache.size() == 2);

		System.out.println();
		System.out.println(str);
		System.out.println();

		// file1 deleted from dest and from cache, file2 - not

		fileCache.remove(destPath1);

		assertTrue("File1 exists: " + destPath1, !new File(destPath1).exists());
		assertTrue("File2 does not exist: " + destPath2, new File(destPath2).exists());

		assertTrue("Cache does not have 0 elements. Cache Size: " + fileCache.size(), fileCache.size() == 1);

		// file1 and file2 deleted from dest and from cache

		fileCache.clear();

		assertTrue("File1 exists: " + destPath1, !new File(destPath1).exists());
		assertTrue("File2 exists: " + destPath2, !new File(destPath2).exists());

		assertTrue("Cache does not have 0 elements. Cache Size: " + fileCache.size(), fileCache.size() == 0);

		TestUtils.deleteTestDirectory();
	}
}
