package org.alf.metric.collector;

import static java.nio.file.StandardOpenOption.READ;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.afc.concurrent.NamedThreadFactory;
import org.alf.metric.line.LineListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeriodicFileCollector implements FileCollector<File> {

	private static final Logger logger = LoggerFactory.getLogger(PeriodicFileCollector.class);
	
	private FileCollector<FileChannel> collector;

	private ScheduledExecutorService executorService;

	private int interval;
	
	public PeriodicFileCollector() {
		this.interval = 30;
		this.collector = new NioFileCollector(8192);
		this.executorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("file"));
	}

	@Override
	public void collect(File source) {
		Path path = Paths.get(source.toURI());
		try (FileChannel fileChannel = FileChannel.open(path, READ)) {
			executorService.scheduleWithFixedDelay(() -> {
				logger.info("collecting file:{}", path);
				collector.collect(fileChannel); 
			}, (int)(Math.random() * interval), interval, TimeUnit.SECONDS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addLineListener(LineListener listener) {
		this.collector.addLineListener(listener);
	}

	public void setCollector(FileCollector<FileChannel> collector) {
		this.collector = collector;
	}
	
	public void setInterval(int interval) {
		this.interval = interval;
	}
}
