package de.dlr.proseo.storagemgr;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import de.dlr.proseo.storagemgr.version2.model.StorageFile;

/**
 * @author Denys Chaykovskiy
 *
 */
@Component
public class StorageTestUtils {

	private static StorageTestUtils theTestUtils;

	public static StorageTestUtils getInstance() {

		return theTestUtils;
	}

	@PostConstruct
	public void init() {

		theTestUtils = this;
	}

	/**
	 * @param message
	 * @param arrayList
	 */
	public static void printStorageFileList(String message, List<StorageFile> list) {

		System.out.println();
		System.out.println(message + " SIZE: " + list.size());
		for (StorageFile element : list) {

			System.out.println(" - " + element.getRelativePath());
		}
		System.out.println();
	}
}
