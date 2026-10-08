package com.diamante.delivery.orderservice.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableResilientMethods // turns on @Retryable processing
public class RestClientConfig {

    /**
     * @LoadBalanced makes the RestTemplate resolve service names (http://PAYMENT-SERVICE/...)
     * through Eureka and distribute the calls between the registered instances.
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2_000);
        factory.setReadTimeout(3_000);
        return new RestTemplate(factory);
    }
}
