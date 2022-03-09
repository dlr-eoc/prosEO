package de.dlr.proseo.samplewrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.dlr.proseo.basewrap.BaseWrapper;
import de.dlr.proseo.model.joborder.JobOrder;

/**
 * Sample for mission-specific wrapper class to modify the job order file/document at defined points in the run sequence
 *
 */
public class SampleWrapper extends BaseWrapper {
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
		super.preFetchInputHook(jobOrderDoc);
		// Mission-specific modifications go here ...
	}

	/**
	 * Creates valid container-context JobOrderFile under given path
	 * 
	 * @param jo JobOrder remapped JobOrder object
	 * @param path file path of newly created JOF
	 * @return True/False
	 */
	@Override
	protected void provideContainerJOF(JobOrder jo, String path) throws WrapperException {	
		super.provideContainerJOF(jo, path);
		logJOF(path);
	}
	
	@Override
	/**
	 * Sample for mission-specific modifications to the job order document after fetching input data
	 * 
	 * @param jobOrderDoc the job order document to modify
	 * @throws WrapperException if some error occurred which forces wrapper termination
	 */
	protected void postFetchInputHook(JobOrder jobOrderDoc) throws WrapperException {
		super.postFetchInputHook(jobOrderDoc);
		// Mission-specific modifications go here ...
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
		super.postProcessingHook(joWork);
		// Mission-specific modifications go here ...
	}

}
