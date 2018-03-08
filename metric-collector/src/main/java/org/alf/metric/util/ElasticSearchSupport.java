package org.alf.metric.util;

import static java.util.Arrays.*;
import static java.util.stream.Collectors.*;
import static org.afc.util.CollectionUtil.*;
import static org.afc.util.CollectionUtil.stream;
import static org.afc.util.DateUtil.*;
import static org.afc.util.OptionalUtil.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import org.afc.AFCException;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.client.indices.GetMappingsRequest;
import org.elasticsearch.client.indices.GetMappingsResponse;
import org.elasticsearch.client.indices.ResizeRequest;
import org.elasticsearch.client.indices.ResizeResponse;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.TermsQueryBuilder;
import org.elasticsearch.index.reindex.AbstractBulkByScrollRequest;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.SneakyThrows;


public interface ElasticSearchSupport {

	static final Logger logger = LoggerFactory.getLogger(ElasticSearchSupport.class);

	public static final int TIME_BUCKET = 1;

	public static final int BATCH_SIZE = 10000;

	public static final String INDEX_PREFIX = "logstash-";

	public static final int MAX_ENTITY_SIZE = 1024 * 1024 * 100;

	public static final int MAX_ENTITY_THRESHOLD = MAX_ENTITY_SIZE * 9 / 10;

	public static final String META_LABEL = "label";

	public static final String MSG_TIME = "msg_time";

	public static final String TIMESTAMP = "@timestamp";

	public static final String TYPE = "type";

	public static final String NAMESPACE = "namespace";

	public static final String REGION = "region";

	public static final String SERVICE = "service";

	public static final String CLUSTER = "cluster";

	public static final String INSTANCE_INDEX = "instance_index";

	public static final String TAG = "tag";

	RestHighLevelClient getEsClient();

	@SneakyThrows
	default RestHighLevelClient restHighLevelClient(String url) {
		logger.info("creating RHL client to:[{}]", url);
		URI uri = new URI(url);

		RestClientBuilder builder = restClientBuilder(uri.getHost(), uri.getPort(), uri.getScheme());
		builder.setHttpClientConfigCallback(b -> {
			trustAllSsl(b);
			return b;
		});

		builder.setRequestConfigCallback(requestConfigBuilder -> {
            requestConfigBuilder.setContentCompressionEnabled(true);
            requestConfigBuilder.setSocketTimeout(1800 * 1000);
            requestConfigBuilder.setConnectTimeout(1800 * 1000);
            requestConfigBuilder.setConnectionRequestTimeout(1800 * 1000);
            return requestConfigBuilder;
        });
		return new RestHighLevelClient(builder);
	}

	@SneakyThrows
	default List<BulkResponse> bulkSave(String indice, List<Map<String, Object>> sources) {
		BulkRequest bulkRequest = new BulkRequest();
		forEach(sources, source -> {
			IndexRequest request = new IndexRequest()
				.index(indice)
				.id(UUIDs.base64UUID())
				.source(source);
			bulkRequest.add(request);
		});

		if (bulkRequest.estimatedSizeInBytes() < MAX_ENTITY_THRESHOLD) {
			return asList(getEsClient().bulk(bulkRequest, RequestOptions.DEFAULT));
		} else {
			logger.info("bulk request size:[{}], split batch", bulkRequest.estimatedSizeInBytes());
			int half = sources.size() / 2;
			return Stream.of(
				bulkSave(indice, sources.subList(0, half)),
				bulkSave(indice, sources.subList(half, sources.size()))
			)
			.flatMap(List::stream)
			.collect(toList());
		}
	}

	@SneakyThrows
	default ResizeResponse cloneIndice(String source, String target) {
		logger.info("cloning indice : [{}] -> [{}]", source, target);
		updateIndiceSetttings(source, map(new HashMap<>(), "index.blocks.write", true));
		ResizeRequest request = new ResizeRequest(target, source);
		ResizeResponse response = getEsClient().indices().clone(request, RequestOptions.DEFAULT);
		updateIndiceSetttings(source, map(new HashMap<>(), "index.blocks.write", false));
		updateIndiceSetttings(target, map(new HashMap<>(), "index.blocks.write", false));
		return response;
	}

