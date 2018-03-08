package org.alf.metric.collector;

import java.util.LinkedList;
import java.util.List;

import org.alf.metric.writer.MetricWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

public class MockMetricWriter<T> implements MetricWriter<T> {

	private static final Logger logger = LoggerFactory.getLogger(MockMetricWriter.class);

	@Getter
	private List<T> metrics;

	public MockMetricWriter() {
		this.metrics = new LinkedList<>();
	}

	@Override
	public void write(T metric) {
		logger.info("metric received : [{}]", metric);
		metrics.add(metric);
	}

	@Override
	public void dispose() {
	}
}
