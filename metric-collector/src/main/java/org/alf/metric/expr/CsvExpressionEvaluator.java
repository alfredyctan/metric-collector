package org.alf.metric.expr;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CsvExpressionEvaluator implements ExpressionEvaulator {

	private static final Logger logger = LoggerFactory.getLogger(CsvExpressionEvaluator.class);

	private Map<String, String> expressions;

	private ScriptEngine engine;

	private Pattern PATTERN = Pattern.compile("\\\"([^\\\"]*)\\\"|(?:[=\\+\\*/]|^)(\\w*)");

	public CsvExpressionEvaluator(String expression) {
		engine = new ScriptEngineManager().getEngineByName("groovy");

		expressions = new HashMap<>();
		for (String expr : expression.split(",")) {
			int index = expr.indexOf('=');
			if (expr.startsWith("\"") && expr.endsWith("\"")) {
				if (index == -1) {
					expressions.put(expr.substring(1, expr.length() - 1), expr);
				} else {
					expressions.put(expr.substring(1, index), '"' + expr.substring(index + 1, expr.length() - 1) + '"');
				}
			} else if (!expr.startsWith("\"") && !expr.endsWith("\"")) {
				if (index == -1) {
					expressions.put(expr, expr);
				} else {
					expressions.put(expr.substring(0, index), expr.substring(index + 1, expr.length()));
				}
			} else {
				throw new RuntimeException("invalid expression [" + expr + ']');
			}
		}
		logger.info("compiled expressions:[{}]", expressions);
	}

	@Override
	public Map<String, String> evaluate(Map<String, Object> params) {
		Map<String, String> evaluated = new HashMap<>();

		for (Map.Entry<String, String> entry : expressions.entrySet()) {
			String entryValue = entry.getValue();
			entryValue = entryValue.startsWith("\"") ? entryValue.substring(1, entryValue.length() - 1) : entryValue;

			Object paramValue = params.get(entryValue);
			String evaluatedValue = null;
			if (paramValue != null) {
				evaluatedValue = paramValue.toString();
			} else {
				Bindings bindings = engine.createBindings();
				bindings.putAll(params);
				try {
					evaluatedValue = engine.eval(entryValue, bindings).toString();
				} catch (ScriptException e) {
					logger.error("cannot evalute expression:[{}]", entry.getValue());
				}
			}
			evaluatedValue = entry.getValue().startsWith("\"") ? '"' + evaluatedValue + '"' : evaluatedValue;
			evaluated.put(entry.getKey(), evaluatedValue);
		}

		return evaluated;
	}

}
