package de.dlr.proseo.model.fs.alluxio;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.concurrent.ThreadSafe;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Joiner;
import com.google.common.io.Closer;

import alluxio.AlluxioURI;
import alluxio.Constants;
import alluxio.client.file.FileInStream;
import alluxio.client.file.FileOutStream;
import alluxio.client.file.FileSystem;
import alluxio.client.file.URIStatus;
import alluxio.exception.AlluxioException;
import alluxio.exception.ExceptionMessage;
import alluxio.exception.FileAlreadyExistsException;
import alluxio.exception.FileDoesNotExistException;
import alluxio.exception.InvalidPathException;
import alluxio.grpc.CreateDirectoryPOptions;
import alluxio.grpc.CreateFilePOptions;
import alluxio.grpc.OpenFilePOptions;
import alluxio.grpc.ReadPType;
import alluxio.grpc.WritePType;
import alluxio.util.io.PathUtils;

@ThreadSafe
public final class AlluxioOps {
	/** Logger for this class */
	private static Logger logger = LoggerFactory.getLogger(AlluxioOps.class);
	private final static FileSystem fs = FileSystem.Factory.get();

	private static final int COPY_FROM_LOCAL_BUFFER_SIZE_DEFAULT = 8 * Constants.MB;
	private static int mCopyFromLocalBufferSize = COPY_FROM_LOCAL_BUFFER_SIZE_DEFAULT;
	private static final int COPY_TO_LOCAL_BUFFER_SIZE_DEFAULT = 64 * Constants.MB;
	private static int mCopyToLocalBufferSize = COPY_TO_LOCAL_BUFFER_SIZE_DEFAULT;
	private static final int mThread = Runtime.getRuntime().availableProcessors() * 2;
	private static final String COPY_SUCCEED_MESSAGE = "Copied %s to %s";
	private static final String COPY_FAIL_MESSAGE = "Failed to copy %s to %s";


	/**
	 * Copies a file or a directory from the Alluxio filesystem to the local filesystem.
	 *
	 * @param srcPath the source {@link AlluxioURI} (could be a file or a directory)
	 * @param dstPath the {@link AlluxioURI} of the destination in the local filesystem
	 */
	public static Boolean copyToLocal(AlluxioURI srcPath, AlluxioURI dstPath, ReadPType readType) {
		try {
			URIStatus srcStatus = fs.getStatus(srcPath);
			//logger.info(srcStatus.toString());
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
							new AlluxioURI(dstPath.getScheme(), dstPath.getAuthority(), subDstFile.getPath()),
							readType
							);
				}

