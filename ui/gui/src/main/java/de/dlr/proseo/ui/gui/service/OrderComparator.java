package de.dlr.proseo.ui.gui.service;

import java.util.Comparator;
import java.util.HashMap;

public class OrderComparator implements Comparator {

	String key = "id";
	
	Boolean up = true;
	
	/**
	 * @param key
	 * @param up
	 */
	public OrderComparator(String key, Boolean up) {
		this.key = key;
		this.up = up;
	}

	@Override
	public int compare(Object o1, Object o2) {
		if ((o1 instanceof HashMap) && (o2 instanceof HashMap)) {
			HashMap h1 = (HashMap) o1;
			HashMap h2 = (HashMap) o2;
			if (h1.get(key) != null && h2.get(key) != null) {
				if (h1.get(key) instanceof Integer) {
					Integer v1 = (Integer) h1.get(key);
					Integer v2 = (Integer) h2.get(key);
					if (up) {
						return Integer.compare(v1, v2);
					} else {
						return Integer.compare(v1, v2) * -1;
					}
				} else if (h1.get(key) instanceof String) {
					String v1 = (String) h1.get(key);
					String v2 = (String) h2.get(key);
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