	@SneakyThrows
	default CountResponse count(String indice, String type, String timeFrom, String timeTo, Map<String, String> filters) {
		BoolQueryBuilder and = query(type, timeFrom, timeTo, filters);

		CountRequest countRequest = new CountRequest();
		countRequest.indices(indice);
		countRequest.query(and);
		return getEsClient().count(countRequest, RequestOptions.DEFAULT);
	}

	@SneakyThrows
	default AcknowledgedResponse deleteIndice(String indice) {
		try {
			GetIndexResponse response = listIndices(indice);
			if (List.of(response.getIndices()).contains(indice)) {
				DeleteIndexRequest request = new DeleteIndexRequest(indice);
				AcknowledgedResponse ack = getEsClient().indices().delete(request, RequestOptions.DEFAULT);
				logger.info("indice [{}] is deleted:[{}]", indice, ack.isAcknowledged());
				return ack;
			} else {
				logger.info("indice [{}] is not exists", indice);
				return new AcknowledgedResponse(true);
			}
		} catch (ElasticsearchStatusException e) {
			logger.info("indice [{}] is not exists", indice);
			return new AcknowledgedResponse(true);
		}
	}

	@SneakyThrows
	default GetMappingsResponse getIndiceMapping(String indice, boolean includeDefaults) {
		GetMappingsRequest request = new GetMappingsRequest()
			.indices(indice)
			.includeDefaults(includeDefaults);
		return getEsClient().indices().getMapping(request, RequestOptions.DEFAULT);
	}

	@SneakyThrows
	default GetSettingsResponse getIndiceSetting(String indice, boolean includeDefaults) {
		GetSettingsRequest request = new GetSettingsRequest()
			.indices(indice)
			.includeDefaults(includeDefaults);
		return getEsClient().indices().getSettings(request, RequestOptions.DEFAULT);
	}

	@SneakyThrows
	default GetIndexResponse listIndices(String indice) {
		GetIndexRequest request = new GetIndexRequest(indice);
	    return getEsClient().indices().get(request, RequestOptions.DEFAULT);
	}

	default BoolQueryBuilder query(String type, String timeFrom, String timeTo, Map<String, String> filters) {
		TermQueryBuilder termType = termQuery(TYPE, type);
		RangeQueryBuilder range = rangeQuery(TIMESTAMP).gte(timeFrom).lt(timeTo);
		BoolQueryBuilder and = boolQuery().must(termType).must(range);
		if (filters != null) {
			stream(filters.entrySet()).forEach(e -> {
				and.must(matchQuery(e.getKey(), e.getValue()));
			});
		}
		return and;
	}


	default RestClientBuilder restClientBuilder(String host, int port, String protocol) {
		return RestClient.builder(new HttpHost(host, port, protocol));
	}


	@SneakyThrows
	default void retainByTimeRange(String indice, String timeFrom, String timeTo) {
		logger.info("retaining log in [{}] between [{}] - [{}]", indice, timeFrom, timeTo);
		OffsetDateTime from = offsetDateTime(timeFrom).minusMinutes(TIME_BUCKET);
		OffsetDateTime to = offsetDateTime(timeTo).plusMinutes(TIME_BUCKET);
		OffsetDateTime min = offsetDateTime(edgeValue(indice, TIMESTAMP, SortOrder.ASC));
		OffsetDateTime max = offsetDateTime(edgeValue(indice, TIMESTAMP, SortOrder.DESC));
		logger.info("head:[{}]-[{}]", min, from);
		logger.info("tail:[{}]-[{}]", to, max);

		for (OffsetDateTime rangeStart = min, rangeEnd = min.plusMinutes(TIME_BUCKET);
			rangeStart.isBefore(from);
			rangeStart = rangeStart.plusMinutes(TIME_BUCKET), rangeEnd = rangeEnd.plusMinutes(TIME_BUCKET)) {

			RangeQueryBuilder range = rangeQuery(TIMESTAMP).gte(rangeStart).lte(rangeEnd);
			BoolQueryBuilder and = boolQuery().must(range);
			DeleteByQueryRequest request = bulkRequest(new DeleteByQueryRequest(indice))
				.setQuery(and)
				.setBatchSize(10000);

			logger.info("deleting docs, between:[{}] - [{}]", rangeStart, rangeEnd);
			BulkByScrollResponse response = getEsClient().deleteByQuery(request, RequestOptions.DEFAULT);
			logger.info("deleted [{}] docs in [{}], between:[{}] - [{}]", response.getDeleted(), response.getTook(), rangeStart, rangeEnd);
		}

		for (OffsetDateTime rangeStart = to, rangeEnd = to.plusMinutes(TIME_BUCKET);
			rangeStart.isBefore(max);
			rangeStart = rangeStart.plusMinutes(TIME_BUCKET), rangeEnd = rangeEnd.plusMinutes(TIME_BUCKET)) {

			RangeQueryBuilder range = rangeQuery(TIMESTAMP).gte(rangeStart).lte(rangeEnd);
			BoolQueryBuilder and = boolQuery().must(range);
			DeleteByQueryRequest request = bulkRequest(new DeleteByQueryRequest(indice))
				.setQuery(and)
				.setBatchSize(10000);

			logger.info("deleting docs, between:[{}] - [{}]", rangeStart, rangeEnd);
			BulkByScrollResponse response = getEsClient().deleteByQuery(request, RequestOptions.DEFAULT);
			logger.info("deleted [{}] docs in [{}], between:[{}] - [{}]", response.getDeleted(), response.getTook(), rangeStart, rangeEnd);
		}
		OffsetDateTime retainMin = offsetDateTime(edgeValue(indice, TIMESTAMP, SortOrder.ASC));
		OffsetDateTime retainMax = offsetDateTime(edgeValue(indice, TIMESTAMP, SortOrder.DESC));
		logger.info("final retained range:[{}]-[{}]", retainMin, retainMax);
	}

