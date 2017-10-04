package org.alf.metric.parse;

import java.math.BigDecimal;

public class ParseUtil {

	public static Object parseNumeric(String value) {
		try {
			return new BigDecimal(value); 
		} catch (Exception e) {
			return value;
		}
	}

}
