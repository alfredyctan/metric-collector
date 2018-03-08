package org.alf.metric.config;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@ConfigurationProperties("metric-collector")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Config {

	private Map<String, Writer> writers;

	private List<Source> sources;

	private Launch launch;

	private Worker worker;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Accessors(chain = true)
	public static class Writer {

		//elastic-search
		private String url;

		//influx-db
		private String host;

		//influx-db
		private Integer port;

		//reactive
		private long interval;

		//reactive
		private int batch;

	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Accessors(chain = true)
	public static class Source {

		private List<String> paths;

		private String pattern;

		private Integer interval;

		private Integer buffer;

		private String pageBreak;

		private Boolean beginning;

		private Map<String, Writer> writers;

		private List<Capture> captures;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Accessors(chain = true)
	public static class Capture {

		private List<String> contains;

		private String pattern;

		private String name;

		private Timestamp timestamp;

		private Map<String, String> indexes;

		private Map<String, String> tags;

	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Accessors(chain = true)
	public static class Timestamp {

		private String tag;

		private String pattern;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Accessors(chain = true)
	public static class Worker {

		private Integer source;

		private Integer process;

		private Integer writer;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	@Accessors(chain = true)
	public static class Launch {

		private OffsetDateTime genesis;

		private Source source;
	}
}
