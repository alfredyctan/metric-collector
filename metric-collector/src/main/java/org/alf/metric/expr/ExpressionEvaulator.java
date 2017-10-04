package org.alf.metric.expr;

import java.util.Map;

public interface ExpressionEvaulator {

	public Map<String, String> evaluate(Map<String, Object> params);
	
}
