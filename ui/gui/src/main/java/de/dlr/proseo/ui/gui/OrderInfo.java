package de.dlr.proseo.ui.gui;

import org.springframework.http.HttpStatus;

public class OrderInfo {
	
	/**
	 * The status of request
	 */
	HttpStatus status;
	/**
	 * The order id
	 */
	String orderId = "0";
	/**
	 * An error message
	 */
	String message = "";
	/**
	 * @return the status
	 */
	public HttpStatus getStatus() {
		return status;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @return the orderId
	 */
	public String getOrderId() {
		return orderId;
	}
	/**
	 * @param orderId the orderId to set
	 */
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(HttpStatus status) {
		this.status = status;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	/**
	 * @param status
	 * @param id
	 * @param message
	 */
	public OrderInfo(HttpStatus status, String orderId, String message) {
		this.status = status;
		this.orderId = orderId;
		this.message = message;
	}
	
	
}
