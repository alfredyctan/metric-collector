package org.alf.metric.collector;

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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.afc.concurrent.NamedThreadFactory;
import org.afc.junit5.extension.TestInfoExtension;
import org.alf.metric.MetricCollectorExtension;
import org.alf.metric.buffer.BufferListener;
import org.alf.metric.config.Config.Source;
import org.apache.commons.io.FileUtils;
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
import org.mockito.Mockito;

@ExtendWith({ MetricCollectorExtension.class, TestInfoExtension.class })
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(Random.class)
class PeriodicNioFileCollectorTest {

	private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("test"));

	private static File logDir;

	private static File logFile;

	private static File initFile;

	private static File increFile;

	private static File rollFile;

	private static Source source;

	private FileCollector.Factory factory;

	private FileCollector<FileChannel> fileCollector;

	private FileCollector.Factory integrationFactory;

	private BufferListener.Factory bufferFactory;

	private NioFileCollector nioCollector;

	private MockBufferListener lineListener;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		logDir = getFile("target/test");
		logFile = getFile("target/test/init.log");
		initFile = getFile("src/test/resources/log/init.log");
		increFile = getFile("src/test/resources/log/incre.log");
		rollFile = getFile("src/test/resources/log/roll.log");
		source = new Source().setInterval(100).setBuffer(20);
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		lineListener = new MockBufferListener();

		bufferFactory = Mockito.mock(BufferListener.Factory.class);
		when(bufferFactory.createLine(any(Source.class), any(Map.class))).thenReturn(lineListener);

		factory = Mockito.mock(FileCollector.Factory.class);
		fileCollector = Mockito.mock(FileCollector.class);
		when(factory.<FileChannel>createReadCollector(any(Source.class), any(Map.class))).thenReturn(fileCollector);

		nioCollector = new NioFileCollector(source, Map.of(), bufferFactory);

		integrationFactory = Mockito.mock(FileCollector.Factory.class);
		when(integrationFactory.<FileChannel>createReadCollector(any(Source.class), any(Map.class))).thenReturn(nioCollector);

		try {
			deleteDirectory(getFile(logDir));
		} catch (Exception e) {
		}
		getFile(logDir).mkdirs();
	}

	@AfterEach
	void tearDown() throws Exception {
		try {
			deleteDirectory(getFile(logDir));
		} catch (Exception e) {
		}
	}

	@Test
	void testScheduling() throws IOException {
		copyFile(initFile, logFile);

		PeriodicNioFileCollector periodicFileCollector = new PeriodicNioFileCollector(source, Map.of(), factory, executorService);
		periodicFileCollector.collect(logFile);

		sleep(source.getInterval() * 3);
		periodicFileCollector.stop();

		verify(fileCollector, atLeast(2)).collect(any(FileChannel.class));
	}

	@Test
	void testFileGrowth() throws IOException {
		copyFile(initFile, logFile);

		PeriodicNioFileCollector periodicFileCollector = new PeriodicNioFileCollector(source, Map.of(), integrationFactory, executorService);
		periodicFileCollector.collect(logFile);
		sleep(source.getInterval() / 10);
		writeByteArrayToFile(logFile, readFileToByteArray(increFile), true);

		sleep(source.getInterval());
		writeByteArrayToFile(logFile, readFileToByteArray(increFile), true);

		sleep(source.getInterval() * 2);
		periodicFileCollector.stop();

		List<String> expect = Arrays.asList(new String[] { "A123456789\n", "B123456789\n", "C123456789\n", "A123456789\n", "B123456789\n", "C123456789\n" });
		assertThat("collect simple", lineListener.getLines(), is(expect));
	}

	@Test
	void testSameFileRolling() throws IOException {
		copyFile(initFile, logFile);

		PeriodicNioFileCollector periodicFileCollector = new PeriodicNioFileCollector(source, Map.of(), integrationFactory, executorService);
		periodicFileCollector.collect(logFile);
		sleep(source.getInterval());
		writeByteArrayToFile(logFile, readFileToByteArray(increFile), false);
		sleep(source.getInterval() * 2);
		periodicFileCollector.stop();

		List<String> expect = Arrays.asList(new String[] {"A123456789\n", "B123456789\n", "C123456789\n"});
		assertThat("collect simple", lineListener.getLines(), is(expect));
	}

	@Test
	void testEmptyFileRolling() throws IOException {
		copyFile(initFile, logFile);

		PeriodicNioFileCollector periodicFileCollector = new PeriodicNioFileCollector(source, Map.of(), integrationFactory, executorService);
		periodicFileCollector.collect(logFile);
		writeByteArrayToFile(logFile, new byte[0], false);
		sleep(source.getInterval() * 2);
		periodicFileCollector.stop();

		List<String> expect = Arrays.asList(new String[] {});
		assertThat("collect simple", lineListener.getLines(), is(expect));
	}

	@Test
	void testNewEmptyFileRolling() throws IOException {
		copyFile(initFile, logFile);

		PeriodicNioFileCollector periodicFileCollector = new PeriodicNioFileCollector(source, Map.of(), integrationFactory, executorService);
		periodicFileCollector.collect(logFile);
		sleep(source.getInterval());
		FileUtils.moveFile(logFile, new File("target/test/init.log.1"));
		copyFile(rollFile, logFile);
		sleep(source.getInterval() * 2);
		periodicFileCollector.stop();

		List<String> expect = Arrays.asList(new String[] {});
		assertThat("collect simple", lineListener.getLines(), is(expect));
	}

	@Test
	void testNewFileRolling() throws IOException {
		copyFile(initFile, logFile);

		PeriodicNioFileCollector periodicFileCollector = new PeriodicNioFileCollector(source, Map.of(), integrationFactory, executorService);
		periodicFileCollector.collect(logFile);
		sleep(source.getInterval());
		FileUtils.moveFile(logFile, new File("target/test/init.log.1"));
		copyFile(initFile, logFile);
		sleep(source.getInterval() * 3);
		periodicFileCollector.stop();

		List<String> expect = Arrays.asList(new String[] {"0123456789\n", "1123456789\n", "2123456789\n", "3123456789\n"});
		assertThat("collect simple", lineListener.getLines(), is(expect));
	}
}
