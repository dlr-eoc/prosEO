package de.dlr.proseo.samplewrap;

import de.dlr.proseo.basewrap.BaseWrapper;

public class SampleWrapper {
	
	public static void main(String[] args) {
		BaseWrapper myCustomWrapper = new BaseWrapper();
		//base.setENV_JOBORDER_FILE("asasas");
		//base.setENV_JOBORDER_FS_TYPE("POSIX");
		//...
		
		System.exit(myCustomWrapper.run());
	}

}
