package de.dlr.proseo.storagemgr.version2.posix;

import static org.junit.Assert.*;

import org.junit.Test;

import de.dlr.proseo.storagemgr.TestUtils;
import de.dlr.proseo.storagemgr.version2.model.Storage;
import de.dlr.proseo.storagemgr.version2.model.StorageFile;

public class PosixStorageTest {

	@Test
	public void testPosixPosixUploadFile() {
		
		String internalStoragePath = "E:\\test\\internalStorage\\";
		String externalStoragePath = "E:\\test\\externalStorage\\";
		String fileName = "source.txt"; 
		String testFileContent = "some text inside file";
		
		StorageFile sourceFile = new PosixStorageFile(internalStoragePath, fileName);
		StorageFile destFile = new PosixStorageFile(externalStoragePath, fileName);
		
		Storage externalStorage = new PosixStorage(internalStoragePath);
		Storage internalStorage = new PosixStorage(externalStoragePath); 
		
		//TestUtils.createFile(sourceFile.getFullPath(), testFileContent);
		
		TestUtils.printDirectoryTree(internalStoragePath);
		TestUtils.printDirectoryTree(externalStoragePath);

		assertTrue("File for upload has not been created: " + sourceFile.getFullPath(), TestUtils.fileExists(sourceFile.getFullPath()));
		
		externalStorage.uploadFile(sourceFile, destFile); 
		
		TestUtils.printDirectoryTree(internalStoragePath);
		TestUtils.printDirectoryTree(externalStoragePath);
	
	}

}
