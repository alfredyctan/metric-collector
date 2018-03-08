package org.alf.metric.collector;

import static java.nio.file.StandardOpenOption.*;
import static java.util.Arrays.*;
import static org.afc.util.JUnitUtil.*;
import static org.afc.util.JUnitUtil.copyFile;
import static org.apache.commons.io.FileUtils.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.afc.junit5.extension.TestInfoExtension;
import org.alf.metric.MetricCollectorExtension;
import org.alf.metric.buffer.BufferListener;
import org.alf.metric.config.Config.Source;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith({ MetricCollectorExtension.class, TestInfoExtension.class })
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodName.class)
class NioFileCollectorTest {

	private static File logDir;

	private static File logFile;

	private static File initFile;

	private static File initLongLineFile;

	private static File initNewLine;

	private static File increFile;

	private Source source;

	private BufferListener.Factory factory;

	private MockBufferListener lineListener;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		logDir = getFile("target/test");
		logFile = getFile("target/test/init.log");
		initFile = getFile("src/test/resources/log/init.log");
		initLongLineFile = getFile("src/test/resources/log/init-long-line.log");
		increFile = getFile("src/test/resources/log/roll-increment.log");
		initNewLine = getFile("src/test/resources/log/init-newline.log");
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		source = new Source().setBuffer(20);
		factory = Mockito.mock(BufferListener.Factory.class);
		lineListener = new MockBufferListener();
		when(factory.createLine(any(Source.class), any(Map.class))).thenReturn(lineListener);
		try {
			deleteDirectory(getFile(logDir));
		} catch (Exception e) {

		}
		logDir.mkdirs();
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void testCollectSizeBufferEqualToLine() throws IOException {
		copyFile(initFile, logFile);

		NioFileCollector collector = new NioFileCollector(source.setBuffer(12), Map.of(), factory);

		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();

		List<CharSequence> actual = actual(lineListener.getLines());
		List<CharSequence> expect = expect(asList("0123456789\n", "1123456789\n", "2123456789\n", "3123456789\n"));
		assertContains("collect simple", actual, expect);
	}

	@Test
	void testCollectSizeBufferLessThanLine() throws IOException {
		copyFile(initFile, logFile);

		NioFileCollector collector = new NioFileCollector(source.setBuffer(10), Map.of(), factory);

		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();

		List<CharSequence> actual = actual(lineListener.getLines());
		List<CharSequence> expect = expect(asList( "0123456789\n", "1123456789\n", "2123456789\n", "3123456789\n"));
		assertContains("collect simple", actual, expect);
	}

	@Test
	void testCollectSizeBufferLessThanLineMore() throws IOException {

		copyFile(initFile, logFile);

		NioFileCollector collector = new NioFileCollector(source.setBuffer(8), Map.of(), factory);

		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();

		List<CharSequence> actual = actual(lineListener.getLines());
		List<CharSequence> expect = expect(asList("0123456789\n", "1123456789\n", "2123456789\n", "3123456789\n" ));
		assertContains("collect simple", actual, expect);
	}

	@Test
	void testCollectSizeBufferMoreThanLine() throws IOException {

		copyFile(initFile, logFile);

		NioFileCollector collector = new NioFileCollector(source.setBuffer(22), Map.of(), factory);

		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();

		List<CharSequence> actual = actual(lineListener.getLines());
		List<CharSequence> expect = expect(asList("0123456789\n", "1123456789\n", "2123456789\n", "3123456789\n" ));
		assertContains("collect simple", actual, expect);
	}

	@Test
	void testCollectSizeBufferMoreThan2Line() throws IOException {

		copyFile(initFile, logFile);

		NioFileCollector collector = new NioFileCollector(source.setBuffer(24), Map.of(), factory);

		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();

		List<CharSequence> actual = actual(lineListener.getLines());
		List<CharSequence> expect = expect(asList("0123456789\n", "1123456789\n", "2123456789\n", "3123456789\n" ));
		assertContains("collect simple", actual, expect);
	}

