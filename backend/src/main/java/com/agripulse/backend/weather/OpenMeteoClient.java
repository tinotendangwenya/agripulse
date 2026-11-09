package com.agripulse.backend.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Fetches a 7-day forecast from Open-Meteo (free, no key). Cached 30 minutes per location. */
@Component
public class OpenMeteoClient {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Forecast(Daily daily) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Daily(List<String> time,
                     List<Double> temperature_2m_max,
                     List<Double> temperature_2m_min,
                     List<Double> precipitation_sum,
                     List<Double> precipitation_probability_max) {}
    }

    private record Cached(long at, Forecast data) {}

    private static final long TTL_MS = 30 * 60 * 1000L;
    private static final String URL = "https://api.open-meteo.com/v1/forecast"
            + "?latitude={lat}&longitude={lon}"
            + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max"
            + "&timezone=Africa/Harare&forecast_days=7";

    private final RestClient http = RestClient.create();
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    Forecast fetch(double lat, double lon) {
        String key = String.format("%.2f,%.2f", lat, lon);
        Cached c = cache.get(key);
        if (c != null && System.currentTimeMillis() - c.at() < TTL_MS) return c.data();
        try {
            Forecast f = http.get().uri(URL, lat, lon).retrieve().body(Forecast.class);
            if (f == null || f.daily() == null || f.daily().time() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Weather service returned no data");
            }
            cache.put(key, new Cached(System.currentTimeMillis(), f));
            return f;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Weather service is unavailable, try again later");
        }
    }
}
