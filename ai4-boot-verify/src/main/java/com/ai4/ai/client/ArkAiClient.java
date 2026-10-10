package com.ai4.ai.client;

import com.ai4.ai.client.dto.ArkResponse;
import com.ai4.ai.client.dto.ArkResponseRequest;
import com.ai4.ai.config.ArkProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ArkAiClient implements AiClient {
    private final RestClient restClient;
    private final ArkProperties properties;

    public ArkAiClient(ArkProperties properties) {
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeaders(headers -> headers.setBearerAuth(properties.apiKey()))
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String generateText(String prompt) {
        ArkResponseRequest request = new ArkResponseRequest(
                properties.model(),
                prompt,
                new ArkResponseRequest.Thinking("disabled")
        );
        ArkResponse response = restClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ArkResponse.class);

        if(response ==null || response.output() == null){
            throw new IllegalStateException("火山返回空响应");
        }

        return response.output().stream()
                .filter(item -> "message".equals(item.type()))
                .filter(item -> item.content() != null)
                .flatMap(item -> item.content().stream())
                .filter(content -> "output_text".equals(content.type()))
                .map(ArkResponse.ContentItem::text)
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("火山方舟响应中没有文本内容"));
    }
}
