package de.dlr.proseo.storagemgr.version2.s3;

import java.io.IOException;
import de.dlr.proseo.storagemgr.version2.model.ProseoException;

/**
 * Bucket Creation Exception
 * 
 * @author Denys Chaykovskiy
 *
 */
public class BucketCreationException extends IOException implements ProseoException   {
	
	/** serial Version UID */
	private static final long serialVersionUID = 1008286849149480269L;

	/** Exception ID */
	private static final int EXCEPTION_ID = 1234; 
	
	/** Exception string, can be loaded from external sources */
	private static String EXCEPTION_STRING = "Cannot create bucket"; 
	
	/** Exception data, like a path to the file or the bucket name */
	private String exceptionData = ""; 
		
	/** Exception string, can be loaded from external sources */
	public BucketCreationException(IOException ioexception, String exceptionData) { 
		super(ioexception); 
		this.exceptionData = exceptionData; 
	}
	
	/** Gets Exception ID */
	public int getProseoExceptionId() { 
		return EXCEPTION_ID; 
	}
	
	/** Gets Exception Message */
	public String getProseoExceptionMessage() { 
		return EXCEPTION_STRING; 
	}
	
	/** Gets full Exception Message */
	public String getFullExceptionMessage() { 
		return "(" + EXCEPTION_ID + ") " + EXCEPTION_STRING + ": " + exceptionData;  
	}
	
	/** Checks if exception is an error  */
	public boolean isError() { 
		return true;
	}
	
	/** Checks if the exception is a warning  */
	public boolean isWarning() { 
		return false; 
	}	
}
