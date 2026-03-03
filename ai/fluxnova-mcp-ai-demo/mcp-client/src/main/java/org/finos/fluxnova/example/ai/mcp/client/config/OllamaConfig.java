package org.finos.fluxnova.example.ai.mcp.client.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.ai.ollama.api.OllamaApi;

@Configuration
public class OllamaConfig {

	@Bean
	public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl,
			ObjectProvider<RestClient.Builder> restClientBuilderProvider,
			ObjectProvider<WebClient.Builder> webClientBuilderProvider,
			ObjectProvider<ResponseErrorHandler> responseErrorHandlerProvider) {
		if (!StringUtils.hasText(baseUrl)) {
			throw new IllegalStateException("Property 'spring.ai.ollama.base-url' must be set to configure Ollama");
		}

		RestClient.Builder restClientBuilder = restClientBuilderProvider.getIfAvailable(RestClient::builder);
		WebClient.Builder webClientBuilder = webClientBuilderProvider.getIfAvailable(WebClient::builder);
		ResponseErrorHandler errorHandler = responseErrorHandlerProvider
			.getIfAvailable(DefaultResponseErrorHandler::new);

		return OllamaApi.builder()
			.baseUrl(baseUrl)
			.restClientBuilder(restClientBuilder)
			.webClientBuilder(webClientBuilder)
			.responseErrorHandler(errorHandler)
			.build();
	}

}

