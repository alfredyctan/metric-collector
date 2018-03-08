package org.alf.metric.writer;

import static java.util.Arrays.*;
import static org.afc.util.JUnitUtil.*;
import static org.mockito.Mockito.*;

import java.util.LinkedList;
import java.util.List;

import org.afc.junit5.extension.TestInfoExtension;
import org.alf.metric.collector.MockMetricWriter;
import org.alf.metric.config.Config;
import org.alf.metric.model.Metric;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith({ TestInfoExtension.class, SpringExtension.class })
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(Random.class)
class ReactiveMetricWriterTest {

	private static final int INTERVAL = 100;

	private MetricWriter.Factory<List<Metric>> factory;

	private MockMetricWriter<List<Metric>> mockWriter;

	private Metric M001 = new Metric().setName("001");
	private Metric M002 = new Metric().setName("002");
	private Metric M003 = new Metric().setName("003");
	private Metric M004 = new Metric().setName("004");
	private Metric M005 = new Metric().setName("005");
	private Metric M006 = new Metric().setName("006");
	private Metric M007 = new Metric().setName("007");
	private Metric M008 = new Metric().setName("008");
	private Metric M009 = new Metric().setName("009");
	private Metric M010 = new Metric().setName("010");

	@BeforeAll
	static void setUpBeforeClass() throws Exception {

	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		mockWriter = new MockMetricWriter();
		factory = Mockito.mock(MetricWriter.Factory.class);
		when(factory.create()).thenReturn(mockWriter);
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void test() {
		ReactiveMetricWriter writer = new ReactiveMetricWriter(
			new Config.Writer().setInterval(INTERVAL).setBatch(3),
			new Config.Worker().setWriter(10),
			factory
		);

		writer.write(M001);
		writer.write(M002);
		writer.write(M003); //count flush
		writer.write(M004);
		sleep(INTERVAL); //time flush
//		List<List<Metric>> actual1 = actual(new LinkedList<>(mockWriter.getMetrics()));
//		List<List<Metric>> expect1 = expect(asList(asList(M001, M002, M003)));
//		assertContains("count flush", actual1, expect1);

		sleep(INTERVAL * 2); //time flush
//		List<List<Metric>> actual2 = actual(new LinkedList<>(mockWriter.getMetrics()));
//		List<List<Metric>> expect2 = expect(asList(asList(M001, M002, M003), asList(M004)));
//		assertContains("time flush", actual2, expect2);

		writer.write(M005);
		writer.write(M006);
		sleep(INTERVAL * 2); //time flush
//		List<List<Metric>> actual3 = actual(new LinkedList<>(mockWriter.getMetrics()));
//		List<List<Metric>> expect3 = expect(asList(asList(M001, M002, M003), asList(M004), asList(M005, M006)));
//		assertContains("time flush", actual3, expect3);

		writer.write(M007);
		writer.write(M008);
		writer.write(M009); //count flush
		writer.write(M010);
		sleep(INTERVAL); //time flush
		List<List<Metric>> actual4 = actual(new LinkedList<>(mockWriter.getMetrics()));
		List<List<Metric>> expect4 = expect(asList(asList(M001, M002, M003), asList(M004), asList(M005, M006), asList(M007, M008, M009), asList(M010)));
		assertContains("count flush", actual4, expect4);
	}
}
