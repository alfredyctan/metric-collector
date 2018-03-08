package org.alf.metric.buffer;

import static org.afc.util.CollectionUtil.*;
import static org.afc.util.OptionalUtil.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.afc.parse.CapturingGroupNamedValueParser;
import org.afc.parse.NamedValueParser;
import org.afc.resolve.Expression;
import org.afc.resolve.Expression.Resolver;
import org.afc.util.ClockUtil;
import org.alf.metric.config.Config.Capture;
import org.alf.metric.config.Config.Source;
import org.alf.metric.launch.LaunchControl;
import org.alf.metric.model.Metric;
import org.alf.metric.writer.MetricWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageBufferListener implements BufferListener {

	private static final String DELETE = "%{!!}";

	private static final Logger logger = LoggerFactory.getLogger(PageBufferListener.class);

	private Source source;

	private Capture capture;

	private Map<String, String> context;

	private NamedValueParser parser;

	private MetricWriter<Metric> writer;

	private Resolver resolver;

	private DateTimeFormatter formatter;

	private LaunchControl launchControl;

	public PageBufferListener(Source source, Capture capture, Map<String, String> context, MetricWriter.Factory<Metric> writerFactory, LaunchControl launchControl) {
		this.source = source;
		this.capture = capture;
		this.context = context;
		this.writer = writerFactory.create();
		this.parser = new CapturingGroupNamedValueParser(capture.getPattern());
		this.resolver = Expression.resolver();
		if (capture.getTimestamp() != null && capture.getTimestamp().getPattern() != null) {
			this.formatter = DateTimeFormatter.ofPattern(capture.getTimestamp().getPattern());
		}
		this.launchControl = launchControl;
	}

	@Override
	public void onReceived(CharSequence recvBuffer) {
		if (!capture.getContains().stream().allMatch(recvBuffer.toString()::contains)) {
			return;
		}

		Map<String, String> entries = parser.parse(recvBuffer);
		if (entries.isEmpty()) {
			return;
		}
		Metric metric = new Metric();

		//Name
		metric.setName(resolver.resolve(capture.getName(), entries));

		//Time
		if (formatter != null && capture.getTimestamp().getTag() != null) {
			metric.setTime(OffsetDateTime.parse(entries.get(capture.getTimestamp().getTag()), formatter).plus(launchControl.getAdjustment(), ChronoUnit.MILLIS));
		} else {
			metric.setTime(ClockUtil.offsetDateTime());
		}

		//Tag
		metric.setTags(new HashMap<>(context));
		metric.getTags().putAll(entries);
		forEach(capture.getTags(), (k, v) -> {
			if (v.equals(DELETE)) {
				metric.getTags().remove(k);
			} else {
				metric.getTags().put(k, resolver.resolve(v, entries));
			}
		});

		//Index
		ifNotNull(capture.getIndexes(), indexes -> {
			metric.setIndexes(new HashMap<>());
			indexes.forEach((k, v) -> {
				metric.getIndexes().put(k, resolver.resolve(v, entries));
			});
		});

		writer.write(metric);
	}

	@Override
	public void stop() {
		writer.dispose();
	}
}
