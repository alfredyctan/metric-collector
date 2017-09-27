package org.alf.metric.collector;

import java.util.LinkedList;
import java.util.List;

import org.alf.metric.line.LineListener;

public class MockListener implements LineListener {

	public List<CharSequence> lines;
	
	public MockListener() {
		this.lines = new LinkedList<>();
	}
	
	@Override
	public void onLineReceived(CharSequence line) {
		lines.add(line.toString());
	}

	public List<CharSequence> getLines() {
		return lines;
	}	
}
