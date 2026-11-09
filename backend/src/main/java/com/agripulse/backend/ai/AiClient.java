package com.agripulse.backend.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;
import java.time.Duration;

/** Talks to the Python AI service. Only the backend calls it, never the browser. */
@Component
public class AiClient {
    private final RestClient http;
    private final String baseUrl;

    public AiClient(@Value("${app.ai.url:http://127.0.0.1:8001}") String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.http = RestClient.builder().requestFactory(factory).build();
        this.baseUrl = baseUrl;
    }

    public AiResult classify(byte[] image, String contentType, String crop) {
        var resource = new ByteArrayResource(image) {
            @Override public String getFilename() { return "leaf.jpg"; }
        };
        var fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(MediaType.parseMediaType(contentType));

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new HttpEntity<>(resource, fileHeaders));
        parts.add("crop", crop);
        try {
            AiResult r = http.post().uri(baseUrl + "/classify")
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(parts)
                    .retrieve().body(AiResult.class);
            if (r == null || r.status() == null)
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI service returned no result");
            return r;
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The photo was rejected. Use a JPEG, PNG or WebP image under 5 MB.");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The leaf checker is not available right now. Try again later.");
        }
    }
}
