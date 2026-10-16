package ar.edu.um.prog2.catalogsync.synchronization.infrastructure.rest;

import java.net.http.HttpClient;

import ar.edu.um.prog2.catalogsync.config.CatedraIntegrationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
public class CatedraRestClientConfiguration {

	@Bean
	CatedraRestClient catedraRestClient(
			CatedraIntegrationProperties properties,
			ObjectMapper objectMapper
	) {
		CatedraIntegrationProperties.RestProperties restProperties = properties.rest();
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(restProperties.connectTimeout())
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(restProperties.readTimeout());

		RestClient restClient = RestClient.builder()
				.baseUrl(restProperties.baseUrl())
				.requestFactory(requestFactory)
				.requestInterceptor((request, body, execution) -> {
					request.getHeaders().setBearerAuth(properties.technicalJwt());
					return execution.execute(request, body);
				})
				.build();

		return new CatedraRestClient(restClient, objectMapper);
	}
}