				if (errorMessages.size() != 0) {
					logger.error(Joiner.on('\n').join(errorMessages));
				}
			} else {
				Boolean transfer = copyFileToLocal(srcPath, dstPath, readType);
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
	private static Boolean copyFileToLocal(AlluxioURI srcPath, AlluxioURI dstPath, ReadPType readType) {
		File dstFile = new File(dstPath.getPath());
		String randomSuffix =
				String.format(".%s_copyToLocal_", RandomStringUtils.randomAlphanumeric(8));
		File outputFile;
		if (dstFile.isDirectory()) {
			outputFile = new File(PathUtils.concatPath(dstFile.getAbsolutePath(), srcPath.getName()));
		} else {
			File subdirs = new File(FilenameUtils.getPath(dstPath.getPath()));
			subdirs.mkdirs();
			outputFile = dstFile;
		}
		File tmpDst = new File(outputFile.getPath() + randomSuffix);

		try (Closer closer = Closer.create()) {

			FileInStream is = closer.register(fs.openFile(srcPath,OpenFilePOptions.newBuilder().setReadType(readType).build()));
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
			logger.info("Copied alluxio://{} to file://{}",srcPath,outputFile.getPath());
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

	/**
	 * Creates a directory in the Alluxio filesystem space. It will not throw any exception if the
	 * destination directory already exists.
	 *
	 * @param dstPath the {@link AlluxioURI} of the destination directory which will be created
	 */
	private static void createDstDir(AlluxioURI dstPath, CreateDirectoryPOptions opt) throws AlluxioException, IOException {
		try {
			fs.createDirectory(dstPath, opt);
		} catch (FileAlreadyExistsException e) {
			// it's fine if the directory already exists
		}

		URIStatus dstStatus = fs.getStatus(dstPath);
		if (!dstStatus.isFolder()) {
			throw new InvalidPathException(ExceptionMessage.DESTINATION_CANNOT_BE_FILE.getMessage());
		}
	}

	/**
	 * A thread pool executor for asynchronous copy.
	 *
	 * Copy tasks can send messages to an output stream in a thread safe way.
	 */
	@ThreadSafe
	private static final class CopyThreadPoolExecutor {
		private static final class CopyException extends Exception {
			/**
			 * 
			 */
			private static final long serialVersionUID = -9149481290182820739L;

			public CopyException(AlluxioURI src, AlluxioURI dst, Exception cause) {
				super(String.format(COPY_FAIL_MESSAGE, src, dst), cause);
			}
		}

		private static final Object MESSAGE_DONE = new Object();

		private final ThreadPoolExecutor mPool;
		/**
		 * Message queue used by mPrinter.
		 * Only supports objects of type String and Exception.
		 * String messages will be printed to stdout;
		 * Exception messages will be printed to stderr and collected into mExceptions.
		 * Other types of messages will be ignored.
		 */
		private final BlockingQueue<Object> mMessages;
		private final ConcurrentLinkedQueue<Exception> mExceptions;
		private final Thread mPrinter;
		private final FileSystem mFileSystem;
		private final AlluxioURI mPath;

		/**
		 * Creates a new thread pool with the specified number of threads,
		 * specify the output stream for tasks to send messages to, and
		 * starts the background thread for printing messages.
		 *
		 * NOTE: needs to call {@link #shutdown()} to release resources.
		 *
		 * @param threads number of threads
		 * @param stdout the stdout stream for tasks to send messages to
		 * @param stderr the stderr stream for tasks to send error messages to
		 * @param fileSystem the Alluxio filesystem used to delete path
		 * @param path the path to delete on shutdown when it's empty, otherwise can be {@code null}
		 */
		public CopyThreadPoolExecutor(int threads,
				FileSystem fileSystem, AlluxioURI path) {
			mPool = new ThreadPoolExecutor(threads, threads,
					1, TimeUnit.SECONDS, new ArrayBlockingQueue<>(threads * 2),
					new ThreadPoolExecutor.CallerRunsPolicy());
			mMessages = new LinkedBlockingQueue<>();
			mExceptions = new ConcurrentLinkedQueue<>();
			mPrinter = new Thread(() -> {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						Object message = mMessages.take();
						if (message == MESSAGE_DONE) {
							break;
						}
						if (message instanceof String) {
							logger.info(message.toString());
						} else if (message instanceof CopyException) {
							CopyException e = (CopyException) message;
							logger.warn(messageAndCause(e));
						} else {
							logger.error("Unsupported message type " + message.getClass()
							+ " in message queue of copy thread pool");
						}
					} catch (InterruptedException e) {
						break;
					}
				}
			});
			mPrinter.start();
			mFileSystem = fileSystem;
			mPath = path;
		}

		/**
		 * Submits a copy task, returns immediately without waiting for completion.
		 *
		 * @param task the copy task
		 */
		public <T> void submit(Callable<T> task) {
			mPool.submit(task);
		}

		/**
		 * Sends a message to the pool to indicate that src is copied to dst,
		 * the message will be displayed in the stdout stream.
		 *
		 * @param src the source path
		 * @param dst the destination path
		 * @throws InterruptedException if interrupted while waiting to send the message
		 */
		public void succeed(AlluxioURI src, AlluxioURI dst) throws InterruptedException {
			mMessages.put(String.format(COPY_SUCCEED_MESSAGE, "file://"+src, "alluxio:/"+dst));
		}

		/**
		 * Sends the exception to the pool to indicate that src fails to be copied to dst,
		 * the exception will be displayed in the stderr stream.
		 *
		 * @param src the source path
		 * @param dst the destination path
		 * @param cause the cause of the failure
		 * @throws InterruptedException if interrupted while waiting to send the exception
		 */
		public void fail(AlluxioURI src, AlluxioURI dst, Exception cause) throws InterruptedException {
			CopyException exception = new CopyException(src, dst, cause);
			mExceptions.add(exception);
			mMessages.put(exception);
		}

		/**
		 * Waits until all asynchronous copy tasks succeed or fail, then shuts down the thread pool,
		 * joins the printer thread, and deletes the copy destination in case of error.
		 *
		 * @throws IOException summarizing all exceptions thrown in the submitted tasks and in shutdown
		 */
		public void shutdown() throws IOException {
			mPool.shutdown();
			try {
				mPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				logger.warn("Copy thread pool is interrupted in shutdown.", e);
				Thread.currentThread().interrupt();
				mPool.shutdownNow();
			}

			try {
				mMessages.put(MESSAGE_DONE);
				mPrinter.join();
			} catch (InterruptedException e) {
				logger.warn("Message queue or printer in copy thread pool is interrupted in shutdown.", e);
				Thread.currentThread().interrupt();
				mPrinter.interrupt();
			}

			try {
				if (mPath != null
						&& mFileSystem.exists(mPath)
						&& mFileSystem.getStatus(mPath).isFolder()
						&& mFileSystem.listStatus(mPath).isEmpty()) {
					mFileSystem.delete(mPath);
				}
			} catch (Exception e) {
				mExceptions.add(new IOException("Failed to delete path " + mPath.toString(), e));
			}

			if (!mExceptions.isEmpty()) {
				List<String> errors = new ArrayList<>();
				for (Exception e : mExceptions) {
					logger.error(stacktrace(e));
					errors.add(messageAndCause(e));
				}
				throw new IOException("ERRORS:\n" + Joiner.on("\n").join(errors));
			}
		}

		private String messageAndCause(Exception e) {
			return e.getMessage() + ": " + e.getCause().getMessage();
		}

		private String stacktrace(Exception e) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(os, true);
			e.printStackTrace(ps);
			ps.close();
			return os.toString();
		}
	}

	/**
	 * Upload files from local FS to Alluxio
	 *
	 * @param srcPath the {@link AlluxioURI} of the source file in the local filesystem
	 * @param dstPath the {@link AlluxioURI} of the destination
	 * @throws InterruptedException when failed to send messages to the pool
	 * @return true/false
	 */

	private static Boolean copyFromLocalFile(AlluxioURI srcPath, AlluxioURI dstPath, WritePType writeType)
			throws AlluxioException, IOException {
		File src = new File(srcPath.getPath());
		if (src.isDirectory()) {
			throw new IOException("Source " + src.getAbsolutePath() + " is not a file.");
		}
		// If the dstPath is a directory, then it should be updated to be the path of the file where
		// src will be copied to.
		if (fs.exists(dstPath) && fs.getStatus(dstPath).isFolder()) {
			dstPath = dstPath.join(src.getName());
		}
		
		FileOutStream os = null;
		try (Closer closer = Closer.create()) {
			// create output dirs in DFS
			createDstDir(dstPath.getParent(), CreateDirectoryPOptions.newBuilder().setRecursive(true).setWriteType(writeType).build());

			os = closer.register(fs.createFile(dstPath, CreateFilePOptions.newBuilder().setWriteType(writeType).build()));
			FileInputStream in = closer.register(new FileInputStream(src));
			FileChannel channel = closer.register(in.getChannel());
			ByteBuffer buf = ByteBuffer.allocate(mCopyFromLocalBufferSize);
			while (channel.read(buf) != -1) {
				buf.flip();
				os.write(buf.array(), 0, buf.limit());
			}
			logger.info("Copied file://{} to alluxio:/{}",srcPath, dstPath);
			return true;
		} catch (FileAlreadyExistsException e) {
			logger.warn("File alluxio:/{} already exists",dstPath);
			return false;
		} catch (IOException e) {
			// Close the out stream and delete the file, so we don't have an incomplete file lying
			// around.
			logger.error(e.getMessage());
			if (os != null) {
				os.cancel();
				if (fs.exists(dstPath)) {
					fs.delete(dstPath);
				}
			}
			return false;
		} catch (AlluxioException e) {
			logger.error(e.getMessage());
			return false;
		}
	}


	/**
	 * Asynchronously copies a file or directory specified by srcPath from the local filesystem to
	 * dstPath in the Alluxio filesystem space, assuming dstPath does not exist.
	 *
	 * @param srcPath the {@link AlluxioURI} of the source file in the local filesystem
	 * @param dstPath the {@link AlluxioURI} of the destination
	 * @throws InterruptedException when failed to send messages to the pool
	 */
	private static void asyncCopyLocalPath(CopyThreadPoolExecutor pool, AlluxioURI srcPath,
			AlluxioURI dstPath, WritePType writeType) throws InterruptedException {
		File src = new File(srcPath.getPath());
		if (!src.isDirectory()) {
			pool.submit(() -> {
				try {
					copyFromLocalFile(srcPath, dstPath,writeType);
					pool.succeed(srcPath, dstPath);
				} catch (Exception e) {
					pool.fail(srcPath, dstPath, e);
				}
				return null;
			});
		} else {
			try {
				fs.createDirectory(dstPath);
			} catch (FileAlreadyExistsException e) {
				// it's fine if the directory already exists
			}
			catch (Exception e) {
				pool.fail(srcPath, dstPath, e);
				return;
			}
			File[] fileList = src.listFiles();
			if (fileList == null) {
				pool.fail(srcPath, dstPath,
						new IOException(String.format("Failed to list directory %s.", src)));
				return;
			}
			for (File srcFile : fileList) {
				AlluxioURI newURI = new AlluxioURI(dstPath, new AlluxioURI(srcFile.getName()));
				asyncCopyLocalPath(pool,
						new AlluxioURI(srcPath.getScheme(), srcPath.getAuthority(), srcFile.getPath()),
						newURI, writeType);
			}
		}
	}

	/**
	 * copies a file or directory specified by srcPath from the local filesystem to
	 * dstPath in the Alluxio filesystem space, assuming dstPath does not exist.
	 *
	 * @param srcPath the {@link AlluxioURI} of the source file in the local filesystem
	 * @param dstPath the {@link AlluxioURI} of the destination
	 * @param writeType {@link WritePType} for controlling caching & persistence
	 * @throws InterruptedException when failed to send messages to the pool
	 */
	public static Boolean copyFromLocal(AlluxioURI srcPath, AlluxioURI dstPath, WritePType writeType)
			throws AlluxioException, IOException {

		Boolean msg = false;
		List<AlluxioURI> srcPaths = new ArrayList<>();
		File src = new File(srcPath.getPath());
		if (src.isDirectory()) {
			File[] files = src.listFiles();
			if (files == null) {
				msg = false;
				throw new IOException(String.format("Failed to list files for directory %s", src));
			}
			for (File f : files) {
				srcPaths.add(new AlluxioURI(srcPath.getScheme(), srcPath.getAuthority(), f.getPath()));
			}
		} else {
			srcPaths.add(srcPath);
		}

		if (srcPaths.size() == 1) {
			Boolean single = copyFromLocalFile(srcPaths.get(0), dstPath, writeType);
			msg = single;
		} else {
			CopyThreadPoolExecutor pool = new CopyThreadPoolExecutor(mThread,
					fs, fs.exists(dstPath) ? null : dstPath);
			try {
				createDstDir(dstPath, CreateDirectoryPOptions.newBuilder().setRecursive(true).setWriteType(writeType).build());
				for (AlluxioURI src1 : srcPaths) {
					AlluxioURI dst = new AlluxioURI(dstPath, new AlluxioURI(src.getName()));
					asyncCopyLocalPath(pool, src1, dst, writeType);
				}
				logger.info(String.format(COPY_SUCCEED_MESSAGE, "file://"+srcPath, "alluxio://"+dstPath));
				msg = true;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				msg = false;
				throw new RuntimeException(e);
			} finally {
				pool.shutdown();
			}
		}
		return msg;
	}
}
