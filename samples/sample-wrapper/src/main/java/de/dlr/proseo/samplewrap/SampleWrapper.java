package de.dlr.proseo.samplewrap;

import de.dlr.proseo.basewrap.BaseWrapper;
import de.dlr.proseo.model.joborder.JobOrder;

/**
 * Sample for mission-specific wrapper class to modify the job order file/document at defined points in the run sequence
 *
 */
public class SampleWrapper extends BaseWrapper {
	
	@Override
	/**
	 * Sample for mission-specific modifications to the job order document before fetching input data
	 * 
	 * @param jobOrderDoc the job order document to modify
	 */
	protected void preFetchInputHook(JobOrder jobOrderDoc) {
		// Mission-specific modifications go here ...
	}

	@Override
	/**
	 * Sample for mission-specific modifications to the job order document after fetching input data
	 * 
	 * @param jobOrderDoc the job order document to modify
	 */
	protected void postFetchInputHook(JobOrder jobOrderDoc) {
		// Mission-specific modifications go here ...
	}

	/**
	 * Sample for mission-specific modifications to the final job order document after execution of the processor (before push of
	 * results).
	 * 
	 * @param joWork the job order document to modify
	 */
	@Override
	protected void postProcessingHook(JobOrder joWork) {
		// Mission-specific modifications go here ...
	}

}
