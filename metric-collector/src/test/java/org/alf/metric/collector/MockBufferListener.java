package org.alf.metric.collector;

import java.util.LinkedList;
import java.util.List;

import org.alf.metric.buffer.BufferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockBufferListener implements BufferListener {

	private static final Logger logger = LoggerFactory.getLogger(MockBufferListener.class);

	public List<CharSequence> lines;

	public MockBufferListener() {
		this.lines = new LinkedList<>();
	}

	@Override
	public void onReceived(CharSequence buffer) {
		logger.info("on line received : [{}]", buffer);
		lines.add(buffer.toString());
	}

	public List<CharSequence> getLines() {
		return lines;
	}

	@Override
	public void stop() {
	}
}
