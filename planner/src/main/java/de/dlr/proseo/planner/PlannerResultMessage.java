package de.dlr.proseo.planner;

import org.slf4j.event.Level;

import de.dlr.proseo.logging.logger.ProseoLogger;
import de.dlr.proseo.logging.messages.ProseoMessage;

public class PlannerResultMessage {
	private static ProseoLogger logger = new ProseoLogger(PlannerResultMessage.class);
	
	/**
	 * The resulting proseo message
	 */
	private ProseoMessage message;
	
	/**
	 * The complete message text
	 */
	private String text;

	/**
	 * @return the message
	 */
	public ProseoMessage getMessage() {
		return message;
	}

	/**
	 * @return the text
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
	 * @param message the message to set
	 */
	public void setMessage(ProseoMessage message) {
		this.message = message;
	}

	/**
	 * @param text the text to set
	 */
	public void setText(String text) {
		if (this.text != null && text != null) {
			this.text = String.join(";; ", this.text, text);
		} else if (text != null) {
			this.text = text;
		}
	}
	
	public PlannerResultMessage(ProseoMessage msg) {
		message = msg;
	}

	public PlannerResultMessage copy(PlannerResultMessage msg) {
		setMessage(msg.getMessage());
		setText(msg.getText());
		return this;
	}
	/**
	 * In most cases, a message is considered successful unless its level is error.
	 * 
	 * @return The message success.
	 */
	public boolean getSuccess() {
		if (message == null) {
			return false;
		} else {
			return message.getSuccess();
		}
	}

	/**
	 * Get the message's code.
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
	 * Get the message's level.
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
