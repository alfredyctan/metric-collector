package org.alf.metric.config;

import static org.mockserver.integration.ClientAndServer.*;
import static org.mockserver.model.HttpClassCallback.*;
import static org.mockserver.model.HttpRequest.*;

import org.alf.metric.mock.BulkPutIndiceDocCallback;
import org.alf.metric.mock.PutIndiceDocCallback;
import org.mockserver.integration.ClientAndServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockContext {

	@Bean
	public ClientAndServer mockServer() {
		ClientAndServer mockServer = startClientAndServer(1083);
		mockServer
			.when(request().withPath("/0001.wip/_doc/.*"))
			.callback(callback().withCallbackClass(PutIndiceDocCallback.class.getName()));
		mockServer
			.when(request().withPath("/_bulk"))
			.callback(callback().withCallbackClass(BulkPutIndiceDocCallback.class.getName()));
		return mockServer;
	}
}
