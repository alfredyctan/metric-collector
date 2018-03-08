package org.alf.metric.mock;


import static org.mockserver.model.HttpResponse.*;

import java.util.LinkedList;
import java.util.List;

import org.mockserver.mock.action.ExpectationCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;

public class PutIndiceDocCallback implements ExpectationCallback {

	public static List<HttpRequest> requests = new LinkedList<>();

    @Override
	public HttpResponse handle(HttpRequest httpRequest) {
    	requests.add(httpRequest);
    	String body = "{\"_index\":\"0001.poc\",\"_type\":\"_doc\",\"_id\":\"2s5sjH8BPWU4bUP8TSa7\",\"_version\":1,\"result\":\"created\",\"_shards\":{\"total\":2,\"successful\":1,\"failed\":0},\"_seq_no\":0,\"_primary_term\":1}";
        return response()
        	.withHeader("Content-Type", "application/json")
        	.withStatusCode(200)
        	.withBody(body);
    }
}
