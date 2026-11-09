package com.agripulse.backend.weather;

import java.util.List;

/** Raw forecast (Open-Meteo) plus rule-based alerts (our own rules, not AI). */
public record WeatherResponse(
        Long farmId, String farmName, double latitude, double longitude,
        String locationSource, String forecastSource, String alertsSource,
        List<Day> days, List<Alert> alerts) {

    public record Day(String date, Double tempMaxC, Double tempMinC,
                      Double rainMm, Double rainProbabilityPercent) {}

    public record Alert(String type, String severity, String date, String message) {}
}
