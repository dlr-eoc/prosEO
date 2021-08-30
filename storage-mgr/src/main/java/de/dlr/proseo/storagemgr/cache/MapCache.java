package de.dlr.proseo.storagemgr.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import java.time.Instant;

/**
 * Map wrapper for file cache with path as key and last accessed and file size
 * as value
 * 
 * @author Denys Chaykovskiy
 *
 */
public class MapCache {

	/** path cache */
	private Map<String, FileInfo> pathCache = new HashMap<>();

	/** sorted pathes */
	private List<Entry<String, FileInfo>> sortedPathes;

	/**
	 * Gets sorted pathes
	 * 
	 * @return list of sorted pathes
	 */
	public List<Entry<String, FileInfo>> getSortedPathes() {

		return sortedPathes;
	}

	/**
	 * Gets a clone of cache
	 * 
	 * @return cloned cache
	 */
	public Map<String, FileInfo> getCache() {

		Map<String, FileInfo> clonedCache = new HashMap<>();
		clonedCache.putAll(pathCache);

		return clonedCache;
	}

	/**
	 * Puts pathkey and fileinfo as a record to path cache
	 * 
	 * @param pathKey  path of the file
	 * @param fileInfo File info as a value
	 */
	public void put(String pathKey, FileInfo fileInfo) {

		pathCache.put(pathKey, fileInfo);
	}

	/**
	 * Gets the file info
	 * 
	 * @param pathKey path of the file
	 * @return file info
	 */
	public FileInfo get(String pathKey) {

		return pathCache.get(pathKey);
	}

	/**
	 * Removes the element from the cache
	 * 
	 * @param pathKey path of the file
	 */
	public void remove(String pathKey) {

		pathCache.remove(pathKey);
	}

	/**
	 * Removes all elements from the cache
	 */
	public void clear() {

		pathCache.clear();
	}

	/**
	 * Returns true if cache has an element with pathkey
	 * 
	 * @param pathKey Path of the file
	 * @return true if cache has an element
	 */
	public boolean containsKey(String pathKey) {

		return pathCache.containsKey(pathKey);
	}

	/**
	 * Returns the number of elements in the cache
	 * 
	 * @return size of the cache
	 */
	public int size() {

		return pathCache.size();
	}

	/**
	 * Sorting by File Size Ascending
	 * 
	 */
	public void sortByFileSizeAsc() {

		sortBy(new SortBySizeAsc());
	}

	/**
	 * Sorting by File Size Descending
	 * 
	 */
	public void sortByFileSizeDesc() {

		sortBy(new SortBySizeDesc());
	}

	/**
	 * Sorting by last Accessed Ascending
	 * 
	 */
	public void sortByAccessedAsc() {

		sortBy(new SortByAccessedAsc());
	}

	/**
	 * Sorting by last Accessed Descending
	 * 
	 */
	public void sortByAccessedDesc() {

		sortBy(new SortByAccessedDesc());
	}

	/**
	 * Common method for sorting strategies
	 * 
	 * @param sortStrategy Sorting strategy
	 */
	private void sortBy(Comparator<Entry<String, FileInfo>> sortStrategy) {

		sortedPathes = new ArrayList<Entry<String, FileInfo>>(pathCache.entrySet());

		Collections.sort(sortedPathes, sortStrategy);
	}

	/**
	 * Sorting by accessed Ascending Comparator
	 *
	 */
	class SortByAccessedAsc implements Comparator<Entry<String, FileInfo>> {
		@Override
		public int compare(Entry<String, FileInfo> e1, Entry<String, FileInfo> e2) {
			Instant v1 = e1.getValue().getAccessed();
			Instant v2 = e2.getValue().getAccessed();
			return v1.compareTo(v2);
		}
	}

	/**
	 * Sorting by accessed Descending Comparator
	 *
	 */
	class SortByAccessedDesc implements Comparator<Entry<String, FileInfo>> {
		@Override
		public int compare(Entry<String, FileInfo> e1, Entry<String, FileInfo> e2) {
			Instant v1 = e1.getValue().getAccessed();
			Instant v2 = e2.getValue().getAccessed();
			return v2.compareTo(v1);
		}
	}

	/**
	 * Sorting by size Ascending Comparator
	 *
	 */
	class SortBySizeAsc implements Comparator<Entry<String, FileInfo>> {

		@Override
		public int compare(Entry<String, FileInfo> e1, Entry<String, FileInfo> e2) {
			Long v1 = e1.getValue().getSize();
			Long v2 = e2.getValue().getSize();
			return v1.compareTo(v2);
		}
	};

	/**
	 * Sorting by size Descending Comparator
	 *
	 */
	class SortBySizeDesc implements Comparator<Entry<String, FileInfo>> {

		@Override
		public int compare(Entry<String, FileInfo> e1, Entry<String, FileInfo> e2) {
			Long v1 = e1.getValue().getSize();
			Long v2 = e2.getValue().getSize();
			return v2.compareTo(v1);
		}
	};

}
