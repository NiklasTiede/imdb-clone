package com.thecodinglab.imdbclone.assistant.internal.mcp;

import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import java.util.List;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "spring.ai.mcp.server",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@ConditionalOnProperty(
    prefix = "spring.ai.mcp.server",
    name = "protocol",
    havingValue = "STATELESS")
@ConditionalOnProperty(
    prefix = "spring.ai.mcp.server",
    name = "type",
    havingValue = "SYNC",
    matchIfMissing = true)
@ConditionalOnProperty(
    prefix = "spring.ai.mcp.server.annotation-scanner",
    name = "enabled",
    havingValue = "false")
class McpToolRegistrationConfig {

  @Bean
  List<McpStatelessServerFeatures.SyncToolSpecification> movieConciergeToolSpecifications(
      MovieSearchMcpTool movieSearchMcpTool,
      MovieDetailsMcpTool movieDetailsMcpTool,
      MovieRecommendationMcpTools movieRecommendationMcpTools) {
    return SyncMcpAnnotationProviders.statelessToolSpecifications(
        List.of(movieSearchMcpTool, movieDetailsMcpTool, movieRecommendationMcpTools));
  }
}
