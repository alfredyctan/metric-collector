package org.alf.metric.buffer;

import static java.util.Arrays.*;
import static org.afc.util.CollectionUtil.*;
import static org.afc.util.JUnitUtil.*;
import static org.alf.metric.util.AssertUtil.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.afc.junit5.extension.TestInfoExtension;
import org.afc.util.DateUtil;
import org.alf.metric.MetricCollectorExtension;
import org.alf.metric.collector.MockMetricWriter;
import org.alf.metric.config.Config.Capture;
import org.alf.metric.config.Config.Source;
import org.alf.metric.config.Config.Timestamp;
import org.alf.metric.launch.LaunchControl;
import org.alf.metric.model.Metric;
import org.alf.metric.writer.MetricWriter;
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
class PageBufferListenerTest {

	private Source source;

	private Capture capture;

	private Map<String, String> context;

	private MetricWriter.Factory factory;

	private MockMetricWriter mockMetricWriter;

	private LaunchControl launchControl;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		source = new Source();
		capture = new Capture();
		context = new HashMap<>();
		context.put("service", "rfq");

		factory = Mockito.mock(MetricWriter.Factory.class);
		mockMetricWriter = new MockMetricWriter();
		when(factory.create()).thenReturn(mockMetricWriter);

		launchControl = Mockito.mock(LaunchControl.class);
		when(launchControl.getAdjustment()).thenReturn(-1000L);
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	@Test
	void testMatch() {
		System.setProperty("BUILD_NUMBER", "0001");
		System.setProperty("TEST_PACK", "wip");

		capture.setContains(asList("[OUT]", "segment:"))
			.setPattern("^(?<msgtime>.*?) \\[\\s?(?<level>\\S*?)\\]\\[(?<ctx>.*?)\\]\\[(?<tid>.*?)\\]\\[(?<logger>\\S*?)\\s*?\\] : (?<msg>\\[(?<check>.*?)\\].*?start:\\[(?<start>.*?)\\].*?total:\\[(?<total>.*?)\\].*?segment:\\[(?<segment>.*?)\\].*$)")
			.setName("%{$sys(BUILD_NUMBER)}.%{$sys(TEST_PACK)}")
			.setTimestamp(new Timestamp("msgtime", "yyyy-MM-dd HH:mm:ss.SSS Z"))
			.setTags(Map.of(
				"label", "junit",
		        "msg_time", "%{#msgtime}",
		        "msgtime", "%{!!}"
			))
			.setIndexes(Map.of(
				"level", "%{#level}",
		        "logger", "%{#logger}"
			));

		PageBufferListener listener = new PageBufferListener(source, capture, context, factory, launchControl);
		String recvBuffer = "2022-03-11 03:49:26.641 +0000 [ INFO][VR2EGXBJ][00050][.rfq.api.v1.setup.channel ] : [OUT] start:[53330920], total:[9], segment:[1]";
		listener.onReceived(recvBuffer);

		List<Metric> actual = actual(mockMetricWriter.getMetrics());
		List<Metric> expect = expect(asList(new Metric()
			.setName("0001.wip")
			.setTime(DateUtil.offsetDateTime("2022-03-11T03:49:25.641Z"))
			.setIndexes(map(new LinkedHashMap<>(), "level", "INFO", "logger", ".rfq.api.v1.setup.channel"))
			.setTags(map(new LinkedHashMap<>(),
				"msg", "[OUT] start:[53330920], total:[9], segment:[1]",
				"total", "9",
				"level", "INFO",
				"service", "rfq",
				"ctx", "VR2EGXBJ",
				"logger", ".rfq.api.v1.setup.channel",
				"segment", "1",
				"start", "53330920",
				"msg_time", "2022-03-11 03:49:26.641 +0000",
				"label", "junit",
				"tid", "00050"
			))
		));
		assertMetrics(actual, expect);
	}
}
