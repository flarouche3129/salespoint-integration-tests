package com.felix.projects.salespoint.testSalespoint.config;

import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.http.client.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EndpointConfig {

  @Bean
  public HttpClient salespointClient() {
    return CitrusEndpoints.http().client().requestUrl("http://localhost:8080").build();
  }
}
