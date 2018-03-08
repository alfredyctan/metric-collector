package org.alf.metric.writer;

public interface MetricWriter<T> {

	public void write(T metric);

	public void dispose();

	public static interface Factory<T> {

		public MetricWriter<T> create();

	}
}
