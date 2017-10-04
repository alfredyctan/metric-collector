package org.alf.metric.parse;

import java.util.Map;

public interface NamedValueParser {

	public Map<String, Object> parse(CharSequence line);
	
}
