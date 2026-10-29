package com.springdocai.llm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class LlmClientConfig {

    @Bean
    public RestClient llmRestClient(RestClient.Builder builder, LlmProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
        return builder.baseUrl(properties.getBaseUrl()).requestFactory(factory).build();
    }

    /** Executor da geração assíncrona (RNF03). */
    @Bean(name = "generationExecutor")
    public TaskExecutor generationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("generation-");
        executor.initialize();
        return executor;
    }
}

