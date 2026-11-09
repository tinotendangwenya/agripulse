package com.agripulse.backend.weather;

import com.agripulse.backend.farm.CropRecord;
import com.agripulse.backend.farm.CropRecordRepository;
import com.agripulse.backend.farm.Farm;
import com.agripulse.backend.farm.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Weather for one of the caller's own farms (others get 404). Alerts are simple rules, not AI. */
@Service
@RequiredArgsConstructor
public class WeatherService {
    private static final double HEAVY_RAIN_MM = 50;
    private static final double HEAT_C = 35;
    private static final double FROST_C = 2;
    private static final double DRY_WEEK_MM = 5;
    private static final Set<String> ACTIVE = Set.of("PLANTED", "GROWING");

    private final FarmRepository farms;
    private final CropRecordRepository crops;
    private final OpenMeteoClient client;

    @Transactional(readOnly = true)
    public WeatherResponse forFarm(Long ownerId, Long farmId) {
        Farm farm = farms.findByIdAndOwnerId(farmId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

        double lat, lon;
        String locationSource;
        if (farm.getLatitude() != null && farm.getLongitude() != null) {
            lat = farm.getLatitude().doubleValue();
            lon = farm.getLongitude().doubleValue();
            locationSource = "Farm coordinates";
        } else {
            double[] c = ProvinceLocations.CENTRES.get(farm.getProvince());
            if (c == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Farm has no usable location");
            lat = c[0];
            lon = c[1];
            locationSource = "Approximate centre of " + farm.getProvince() + " (set farm coordinates for accuracy)";
        }

        var d = client.fetch(lat, lon).daily();
        List<WeatherResponse.Day> days = new ArrayList<>();
        for (int i = 0; i < d.time().size(); i++) {
            days.add(new WeatherResponse.Day(d.time().get(i), at(d.temperature_2m_max(), i),
                    at(d.temperature_2m_min(), i), at(d.precipitation_sum(), i),
                    at(d.precipitation_probability_max(), i)));
        }

        boolean activeCrops = crops.findByFarmIdOrderByIdDesc(farmId).stream()
                .map(CropRecord::getStatus).anyMatch(ACTIVE::contains);

        return new WeatherResponse(farm.getId(), farm.getName(), lat, lon, locationSource,
                "Open-Meteo forecast (model data, accuracy varies)",
                "Rule-based alerts from the forecast, not AI predictions",
                days, alerts(days, activeCrops));
    }

    private List<WeatherResponse.Alert> alerts(List<WeatherResponse.Day> days, boolean activeCrops) {
        List<WeatherResponse.Alert> out = new ArrayList<>();
        double totalRain = 0;
        for (var day : days) {
            if (day.rainMm() != null) totalRain += day.rainMm();
            if (day.rainMm() != null && day.rainMm() >= HEAVY_RAIN_MM) {
                out.add(new WeatherResponse.Alert("HEAVY_RAIN", "HIGH", day.date(),
                        "Heavy rain forecast (" + day.rainMm() + " mm). Check drainage and protect stored produce."));
            }
            if (day.tempMaxC() != null && day.tempMaxC() >= HEAT_C) {
                out.add(new WeatherResponse.Alert("HEAT", "MEDIUM", day.date(),
                        "High temperature forecast (" + day.tempMaxC() + " C). Watch livestock water and crop heat stress."));
            }
            if (day.tempMinC() != null && day.tempMinC() <= FROST_C) {
                out.add(new WeatherResponse.Alert("FROST", "HIGH", day.date(),
                        "Low temperature forecast (" + day.tempMinC() + " C). Frost can damage young crops."));
            }
        }
        if (activeCrops && totalRain < DRY_WEEK_MM) {
            out.add(new WeatherResponse.Alert("DRY_SPELL", "MEDIUM", days.isEmpty() ? null : days.get(0).date(),
                    "Very little rain forecast this week (" + Math.round(totalRain * 10) / 10.0
                            + " mm total) and you have planted crops. Consider irrigation."));
        }
        return out;
    }

    private static Double at(List<Double> list, int i) {
        return (list == null || i >= list.size()) ? null : list.get(i);
    }
}
