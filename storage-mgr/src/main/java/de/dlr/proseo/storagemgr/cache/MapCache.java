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
 * @author Denys Chaykovskiy
 *
 */
public class MapCache {

	private Map<String, FileInfo> pathCache = new HashMap<>();

	private List<Entry<String, FileInfo>> sortedPathes;

	/**
	 * @return the sortedPathes
	 */
	public List<Entry<String, FileInfo>> getSortedPathes() {

		return sortedPathes;
	}

	/**
	 * @return
	 */
	public Map<String, FileInfo> getCache() {

		Map<String, FileInfo> clonedCache = new HashMap<>();
		clonedCache.putAll(pathCache);

		return clonedCache;
	}

	/**
	 * @param pathKey
	 * @param fileInfo
	 */
	public void put(String pathKey, FileInfo fileInfo) {

		pathCache.put(pathKey, fileInfo);
	}

	/**
	 * @param pathKey
	 * @return
	 */
	public FileInfo get(String pathKey) {

		return pathCache.get(pathKey);
	}

	/**
	 * @param pathKey
	 */
	public void remove(String pathKey) {

		pathCache.remove(pathKey);
	}

	/**
	 * 
	 */
	public void clear() {

		pathCache.clear();
	}

	/**
	 * @param pathKey
	 * @return
	 */
	public boolean containsKey(String pathKey) {

		return pathCache.containsKey(pathKey);
	}
	
	
	/**
	 * @return
	 */
	public int size() {
		
		return pathCache.size();
		
	}

	/**
	 * 
	 */
	public void sortByFileSizeAsc() {

		sortBy(new SortBySizeAsc());
	}

	/**
	 * 
	 */
	public void sortByFileSizeDesc() {

		sortBy(new SortBySizeDesc());
	}

	/**
	 * 
	 */
	public void sortByAccessedAsc() {

		sortBy(new SortByAccessedAsc());
	}

	/**
	 * 
	 */
	public void sortByAccessedDesc() {

		sortBy(new SortByAccessedDesc());
	}

	/**
	 * @param sortStrategy
	 */
	private void sortBy(Comparator<Entry<String, FileInfo>> sortStrategy) {

		sortedPathes = new ArrayList<Entry<String, FileInfo>>(pathCache.entrySet());

		Collections.sort(sortedPathes, sortStrategy);
	}

	/**
	 * @author den
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
	 * @author den
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
	 * @author den
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
	 * @author den
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
