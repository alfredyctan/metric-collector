package org.alf.metric.collector;

import org.alf.metric.buffer.BufferListener;

public interface FileCollector<S> {

	public void collect(S source);
	
	public void addBufferListener(BufferListener listener);
}
