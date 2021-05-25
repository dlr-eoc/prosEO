/**
 * Messages.java
 * 
 * © 2021 Ernst Melchinger, Prophos Informatik GmbH
 */

package de.dlr.proseo.planner;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

/**
 * Class to collect Messages during planner actions (e.g. plan processing order)
 * 
 * @author melchinger
 *
 */
public class Message {
	/**
	 * Last Messages element
	 */
	private Messages msg = Messages.UNDEFINED;
	
	/**
	 * Collected message strings, as displayed in logger. 
	 */
	private ArrayList<String> msgStrings = new ArrayList<String>();
	
	/**
	 * Create new Message
	 * 
	 * @param msg Messages element
	 */
	public Message(Messages msg) {
		setMessage(msg);
	}
	
	/**
	 * Set last Messages element
	 * @param msg Messages element
	 * @return this
	 */
	public Message setMessage(Messages msg) {
		this.msg = msg;
		return this;
	}
	
	/**
	 * Set/Add Message msg:
	 * Set last Messages element and add message strings of msg element
	 * 
	 * @param msg Message
	 * @return this
	 */ 
	public Message setMessage(Message msg) {
		this.msg = msg.getMessage();
		msgStrings.addAll(msg.getMsgStrings());
		return this;
	}
	
	/**
	 * Add string msg to message strings
	 * 
	 * @param msg String
	 * @return this
	 */
	public Message addMsgString(String msg) {
		if (msg != null) {
			msgStrings.add(msg);
		}
		return this;
	}
	
	/**
	 * @return true if no error occurred 
	 */
	public boolean isTrue() {
		return this.msg.isTrue();
	}
	
	/**
	 * @return The Messages element
	 */
	public Messages getMessage() {
		return msg;
	}

	/**
	 * @return The message strings as list.
	 */
	public List<String> getMsgStrings() {
		return msgStrings;
	}
	
	/**
	 * @return The concatenated message strings (divided by " | ")
	 */
	public String getMsgString() {
		String result = null;
		for (String m : msgStrings) {
			if (result == null) {
				result = m;
			} else {
				result += " | ";
				result += m;
			}
		}
		return result;
	}
	
	/**
	 * Log the Message
	 * 
	 * @param logger Logger to log
	 * @param messageParameters Parameters to fill the message string
	 * @return The logged string
	 */
	public String log(Logger logger, Object... messageParameters) {
		msgStrings.add(msg.format(messageParameters));
		return msg.log(logger, messageParameters);
	}

}
