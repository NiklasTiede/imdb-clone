package com.thecodinglab.imdbclone.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsClientConfig {

  @Value("${elasticsearch.url}")
  public String elasticsearchUrl;

  public ElasticsearchClient getClient() {
    RestClient restClient = RestClient.builder(new HttpHost(elasticsearchUrl, 9200)).build();
    ElasticsearchTransport transport =
        new RestClientTransport(restClient, new JacksonJsonpMapper());
    return new ElasticsearchClient(transport);
  }
}
