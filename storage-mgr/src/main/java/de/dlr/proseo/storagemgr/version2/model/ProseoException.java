package de.dlr.proseo.storagemgr.version2.model;

/**
 * Proseo Error Interface // UserException 
 * 
 * @author Denys Chaykovskiy
 *
 */

// TODO: Another names - UserException, OperatorException, BookExeption - discuss with Thomas and Katharina

public interface ProseoException {
	
	/** Gets Exception ID t */
	public int getProseoExceptionId(); 
	
	/** Gets Exception Message */
	public String getProseoExceptionMessage();
	
	/** Gets full Exception Message */
	public String getFullExceptionMessage();
	
	/** Checks if exception is an error  */
	public boolean isError();
	
	/** Checks if the exception is a warning  */
	public boolean isWarning(); 
}
