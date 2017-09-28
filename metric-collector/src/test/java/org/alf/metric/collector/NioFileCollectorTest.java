package org.alf.metric.collector;

import static java.nio.file.StandardOpenOption.READ;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.getFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import org.afc.util.JUnit4Util;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class NioFileCollectorTest {

	private static File logDir;

	private static File logFile;
	
	private static File initFile;
	
	private static File initLongLineFile;

	private static File increFile;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		logDir = getFile("target/test");
		logFile = getFile("target/test/init.log");
		initFile = getFile("src/test/resources/log/init.log");
		initLongLineFile = getFile("src/test/resources/log/init-long-line.log");
		increFile = getFile("src/test/resources/log/roll-increment.log");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		try {
			deleteDirectory(getFile(logDir));
		} catch (Exception e) {
			
		}
	}

	@After
	public void tearDown() throws Exception {
		try {
			deleteDirectory(getFile(logDir));
		} catch (Exception e) {

		}
	}

	@Test
	public void testCollectSizeBufferEqualToLine() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);
		
		NioFileCollector collector = new NioFileCollector(12);
		MockListener listener = new MockListener();
		collector.addLineListener(listener);
		
		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();
		
		List<String> expect = Arrays.asList(new String[] { "0123456789", "1123456789", "2123456789", "3123456789" });
		assertThat("collect simple", listener.getLines(), is(expect));
		JUnit4Util.endCurrentTest(getClass());
	}

	@Test
	public void testCollectSizeBufferLessThanLine() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);
		
		NioFileCollector collector = new NioFileCollector(10);
		MockListener listener = new MockListener();
		collector.addLineListener(listener);
		
		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();
		
		List<String> expect = Arrays.asList(new String[] { "0123456789", "1123456789", "2123456789", "3123456789" });
		assertThat("collect simple", listener.getLines(), is(expect));
		JUnit4Util.endCurrentTest(getClass());
	}

	@Test
	public void testCollectSizeBufferLessThanLineMore() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);
		
		NioFileCollector collector = new NioFileCollector(8);
		MockListener listener = new MockListener();
		collector.addLineListener(listener);
		
		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();
		
		List<String> expect = Arrays.asList(new String[] { "0123456789", "1123456789", "2123456789", "3123456789" });
		assertThat("collect simple", listener.getLines(), is(expect));
		JUnit4Util.endCurrentTest(getClass());
	}

	@Test
	public void testCollectSizeBufferMoreThanLine() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);
		
		NioFileCollector collector = new NioFileCollector(22);
		MockListener listener = new MockListener();
		collector.addLineListener(listener);
		
		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();
		
		List<String> expect = Arrays.asList(new String[] { "0123456789", "1123456789", "2123456789", "3123456789" });
		assertThat("collect simple", listener.getLines(), is(expect));
		JUnit4Util.endCurrentTest(getClass());
	}

	@Test
	public void testCollectSizeBufferMoreThan2Line() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);
		
		NioFileCollector collector = new NioFileCollector(24);
		MockListener listener = new MockListener();
		collector.addLineListener(listener);
		
		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();
		
		List<String> expect = Arrays.asList(new String[] { "0123456789", "1123456789", "2123456789", "3123456789" });
		assertThat("collect simple", listener.getLines(), is(expect));
		JUnit4Util.endCurrentTest(getClass());
	}	

	@Test
	public void testCollectSizeBufferMoreThan3Line() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);
		
		NioFileCollector collector = new NioFileCollector(40);
		MockListener listener = new MockListener();
		collector.addLineListener(listener);
		
		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();
		
		List<String> expect = Arrays.asList(new String[] { "0123456789", "1123456789", "2123456789", "3123456789" });
		assertThat("collect simple", listener.getLines(), is(expect));
		JUnit4Util.endCurrentTest(getClass());
	}	
	
	@Test
	public void testCollectFromMiddle() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);
		
		NioFileCollector collector = new NioFileCollector(20);
		MockListener listener = new MockListener();
		collector.addLineListener(listener);
		
		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		fileChannel.position(5);
		collector.collect(fileChannel);
		fileChannel.close();
		
		List<String> expect = Arrays.asList(new String[] { "56789", "1123456789", "2123456789", "3123456789" });
		assertThat("collect simple", listener.getLines(), is(expect));
		JUnit4Util.endCurrentTest(getClass());
	}	

	@Test
	public void testCollectOneByteBuffer() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);
		
		NioFileCollector collector = new NioFileCollector(1);
		MockListener listener = new MockListener();
		collector.addLineListener(listener);
		
		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();
		
		List<String> expect = Arrays.asList(new String[] { "0123", "1123", "2123", "3123" });
		assertThat("collect simple", listener.getLines(), is(expect));
		JUnit4Util.endCurrentTest(getClass());
	}
	
	@Test
	public void testCollectLongLine() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initLongLineFile, logFile);
		
		NioFileCollector collector = new NioFileCollector(4);
		MockListener listener = new MockListener();
		collector.addLineListener(listener);
		
		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		collector.collect(fileChannel);
		fileChannel.close();
		
		List<String> expect = Arrays.asList(new String[] { "0123456789012345", "1123456789112345", "2123456789212345", "3123456789312345" });
		assertThat("collect simple", listener.getLines(), is(expect));
		JUnit4Util.endCurrentTest(getClass());
	}

	@Test
	public void testCollectFromEnd() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);
		
		NioFileCollector collector = new NioFileCollector(20);
		MockListener listener = new MockListener();
		collector.addLineListener(listener);
		
		Path path = Paths.get(logFile.toURI());
		FileChannel fileChannel = FileChannel.open(path, READ);
		fileChannel.position(fileChannel.size());
		collector.collect(fileChannel);
		
		
		
		
		JUnit4Util.sleep(600000000);
		fileChannel.close();
		
		List<String> expect = Arrays.asList(new String[] { "56789", "1123456789", "2123456789", "3123456789" });
		assertThat("collect simple", listener.getLines(), is(expect));
		JUnit4Util.endCurrentTest(getClass());
	}	

	@Test
	public void testFileGrowth() throws IOException {
		JUnit4Util.startCurrentTest(getClass());
		
		copyFile(initFile, logFile);
		
//		Path path = Paths.get(logFile.toURI());
//		FileChannel fileChannel = FileChannel.open(path, READ);
//		fileChannel.position(fileChannel.size());
		while (true) {
			try {
//			logFile = getFile("target/test/init.log");
			BasicFileAttributes attr = Files.readAttributes(logFile.toPath(), BasicFileAttributes.class);
			System.out.println(attr.creationTime() + " " + attr.lastModifiedTime());
			} catch (Exception e) {
				System.out.println("no file");
			}
//			System.out.println(fileChannel.size());
			JUnit4Util.sleep(1000);
		}


////		JUnit4Util.sleep(1500000);
//
//		JUnit4Util.endCurrentTest(getClass());
	}
}
