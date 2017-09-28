package org.alf.metric.collector;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.getFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

import org.afc.util.JUnit4Util;
import org.apache.commons.io.FileUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class PeriodicFileCollectorTest {

	private static File logDir;

	private static File logFile;
	
	private static File initFile;
	
	private static File increFile;
	
	private static File rollFile;

	private static long interval;
	
	private Mockery mockery;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		logDir = getFile("target/test");
		logFile = getFile("target/test/init.log");
		initFile = getFile("src/test/resources/log/init.log");
		increFile = getFile("src/test/resources/log/incre.log");
		rollFile = getFile("src/test/resources/log/roll.log");
		interval = 100;
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		mockery = new JUnit4Mockery() {{
			setThreadingPolicy(new Synchroniser());			
		}};
		
		
		try {
			deleteDirectory(getFile(logDir));
		} catch (Exception e) {
		}
	}

	@After
	public void tearDown() throws Exception {
		mockery.assertIsSatisfied();
		try {
			deleteDirectory(getFile(logDir));
		} catch (Exception e) {
		}
	}

	@Test
	public void testScheduling() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);

		FileCollector<FileChannel> mockCollector = mockery.mock(FileCollector.class);
	    mockery.checking(new Expectations() {{
	    	atLeast(2).of(mockCollector).collect(with(any(FileChannel.class)));
	    }});		
		
		PeriodicFileCollector periodicFileCollector = new PeriodicFileCollector();
		periodicFileCollector.setCollector(mockCollector);
		periodicFileCollector.setInterval(interval);
		periodicFileCollector.collect(logFile);
		
		JUnit4Util.sleep(interval * 3);
		periodicFileCollector.cancel();

		JUnit4Util.endCurrentTest(getClass());
	}

	@Test
	public void testFileGrowth() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);

		NioFileCollector nioCollector = new NioFileCollector(20);
		MockListener listener = new MockListener();
		nioCollector.addLineListener(listener);
		
		PeriodicFileCollector periodicFileCollector = new PeriodicFileCollector();
		periodicFileCollector.setCollector(nioCollector);
		periodicFileCollector.setInterval(interval);
		periodicFileCollector.collect(logFile);
		JUnit4Util.sleep(interval / 10);
		FileUtils.writeByteArrayToFile(logFile, FileUtils.readFileToByteArray(increFile), true);
		JUnit4Util.sleep(interval);
		FileUtils.writeByteArrayToFile(logFile, FileUtils.readFileToByteArray(increFile), true);
		JUnit4Util.sleep(interval * 2);
		periodicFileCollector.cancel();

		List<String> expect = Arrays.asList(new String[] { "A123456789", "B123456789", "C123456789", "A123456789", "B123456789", "C123456789" });
		assertThat("collect simple", listener.getLines(), is(expect));
		
		JUnit4Util.endCurrentTest(getClass());
	}

	@Test
	public void testSameFileRolling() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);

		NioFileCollector nioCollector = new NioFileCollector(20);
		MockListener listener = new MockListener();
		nioCollector.addLineListener(listener);
		
		PeriodicFileCollector periodicFileCollector = new PeriodicFileCollector();
		periodicFileCollector.setCollector(nioCollector);
		periodicFileCollector.setInterval(interval);
		periodicFileCollector.collect(logFile);
		JUnit4Util.sleep(interval);
		FileUtils.writeByteArrayToFile(logFile, FileUtils.readFileToByteArray(increFile), false);
		JUnit4Util.sleep(interval * 2);
		periodicFileCollector.cancel();

		List<String> expect = Arrays.asList(new String[] {"A123456789", "B123456789", "C123456789"});
		assertThat("collect simple", listener.getLines(), is(expect));
		
		JUnit4Util.endCurrentTest(getClass());
	}

	@Test
	public void testEmptyFileRolling() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);

		NioFileCollector nioCollector = new NioFileCollector(20);
		MockListener listener = new MockListener();
		nioCollector.addLineListener(listener);
		
		PeriodicFileCollector periodicFileCollector = new PeriodicFileCollector();
		periodicFileCollector.setCollector(nioCollector);
		periodicFileCollector.setInterval(interval);
		periodicFileCollector.collect(logFile);
		FileUtils.writeByteArrayToFile(logFile, new byte[0], false);
		JUnit4Util.sleep(interval * 2);
		periodicFileCollector.cancel();

		List<String> expect = Arrays.asList(new String[] {});
		assertThat("collect simple", listener.getLines(), is(expect));
		
		JUnit4Util.endCurrentTest(getClass());
	}

	@Test
	public void testNewEmptyFileRolling() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);

		NioFileCollector nioCollector = new NioFileCollector(20);
		MockListener listener = new MockListener();
		nioCollector.addLineListener(listener);
		
		PeriodicFileCollector periodicFileCollector = new PeriodicFileCollector();
		periodicFileCollector.setCollector(nioCollector);
		periodicFileCollector.setInterval(interval);
		periodicFileCollector.collect(logFile);
		JUnit4Util.sleep(interval);
		FileUtils.moveFile(logFile, new File("target/test/init.log.1"));
		copyFile(rollFile, logFile);
		JUnit4Util.sleep(interval * 2);
		periodicFileCollector.cancel();

		List<String> expect = Arrays.asList(new String[] {});
		assertThat("collect simple", listener.getLines(), is(expect));
		
		JUnit4Util.endCurrentTest(getClass());
	}

	@Test
	public void testNewFileRolling() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);

		NioFileCollector nioCollector = new NioFileCollector(20);
		MockListener listener = new MockListener();
		nioCollector.addLineListener(listener);
		
		PeriodicFileCollector periodicFileCollector = new PeriodicFileCollector();
		periodicFileCollector.setCollector(nioCollector);
		periodicFileCollector.setInterval(interval);
		periodicFileCollector.collect(logFile);
		JUnit4Util.sleep(interval);
		FileUtils.moveFile(logFile, new File("target/test/init.log.1"));
		copyFile(initFile, logFile);
		JUnit4Util.sleep(interval * 3);
		periodicFileCollector.cancel();

		List<String> expect = Arrays.asList(new String[] {"0123456789", "1123456789", "2123456789", "3123456789"});
		assertThat("collect simple", listener.getLines(), is(expect));
		
		JUnit4Util.endCurrentTest(getClass());
	}
}
