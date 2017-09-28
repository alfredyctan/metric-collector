package org.alf.metric.collector;

import static java.nio.file.StandardOpenOption.READ;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.afc.concurrent.NamedThreadFactory;
import org.afc.logging.SDC;
import org.alf.metric.line.LineListener;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeriodicFileCollector implements FileCollector<File> {

	private static final Logger logger = LoggerFactory.getLogger(PeriodicFileCollector.class);
	
	private ScheduledExecutorService executorService;

	private FileCollector<FileChannel> collector;

	private long interval;
	
	private Path path;
	
	private FileChannel fileChannel;
	
	private long createTime;
	
	private ScheduledFuture<?> scanner;

	private ScheduledFuture<?> checker;

	public PeriodicFileCollector() {
		this.interval = 30;
		this.executorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("file"));
	}

	@Override
	public void collect(File source) {
		path = Paths.get(source.toURI());
		try  {
			fileChannel = FileChannel.open(path, READ);
			fileChannel.position(fileChannel.size());
			createTime = Files.readAttributes(source.toPath(), BasicFileAttributes.class).creationTime().toMillis();
			scheduleFileScanner();
			scheduleFileRollingChecker(source);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	private void scheduleFileScanner() {
		scanner = executorService.scheduleWithFixedDelay(() -> {
			logger.info("collecting file:{}", path);
			collector.collect(fileChannel);
		}, (long)(Math.random() * interval), interval, TimeUnit.MILLISECONDS);
	}
	
	private void scheduleFileRollingChecker(File source) {
		checker = executorService.scheduleWithFixedDelay(() -> {
			try {
				if (fileChannel.size() < fileChannel.position()) {
					logger.info("same file rolling:[{}]", path);
					fileChannel.position(0);
				} else {
					long now = Files.readAttributes(source.toPath(), BasicFileAttributes.class).creationTime().toMillis();
					if (now > createTime) {
						logger.info("new file rolling:[{}]", path);
						createTime = now;
						fileChannel.close();
						fileChannel = FileChannel.open(path, READ);
					}
				} 
			} catch (IOException e) {
				logger.error("error on checking file rolling: {}", path);
				logger.debug("details:", e);
			}
		}, (long)(Math.random() * interval), interval, TimeUnit.MILLISECONDS);
	}

	@Override
	public void addLineListener(LineListener listener) {
		this.collector.addLineListener(listener);
	}

	public void setCollector(FileCollector<FileChannel> collector) {
		this.collector = collector;
	}
	
	public void setInterval(long interval) {
		this.interval = interval;
	}

	/*
	 * for unit test clean up
	 */
	public void cancel() {
		checker.cancel(true);
		scanner.cancel(true);
		try {
			fileChannel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
