package org.alf.metric.collector;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.getFile;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.afc.util.JUnit4Util;
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
	
	private Mockery mockery;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		logDir = getFile("target/test");
		logFile = getFile("target/test/init.log");
		initFile = getFile("src/test/resources/log/init.log");
		increFile = getFile("src/test/resources/log/roll-increment.log");
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
		periodicFileCollector.setInterval(1);
		periodicFileCollector.collect(logFile);
		
		JUnit4Util.sleep(3000);
		
		JUnit4Util.endCurrentTest(getClass());
	}

}
