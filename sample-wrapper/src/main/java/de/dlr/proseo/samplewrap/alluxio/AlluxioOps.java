package de.dlr.proseo.samplewrap.alluxio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.io.Closer;

import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.client.file.FileInStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.URIStatus;
import alluxio.exception.AlluxioException;
import alluxio.exception.FileDoesNotExistException;
import alluxio.util.io.PathUtils;

@ThreadSafe
public final class AlluxioOps {
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(AlluxioOps.class);
	private final static FileSystem fs = FileSystem.Factory.get();
	private static final int COPY_TO_LOCAL_BUFFER_SIZE_DEFAULT = 64 * Constants.MB;
	private static int mCopyToLocalBufferSize = COPY_TO_LOCAL_BUFFER_SIZE_DEFAULT;
	
	
	/**
	   * Copies a file or a directory from the Alluxio filesystem to the local filesystem.
	   *
	   * @param srcPath the source {@link AlluxioURI} (could be a file or a directory)
	   * @param dstPath the {@link AlluxioURI} of the destination in the local filesystem
	   */
	public static Boolean copyToLocal(AlluxioURI srcPath, AlluxioURI dstPath) {
		try {
			URIStatus srcStatus = fs.getStatus(srcPath);
			logger.info(srcStatus.toString());
			File dstFile = new File(dstPath.getPath());
			if (srcStatus.isFolder()) {
				// make a local directory
				if (!dstFile.exists()) {
					if (!dstFile.mkdirs()) {
						logger.error("mkdir failure for directory: " + dstPath);
					} else {
						logger.info("Create directory: " + dstPath);
					}
				}

				List<URIStatus> statuses;
                statuses = fs.listStatus(srcPath);

				List<String> errorMessages = new ArrayList<>();
				for (URIStatus status : statuses) {
					File subDstFile = new File(dstFile.getAbsolutePath(), status.getName());
					copyToLocal(
							new AlluxioURI(srcPath.getScheme(), srcPath.getAuthority(), status.getPath()),
							new AlluxioURI(dstPath.getScheme(), dstPath.getAuthority(), subDstFile.getPath()));
				}

				if (errorMessages.size() != 0) {
					logger.error(Joiner.on('\n').join(errorMessages));
				}
			} else {
				Boolean transfer = copyFileToLocal(srcPath, dstPath);
				if (!transfer) return false;
			}
			return true;
		} catch(AlluxioException e) {
			logger.error(e.getMessage());
			return false;

		} catch (IOException e) {
			logger.error(e.getMessage());
			return false;
		}
	}
	
	
	/**
	 * fetch file from ALLUXIO DistributedFileSystem to local file
	 * 
	 * @param dfs a given instantiated AlluxioFS
	 * @param srcPath URI of Alluxio-Object (e.g. alluxio://bucket/path/to/some/file)
	 * @param dstPath local target filePath
	 * @return Boolean
	 * @throws AlluxioException 
	 * @throws FileDoesNotExistException 
	 * @throws IOException 
	 */
	private static Boolean copyFileToLocal(AlluxioURI srcPath, AlluxioURI dstPath) {
		File dstFile = new File(dstPath.getPath());
		String randomSuffix =
				String.format(".%s_copyToLocal_", RandomStringUtils.randomAlphanumeric(8));
		File outputFile;
		if (dstFile.isDirectory()) {
			outputFile = new File(PathUtils.concatPath(dstFile.getAbsolutePath(), srcPath.getName()));
		} else {
			outputFile = dstFile;
		}
		File tmpDst = new File(outputFile.getPath() + randomSuffix);
		logger.info(tmpDst.getAbsolutePath());

		try (Closer closer = Closer.create()) {

			FileInStream is = closer.register(fs.openFile(srcPath));
			FileOutputStream out = closer.register(new FileOutputStream(tmpDst));
			byte[] buf = new byte[mCopyToLocalBufferSize];
			int t = is.read(buf);
			while (t != -1) {
				out.write(buf, 0, t);
				t = is.read(buf);
			}
			if (!tmpDst.renameTo(outputFile)) {
				logger.error("Failed to rename " + tmpDst.getPath() + " to destination " + outputFile.getPath());
				return false;
			}
			logger.info("Copied " + srcPath + " to " + "file://" + outputFile.getPath());
			return true;
		} catch (IOException e) {
			logger.error(e.getMessage());
			return false;
		} catch (FileDoesNotExistException e) {
			logger.error(e.getMessage());
			return false;
		} catch (AlluxioException e) {
			logger.error(e.getMessage());
			return false;
		} finally {
			tmpDst.delete();
		}
	}
	
}
