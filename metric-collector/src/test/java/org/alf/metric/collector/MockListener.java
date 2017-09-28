package org.alf.metric.collector;

import java.util.LinkedList;
import java.util.List;

import org.alf.metric.line.LineListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockListener implements LineListener {

	private static final Logger logger = LoggerFactory.getLogger(MockListener.class);
	
	public List<CharSequence> lines;
	
	public MockListener() {
		this.lines = new LinkedList<>();
	}
	
	@Override
	public void onLineReceived(CharSequence line) {
		logger.info("on line received : [{}]", line);
		lines.add(line.toString());
	}

	public List<CharSequence> getLines() {
		return lines;
	}	
}
