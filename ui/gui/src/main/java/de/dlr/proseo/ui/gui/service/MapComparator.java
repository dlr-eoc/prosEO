/**
 * MapComparator.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui.service;

import java.util.Comparator;
import java.util.HashMap;

/**
 * A class for comparing HashMap objects based on one or two specified keys
 * 
 * @author Ernst Melchinger
 */
public class MapComparator implements Comparator<Object> {

	/** the default value of the first key */
	private static final String MAPKEY_ID = "id";

	/** the first key to compare */
	String key = MAPKEY_ID;

	/** the second key to compare */
	String key2 = null;

	/** the sorting order */
	Boolean up = true;

	/**
	 * Constructor that takes a key and a boolean argument for the sorting order
	 *
	 * @param key the key to look for in compared hash maps
	 * @param up  true, if the comparison is to be made in ascending order, false otherwise
	 */
	public MapComparator(String key, Boolean up) {
		this.key = key;
		this.up = up;
	}

	/**
	 * Constructor that takes two keys and a boolean argument for the sorting order
	 *
	 * @param key  the key to look for in compared hash maps
	 * @param key2 the second key to look for
	 * @param up   true, if the comparison is to be made in ascending order, false otherwise
	 */
	public MapComparator(String key, String key2, Boolean up) {
		this.key = key;
		this.key2 = key2;
		this.up = up;
	}

	/**
	 * Compares two objects, checks whether they are instances of HashMap and then extracts the values associated with the specified
	 * keys from each HashMap.
	 * 
	 * @param object1 the first object (HashMap) to compare
	 * @param object2 the second object (HashMap) to compare
	 * @return a negative value if the first object should be placed before the second object, a positive value if the first object
	 *         should be placed after the second object, or zero if the objects are either not HashMaps, or null, or their keys
	 *         neither integer nor string, or considered equal in terms of sorting
	 */
	@Override
	public int compare(Object object1, Object object2) {

		if ((object1 instanceof HashMap) && (object2 instanceof HashMap)) {
			// If both objects are HashMaps, cast to HashMap
			HashMap<?, ?> hashMap1 = (HashMap<?, ?>) object1;
			HashMap<?, ?> hashMap2 = (HashMap<?, ?>) object2;

			// Compare only if the HashMaps are not null
			if (hashMap1.get(key) != null && hashMap2.get(key) != null) {

				if (hashMap1.get(key) instanceof Integer) {

					// Comparison if keys are integer
					Integer v1 = (Integer) hashMap1.get(key);
					Integer v2 = (Integer) hashMap2.get(key);

					if (key2 != null) {
						v1 += (Integer) hashMap1.get(key2);
						v2 += (Integer) hashMap2.get(key2);
					}

					// Compare according to sort order
					if (up) {
						return Integer.compare(v1, v2);
					} else {
						return Integer.compare(v1, v2) * -1;
					}

				} else if (hashMap1.get(key) instanceof String) {

					// Comparison if keys are strings
					String v1 = (String) hashMap1.get(key);
					if (v1 == null)
						v1 = "";

					String v2 = (String) hashMap2.get(key);
					if (v2 == null)
						v2 = "";
					if (key2 != null) {
						v1 += (String) hashMap1.get(key2);
						v2 += (String) hashMap2.get(key2);
					}

					// Compare according to sort order
					if (up) {
						return v1.compareToIgnoreCase(v2);
					} else {
						return v1.compareToIgnoreCase(v2) * -1;
					}
				}
			}
		}
		return 0;
	}

}