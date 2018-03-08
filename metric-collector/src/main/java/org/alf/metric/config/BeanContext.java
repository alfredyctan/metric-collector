package org.alf.metric.config;

import static java.util.stream.Collectors.*;
import static org.afc.util.CollectionUtil.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.afc.concurrent.NamedThreadFactory;
import org.alf.metric.buffer.AsyncBufferListener;
import org.alf.metric.buffer.BufferListener;
import org.alf.metric.buffer.LineBufferListener;
import org.alf.metric.buffer.PageBufferListener;
import org.alf.metric.collector.FileCollector;
import org.alf.metric.collector.NioFileCollector;
import org.alf.metric.collector.PathFileCollector;
import org.alf.metric.collector.PeriodicNioFileCollector;
import org.alf.metric.config.Config.Capture;
import org.alf.metric.config.Config.Source;
import org.alf.metric.launch.LaunchControl;
import org.alf.metric.launch.LogFileLaunchControl;
import org.alf.metric.launch.NoLaunchControl;
import org.alf.metric.model.Metric;
import org.alf.metric.writer.ElasticMetricWriter;
import org.alf.metric.writer.MetricWriter;
import org.alf.metric.writer.ReactiveMetricWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(Config.class)
@Configuration
public class BeanContext {

	@Autowired
	private Config config;

	@Bean
	public ScheduledExecutorService sourceExecutors() {
		return Executors.newScheduledThreadPool(config.getWorker().getSource(), new NamedThreadFactory("source"));
	}

	@Bean
	public List<PathFileCollector> pathFileCollectors() {
		return stream(config.getSources())
			.map(source -> {
				PathFileCollector collector = new PathFileCollector(
					source,
					fileCollectorFactory(),
					sourceExecutors(),
					launchControl()
				);
				collector.collect(source);
				return collector;
			})
			.collect(toList());
	}

	@Bean
	public LaunchControl launchControl() {
		if (config.getLaunch() == null) {
			return new NoLaunchControl();
		}
		Source source = config.getLaunch().getSource();
		PathFileCollector collector = new PathFileCollector(
			source,
			launchCollectorFactory(),
			sourceExecutors(),
			null
		);
		LogFileLaunchControl launchControl = new LogFileLaunchControl(config.getLaunch(), first(source.getCaptures()), collector);
		collector.collect(source);
		return launchControl;
	}

	@Bean
	public FileCollector.Factory launchCollectorFactory() {
		return new FileCollector.Factory() {

			@Override
			public <T> FileCollector<T> createScanCollector(Source source, ScheduledExecutorService executorService, Map<String, String> context) {
				return (FileCollector<T>)new PeriodicNioFileCollector(source, context, this, executorService);
			}

			@Override
			public <T> FileCollector<T> createReadCollector(Source source, Map<String, String> context) {
				return (FileCollector<T>)new NioFileCollector(source, context, null);
			}
		};
	}

	@Bean
	public FileCollector.Factory fileCollectorFactory() {
		return new FileCollector.Factory() {

			@Override
			public <T> FileCollector<T> createScanCollector(Source source, ScheduledExecutorService executorService, Map<String, String> context) {
				return (FileCollector<T>)new PeriodicNioFileCollector(source, context, this, executorService);
			}

			@Override
			public <T> FileCollector<T> createReadCollector(Source source, Map<String, String> context) {
				return (FileCollector<T>)new NioFileCollector(source, context, bufferListenerFactory());
			}
		};
	}

	@Bean
	public BufferListener.Factory bufferListenerFactory() {
		return new BufferListener.Factory() {

			@Override
			public BufferListener createAsync(Source source, Map<String, String> context) {
				return new AsyncBufferListener(config.getWorker(), source, context, this);
			}

			@Override
			public BufferListener createLine(Source source, Map<String, String> context) {
				return new LineBufferListener(source, context, this);
			}

			@Override
			public BufferListener createPage(Source source, Capture capture, Map<String, String> context) {
				return new PageBufferListener(source, capture, context, reactiveWriterFactory(), launchControl());
			}
		};
	}

	@Bean
	public MetricWriter.Factory<Metric> reactiveWriterFactory() {
		return new MetricWriter.Factory<Metric>() {

			@Override
			public MetricWriter<Metric> create() {
				return new ReactiveMetricWriter(config.getWriters().get("reactive"), config.getWorker(), elasticWriterFactory());
			}
		};
	}


	@Bean
	public MetricWriter.Factory<List<Metric>> elasticWriterFactory() {
		return new MetricWriter.Factory<List<Metric>>() {

			@Override
			public MetricWriter<List<Metric>> create() {
				return new ElasticMetricWriter(config.getWriters().get("elastic-search"));
			}
		};
	}
}
