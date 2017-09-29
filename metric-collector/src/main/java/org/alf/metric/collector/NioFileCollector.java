package org.alf.metric.collector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.LinkedList;
import java.util.List;

import org.alf.metric.buffer.BufferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NioFileCollector implements FileCollector<FileChannel> {

	private static final Logger logger = LoggerFactory.getLogger(NioFileCollector.class);

	private static final byte R = 0x0D;

	private static final byte N = 0x0A;

	private List<BufferListener> listeners;

	private CharsetDecoder decoder;
	
	private ByteBuffer sourceBuffer;

	private CharBuffer lineBuffer;
	
	public NioFileCollector(int buffer) {
		this.listeners = new LinkedList<>();
		this.decoder = Charset.defaultCharset().newDecoder();
		this.sourceBuffer = ByteBuffer.allocate(buffer);
		this.lineBuffer = CharBuffer.allocate(buffer * 4); // it is a magic number, change this, change test case
		logger.info("{} created. buffer size:{}", getClass().getName(), buffer);
	}

	/*
	 * read R N or RN then split the line
	 * I hate this function, this make me headache! 
	 */
	@Override
	public void collect(FileChannel source) {
		try {
			int length = 0;
			int read = 0;
			while ((length = source.read(sourceBuffer)) != -1) {
				read += length;
				sourceBuffer.position(0);   //set sourceBuffer to be readable
				sourceBuffer.limit(length); //in case of the sourceBuffer is not full 
				byte[] bytes = sourceBuffer.array();
				for (int i = 0; i < length; i++) {
					if (bytes[i] == R || bytes[i] == N) {
						int limit = Math.min(
							lineBuffer.position() + i - sourceBuffer.position(), // set limit = "from lineBuffer position" + ("sourceBuffer current pilot" - "sourceBuffer last pilot" ie. length of scanned bytes) 
							lineBuffer.capacity()
						); //limit to the line capacity for long line trigger
						lineBuffer.limit(limit); 
						decoder.decode(sourceBuffer, lineBuffer, true);

						// move 1 more byte if next byte is \r or \n
						while ((i < bytes.length) && (bytes[i] == R || bytes[i] == N)) {
							i++;
						}
						sourceBuffer.position(i); // move the last pilot
						
						lineBuffer.position(0); //set lineBuffer to be readable
						if (lineBuffer.limit() != 0) { // do not trigger empty line
							fireOnReceived(lineBuffer);
						}
						lineBuffer.clear();
					}
				}
				
				//no more \r\n found, read remaining to lineBuffer
				int limit = Math.min(
					lineBuffer.position() + sourceBuffer.limit() - sourceBuffer.position(), // set limit = "from lineBuffer position" + ("sourceBuffer limit" - "sourceBuffer last pilot" ie. length of remaining bytes)
					lineBuffer.capacity()
				);
				lineBuffer.limit(limit); //set lineBuffer limit at the read byte location
				decoder.decode(sourceBuffer, lineBuffer, true);
				sourceBuffer.clear(); //clean buffer for next read on the file channel available
			}
			logger.info("bytes read:[{}], source file position:[{}]", read, source.position());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void addBufferListener(BufferListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	private void fireOnReceived(CharSequence buffer) {
		synchronized (listeners) {
			for (BufferListener listener : listeners) {
				listener.onReceived(buffer);
			}
		}
	}
}
