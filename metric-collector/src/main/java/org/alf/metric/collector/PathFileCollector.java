package org.alf.metric.collector;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;
import static org.afc.util.CollectionUtil.*;
import static org.afc.util.MathUtil.*;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.afc.parse.CapturingGroupNamedValueParser;
import org.afc.parse.NamedValueParser;
import org.afc.util.FileUtil;
import org.alf.metric.buffer.BufferListener;
import org.alf.metric.config.Config.Source;
import org.alf.metric.launch.LaunchControl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PathFileCollector implements FileCollector<Source> {

	private static final Logger logger = LoggerFactory.getLogger(PathFileCollector.class);

	private ScheduledExecutorService executorService;

	private ScheduledFuture<?> checker;

	private Source source;

	private FileCollector.Factory factory;

	private Map<String, FileCollector<File>> collectors;

	private List<File> folders;

	private NamedValueParser parser;

	private LaunchControl launchControl;

	private List<BufferListener> listeners;

	public PathFileCollector(Source source, FileCollector.Factory factory, ScheduledExecutorService executorService, LaunchControl launchControl) {
		this.launchControl = launchControl;
		this.source = source;
		this.executorService = executorService;
		this.factory = factory;
		this.collectors = new HashMap<>();
		this.listeners = new LinkedList<>();
	}

	@Override
	public void collect(Source source) {
		if (isNotEmpty(source.getPaths())) {
			folders =  source.getPaths().stream().map(File::new).collect(toList());
		} else {
			folders = asList(new File(System.getProperty("user.dir")));
		}
		parser = new CapturingGroupNamedValueParser(source.getPattern());
		logger.info("collecting [{}] for pattern : {}", folders, parser);
		checker = executorService.scheduleWithFixedDelay(this::doCollect, random(source.getInterval()), source.getInterval(), TimeUnit.MILLISECONDS);
	}

	private void doCollect() {
		if (launchControl != null && !launchControl.isLaunched()) {
			logger.debug("hold for launch:[{}]", source.getPattern());
			return;
		}

		allFiles(folders)
			.stream()
			.filter(file -> {
				return !collectors.keySet().contains(file.getPath());
			})
			.distinct()
			.sorted(FileUtil.BY_LAST_MODIFIED_TIME)
			.forEachOrdered(file -> {
				Map<String, String> context = parser.parse(file.getName());
				if (isNotEmpty(context)) {
					collectors.compute(file.getPath(), (k, v) ->
						factory.createScanCollector(source, executorService, context))
							.addBufferListeners(listeners)
							.collect(file);
				}
			});
		logger.debug("collected {} for pattern : {}", folders, parser);
	}

	private List<File> allFiles(List<File> files) {
		if (files.isEmpty()) {
			return files;
		}
		List<File> all = files.stream().filter(File::isFile).collect(toList());
		all.addAll(allFiles(files.stream().filter(File::isDirectory).map(File::listFiles).map(Stream::of).flatMap(s -> s).collect(toList())));
		return all;
	}

	@Override
	public void stop() {
		checker.cancel(true);
		forEach(collectors.values(), FileCollector::stop);
	}

	@Override
	public FileCollector<Source> addBufferListener(BufferListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
			forEach(collectors.values(), collector -> collector.addBufferListener(listener));
		}
		return this;
	}

	@Override
	public FileCollector<Source> addBufferListeners(List<BufferListener> listeners) {
		synchronized (this.listeners) {
			this.listeners.addAll(listeners);
			forEach(collectors.values(), collector -> collector.addBufferListeners(listeners));
		}
		return this;
	}
}
