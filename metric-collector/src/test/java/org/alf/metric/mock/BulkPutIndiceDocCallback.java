package org.alf.metric.mock;


import static org.mockserver.model.HttpResponse.*;

import java.util.LinkedList;
import java.util.List;

import org.mockserver.mock.action.ExpectationCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

public class BulkPutIndiceDocCallback implements ExpectationCallback {

	public static List<HttpRequest> requests = new LinkedList<>();

    @Override
	public HttpResponse handle(HttpRequest httpRequest) {
    	requests.add(httpRequest);
    	String body = "{\"took\":1097,\"errors\":false,\"items\":[{\"index\":{\"_index\":\"0001.wip\",\"_type\":\"_doc\",\"_id\":\"vKq3jH8Bzb-dy9S87fAI\",\"_version\":1,\"result\":\"created\",\"_shards\":{\"total\":2,\"successful\":1,\"failed\":0},\"_seq_no\":0,\"_primary_term\":1,\"status\":201}}]}";
        return response()
        	.withHeader("Content-Type", "application/json")
        	.withStatusCode(200)
        	.withBody(body);
    }
}
