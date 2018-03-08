package org.alf.metric.buffer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.alf.metric.config.Config;
import org.alf.metric.config.Config.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LineBufferListener implements BufferListener {

	private static final Logger logger = LoggerFactory.getLogger(LineBufferListener.class);

	private Config.Source source;

	private Pattern pageBreak;

	private BufferListener listener;

	private StringBuilder pageBuffer;

	public LineBufferListener(Source source, Map<String, String> context, BufferListener.Factory factory) {
		this.source = source;
		this.pageBuffer = new StringBuilder(source.getBuffer());
		this.pageBreak = Pattern.compile(source.getPageBreak());
		this.listener = factory.createAsync(source, context);

	}

	@Override
	public void onReceived(CharSequence recvBuffer) {
		Matcher matcher = pageBreak.matcher(recvBuffer);
		if (matcher.find()) {
			int indexOf = matcher.start();
			pageBuffer.append(recvBuffer, 0, indexOf);
			pageBuffer = fireOnReceived(pageBuffer);
			pageBuffer.append(recvBuffer, indexOf, recvBuffer.length());
		} else {
			pageBuffer.append(recvBuffer);
		}
	}

	private StringBuilder fireOnReceived(StringBuilder pageBuffer) {
		if (pageBuffer.length() == 0) {
			return pageBuffer;
		}
		listener.onReceived(pageBuffer);
		return new StringBuilder(source.getBuffer());
	}

	@Override
	public void stop() {
		listener.stop();
	}
}
