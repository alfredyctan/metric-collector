package org.alf.metric.collector;

import static org.afc.util.CollectionUtil.*;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.alf.metric.buffer.BufferListener;
import org.alf.metric.config.Config.Source;
import org.alf.metric.util.DynamicCharBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.SneakyThrows;

public class NioFileCollector implements FileCollector<FileChannel> {

	private static final Logger logger = LoggerFactory.getLogger(NioFileCollector.class);

	private static final byte N = 0x0A;

	private List<BufferListener> listeners;

	private CharsetDecoder decoder;

	private ByteBuffer sourceBuffer;

	private DynamicCharBuffer lineBuffer;

	private boolean sourceBufferEmpty;

	public NioFileCollector(Source source, Map<String, String> context, BufferListener.Factory factory) {
		this.decoder = Charset.defaultCharset().newDecoder();
		this.sourceBuffer = ByteBuffer.allocate(source.getBuffer());
		this.lineBuffer = DynamicCharBuffer.allocate(source.getBuffer(), 4);
		this.listeners = new LinkedList<>();
		if (factory != null) {
			this.listeners.add(factory.createLine(source, context));
		}
		this.sourceBufferEmpty = true;
		logger.info("{} created. buffer size:{}", getClass().getName(), source.getBuffer());
	}

	/*
	 * read R N or RN then split the line
	 * I hate this function, this make me headache!
	 */
	@SneakyThrows
	@Override
	public void collect(FileChannel source) {
		try {
			int readLength = 0;
			int totalRead = 0;
			while (!sourceBufferEmpty || (readLength = source.read(sourceBuffer)) != -1) {
				if (sourceBufferEmpty) {
					totalRead += readLength;
					sourceBuffer.position(0);   //set sourceBuffer to be readable
					sourceBuffer.limit(readLength); //in case of the sourceBuffer is not full
				}
				byte[] sourceBytes = sourceBuffer.array();
				for (int i = 0; i < readLength; i++) {
					if (sourceBytes[i] == N) {
						int limit = Math.min(
							 // set limit = "from lineBuffer position" + ("sourceBuffer current pilot" - "sourceBuffer last pilot")
							// ie. length of scanned bytes
							lineBuffer.position() + (i - sourceBuffer.position() + 1),
							lineBuffer.capacity()
						); //limit to the line capacity for long line trigger (overflow discarded) dynamic line buffer
						lineBuffer.limit(limit);
						sourceBuffer.limit(i + 1);
						lineBuffer.decode(decoder, sourceBuffer, true);
						sourceBuffer.limit(readLength); //in case of the sourceBuffer is not full
						sourceBuffer.position(i + 1); // move the last pilot
						fireOnReceived(lineBuffer);
					}
				}

				//no more \r\n found, read remaining to lineBuffer
				int limit = Math.min(
					lineBuffer.position() + sourceBuffer.remaining(),
					lineBuffer.capacity()
				);
				lineBuffer.limit(limit); //set lineBuffer limit at the read byte location
				lineBuffer.decode(decoder, sourceBuffer, true);
				if (lineBuffer.limit() == lineBuffer.capacity()) {
					fireOnReceived(lineBuffer);
				}
				if (sourceBuffer.remaining() == 0) {
					sourceBuffer.clear(); // clean buffer for next read on the file channel available
					sourceBufferEmpty = true;
				} else {
					sourceBufferEmpty = false;
				}
			}
			logger.debug("bytes read:[{}], source file position:[{}]", totalRead, source.position());
		} catch (ClosedChannelException e) {
			logger.info("channel closed");
		}
	}

	private void fireOnReceived(DynamicCharBuffer buffer) {
		synchronized (listeners) {
			forEach(listeners, listener -> {
				lineBuffer.position(0); //set lineBuffer to be readable
				listener.onReceived(buffer.toCharSequence());
			});
		}
		lineBuffer.clear();
	}

	@Override
	public void stop() {
		synchronized (listeners) {
			forEach(listeners, BufferListener::stop);
		}
	}

	@Override
	public FileCollector<FileChannel> addBufferListener(BufferListener listener) {
		synchronized (this.listeners) {
			this.listeners.add(listener);
		}
		return this;
	}

	@Override
	public FileCollector<FileChannel> addBufferListeners(List<BufferListener> listeners) {
		synchronized (this.listeners) {
			this.listeners.addAll(listeners);
		}
		return this;
	}
}
