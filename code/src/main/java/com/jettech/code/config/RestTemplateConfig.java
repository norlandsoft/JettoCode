package com.jettech.code.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    @Value("${opencode.timeout:300000}")
    private int openCodeTimeout;

    /**
     * 默认 RestTemplate，用于一般请求
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * 用于 OpenCode 服务的 RestTemplate，配置了较长的超时时间
     * AI 分析可能需要较长时间
     */
    @Bean(name = "openCodeRestTemplate")
    public RestTemplate openCodeRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofMillis(openCodeTimeout));
        return new RestTemplate(factory);
    }
}
