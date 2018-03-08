package org.alf.metric;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "org.alf.metric.config"})
public class MetricCollector {
	public static void main(String[] args) {
		SpringApplication.run(MetricCollector.class, args);
	}
}
