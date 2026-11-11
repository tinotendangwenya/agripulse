package com.agripulse.backend.transport;

import com.agripulse.backend.weather.ProvinceLocations;

/** Rough distances between province centres. Real roads are longer, so every figure built on this is an estimate. */
final class Geo {
    static final double ROAD_FACTOR = 1.3;

    private Geo() {}

    static String province(String input) {
        if (input == null) return null;
        return ProvinceLocations.CENTRES.keySet().stream()
                .filter(p -> p.equalsIgnoreCase(input.trim())).findFirst().orElse(null);
    }

    static Double km(String a, String b) {
        double[] x = ProvinceLocations.CENTRES.get(a), y = ProvinceLocations.CENTRES.get(b);
        if (x == null || y == null) return null;
        double dLat = Math.toRadians(y[0] - x[0]), dLon = Math.toRadians(y[1] - x[1]);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(x[0]))
                * Math.cos(Math.toRadians(y[0])) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return Math.round(6371.0 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h)) * 10) / 10.0;
    }
}
