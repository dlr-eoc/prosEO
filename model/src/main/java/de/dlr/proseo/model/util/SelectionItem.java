/**
 * SelectionItem.java
 * 
 * (C) 2016 - 2019 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.model.util;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import de.dlr.proseo.model.Product;

/**
 * This class describes a selectable item consisting of an item type (auxiliary product type), a validity start
 * time, a validity end time, and the original object for which the selection shall be effected. As this is only a
 * support structure, all instance variables are publicly accessible.
 * 
 * @author Dr. Thomas Bassler
 */
public class SelectionItem {
	/** The product type of the item */
	public String itemType;
	/** The start time of the item validity period */
	public Instant startTime;
	/** The end time of the item validity period */
	public Instant stopTime;
	/** The generation time of the item */
	public Instant generationTime;
	/** The original object belonging to the item */
	public Object itemObject;
	
	/**
	 * Create a selection item wrapped around the given product
	 * 
	 * @param product the product to create the selection item from
	 * @return a selection item with the same product type and start, stop and generation times as the given product
	 */
	public static SelectionItem fromProduct(Product product) {
		return new SelectionItem(product.getProductClass().getProductType(), product.getSensingStartTime(),
				product.getSensingStopTime(), product.getGenerationTime(), product);
	}
	
	/**
	 * Creates a list of selection items from a collection of products
	 * 
	 * @param products the products to add to the selection item list
	 * @return a list of selection items with the same product type and start, stop and generation times as the given products
	 */
	public static List<SelectionItem> asSelectionItems(Collection<Product> products) {
		List<SelectionItem> items = new ArrayList<>();
		
		for (Product product: products) {
			items.add(SelectionItem.fromProduct(product));
		}
		
		return items;
	}
	
	/**
	 * Default constructor
	 */
	public SelectionItem() {}
	
	/**
	 * Convenience constructor to create a SelectionItem with all attributes set at once.
	 * 
	 * @param itemType the item product type to set
	 * @param startTime the start time of the item validity period
	 * @param stopTime the end time of the item validity period
	 * @param generationTime the generation time of the item
	 * @param itemObject the original object belonging to the item
	 */
	public SelectionItem(String itemType, Instant startTime, Instant stopTime, Instant generationTime, Object itemObject) {
		this.itemType = itemType;
		this.startTime = startTime;
		this.stopTime = stopTime;
		this.generationTime = generationTime;
		this.itemObject = itemObject;
	}
	
	/**
	 * Convenience method to set the validity start time from a Unix time value (milliseconds since 1970-01-01T00:00:00 UTC)
	 * 
	 * @param time the time in milliseconds to set the start time from
	 */
	public void setStartTime(long time) {
		startTime = Instant.ofEpochMilli(time);
	}
	
	/**
	 * Convenience method to set the validity start time from a Java date object
	 * 
	 * @param date the date object to set the start time from
	 */
	public void setStartTime(Date date) {
		setStartTime(date.getTime());
	}

	/**
	 * Convenience method to set the validity end time from a Unix time value (milliseconds since 1970-01-01T00:00:00 UTC)
	 * 
	 * @param time the time in milliseconds to set the end time from
	 */
	public void setStopTime(long time) {
		stopTime = Instant.ofEpochMilli(time);
	}
	
	/**
	 * Convenience method to set the validity end time from a Java date object
	 * 
	 * @param date the date object to set the end time from
	 */
	public void setStopTime(Date date) {
		setStopTime(date.getTime());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "{ type: " + itemType + ", start: " + startTime + ", stop: " + stopTime + ", generated: " 
				+ generationTime + ", object: " + itemObject + " }";
	}

	@Override
	public int hashCode() {
		return Objects.hash(generationTime, itemObject, itemType, startTime, stopTime);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof SelectionItem))
			return false;
		SelectionItem other = (SelectionItem) obj;
		return Objects.equals(generationTime, other.generationTime) && Objects.equals(itemObject, other.itemObject)
				&& Objects.equals(itemType, other.itemType) && Objects.equals(startTime, other.startTime)
				&& Objects.equals(stopTime, other.stopTime);
	}
}
