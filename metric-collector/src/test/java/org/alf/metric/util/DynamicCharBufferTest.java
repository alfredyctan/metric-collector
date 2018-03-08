package org.alf.metric.util;

import static org.afc.util.JUnitUtil.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

import org.afc.junit5.extension.TestInfoExtension;
import org.alf.metric.MetricCollectorExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;


@ExtendWith({ MetricCollectorExtension.class, TestInfoExtension.class })
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(Random.class)
class DynamicCharBufferTest {

	private CharsetDecoder decoder;

	private ByteBuffer in1;

	private ByteBuffer in2;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		decoder = Charset.defaultCharset().newDecoder();
		in1 = ByteBuffer.wrap("01234567891123456789".getBytes(Charset.defaultCharset()));
		in2 = ByteBuffer.wrap("CCCCCCCCCCDDDDDDDDDD".getBytes(Charset.defaultCharset()));
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void testWriteWithinBucket() {
		DynamicCharBuffer buffer = new DynamicCharBuffer(40, 4);

		buffer.clear();
		buffer.limit(in1.remaining())
			.decode(decoder, in1, true)
			.position(0); // allow read after decode
		CharSequence actual1 = actual(buffer.toCharSequence());
		CharSequence expect1 = expect("01234567891123456789");
		assertEquals("1st read", actual1.toString(), expect1.toString());

		buffer.clear();
		buffer.limit(in2.remaining())
			.decode(decoder, in2, true)
			.position(0); // allow read after decode
		CharSequence actual2 = actual(buffer.toCharSequence());
		CharSequence expect2 = expect("CCCCCCCCCCDDDDDDDDDD");
		assertEquals("2nd read", actual2.toString(), expect2.toString());
	}

	@Test
	void testWriteExtendBucket() {
		DynamicCharBuffer buffer = new DynamicCharBuffer(10, 4);

		buffer.clear();
		buffer.limit(in1.remaining())
			.decode(decoder, in1, true)
			.position(0); // allow read after decode
		CharSequence actual1 = actual(buffer.toCharSequence());
		CharSequence expect1 = expect("01234567891123456789");
		assertEquals("1st read", actual1.toString(), expect1.toString());

		buffer.clear();
		buffer.limit(in2.remaining())
			.decode(decoder, in2, true)
			.position(0); // allow read after decode
		CharSequence actual2 = actual(buffer.toCharSequence());
		CharSequence expect2 = expect("CCCCCCCCCCDDDDDDDDDD");
		assertEquals("2nd read", actual2.toString(), expect2.toString());
	}

	@Test
	void testWriteExtendBucketOverFlow() {
		DynamicCharBuffer buffer = new DynamicCharBuffer(10, 3);

		buffer.clear();
		buffer.limit(in1.limit())
			.decode(decoder, in1, true);

		buffer.limit(buffer.capacity())
			.decode(decoder, in2, true)
			.position(0); // allow read after decode

		CharSequence actual1 = actual(buffer.toCharSequence());
		CharSequence expect1 = expect("01234567891123456789CCCCCCCCCC");
		assertEquals("1st read", actual1.toString(), expect1.toString());

		buffer.clear();
		buffer.limit(in2.remaining())
			.decode(decoder, in2, true)
			.position(0); // allow read after decode
		CharSequence actual2 = actual(buffer.toCharSequence());
		CharSequence expect2 = expect("DDDDDDDDDD");
		assertEquals("2nd read", actual2.toString(), expect2.toString());
	}
}
