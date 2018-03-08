package org.alf.metric;

import org.afc.SystemEnvironment;
import org.afc.util.ClasspathUtil;
import org.afc.util.FileUtil;

public class MetricCollectorLocal {

	public static void main(String[] args) {
		System.setProperty("BUILD_NUMBER", "0001");
		System.setProperty("TEST_PACK", "wip");
		System.setProperty("sys.log", FileUtil.resolveAbsolutePath("target"));
		SystemEnvironment.set("mc", "local", "sg", "default", "sg1");
		ClasspathUtil.addSystemClasspath("target/config");
		MetricCollector.main(new String[] { "--spring.profiles.active=local,sg,default,sg1" });
	}
}
