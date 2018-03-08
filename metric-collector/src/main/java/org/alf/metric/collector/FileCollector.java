package org.alf.metric.collector;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.alf.metric.buffer.BufferListener;
import org.alf.metric.config.Config.Source;

public interface FileCollector<S> {

	public void collect(S source);

	public void stop();

	public FileCollector<S> addBufferListener(BufferListener listener);

	public FileCollector<S> addBufferListeners(List<BufferListener> listeners);

	public static interface Factory {

		public <T> FileCollector<T> createScanCollector(Source source, ScheduledExecutorService executorService, Map<String, String> context);

		public <T> FileCollector<T> createReadCollector(Source source, Map<String, String> context);
	}
}
