package org.alf.metric.collector;

import org.alf.metric.line.LineListener;

public interface FileCollector<S> {

	public void collect(S source);
	
	public void addLineListener(LineListener listener);
}
