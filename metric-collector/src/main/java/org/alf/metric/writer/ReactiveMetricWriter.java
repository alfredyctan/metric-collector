package org.alf.metric.writer;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.afc.concurrent.ElasticExecutorService;
import org.afc.concurrent.NamedThreadFactory;
import org.alf.metric.config.Config;
import org.alf.metric.model.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.internal.schedulers.ExecutorScheduler;

public class ReactiveMetricWriter implements MetricWriter<Metric> {

	private static final Logger logger = LoggerFactory.getLogger(ReactiveMetricWriter.class);

	private MetricWriter<List<Metric>> metricWriter;

	private BlockingQueue<Metric> metrics;

	private Disposable disposable;

	public ReactiveMetricWriter(Config.Writer writer, Config.Worker worker, MetricWriter.Factory<List<Metric>> factory) {
		this.metricWriter = factory.create();
		this.metrics = new LinkedBlockingQueue<>();
		this.disposable = Observable.<Metric>create(emitter -> {
			try {
				Metric take;
				while ((take = metrics.take()) != null) {
					emitter.onNext(take);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception t) {
				emitter.onError(t);
			} finally {
				emitter.onComplete();
			}
		})
	    .subscribeOn(new ExecutorScheduler(new ElasticExecutorService(1, worker.getWriter(), 60, TimeUnit.SECONDS, "write"), true, true))
	    .observeOn(new ExecutorScheduler(Executors.newSingleThreadExecutor(new NamedThreadFactory("observe")), true, true))
		.buffer(writer.getInterval(), TimeUnit.MILLISECONDS, writer.getBatch())
	    .filter(m -> !m.isEmpty())
		.subscribe(metricWriter::write);
		logger.info("reactive writer:[{}] writing on batch:[{}] for every:[{}]ms", worker.getWriter(), writer.getBatch(), writer.getInterval());
	}

	@Override
	public void write(Metric metric) {
		try {
			metrics.put(metric);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.error("queue interrupted", e);
		}
	}

	@Override
	public void dispose() {
		disposable.dispose();
	}
}
