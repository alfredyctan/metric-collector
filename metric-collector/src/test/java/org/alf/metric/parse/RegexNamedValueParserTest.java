package org.alf.metric.parse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;

import java.nio.CharBuffer;
import java.util.Map;

import org.afc.util.JUnit4Util;
import org.alf.metric.parse.RegexNamedValueParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class RegexNamedValueParserTest {

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
	public void testParseString() {
		JUnit4Util.startCurrentTest(getClass());
		
		RegexNamedValueParser parser = new RegexNamedValueParser(".*?\\[(%thread=.*?%)\\]\\[.*?\\]\\[(%clazz=.*?%)\\].*");
		Map<String, Object> actual = parser.parse("2017-09-25 21:23:13.141 [main][INFO ][o.a.m.collector.NioFileCollector] last size : 58");
		
		assertThat("name and value", actual, allOf(
				hasEntry("thread", "main"),
                hasEntry("clazz", "o.a.m.collector.NioFileCollector")
        ));
		
		JUnit4Util.endCurrentTest(getClass());
	}

	@Test
	public void testParseCharBuffer() {
		JUnit4Util.startCurrentTest(getClass());
		
		RegexNamedValueParser parser = new RegexNamedValueParser(".*?\\[(%thread=.*?%)\\]\\[.*?\\]\\[(%clazz=.*?%)\\].*");
		
		CharBuffer buffer = CharBuffer.allocate(256);
		buffer.append("2017-09-25 21:23:13.141 [main][INFO ][o.a.m.collector.NioFileCollector] last size : 58");
		buffer.position(0);
		
		Map<String, Object> actual = parser.parse(buffer);
		
		assertThat("name and value", actual, allOf(
				hasEntry("thread", "main"),
                hasEntry("clazz", "o.a.m.collector.NioFileCollector")
        ));
		
		JUnit4Util.endCurrentTest(getClass());
	}
}
