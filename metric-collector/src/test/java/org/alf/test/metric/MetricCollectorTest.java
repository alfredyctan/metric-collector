package org.alf.test.metric;

import static org.afc.util.JUnitUtil.*;
import static org.afc.util.JUnitUtil.copyFile;
import static org.apache.commons.io.FileUtils.*;

import java.io.File;
import java.util.List;

import org.afc.junit5.extension.TestInfoExtension;
import org.alf.metric.MetricCollector;
import org.alf.metric.MetricCollectorExtension;
import org.alf.metric.config.MockContext;
import org.alf.metric.mock.BulkPutIndiceDocCallback;
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
import org.mockserver.model.HttpRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;


@SpringBootTest(classes = {
	MetricCollector.class, MockContext.class
}, webEnvironment = WebEnvironment.RANDOM_PORT, properties = { "management.port=0" })
@ActiveProfiles({ "mc", "test", "hk", "default", "hk1", "debug" })

@ExtendWith({ MetricCollectorExtension.class, TestInfoExtension.class, SpringExtension.class })
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(Random.class)
class MetricCollectorTest {

	private static File logDir;

	private static File logFile;

	private static File logSrc;

	private static File markerFile;

	private static File markerSrc;

	static {
		System.setProperty("BUILD_NUMBER", "0001");
		System.setProperty("TEST_PACK", "wip");
	}

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		logDir = getFile("target/test");
		logFile = getFile("target/test/app.cluster.instance.date.hash.log.0");
		markerFile = getFile("target/test/jmeter.cluster.instance.date.log.0");
		logSrc = getFile("src/test/resources/log/mc-app.log");
		markerSrc = getFile("src/test/resources/log/mc-marker.log");
		try {
			deleteDirectory(getFile("target/test"));
		} catch (Exception e) {
		}
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
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
	void testHoldAndLaunch() {
		sleep(1000);
		copyFile(logSrc, logFile);
		sleep(1000);
		copyFile(markerSrc, markerFile);
		sleep(3000);

		List<HttpRequest> actual = actual(BulkPutIndiceDocCallback.requests);
		assertFalse("msg sent", actual.isEmpty());
	}
}
