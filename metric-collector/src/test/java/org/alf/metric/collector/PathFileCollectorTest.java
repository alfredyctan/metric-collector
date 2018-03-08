package org.alf.metric.collector;

import static java.util.Arrays.*;
import static org.afc.util.JUnitUtil.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.afc.concurrent.NamedThreadFactory;
import org.afc.junit5.extension.TestInfoExtension;
import org.alf.metric.MetricCollectorExtension;
import org.alf.metric.config.Config.Source;
import org.alf.metric.launch.LaunchControl;
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
class PathFileCollectorTest {

	private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("test"));

	private FileCollector.Factory factory;

	private FileCollector<File> fileCollector;

	private Source source;

	private LaunchControl launchControl;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		deleteFile("target/path");
		createFolder("target/path/app1");
		createFolder("target/path/app2");
		createFolder("target/path/app3");

		source = new Source().setInterval(100);
		factory = Mockito.mock(FileCollector.Factory.class);
		fileCollector = Mockito.mock(FileCollector.class);
		when(factory.<File>createScanCollector(any(Source.class), any(ScheduledExecutorService.class), any(Map.class))).thenReturn(fileCollector);

		when(fileCollector.addBufferListeners(any(List.class))).thenReturn(fileCollector);

		launchControl = Mockito.mock(LaunchControl.class);
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void testHoldLaunch() {
		createFile("target/path/app1/mc.default.in0.2022-02-24.5000254d.log.0",  "2022-02-24T04:39:00Z" , "2022-02-24T04:39:00Z" , "2022-02-24T04:39:00Z");

		when(launchControl.isLaunched()).thenReturn(false);
		PathFileCollector pathFileCollector = new PathFileCollector(source, factory, executorService, launchControl);
		pathFileCollector.collect(source.setPaths(asList("target/path", "target/path/app1")).setPattern("(?<service>.*?)\\.(?<cluster>.*?)\\.(?<instance>.*?)\\..*?\\..*?\\.log\\..*$"));
		sleep(200);
		verify(fileCollector, times(0)).collect(any(File.class));
	}

	@Test
	void testExistingAppLog() {
		when(launchControl.isLaunched()).thenReturn(true);
		createFile("target/path/app1/mc.default.in0.2022-02-24.5000254d.log.0",  "2022-02-24T04:39:00Z" , "2022-02-24T04:39:00Z" , "2022-02-24T04:39:00Z");
		createFile("target/path/app1/mc.default.in0.2022-02-24.5000254d.log.1",  "2022-02-24T23:58:00Z" , "2022-02-24T23:58:00Z" , "2022-02-24T23:58:00Z");
		createFile("target/path/app2/mc.default.in1.2022-02-24.9a88525b.log.0",  "2022-02-24T00:49:00Z" , "2022-02-24T00:49:00Z" , "2022-02-24T00:49:00Z");
		createFile("target/path/app2/mc.default.in1.2022-02-24.9a88525b.log.1",  "2022-02-24T01:20:00Z" , "2022-02-24T01:20:00Z" , "2022-02-24T01:20:00Z");
		createFile("target/path/app2/mc.default.in1.2022-02-24.9a88525b.log.10", "2022-02-24T11:32:00Z" , "2022-02-24T11:32:00Z" , "2022-02-24T11:32:00Z");
		createFile("target/path/app2/mc.default.in1.2022-02-24.9a88525b.log.11", "2022-02-24T12:51:00Z" , "2022-02-24T12:51:00Z" , "2022-02-24T12:51:00Z");
		createFile("target/path/app3/mc.default.in2.2022-03-03.23933e61.log.0",  "2022-03-03T02:50:00Z" , "2022-03-03T02:50:00Z" , "2022-03-03T02:50:00Z");
		createFile("target/path/app3/mc.default.in2.2022-03-03.23933e61.log.1",  "2022-03-03T22:31:00Z" , "2022-03-03T22:31:00Z" , "2022-03-03T22:31:00Z");
		createFile("target/path/app3/mm-ddddddd-iii-2022-03-03.23933e61.log.1",  "2022-03-03T22:31:00Z" , "2022-03-03T22:31:00Z" , "2022-03-03T22:31:00Z");

		PathFileCollector pathFileCollector = new PathFileCollector(source, factory, executorService, launchControl);
		pathFileCollector.collect(source.setPaths(asList("target/path", "target/path/app1")).setPattern("(?<service>.*?)\\.(?<cluster>.*?)\\.(?<instance>.*?)\\..*?\\..*?\\.log\\..*$"));
		sleep(200);
		verify(fileCollector, times(8)).collect(any(File.class));
	}

	@Test
	void testExistingGcLog() {
		when(launchControl.isLaunched()).thenReturn(true);
		createFile("target/path/mc.cluster.in0.2022-02-20.086fb8f2.gc.log",  "2022-02-24T04:39:00Z" , "2022-02-24T04:39:00Z" , "2022-02-24T04:39:00Z");
		createFile("target/path/mc.cluster.in0.2022-02-27.e5699de4.gc.log",  "2022-02-24T23:58:00Z" , "2022-02-24T23:58:00Z" , "2022-02-24T23:58:00Z");
		createFile("target/path/mc.cluster.in1.2022-03-06.6c657630.gc.log",  "2022-02-24T00:49:00Z" , "2022-02-24T00:49:00Z" , "2022-02-24T00:49:00Z");
		createFile("target/path/mc.cluster.in1.2022-03-07.29e9d528.gc.log",  "2022-02-24T01:20:00Z" , "2022-02-24T01:20:00Z" , "2022-02-24T01:20:00Z");
		createFile("target/path/mc.default.in0.2022-02-20.5424b0cf.gc.log",  "2022-02-24T11:32:00Z" , "2022-02-24T11:32:00Z" , "2022-02-24T11:32:00Z");
		createFile("target/path/mc.default.in0.2022-02-27.a7bf6ffe.gc.log",  "2022-02-24T12:51:00Z" , "2022-02-24T12:51:00Z" , "2022-02-24T12:51:00Z");
		createFile("target/path/mc.default.in1.2022-03-01.8f0f2052.gc.log",  "2022-03-03T02:50:00Z" , "2022-03-03T02:50:00Z" , "2022-03-03T02:50:00Z");

		PathFileCollector pathFileCollector = new PathFileCollector(source, factory, executorService, launchControl);
		pathFileCollector.collect(source.setPaths(asList("target/path")).setPattern("(?<service>.*?)\\.(?<cluster>.*?)\\.(?<instance>.*?)\\..*?\\..*?\\.gc\\.log$"));
		sleep(200);
		verify(fileCollector, times(7)).collect(any(File.class));
	}

	@Test
	void testNewAppLog() {
		describe("read existing file");
		when(launchControl.isLaunched()).thenReturn(true);
		createFile("target/path/mc.default.in0.2022-02-24.5000254d.log.0",  "2022-02-24T04:39:00Z" , "2022-02-24T04:39:00Z" , "2022-02-24T04:39:00Z");
		createFile("target/path/mc.default.in0.2022-02-24.5000254d.log.1",  "2022-02-24T23:58:00Z" , "2022-02-24T23:58:00Z" , "2022-02-24T23:58:00Z");
		PathFileCollector pathFileCollector = new PathFileCollector(source.setInterval(100), factory, executorService, launchControl);
		pathFileCollector.collect(source.setPaths(asList("target/path")).setPattern("(?<service>.*?)\\.(?<cluster>.*?)\\.(?<instance>.*?)\\..*?\\..*?\\.log\\..*$"));
		sleep(200);
		verify(fileCollector, times(2)).collect(any(File.class));

		describe("read new file");
		createFile("target/path/mc.default.in1.2022-02-24.9a88525b.log.0",  "2022-02-24T00:49:00Z" , "2022-02-24T00:49:00Z" , "2022-02-24T00:49:00Z");
		createFile("target/path/mc.default.in1.2022-02-24.9a88525b.log.1",  "2022-02-24T01:20:00Z" , "2022-02-24T01:20:00Z" , "2022-02-24T01:20:00Z");
		sleep(200);
		verify(fileCollector, times(4)).collect(any(File.class));

		describe("skip new file do not match pattern");
		createFile("target/path/mm-ddddddd-iii-2022-03-03.23933e61.log.1",  "2022-03-03T22:31:00Z" , "2022-03-03T22:31:00Z" , "2022-03-03T22:31:00Z");
		sleep(200);
		verify(fileCollector, times(4)).collect(any(File.class));

		describe("stop collecting");
		pathFileCollector.stop();
		createFile("target/path/mc.default.in1.2022-02-24.9a88525b.log.11", "2022-02-24T12:51:00Z" , "2022-02-24T12:51:00Z" , "2022-02-24T12:51:00Z");
		createFile("target/path/mc.default.in2.2022-03-03.23933e61.log.0",  "2022-03-03T02:50:00Z" , "2022-03-03T02:50:00Z" , "2022-03-03T02:50:00Z");
		sleep(200);
		verify(fileCollector, times(4)).collect(any(File.class));
	}
}
