package de.dlr.proseo.storagemgr;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import javax.annotation.PostConstruct;

import org.junit.Rule;
import org.junit.rules.TestName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.version2.PathConverter;
import de.dlr.proseo.storagemgr.version2.StorageProvider;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;

/**
 * @author Denys Chaykovskiy
 *
 */
@Component
public class StorageTestUtils extends BaseStorageTestUtils {


	@Autowired
	private TestUtils testUtils;

	@Autowired
	private StorageProvider storageProvider;

	@Rule
	public TestName testName = new TestName();

	@PostConstruct
	private void init() {

		sourcePath = testUtils.getSourcePath();
		storagePath = testUtils.getStoragePath();
		cachePath = testUtils.getCachePath();

		theTestUtils = this;
	}

	private static StorageTestUtils theTestUtils;

	public static StorageTestUtils getInstance() {

		return theTestUtils;
	}

	public void printVersion(String msg) {

		System.out.println(msg + (storageProvider.isVersion2() ? " Version-2" : " Version-1"));
	}
}
