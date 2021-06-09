package de.dlr.proseo.storagemgr.fs.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.transfer.*;
import com.amazonaws.services.s3.transfer.Transfer.TransferState;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class V1XferMgrProgress {
	
	private static Logger logger = LoggerFactory.getLogger(V1XferMgrProgress.class);
	// waits for the transfer to complete, catching any exceptions that occur.
	public static void waitForCompletion(Transfer xfer) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> waitForCompletion({})", 
				(null == xfer ? "MISSING" : xfer.getDescription()));
		
		// snippet-start:[s3.java1.s3_xfer_mgr_progress.wait_for_transfer]
		try {
			xfer.waitForCompletion();
		} catch (AmazonServiceException e) {
			logger.error("Amazon service error: " + e.getMessage());
			throw e;
		} catch (AmazonClientException e) {
			logger.error("Amazon client error: " + e.getMessage());
			throw e;
		} catch (InterruptedException e) {
			logger.error("Transfer interrupted: " + e.getMessage());
		}
		// snippet-end:[s3.java1.s3_xfer_mgr_progress.wait_for_transfer]
	}

	// Prints progress while waiting for the transfer to finish.
	public static void showTransferProgress(Transfer xfer) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> showTransferProgress({})", 
				(null == xfer ? "MISSING" : xfer.getDescription()));
		
		// snippet-start:[s3.java1.s3_xfer_mgr_progress.poll]
		// print the transfer's human-readable description
		logger.info(xfer.getDescription());
		// print an empty progress bar...
		printProgressBar(0.0);
		// update the progress bar while the xfer is ongoing.
		do {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
			// Note: so_far and total aren't used, they're just for
			// documentation purposes.
			TransferProgress progress = xfer.getProgress();
			@SuppressWarnings("unused")
			long so_far = progress.getBytesTransferred();
			@SuppressWarnings("unused")
			long total = progress.getTotalBytesToTransfer();
			double pct = progress.getPercentTransferred();
			eraseProgressBar();
			printProgressBar(pct);
		} while (xfer.isDone() == false);
		// print the final state of the transfer.
		TransferState xfer_state = xfer.getState();
		logger.info(": " + xfer_state);
		// snippet-end:[s3.java1.s3_xfer_mgr_progress.poll]
	}

	// Prints progress of a multiple file upload while waiting for it to finish.
	public static void showMultiUploadProgress(MultipleFileUpload multi_upload) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> showMultiUploadProgress({})", 
				(null == multi_upload ? "MISSING" : multi_upload.getDescription()));
		
		// print the upload's human-readable description
		logger.info(multi_upload.getDescription());

		// snippet-start:[s3.java1.s3_xfer_mgr_progress.substranferes]
		Collection<? extends Upload> sub_xfers = new ArrayList<Upload>();
		sub_xfers = multi_upload.getSubTransfers();

		do {
			logger.info("\nSubtransfer progress:\n");
			for (Upload u : sub_xfers) {
				logger.info("  " + u.getDescription());
				if (u.isDone()) {
					TransferState xfer_state = u.getState();
					logger.info("  " + xfer_state);
				} else {
					TransferProgress progress = u.getProgress();
					double pct = progress.getPercentTransferred();
					printProgressBar(pct);
				}
			}

			// wait a bit before the next update.
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				return;
			}
		} while (multi_upload.isDone() == false);
		// print the final state of the transfer.
		TransferState xfer_state = multi_upload.getState();
		logger.info("\nMultipleFileUpload " + xfer_state);
		// snippet-end:[s3.java1.s3_xfer_mgr_progress.substranferes]
	}

	// prints a simple text progressbar: [##### ]
	public static void printProgressBar(double pct) {
		
		if (logger.isTraceEnabled()) logger.trace(">>> printProgressBar({})", pct);
		
		// if bar_size changes, then change erase_bar (in eraseProgressBar) to
		// match.
		final int bar_size = 40;
		final String empty_bar = "                                        ";
		final String filled_bar = "########################################";
		int amt_full = (int) (bar_size * (pct / 100.0));
		logger.info("  [%s%s]", filled_bar.substring(0, amt_full), empty_bar.substring(0, bar_size - amt_full));
	}

	// erases the progress bar.
	public static void eraseProgressBar() {
		
		if (logger.isTraceEnabled()) logger.trace(">>> eraseProgressBar()");
		
		// erase_bar is bar_size (from printProgressBar) + 4 chars.
		final String erase_bar = "\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b";
		logger.info(erase_bar);
	}

	/* --- Methods below never used --- */
	
//	public static void uploadFileWithListener(String file_path, String bucket_name, String key_prefix, boolean pause) {
//		logger.info("file: " + file_path + (pause ? " (pause)" : ""));
//
//		String key_name = null;
//		if (key_prefix != null) {
//			key_name = key_prefix + '/' + file_path;
//		} else {
//			key_name = file_path;
//		}
//
//		// snippet-start:[s3.java1.s3_xfer_mgr_progress.progress_listener]
//		File f = new File(file_path);
//		TransferManager xfer_mgr = TransferManagerBuilder.standard().build();
//		try {
//			Upload u = xfer_mgr.upload(bucket_name, key_name, f);
//			// print an empty progress bar...
//			printProgressBar(0.0);
//			u.addProgressListener(new ProgressListener() {
//				public void progressChanged(ProgressEvent e) {
//					double pct = e.getBytesTransferred() * 100.0 / e.getBytes();
//					eraseProgressBar();
//					printProgressBar(pct);
//				}
//			});
//			// block with Transfer.waitForCompletion()
//			V1XferMgrProgress.waitForCompletion(u);
//			// print the final state of the transfer.
//			TransferState xfer_state = u.getState();
//			logger.info(": " + xfer_state);
//		} catch (AmazonServiceException e) {
//			logger.error(e.getErrorMessage());
//		}
//		xfer_mgr.shutdownNow();
//		// snippet-end:[s3.java1.s3_xfer_mgr_progress.progress_listener]
//	}
//
//	public static void uploadDirWithSubprogress(String dir_path, String bucket_name, String key_prefix,
//			boolean recursive, boolean pause) {
//		logger.info("directory: " + dir_path + (recursive ? " (recursive)" : "") + (pause ? " (pause)" : ""));
//
//		TransferManager xfer_mgr = TransferManagerBuilder.standard().build();
//		try {
//			MultipleFileUpload multi_upload = xfer_mgr.uploadDirectory(bucket_name, key_prefix, new File(dir_path),
//					recursive);
//			// loop with Transfer.isDone()
//			V1XferMgrProgress.showMultiUploadProgress(multi_upload);
//			// or block with Transfer.waitForCompletion()
//			V1XferMgrProgress.waitForCompletion(multi_upload);
//		} catch (AmazonServiceException e) {
//			logger.error(e.getErrorMessage());
//		}
//		xfer_mgr.shutdownNow();
//	}
}