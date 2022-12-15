package de.dlr.proseo.samplewrap;

import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.dlr.proseo.basewrap.BaseWrapper;
import de.dlr.proseo.model.enums.JobOrderVersion;
import de.dlr.proseo.model.joborder.JobOrder;

/**
 * Sample for mission-specific wrapper class to modify the job order file/document at defined points in the run sequence
 *
 */
public class SampleWrapper extends BaseWrapper {

	// Message strings
	private static final String MSG_UPDATED_JOB_ORDER_FILE = "Updated Job Order file:\n";
	
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(SampleWrapper.class);

	
	@Override
	/**
	 * Sample for mission-specific modifications to the job order document before fetching input data
	 * 
	 * @param jobOrderDoc the job order document to modify
	 * @throws WrapperException if some error occurred which forces wrapper termination
	 */
	protected void preFetchInputHook(JobOrder jobOrderDoc) throws WrapperException {	
		if (logger.isTraceEnabled()) logger.trace(">>> preFetchInputHook({})", jobOrderDoc.getFileName());

		// Mission-specific modifications go here ...
	}

	@Override
	/**
	 * Sample for mission-specific modifications to the job order document after fetching input data
	 * 
	 * @param jobOrderDoc the job order document to modify
	 * @throws WrapperException if some error occurred which forces wrapper termination
	 */
	protected void postFetchInputHook(JobOrder jobOrderDoc) throws WrapperException {
		if (logger.isTraceEnabled()) logger.trace(">>> postFetchInputHook({})", jobOrderDoc.getFileName());

		// Mission-specific modifications go here ...
		
		// Save the final JOF in the log for post-mortem analysis
		ByteArrayOutputStream jof = new ByteArrayOutputStream();
		jobOrderDoc.writeXMLToStream(jof, false, JobOrderVersion.valueOf(ENV_JOBORDER_VERSION));
		
		logger.info(MSG_UPDATED_JOB_ORDER_FILE + jof.toString());
	}

	/**
	 * Sample for mission-specific modifications to the final job order document after execution of the processor (before push of
	 * results).
	 * 
	 * @param joWork the job order document to modify
	 * @throws WrapperException if some error occurred which forces wrapper termination
	 */
	@Override
	protected void postProcessingHook(JobOrder joWork) throws WrapperException {
		if (logger.isTraceEnabled()) logger.trace(">>> postProcessingHook({})", joWork.getFileName());

		// Mission-specific modifications go here ...
	}

}
