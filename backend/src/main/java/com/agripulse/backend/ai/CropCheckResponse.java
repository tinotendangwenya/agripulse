package com.agripulse.backend.ai;

import java.util.List;

public record CropCheckResponse(Long id, String status, String crop, List<AiResult.Prediction> predictions,
                                String advice, String model, String limitations) {}
