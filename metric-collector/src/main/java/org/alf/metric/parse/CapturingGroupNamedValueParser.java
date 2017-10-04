package org.alf.metric.parse;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CapturingGroupNamedValueParser implements NamedValueParser {

	private static final Pattern NAMED_PATTERN = Pattern.compile("\\(\\?\\<.*?\\>.*?\\)");

	private List<String> names;

	private Pattern compliedLinePattern;

	public CapturingGroupNamedValueParser(String linePattern) {
		this.names = new LinkedList<>();
		compile(linePattern);
	}

	@Override
	public Map<String, Object> parse(CharSequence line) {
		Map<String, Object> map = new LinkedHashMap<>();
		Matcher matcher = compliedLinePattern.matcher(line);
		if (matcher.find()) {
			for (int i = 0; i < names.size(); i++) {
				map.put(names.get(i), ParseUtil.parseNumeric(matcher.group(i + 1)));
			}
		}
		return map;
	}

	private void compile(String linePattern) {
		for (Matcher matcher = NAMED_PATTERN.matcher(linePattern); 
			 matcher.find(); 
			 matcher = NAMED_PATTERN.matcher(linePattern)) {

			String matchedName = matcher.group();
			String name = matchedName.substring(3, matchedName.indexOf('>'));
			String value = matchedName.substring(matchedName.indexOf('>') + 1, matchedName.length() - 1);
			linePattern = matcher.replaceFirst('(' + value + ')');
			names.add(name);
		}
		compliedLinePattern = Pattern.compile(linePattern);
	}
}