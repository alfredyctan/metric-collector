package org.alf.metric.writer;

import static java.util.stream.Collectors.*;
import static org.afc.util.CollectionUtil.*;

import java.util.List;
import java.util.Map;

import org.afc.util.IOUtil;
import org.alf.metric.config.Config;
import org.alf.metric.model.Metric;
import org.alf.metric.util.ElasticSearchSupport;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;

public class ElasticMetricWriter implements MetricWriter<List<Metric>>, ElasticSearchSupport {

	private static final Logger logger = LoggerFactory.getLogger(ElasticMetricWriter.class);

	@Getter
	private RestHighLevelClient esClient;

	public ElasticMetricWriter(Config.Writer writer) {
		this.esClient = restHighLevelClient(writer.getUrl());
	}

	@Override
	public void write(List<Metric> metric) {
		stream(metric)
			.collect(groupingBy(Metric::getName, toList()))
			.forEach((name, metrics) -> {
				List<Map<String, Object>> sources = metrics.stream()
					.map(m -> {
						m.getTags().put(TIMESTAMP, m.getTime().toString());
						Map<String, Object> source = (Map)m.getTags();
						return source;
					})
					.collect(toList());
				bulkSave(name, sources);
			});
	}

	@Override
	public void dispose() {
		IOUtil.close(esClient);
	}
}
