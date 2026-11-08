package com.agripulse.backend.farm;

/**
 * Service with ownership checks. Every lookup goes through findByIdAndOwnerId,
 * so another farmer's farm returns 404 instead of leaking that it exists.
 * Also validates the province against Zimbabwe's 10 provinces.
 */

import com.agripulse.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FarmService {
    static final Set<String> PROVINCES = Set.of(
            "Harare", "Bulawayo", "Manicaland", "Mashonaland Central", "Mashonaland East",
            "Mashonaland West", "Masvingo", "Matabeleland North", "Matabeleland South", "Midlands");
    static final Set<String> CROP_STATUSES = Set.of("PLANNED", "PLANTED", "GROWING", "HARVESTED", "FAILED");

    private final FarmRepository farms;
    private final CropRecordRepository crops;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<FarmResponse> list(Long ownerId) {
        return farms.findByOwnerIdOrderByIdDesc(ownerId).stream().map(FarmResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public FarmResponse get(Long ownerId, Long id) {
        return FarmResponse.of(owned(ownerId, id));
    }

    @Transactional
    public FarmResponse create(Long ownerId, FarmRequest r) {
        Farm f = new Farm();
        f.setOwner(users.getReferenceById(ownerId));
        apply(f, r);
        return FarmResponse.of(farms.save(f));
    }

    @Transactional
    public FarmResponse update(Long ownerId, Long id, FarmRequest r) {
        Farm f = owned(ownerId, id);
        apply(f, r);
        return FarmResponse.of(f);
    }

    @Transactional
    public void delete(Long ownerId, Long id) {
        Farm f = owned(ownerId, id);
        crops.deleteAll(crops.findByFarmIdOrderByIdDesc(f.getId()));
        farms.delete(f);
    }

    @Transactional(readOnly = true)
    public List<CropResponse> listCrops(Long ownerId, Long farmId) {
        owned(ownerId, farmId);
        return crops.findByFarmIdOrderByIdDesc(farmId).stream().map(CropResponse::of).toList();
    }

    @Transactional
    public CropResponse addCrop(Long ownerId, Long farmId, CropRequest r) {
        CropRecord c = new CropRecord();
        c.setFarm(owned(ownerId, farmId));
        applyCrop(c, r);
        return CropResponse.of(crops.save(c));
    }

    @Transactional
    public CropResponse updateCrop(Long ownerId, Long farmId, Long cropId, CropRequest r) {
        owned(ownerId, farmId);
        CropRecord c = crops.findByIdAndFarmId(cropId, farmId).orElseThrow(this::notFound);
        applyCrop(c, r);
        return CropResponse.of(c);
    }

    @Transactional
    public void deleteCrop(Long ownerId, Long farmId, Long cropId) {
        owned(ownerId, farmId);
        crops.delete(crops.findByIdAndFarmId(cropId, farmId).orElseThrow(this::notFound));
    }

    private Farm owned(Long ownerId, Long id) {
        return farms.findByIdAndOwnerId(id, ownerId).orElseThrow(this::notFound);
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");
    }

    private void apply(Farm f, FarmRequest r) {
        String province = PROVINCES.stream()
                .filter(p -> p.equalsIgnoreCase(r.province().trim())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Province must be one of " + PROVINCES));
        f.setName(r.name().trim());
        f.setProvince(province);
        f.setDistrict(r.district());
        f.setLatitude(r.latitude());
        f.setLongitude(r.longitude());
        f.setSizeHectares(r.sizeHectares());
    }

    private void applyCrop(CropRecord c, CropRequest r) {
        String status = (r.status() == null || r.status().isBlank()) ? "PLANNED" : r.status().trim().toUpperCase();
        if (!CROP_STATUSES.contains(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status must be one of " + CROP_STATUSES);
        }
        if (r.plantingDate() != null && r.expectedHarvestDate() != null
                && r.expectedHarvestDate().isBefore(r.plantingDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Harvest date cannot be before planting date");
        }
        c.setCropName(r.cropName().trim());
        c.setVariety(r.variety());
        c.setAreaHectares(r.areaHectares());
        c.setPlantingDate(r.plantingDate());
        c.setExpectedHarvestDate(r.expectedHarvestDate());
        c.setStatus(status);
        c.setNotes(r.notes());
    }
}
