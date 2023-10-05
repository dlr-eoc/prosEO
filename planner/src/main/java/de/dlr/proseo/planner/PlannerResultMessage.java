/**
 * package-info.java
 * 
 * (C) 2023 Dr. Bassler & Co. Managementberatung GmbH
 */
package de.dlr.proseo.planner;

import org.slf4j.event.Level;

import de.dlr.proseo.logging.messages.ProseoMessage;

/**
 * Wrapper class for ProseoMessages
 * 
 * @author Ernst Melchinger
 */
public class PlannerResultMessage {

	/** The resulting prosEO message. */
	private ProseoMessage message;

	/** The complete message text. */
	private String text;

	/**
	 * Get the prosEO message.
	 *
	 * @return The prosEO message.
	 */
	public ProseoMessage getMessage() {
		return message;
	}

	/**
	 * Get the complete message text.
	 *
	 * @return The complete message text.
	 */
	public String getText() {
		if (text == null) {
			if (message == null) {
				return "";
			} else {
				message.getMessage();
			}
		} else {
			return text;
		}
		return text;
	}

	/**
	 * Set the prosEO message.
	 *
	 * @param message The prosEO message to set.
	 */
	public void setMessage(ProseoMessage message) {
		this.message = message;
	}

	/**
	 * Set the complete message text.
	 *
	 * @param text The complete message text to set.
	 */
	public void setText(String text) {
		if (this.text != null && text != null) {
			this.text = String.join(";; ", this.text, text);
		} else if (text != null) {
			this.text = text;
		}
	}

	/**
	 * Create a new instance of PlannerResultMessage with a given prosEO message.
	 *
	 * @param msg The prosEO message.
	 */
	public PlannerResultMessage(ProseoMessage msg) {
		message = msg;
	}

	/**
	 * Copy the contents of another PlannerResultMessage to this instance.
	 *
	 * @param msg The PlannerResultMessage to copy.
	 * @return This PlannerResultMessage instance.
	 */
	public PlannerResultMessage copy(PlannerResultMessage msg) {
		setMessage(msg.getMessage());
		setText(msg.getText());
		return this;
	}

	/**
	 * Check if the message is considered successful. A message is considered successful unless its level is error.
	 *
	 * @return True if the message is successful, false otherwise.
	 */
	public boolean getSuccess() {
		if (message == null) {
			return false;
		} else {
			return message.getSuccess();
		}
	}

	/**
	 * Get the message code.
	 *
	 * @return The message code.
	 */
	public int getCode() {
		if (message == null) {
			return 0;
		} else {
			return message.getCode();
		}
	}

	/**
	 * Get the message level.
	 *
	 * @return The message level.
	 */
	public Level getLevel() {
		if (message == null) {
			return Level.ERROR;
		} else {
			return message.getLevel();
		}
	}

}