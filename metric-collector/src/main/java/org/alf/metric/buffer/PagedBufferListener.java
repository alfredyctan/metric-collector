package org.alf.metric.buffer;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PagedBufferListener implements BufferListener {

	private static final Logger logger = LoggerFactory.getLogger(PagedBufferListener.class);
	
	private List<BufferListener> listeners;

	private CharBuffer pageBuffer;
	
	private String pageBreak;

	
	public PagedBufferListener(String pageBreak, BufferListener listener) {
		this(pageBreak, Arrays.asList(listener));
	}

	public PagedBufferListener(String pageBreak, List<BufferListener> listeners) {
		this.pageBreak = pageBreak;
		this.listeners = listeners;
		this.pageBuffer = CharBuffer.allocate(8192);
	}

	/* for junit test */
	public PagedBufferListener(String pageBreak, int size, BufferListener listener) {
		this.pageBreak = pageBreak;
		this.listeners = Arrays.asList(listener);
		this.pageBuffer = CharBuffer.allocate(size);
	}
	
	@Override
	public void onReceived(CharSequence recvBuffer) {
		String recvString = recvBuffer.toString();
		int indexOf = recvString.indexOf(pageBreak);
		if (indexOf == -1) {
			ensureBuffer(recvBuffer.length());
			pageBuffer.append(recvBuffer);
		} else {
			ensureBuffer(indexOf);
			pageBuffer.append(recvBuffer, 0, indexOf);

			fireOnReceived(pageBuffer);

			ensureBuffer(recvBuffer.length() - indexOf);
			pageBuffer.append(recvBuffer, indexOf, recvBuffer.length());
		}
	}

	private void ensureBuffer(int length) {
		int required = pageBuffer.position() + length;
		if (pageBuffer.capacity() < required) {
			pageBuffer.limit(pageBuffer.position());
			pageBuffer.position(0);
			pageBuffer = CharBuffer.allocate(required * 2).append(pageBuffer);
			logger.info("extend buffer capacity to [{}]", pageBuffer.capacity());
		}
	}
	
	public void fireOnReceived(CharBuffer pageBuffer) {
		if (pageBuffer.position() == 0) {
			return;
		}
		pageBuffer.limit(pageBuffer.position());
		pageBuffer.position(0);
		synchronized (listeners) {
			for (BufferListener listener : listeners) {
				listener.onReceived(pageBuffer);
			}
		}
		pageBuffer.clear();
	}
}
