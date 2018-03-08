package org.alf.metric.buffer;

import static java.util.stream.Collectors.*;
import static org.afc.util.CollectionUtil.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.afc.concurrent.ElasticExecutorService;
import org.alf.metric.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncBufferListener implements BufferListener {

	private static final Logger logger = LoggerFactory.getLogger(AsyncBufferListener.class);

	private ExecutorService executor;

	private List<BufferListener> listeners;

	public AsyncBufferListener(Config.Worker worker, Config.Source source, Map<String, String> context, BufferListener.Factory factory) {
		this.executor = new ElasticExecutorService(1, worker.getProcess(), 5, TimeUnit.MINUTES, "line");
		this.listeners = stream(source.getCaptures())
			.map(metric -> factory.createPage(source, metric, context))
			.collect(toList());
	}

	@Override
	public void onReceived(CharSequence recvBuffer) {
//		executor.execute(() -> listener.onReceived(recvBuffer));
		executor.execute(() -> {
			synchronized (listeners) {
				for (BufferListener listener : listeners) {
					listener.onReceived(recvBuffer);
				}
			}
		});
	}

	@Override
	public void stop() {
		synchronized (listeners) {
			for (BufferListener listener : listeners) {
				listener.stop();
			}
		}
		executor.shutdown();
	}
}
