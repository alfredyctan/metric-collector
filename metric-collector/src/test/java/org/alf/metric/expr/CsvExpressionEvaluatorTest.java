package org.alf.metric.expr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CsvExpressionEvaluatorTest {

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
	public void testAddition() {
		CsvExpressionEvaluator evaluator =  new CsvExpressionEvaluator("num1,num2=num3+num4,\"num5\",\"num6=num3+num4\"");
		
		Map<String, Object> params = new HashMap<>();
		params.put("num1", new BigDecimal("10"));
		params.put("num3", new BigDecimal("30"));
		params.put("num4", new BigDecimal("40"));
		params.put("num5", new BigDecimal("50"));
		
		
		Map<String, String> actual = evaluator.evaluate(params);
		
		assertThat("name and value", actual, allOf(
				hasEntry("num1", "10"),
				hasEntry("num2", "70"),
                hasEntry("num5", "\"50\""),
                hasEntry("num6", "\"70\"")
        ));
	}

	@Test
	public void testConcat() {
		CsvExpressionEvaluator evaluator =  new CsvExpressionEvaluator("num1,num2=num3+num4,\"num5\",\"num6=''+num3+num4\"");
		
		Map<String, Object> params = new HashMap<>();
		params.put("num1", new BigDecimal("10"));
		params.put("num3", new BigDecimal("30"));
		params.put("num4", new BigDecimal("40"));
		params.put("num5", new BigDecimal("50"));
		
		
		Map<String, String> actual = evaluator.evaluate(params);
		
		assertThat("name and value", actual, allOf(
				hasEntry("num1", "10"),
				hasEntry("num2", "70"),
                hasEntry("num5", "\"50\""),
                hasEntry("num6", "\"3040\"")
        ));
	}	
}
