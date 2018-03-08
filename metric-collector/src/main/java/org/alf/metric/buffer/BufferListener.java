package org.alf.metric.buffer;

import java.util.Map;

import org.alf.metric.config.Config.Capture;
import org.alf.metric.config.Config.Source;

public interface BufferListener {

	public void onReceived(CharSequence buffer);

	public void stop();

	public static interface Factory {

		public BufferListener createAsync(Source source, Map<String, String> context);

		public BufferListener createLine(Source source, Map<String, String> context);

		public BufferListener createPage(Source source, Capture capture, Map<String, String> context);
	}
}
