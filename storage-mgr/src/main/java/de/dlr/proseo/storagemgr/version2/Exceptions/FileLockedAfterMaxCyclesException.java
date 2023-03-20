package de.dlr.proseo.storagemgr.version2.Exceptions;

import java.io.IOException;

/**
 * File Locked After Max Cycles Exception
 * 
 * @author Denys Chaykovskiy
 *
 */
public class FileLockedAfterMaxCyclesException extends IOException {

	/** Serial version UID */
	private static final long serialVersionUID = 2205920887732503437L;

	/**
	 * Parameterless Constructor
	 */
	public FileLockedAfterMaxCyclesException() {
	}

	/**
	 * Constructor with message
	 * 
	 * @param message exception message
	 */
	public FileLockedAfterMaxCyclesException(String message) {
		super(message);
	}
}
