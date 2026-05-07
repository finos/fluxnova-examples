package org.finos.fluxnova.example.ai.mcp.fluxnova;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component("chatService")
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${chat.service.url:http://mcp-client:8080/chat}")
    private String chatUrl;

    public String chat(String prompt) throws Exception {
        log.info("prompt pre encode: {}",prompt);
        String url = chatUrl + "?prompt=" + URLEncoder.encode(prompt, StandardCharsets.UTF_8);
        log.info("url post encode: {}", url);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }
}
