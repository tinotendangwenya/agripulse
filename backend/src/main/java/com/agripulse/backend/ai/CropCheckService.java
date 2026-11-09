package com.agripulse.backend.ai;

import com.agripulse.backend.farm.FarmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;

/** Validates the request, calls the AI service, and records the result. No DB transaction is held during the AI call. */
@Service
@RequiredArgsConstructor
public class CropCheckService {
    private final AiClient ai;
    private final AiPredictionRepository predictions;
    private final FarmRepository farms;

    public CropCheckResponse check(Long userId, Long farmId, String crop, MultipartFile file) {
        String c = crop == null ? "" : crop.trim().toLowerCase();
        if (c.isEmpty() || c.length() > 40) throw bad("Choose a crop");
        if (file == null || file.isEmpty()) throw bad("Add a photo");
        String type = file.getContentType() == null ? "image/jpeg" : file.getContentType();
        if (!type.startsWith("image/")) throw bad("The file must be an image");
        if (farmId != null && farms.findByIdAndOwnerId(farmId, userId).isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");

        byte[] bytes;
        try { bytes = file.getBytes(); } catch (IOException e) { throw bad("Could not read the photo"); }

        AiResult r = ai.classify(bytes, type, c);

        AiPrediction row = new AiPrediction();
        row.setUserId(userId);
        row.setFarmId(farmId);
        row.setCrop(c);
        row.setStatus(r.status());
        row.setModel(r.model() == null ? "unknown" : r.model());
        if (r.predictions() != null && !r.predictions().isEmpty()) {
            row.setTopLabel(r.predictions().get(0).label());
            row.setTopConfidence(BigDecimal.valueOf(r.predictions().get(0).confidence()));
        }
        predictions.save(row);

        return new CropCheckResponse(row.getId(), r.status(), r.crop(),
                r.predictions() == null ? java.util.List.of() : r.predictions(),
                r.advice(), r.model(), r.limitations());
    }

    private static ResponseStatusException bad(String m) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
    }
}
