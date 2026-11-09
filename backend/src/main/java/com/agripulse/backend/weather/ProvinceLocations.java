package com.agripulse.backend.weather;

import java.util.Map;

/** Approximate province centres, used only when a farm has no coordinates. */
final class ProvinceLocations {
    static final Map<String, double[]> CENTRES = Map.of(
            "Harare", new double[]{-17.83, 31.05},
            "Bulawayo", new double[]{-20.15, 28.58},
            "Manicaland", new double[]{-18.97, 32.67},
            "Mashonaland Central", new double[]{-17.30, 31.33},
            "Mashonaland East", new double[]{-18.19, 31.55},
            "Mashonaland West", new double[]{-17.36, 30.20},
            "Masvingo", new double[]{-20.07, 30.83},
            "Matabeleland North", new double[]{-18.93, 27.81},
            "Matabeleland South", new double[]{-20.94, 29.00},
            "Midlands", new double[]{-19.45, 29.82});

    private ProvinceLocations() {}
}
