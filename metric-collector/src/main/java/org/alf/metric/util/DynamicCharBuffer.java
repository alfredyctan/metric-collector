package org.alf.metric.util;

import static org.afc.util.OptionalUtil.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;

public class DynamicCharBuffer {

	private int bucketCapacity;

	private int capacity;

	private int limit;

	private int position;

	private CharBuffer[] delegates;

	private int index;

	DynamicCharBuffer(int bucketCapacity, int bucketSize) {
		this.bucketCapacity = bucketCapacity;
		this.capacity = bucketCapacity * bucketSize;
		this.delegates = new CharBuffer[bucketSize];
		this.delegates[0] = CharBuffer.allocate(bucketCapacity);
		this.index = 0;
	}

	public int capacity() {
        return capacity;
    }

	public DynamicCharBuffer position(int newPosition) {
		for (int i = 0; i < index; i++) {
			if (newPosition >= bucketCapacity) {
				delegates[i].position(delegates[i].capacity());
			} else {
				delegates[i].position(0);
			}
		}
		delegates[index].position(newPosition%bucketCapacity);
//		position = newPosition;
		return this;
	}

	public int position() {
		int pos = 0;
		pos = index * bucketCapacity;
		pos += delegates[index].position();
		return pos;
//		return position;
	}

	public DynamicCharBuffer limit(int newLimit) {
		int maxIndex = Math.max(newLimit - 1, 0) / bucketCapacity;
		if (maxIndex == 0) {
			delegates[0].limit(newLimit);
		} else {
			for (int i = 1; i <= maxIndex; i++) {
				delegates[i] = iifNotNull(delegates[i], () -> CharBuffer.allocate(bucketCapacity));
			}
			delegates[maxIndex].limit(newLimit - (bucketCapacity * maxIndex));
		}
		limit = newLimit;
        return this;
    }

	public int limit() {
		return limit;
    }

	public DynamicCharBuffer clear() {
		delegates[0].clear();
		for (int i = 1; i <= index; i++) {
			delegates[i] = null;
		}
		index = 0;
        return this;
    }

	public DynamicCharBuffer decode(CharsetDecoder decoder, ByteBuffer in, boolean endOfInput) {
		int toRead = in.limit() - in.position();
		while (toRead > 0 && position() < capacity) {
			if (delegates[index].limit() == delegates[index].capacity() && delegates[index].remaining() <= 0) {
				index++;
			}
			int tailLimit = Math.min(delegates[index].position() + toRead, bucketCapacity);
			delegates[index].limit(tailLimit);
			toRead -= (tailLimit - delegates[index].position());
			decoder.decode(in, delegates[index], endOfInput);
		}
		return this;
	}

	public CharSequence toCharSequence() {
		if (index == 0) {
			return delegates[0];
		} else {
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i <= index; i++) {
				builder.append(delegates[i]); // assume last will be appended between pos and lmt
			}
			return builder;
		}
	}

	public static DynamicCharBuffer allocate(int capacity, int maxRatio) {
		if (capacity < 0) {
			throw createCapacityException(capacity);
		}
		return new DynamicCharBuffer(capacity, maxRatio);
	}

	private static IllegalArgumentException createCapacityException(int capacity) {
		assert capacity < 0 : "capacity expected to be negative";
		return new IllegalArgumentException("capacity < 0: (" + capacity + " < 0)");
	}
}
