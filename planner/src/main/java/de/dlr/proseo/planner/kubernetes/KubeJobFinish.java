package de.dlr.proseo.planner.kubernetes;

public class KubeJobFinish extends Thread {

	private String jobName;
	private KubeJob kubeJob;
	
	public KubeJobFinish(KubeJob aJob, String aJobName) {
		kubeJob = aJob;
		jobName = aJobName;
	}
	
    public void run() {
    	if (kubeJob != null && jobName != null && !jobName.isEmpty()) {
    		boolean found = false;
    		int i = 0;
    		while (!found && i < 50) {
    	        try {
    	        	found = kubeJob.getFinishInfo(jobName);
    	            sleep(5000);
    	          }
    	          catch(InterruptedException e) {
    	          }
    			
    		}
    	}
    }    	
}
