package com.example.demo.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.HistogramBucket;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.example.demo.dto.Todo;
import java.io.IOException;
import java.util.List;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class something {

  private static final Logger logger = LoggerFactory.getLogger(something.class);

  private ElasticsearchClient connectES() throws IOException {

    // docker pull elasticsearch:8.2.2
    // sudo sysctl -w vm.max_map_count=262144

    // docker run --name es01 --net elastic -p 9200:9200 -p 9300:9300 -it elasticsearch:8.2.2

    // idea: run kibana as well and then start all of it!

    // I need probably 64gb for my server to run all ofd this stuff. minimum 32gb, maybe 64

    // es consumes 10gb RAM minimum and is 1.23gb big!

    // build up connection
    RestClient restClient = RestClient.builder(new HttpHost("localhost", 9200)).build();

    ElasticsearchTransport transport =
        new RestClientTransport(restClient, new JacksonJsonpMapper());

    ElasticsearchClient client = new ElasticsearchClient(transport);

    // indexRequest
    Todo todo1 = new Todo(1L, 5L, "buy food", false);
    IndexRequest<Object> indexRequest =
        new IndexRequest.Builder<>().index("Todos").id("abc").document(todo1).build();

    try {
      client.index(indexRequest);
    } catch (IOException e) {
      e.printStackTrace();
    }

    try {
      client.index(builder -> builder.index("Todos").id(todo1.getId().toString()).document(todo1));
    } catch (IOException e) {
      e.printStackTrace();
    }

    // making a searchRequest
    String searchText = "bike";
    double maxPrice = 200.0;

    // Search by product name
    Query byName = MatchQuery.of(m -> m.field("name").query(searchText))._toQuery();

    // Search by max price
    Query byMaxPrice = RangeQuery.of(r -> r.field("price").gte(JsonData.of(maxPrice)))._toQuery();

    // Combine name and price queries to search the product index
    SearchResponse<Todo> response =
        client.search(
            s -> s.index("products").query(q -> q.bool(b -> b.must(byName).must(byMaxPrice))),
            Todo.class);

    List<Hit<Todo>> hits = response.hits().hits();
    for (Hit<Todo> hit : hits) {
      Todo todo = hit.source();
      logger.info(
          "Found todo " + (todo != null ? todo.getTitle() : null) + ", score " + hit.score());
    }

    // aggregations
    String searchText2 = "bike";

    Query query = MatchQuery.of(m -> m.field("name").query(searchText2))._toQuery();

    SearchResponse<Void> response2 =
        client.search(
            b ->
                b.index("products")
                    .size(0)
                    .query(query)
                    .aggregations(
                        "price-histogram", a -> a.histogram(h -> h.field("price").interval(50.0))),
            Void.class);

    List<HistogramBucket> buckets =
        response.aggregations().get("price-histogram").histogram().buckets().array();

    for (HistogramBucket bucket : buckets) {
      logger.info("There are " + bucket.docCount() + " bikes under " + bucket.key());
    }

    return client;
  }
}
