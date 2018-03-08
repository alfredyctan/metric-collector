package org.alf.metric.buffer;

import static java.util.Arrays.*;
import static org.afc.util.JUnitUtil.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.afc.junit5.extension.TestInfoExtension;
import org.afc.util.JUnitUtil;
import org.alf.metric.MetricCollectorExtension;
import org.alf.metric.collector.MockBufferListener;
import org.alf.metric.config.Config.Capture;
import org.alf.metric.config.Config.Source;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;


@ExtendWith({ MetricCollectorExtension.class, TestInfoExtension.class })
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(Random.class)
class LineBufferListenerTest {

	private Source source;

	private BufferListener.Factory factory;

	private MockBufferListener lineListener;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		source = new Source().setInterval(100).setBuffer(20).setCaptures(asList(new Capture()));
		factory = Mockito.mock(BufferListener.Factory.class);
		lineListener = new MockBufferListener();
		when(factory.createAsync(any(Source.class), any(Map.class))).thenReturn(lineListener);
	}

	@Test
	void testBuffer() throws IOException {
		JUnitUtil.startCurrentTest(getClass());
		LineBufferListener listener = new LineBufferListener(source.setPageBreak("01"), new HashMap<>(), factory);
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

		List<CharSequence> actual = lineListener.getLines();

		List<String> expect = Arrays.asList(new String[] { "01234567891123456789", "01234567891123456789" });
		assertThat("collect simple", actual, is(expect));


		JUnitUtil.endCurrentTest(getClass());
	}

	@Test
	void testBufferOverflow() throws IOException {
		JUnitUtil.startCurrentTest(getClass());

		LineBufferListener listener = new LineBufferListener(source.setPageBreak("01").setBuffer(4), new HashMap<>(), factory);
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

		List<CharSequence> actual = actual(lineListener.getLines());

		List<String> expect = expect(Arrays.asList(new String[] { "01234567891123456789", "01234567891123456789212345678931234567894123456789" }));
		assertThat("collect simple", actual, is(expect));

		JUnitUtil.endCurrentTest(getClass());
	}

	private static CharBuffer createCharBuffer(int size, String content) {
		CharBuffer buffer0 = CharBuffer.allocate(size).append(content);
		buffer0.position(0);
		buffer0.limit(content.length());
		return buffer0;
	}
}
