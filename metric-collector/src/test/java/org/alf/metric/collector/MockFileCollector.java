package org.alf.metric.collector;

import java.util.List;

import org.alf.metric.buffer.BufferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockFileCollector<T> implements FileCollector<T> {

	private static final Logger logger = LoggerFactory.getLogger(MockFileCollector.class);

	@Override
	public void collect(T source) {
		logger.info("mock collect : {}", source);
	}

	@Override
	public void stop() {
		logger.info("mock stop");
	}

	@Override
	public FileCollector<T> addBufferListener(BufferListener listener) {
		logger.info("mock addBufferListener : {}", listener);
		return this;
	}

	@Override
	public FileCollector<T> addBufferListeners(List<BufferListener> listeners) {
		logger.info("mock addBufferListeners : {}", listeners);
		return this;
	}
}
