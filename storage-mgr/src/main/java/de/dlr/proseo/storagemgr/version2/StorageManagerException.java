package de.dlr.proseo.storagemgr.version2;

import java.io.IOException;

import de.dlr.proseo.storagemgr.version2.model.ProseoException;

public class StorageManagerException extends IOException implements ProseoException  {

	/**
	 * 
	 */
	private static final long serialVersionUID = -600031918237878382L;

	public static class ExceptionType {
		
		public static int NO_BUCKET_EXCEPTION = 123;  
		
		int id;
		String message;
		boolean isError;

		private ExceptionType(int id, String message, boolean error) {
			this.id = id;
			this.message = message;
			this.isError = error;
		}
		
		// id is enum
		private ExceptionType(int id) { 
			this.id = id;
			// find message and error in array 
			
			
		}
		
		public int getId() {	
			return id; 
		}
	
		public String getMessage() { 
			return message; 
		}
		
		public boolean isError() { 
			return isError; 
		}
		
		public boolean isWarning() {
			return !isError; 
		}
	}
	
	static ExceptionType[] myList = { new ExceptionType(ExceptionType.NO_BUCKET_EXCEPTION, "Bucket not found", true),
			new ExceptionType(2345, "File cannot be downloaded", true) };
		
	
	private ExceptionType exceptionType; 
	
	/** Exception data, like a path to the file or the bucket name */
	private String exceptionData = ""; 
	
	/** Exception string, can be loaded from external sources */
	public StorageManagerException(IOException ioexception, int exceptionId, String exceptionData) { 
		super(ioexception); 
		exceptionType = new ExceptionType(exceptionId);
		this.exceptionData = exceptionData; 
	}
	
	/** Gets Exception ID */
	public int getProseoExceptionId() { 
		return exceptionType.getId(); 
	}
	
	/** Gets Exception Message */
	public String getProseoExceptionMessage() { 
		return exceptionType.getMessage(); 
	}
	
	/** Gets full Exception Message */
	public String getFullExceptionMessage() { 
		return "(" + getProseoExceptionId() + ") " + getProseoExceptionMessage() + ": " + exceptionData;  
	}
	
	/** Checks if exception is an error  */
	public boolean isError() { 
		return exceptionType.isError();
	}
	
	/** Checks if the exception is a warning  */
	public boolean isWarning() { 
		return !exceptionType.isError();
	}	
	
	
	
	

}