	@SneakyThrows
	default BulkByScrollResponse retainByType(String indice, String... types) {
		logger.info("retaining log in [{}] for {}", indice, Arrays.toString(types));
		DeleteByQueryRequest request = new DeleteByQueryRequest(indice);
		TermsQueryBuilder type = termsQuery(TYPE, types);
		BoolQueryBuilder nor = boolQuery().mustNot(type);
		request.setQuery(nor);
		request.setRefresh(true);
		request.setTimeout(TimeValue.timeValueMinutes(10));
		return getEsClient().deleteByQuery(request, RequestOptions.DEFAULT);
	}

	@SneakyThrows
	default IndexResponse save(String indice, Map<String, Object> source) {
		IndexRequest request = new IndexRequest()
			.index(indice)
			.id(UUIDs.base64UUID())
			.source(source);
		return getEsClient().index(request, RequestOptions.DEFAULT);
	}

	@SneakyThrows
	default SearchResponse scroll(String scrollId) {
		SearchScrollRequest searchRequest = new SearchScrollRequest(scrollId);
		searchRequest.scroll(TimeValue.timeValueMinutes(1L));
		return getEsClient().scroll(searchRequest, RequestOptions.DEFAULT);
	}

	@SneakyThrows
	default SearchResponse scrollable(String indice, String type, String timeFrom, String timeTo, Map<String, String> filters, Integer size) {
		BoolQueryBuilder and = query(type, timeFrom, timeTo, filters);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
			.timeout(new TimeValue(60, TimeUnit.SECONDS))
			.query(and);
		ifNotNull(size, searchSourceBuilder::size);

		SearchRequest searchRequest = new SearchRequest();
		searchRequest.indices(indice);
		searchRequest.source(searchSourceBuilder);
		searchRequest.scroll(TimeValue.timeValueMinutes(1L));
		return getEsClient().search(searchRequest, RequestOptions.DEFAULT);
	}

	@SneakyThrows
	default SearchResponse search(String indice, String type, String timeFrom, String timeTo, Map<String, String> filters, Integer from, Integer size) {
		BoolQueryBuilder and = query(type, timeFrom, timeTo, filters);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
			.timeout(new TimeValue(60, TimeUnit.SECONDS))
			.query(and);
		ifNotNull(from, searchSourceBuilder::from);
		ifNotNull(size, searchSourceBuilder::size);


		SearchRequest searchRequest = new SearchRequest();
		searchRequest.indices(indice);
		searchRequest.source(searchSourceBuilder);
		return getEsClient().search(searchRequest, RequestOptions.DEFAULT);
	}

	@SneakyThrows
	default void shiftTimestamp(String indice, String timeFrom, String timeTo, long ms, String... types) {
		/*enhance the script to put addition field to the log if needed*/
		String script = "ctx._source['@timestamp'] = OffsetDateTime.parse("
			+ "ctx._source['@timestamp']"
		+ ")"
		+ ".plus(" + ms + "L, ChronoUnit.MILLIS)"
		+ ".toString();";
		shiftTimestampByScript(indice, timeFrom, timeTo, script, types);
	}