	@Test
	void testCollectSizeBufferMoreThan3Line() throws IOException {

		copyFile(initFile, logFile);

		NioFileCollector collector = new NioFileCollector(source.setBuffer(40), Map.of(), factory);

		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();

		List<CharSequence> actual = actual(lineListener.getLines());
		List<CharSequence> expect = expect(asList("0123456789\n", "1123456789\n", "2123456789\n", "3123456789\n" ));
		assertContains("collect simple", actual, expect);
	}

	@Test
	void testCollectFromMiddle() throws IOException {

		copyFile(initFile, logFile);

		NioFileCollector collector = new NioFileCollector(source.setBuffer(20), Map.of(), factory);

		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		fileChannel.position(5);
		collector.collect(fileChannel);
		fileChannel.close();

		List<CharSequence> actual = actual(lineListener.getLines());
		List<CharSequence> expect = expect(asList("56789\n", "1123456789\n", "2123456789\n", "3123456789\n" ));
		assertContains("collect simple", actual, expect);
	}

	@Test
	void testCollectOneByteBuffer() throws IOException {

		copyFile(initFile, logFile);

		NioFileCollector collector = new NioFileCollector(source.setBuffer(1), Map.of(), factory);

		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();

		List<CharSequence> actual = actual(lineListener.getLines());
		List<CharSequence> expect = expect(asList("0123", "4567", "89\n", "1123", "4567", "89\n", "2123", "4567", "89\n", "3123", "4567", "89\n", "4123", "4567"));
		assertContains("collect simple", actual, expect);
	}

	@Test
	void testCollectLongLine() throws IOException {
		copyFile(initLongLineFile, logFile);

		NioFileCollector collector = new NioFileCollector(source.setBuffer(4), Map.of(), factory);

		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();

		List<CharSequence> actual = actual(lineListener.getLines());
		List<CharSequence> expect = expect(asList("0123456789012345", "6789\n", "1123456789112345", "6789\n", "2123456789212345", "6789\n", "3123456789312345", "6789\n", "4123456789412345"));
		assertThat("collect simple", actual, is(expect));
	}

	@Test
	void testCollectLongLineOverflowDiscarded() throws IOException {
		copyFile(initLongLineFile, logFile);

		NioFileCollector collector = new NioFileCollector(source.setBuffer(3), Map.of(), factory);

		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();

		List<CharSequence> actual = actual(lineListener.getLines());
		List<CharSequence> expect = expect(asList("012345678901", "23456789\n", "112345678911", "23456789\n", "212345678921", "23456789\n", "312345678931", "23456789\n", "412345678941"));
		assertContains("collect simple", actual, expect);
	}

	@Test
	void testCollectFromEnd() throws IOException {
		copyFile(initFile, logFile);

		NioFileCollector collector = new NioFileCollector(source, Map.of(), factory);

		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		fileChannel.position(fileChannel.size());
		collector.collect(fileChannel);

		fileChannel.close();

		List<CharSequence> actual = actual(lineListener.getLines());
		List<CharSequence> expect = Arrays.asList(new String[] {});
		assertContains("collect simple", actual, expect);
	}

	@Test
	void testCollectPreviousNewLineBuffer() throws IOException {
		NioFileCollector collector = new NioFileCollector(source.setBuffer(12), Map.of(), factory);

		copyFile(initNewLine, logFile);
		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);

		copyFile(initFile, logFile);
		fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);

		fileChannel.close();

		List<CharSequence> actual = actual(lineListener.getLines());
		List<CharSequence> expect = expect(asList("\n", "\n", "\n", "\n", "\n", "\n", "\n", "\n", "\n", "\n", "\n", "0123456789\n", "1123456789\n", "2123456789\n", "3123456789\n" ));
		assertContains("collect simple", actual, expect);
	}
}
