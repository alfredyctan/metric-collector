package org.alf.metric;

import org.afc.SystemEnvironment;
import org.junit.jupiter.api.extension.Extension;

public class MetricCollectorExtension implements Extension {

	static {
		SystemEnvironment.set("mc", "test", "sg", "default", "sg1");
	}

}
