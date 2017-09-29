package org.alf.metric.buffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;

import org.afc.util.JUnit4Util;
import org.alf.metric.collector.MockListener;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PagedBufferListenerTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testBuffer() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		MockListener mock = new MockListener();
		PagedBufferListener listener = new PagedBufferListener("01", mock);
		CharBuffer buffer0 = createCharBuffer(100, "0123456789");
		CharBuffer buffer1 = createCharBuffer(100, "1123456789");
		CharBuffer buffer2 = createCharBuffer(100, "0123456789");
		CharBuffer buffer3 = createCharBuffer(100, "1123456789");
		CharBuffer buffer4 = createCharBuffer(100, "0123456789");

		listener.onReceived(buffer0);
		listener.onReceived(buffer1);
		listener.onReceived(buffer2);
		listener.onReceived(buffer3);
		listener.onReceived(buffer4);
		
		List<CharSequence> actual = mock.getLines();
		
		List<String> expect = Arrays.asList(new String[] { "01234567891123456789", "01234567891123456789" });
		assertThat("collect simple", actual, is(expect));
		
		
		JUnit4Util.endCurrentTest(getClass());
	}

	@Test
	public void testBufferOverflow() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		MockListener mock = new MockListener();
		PagedBufferListener listener = new PagedBufferListener("01", 15, mock);
		CharBuffer buffer0 = createCharBuffer(100, "0123456789");
		CharBuffer buffer1 = createCharBuffer(100, "1123456789");
		CharBuffer buffer2 = createCharBuffer(100, "2123456789");
		CharBuffer buffer3 = createCharBuffer(100, "3123456789");
		CharBuffer buffer4 = createCharBuffer(100, "4123456789");

		listener.onReceived(buffer0);
		listener.onReceived(buffer1);
		listener.onReceived(buffer0);
		listener.onReceived(buffer1);
		listener.onReceived(buffer2);
		listener.onReceived(buffer3);
		listener.onReceived(buffer4);
		listener.onReceived(buffer0);
		
		List<CharSequence> actual = mock.getLines();
		
		List<String> expect = Arrays.asList(new String[] { "01234567891123456789", "01234567891123456789212345678931234567894123456789" });
		assertThat("collect simple", actual, is(expect));
		
		
		JUnit4Util.endCurrentTest(getClass());
	}

	private static CharBuffer createCharBuffer(int size, String content) {
		CharBuffer buffer0 = CharBuffer.allocate(size).append(content);
		buffer0.position(0);
		buffer0.limit(content.length());
		return buffer0;
	}
}
