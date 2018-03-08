package org.alf.metric.model;

import java.time.OffsetDateTime;
//import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class Metric {

	private String name;

	private Map<String, String> indexes;

	private Map<String, String> tags;

	private OffsetDateTime time;

//	private List<Metric> metrics;
}
