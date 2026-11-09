package com.agripulse.backend.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** What the Python AI service returns. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiResult(String status, String crop, List<Prediction> predictions,
                       String advice, String model, String limitations) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Prediction(String label, double confidence) {}
}
