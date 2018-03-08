package org.alf.metric.util;

import static java.util.stream.Collectors.*;
import static org.afc.util.CollectionUtil.*;
import static org.afc.util.JUnitUtil.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.alf.metric.model.Metric;


public class AssertUtil {

	public static void assertMetrics(List<Metric> actual, List<Metric> expect) {
		assertThat("size", actual.size(), is(equalTo(expect.size())));
		if (expect != null && expect.size() > 0 && expect.get(0).getTime() != null) {
			Map<OffsetDateTime, Metric> expectMap = expect.stream().collect(toMap(q -> q.getTime(), v -> v));
			actual.stream().forEach(a -> assertMetric(a, expectMap.get(a.getTime())));
		} else {
			assertThat("size", actual.size(), is(equalTo(expect.size())));
			AtomicInteger seq = new AtomicInteger(0);
			coStream(actual, expect).forEach(co -> assertMetric(co.x, co.y));
		}
	}

	public static void assertMetric(Metric actual, Metric expect) {
		assertMap("metric.indexes", actual.getIndexes(), expect.getIndexes());
		assertMap("metric.tags", actual.getTags(), expect.getTags());
		assertEquals("metric", strip(actual), strip(expect));
	}

	private static Metric strip(Metric metric) {
		metric.setIndexes(null);
		metric.setTags(null);
		return metric;
	}
}
