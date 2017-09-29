package org.alf.metric.collector;

import java.util.LinkedList;
import java.util.List;

import org.alf.metric.buffer.BufferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockListener implements BufferListener {

	private static final Logger logger = LoggerFactory.getLogger(MockListener.class);
	
	public List<CharSequence> lines;
	
	public MockListener() {
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
}