	@SneakyThrows
	default void shiftTimestampByMsgTime(String indice, String timeFrom, String timeTo, long ms, String... types) {
		/*enhance the script to put addition field to the log if needed*/
		String script = "ctx._source['@timestamp'] = OffsetDateTime.parse("
			+ "ctx._source.msg_time, "
			+ "DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss.SSS Z')"
		+ ")"
		+ ".plus(" + ms + "L, ChronoUnit.MILLIS)"
		+ ".toString();";
		shiftTimestampByScript(indice, timeFrom, timeTo, script, types);
	}

	@SneakyThrows
	default String edgeValue(String indice, String field, SortOrder order) {
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
			.size(1)
			.sort(field, order)
			.fetchSource(field, null)
			.timeout(new TimeValue(60, TimeUnit.SECONDS));

		SearchRequest searchRequest = new SearchRequest();
		searchRequest.indices(indice);
		searchRequest.source(searchSourceBuilder);

		SearchResponse response = getEsClient().search(searchRequest, RequestOptions.DEFAULT);
		SearchHit[] hits = response.getHits().getHits();
		if (hits.length > 0) {
			return hits[0].getSourceAsMap().get(field).toString();
		} else {
			return null;
		}
	}

	private void shiftTimestampByScript(String indice, String timeFrom, String timeTo, String script, String... types) throws IOException, InterruptedException {
		logger.info("executing script update:[{}]", script);

		OffsetDateTime min = offsetDateTime(timeFrom);
		OffsetDateTime max = offsetDateTime(timeTo);
		logger.info("between:[{}]-[{}]", min, max);

		for (OffsetDateTime rangeStart = min, rangeEnd = min.plusMinutes(TIME_BUCKET);
			rangeStart.isBefore(max);
			rangeStart = rangeStart.plusMinutes(TIME_BUCKET), rangeEnd = rangeEnd.plusMinutes(TIME_BUCKET)) {
			TermsQueryBuilder termsType = termsQuery(TYPE, types);
			RangeQueryBuilder range = rangeQuery(TIMESTAMP).gte(rangeStart).lt(rangeEnd);
			BoolQueryBuilder and = boolQuery().must(termsType).must(range);
			UpdateByQueryRequest request = bulkRequest(new UpdateByQueryRequest(indice))
				.setQuery(and)
				.setBatchSize(10000)
				.setScript(new Script(ScriptType.INLINE, "painless", script, Collections.emptyMap()));

			BulkByScrollResponse response = getEsClient().updateByQuery(request, RequestOptions.DEFAULT);
			logger.info("updated [{}][{}] docs for {} in [{}], between:[{}]-[{}]", response.getUpdated(), response.getStatus().getUpdated(), Arrays.toString(types), response.getTook(), rangeStart, rangeEnd);

		}
	}

	private <T extends AbstractBulkByScrollRequest<T>> T bulkRequest(AbstractBulkByScrollRequest<T> request) {
		request.setRefresh(true);
		request.setSlices(10);
		request.setShouldStoreResult(false);
		request.setScroll(TimeValue.timeValueMinutes(5));
		request.setTimeout(TimeValue.timeValueMinutes(5));
		return (T)request;
	}

	default HttpAsyncClientBuilder trustAllSsl(HttpAsyncClientBuilder builder) {
		SSLContext sslContext;
		try {
			sslContext = SSLContexts.custom().loadTrustMaterial(null, (x509Certificates, s) -> true).build();
			builder.setSSLContext(sslContext).setSSLHostnameVerifier((h, s) -> true);
			return builder;
		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			throw new AFCException(e);
		}
	}

	@SneakyThrows
	default AcknowledgedResponse updateIndiceSetttings(String indice, Map<String, Object> settings) {
		UpdateSettingsRequest request = new UpdateSettingsRequest(indice);
		request.settings(settings);
		return getEsClient().indices().putSettings(request, RequestOptions.DEFAULT);
	}

	default HttpAsyncClientBuilder withLogin(HttpAsyncClientBuilder builder, String username, String password) {
		CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
		credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
		builder.setDefaultCredentialsProvider(credentialsProvider);
		return builder;
	}
}
