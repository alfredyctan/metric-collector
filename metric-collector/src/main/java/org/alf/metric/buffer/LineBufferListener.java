package org.alf.metric.buffer;

import java.util.Map;

import org.alf.metric.parse.NamedValueParser;

public class LineBufferListener implements BufferListener {

	private NamedValueParser parser;
	
	@Override
	public void onReceived(CharSequence recvBuffer) {
		 Map<String, String> entries = parser.parse(recvBuffer);
		 
		 
		 
	}
}
