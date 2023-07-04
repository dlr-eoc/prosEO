/**
 * OrderService.java
 *
 * (C) 2020 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.ui.gui;

import org.springframework.http.HttpStatus;

/**
 * A collector for information on orders, namely order status, order id, and an error message
 *
 * @author Ernst Melchinger
 */
public class OrderInfo {

	/** The status of request */
	HttpStatus status;

	/** The order id */
	String orderId = "0";

	/** An error message */
	String message = "";

	/**
	 * A constructor which sets the order's status, id and error message
	 *
	 * @param status  the order status to set
	 * @param id      the order id to set
	 * @param message the error message to set
	 */
	public OrderInfo(HttpStatus status, String orderId, String message) {
		this.status = status;
		this.orderId = orderId;
		this.message = message;
	}

	/**
	 * Returns the HTTP status
	 *
	 * @return the HTTP status
	 */
	public HttpStatus getStatus() {
		return status;
	}

	/**
	 * Returns the error message
	 *
	 * @return the error message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Returns the order id
	 *
	 * @return the orderId
	 */
	public String getOrderId() {
		return orderId;
	}

	/**
	 * Sets the order id
	 *
	 * @param orderId the orderId to set
	 */
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	/**
	 * Sets the order's status
	 *
	 * @param status the status to set
	 */
	public void setStatus(HttpStatus status) {
		this.status = status;
	}

	/** @param message the message to set */
	public void setMessage(String message) {
		this.message = message;
	}

}