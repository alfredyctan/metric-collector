package org.alf.metric.expr;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.afc.junit5.extension.TestInfoExtension;
import org.alf.metric.MetricCollectorExtension;
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

@ExtendWith({ MetricCollectorExtension.class, TestInfoExtension.class })
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(Random.class)
class CsvExpressionEvaluatorTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void testAddition() {
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
	void testConcat() {
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
