package org.alf.metric.collector;

import static java.nio.file.StandardOpenOption.*;
import static org.afc.util.MathUtil.*;

import java.io.File;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.afc.concurrent.VerboseRunnable;
import org.afc.util.IOUtil;
import org.alf.metric.buffer.BufferListener;
import org.alf.metric.config.Config.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.SneakyThrows;

public class PeriodicNioFileCollector implements FileCollector<File> {

	private static final Logger logger = LoggerFactory.getLogger(PeriodicNioFileCollector.class);

	private Source source;

	private ScheduledExecutorService executorService;

	private FileCollector<FileChannel> collector;

	private Path path;

	private FileChannel fileChannel;

	private long createTime;

	private ScheduledFuture<?> scanner;

	private ScheduledFuture<?> checker;

	public PeriodicNioFileCollector(Source source, Map<String, String> context, FileCollector.Factory factory, ScheduledExecutorService executorService) {
		this.source = source;
		this.executorService = executorService;
		this.collector = factory.createReadCollector(source, context);
	}

	@SneakyThrows
	@Override
	public void collect(File file) {
		path = Paths.get(file.toURI());
		logger.info("start collecting file:[{}]", path);
		fileChannel = FileChannel.open(path, READ);
		if (!Boolean.TRUE.equals(source.getBeginning())) {
			fileChannel.position(fileChannel.size());
		}
		createTime = Files.readAttributes(file.toPath(), BasicFileAttributes.class).creationTime().toMillis();
		scanner = executorService.scheduleWithFixedDelay(new VerboseRunnable(this::doScan), random(source.getInterval()), source.getInterval(), TimeUnit.MILLISECONDS);
		checker = executorService.scheduleWithFixedDelay(new VerboseRunnable(this::doFileRollingCheck), random(source.getInterval()), source.getInterval(), TimeUnit.MILLISECONDS);
	}

	private void doScan() {
		logger.debug("collecting file:{}", path);
		collector.collect(fileChannel);
	}

	@SneakyThrows
	private void doFileRollingCheck() {
		if (!path.toFile().exists()) {
			logger.info("[{}] is removed, stop file scanning", path);
			stop();
			return;
		}
		try {
			if (fileChannel.size() < fileChannel.position()) {
				logger.info("same file channel rolling:[{}]", path);
				fileChannel.position(0);
			} else {
				long now = Files.readAttributes(path, BasicFileAttributes.class).creationTime().toMillis();
				if (now > createTime) {
					logger.info("new file channel rolling with same path:[{}]", path);
					createTime = now;
					fileChannel.close();
					fileChannel = FileChannel.open(path, READ);
				}
			}
		} catch (ClosedChannelException e) {
			logger.info("file channel [{}] closed", path);
		}
	}

	@Override
	public void stop() {
		collector.stop();
		checker.cancel(true);
		scanner.cancel(true);
		IOUtil.close(fileChannel);
	}

	@Override
	public FileCollector<File> addBufferListener(BufferListener listener) {
		this.collector.addBufferListener(listener);
		return this;
	}

	@Override
	public FileCollector<File> addBufferListeners(List<BufferListener> listeners) {
		this.collector.addBufferListeners(listeners);
		return this;
	}
}
