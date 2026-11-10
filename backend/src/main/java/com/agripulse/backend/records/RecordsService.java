package com.agripulse.backend.records;

import com.agripulse.backend.farm.CropRecord;
import com.agripulse.backend.farm.CropRecordRepository;
import com.agripulse.backend.farm.FarmRepository;
import com.agripulse.backend.records.RecordsDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Farm records for the caller's own farms (anyone else's farm returns 404).
 * Totals are plain arithmetic over what the farmer recorded. Currencies are never converted.
 */
@Service
@RequiredArgsConstructor
public class RecordsService {
    static final Set<String> INCOME_CATS = Set.of("SALES", "OTHER_INCOME");
    static final Set<String> EXPENSE_CATS = Set.of("SEEDS", "FERTILISER", "CHEMICALS", "LABOUR", "FEED",
            "VETERINARY", "MACHINERY", "FUEL", "TRANSPORT", "OTHER");
    static final Set<String> CURRENCIES = Set.of("USD", "ZIG", "ZAR");
    static final Set<String> ANIMALS = Set.of("CATTLE", "GOAT", "SHEEP", "PIG", "POULTRY", "FISH", "OTHER");
    static final Set<String> GAIN = Set.of("OPENING", "PURCHASE", "BIRTH");
    static final Set<String> LOSS = Set.of("DEATH", "SALE");
    static final Set<String> EVENTS = Set.of("OPENING", "PURCHASE", "BIRTH", "DEATH", "SALE",
            "VACCINATION", "TREATMENT", "FEEDING", "OTHER");
    private static final ZoneId HARARE = ZoneId.of("Africa/Harare");

    private final FarmRepository farms;
    private final CropRecordRepository crops;
    private final TransactionRepository txs;
    private final HarvestRepository harvests;
    private final LivestockRepository livestock;

    // ---------- money ----------
    @Transactional(readOnly = true)
    public List<TransactionView> transactions(Long ownerId, Long farmId, LocalDate from, LocalDate to) {
        owned(ownerId, farmId);
        LocalDate[] r = range(from, to);
        return txs.findTop500ByFarmIdAndTxDateBetweenOrderByTxDateDescIdDesc(farmId, r[0], r[1]).stream()
                .map(t -> new TransactionView(t.getId(), t.getEntryType(), t.getCategory(), t.getAmount(),
                        t.getCurrency(), t.getTxDate(), t.getDescription(), t.getCropRecordId())).toList();
    }

    @Transactional
    public TransactionView addTransaction(Long ownerId, Long farmId, TransactionRequest r) {
        owned(ownerId, farmId);
        String type = up(r.type()), cat = up(r.category()), cur = up(r.currency());
        if (!type.equals("INCOME") && !type.equals("EXPENSE")) throw bad("Type must be INCOME or EXPENSE");
        Set<String> allowed = type.equals("INCOME") ? INCOME_CATS : EXPENSE_CATS;
        if (!allowed.contains(cat)) throw bad("For " + type + ", category must be one of " + allowed);
        if (!CURRENCIES.contains(cur)) throw bad("Currency must be one of " + CURRENCIES);
        notFuture(r.txDate());
        if (r.cropRecordId() != null && crops.findByIdAndFarmId(r.cropRecordId(), farmId).isEmpty())
            throw bad("That crop is not on this farm");

        FarmTransaction t = new FarmTransaction();
        t.setFarmId(farmId);
        t.setCropRecordId(r.cropRecordId());
        t.setEntryType(type);
        t.setCategory(cat);
        t.setAmount(r.amount().setScale(2, RoundingMode.HALF_UP));
        t.setCurrency(cur);
        t.setTxDate(r.txDate());
        t.setDescription(r.description());
        txs.save(t);
        return new TransactionView(t.getId(), type, cat, t.getAmount(), cur, t.getTxDate(), t.getDescription(), t.getCropRecordId());
    }

    @Transactional
    public void deleteTransaction(Long ownerId, Long farmId, Long id) {
        owned(ownerId, farmId);
        txs.delete(txs.findByIdAndFarmId(id, farmId).orElseThrow(this::notFound));
    }

    // ---------- harvests ----------
    @Transactional(readOnly = true)
    public List<HarvestView> harvests(Long ownerId, Long farmId, LocalDate from, LocalDate to) {
        owned(ownerId, farmId);
        LocalDate[] r = range(from, to);
        Map<Long, String> names = cropNames(farmId);
        return harvests.findTop500ByFarmIdAndHarvestDateBetweenOrderByHarvestDateDescIdDesc(farmId, r[0], r[1]).stream()
                .map(h -> new HarvestView(h.getId(), h.getCropRecordId(), names.getOrDefault(h.getCropRecordId(), "Crop"),
                        h.getHarvestDate(), h.getQuantityKg(), h.getNote())).toList();
    }

    @Transactional
    public HarvestView addHarvest(Long ownerId, Long farmId, HarvestRequest r) {
        owned(ownerId, farmId);
        CropRecord crop = crops.findByIdAndFarmId(r.cropRecordId(), farmId).orElseThrow(() -> bad("That crop is not on this farm"));
        String unit = up(r.unit());
        if (!unit.equals("KG") && !unit.equals("TONNE")) throw bad("Unit must be KG or TONNE");
        notFuture(r.harvestDate());
        BigDecimal kg = r.quantity().multiply(unit.equals("TONNE") ? BigDecimal.valueOf(1000) : BigDecimal.ONE)
                .setScale(2, RoundingMode.HALF_UP);
        Harvest h = new Harvest();
        h.setFarmId(farmId);
        h.setCropRecordId(crop.getId());
        h.setHarvestDate(r.harvestDate());
        h.setQuantityKg(kg);
        h.setNote(r.note());
        harvests.save(h);
        return new HarvestView(h.getId(), crop.getId(), crop.getCropName(), h.getHarvestDate(), kg, h.getNote());
    }

    @Transactional
    public void deleteHarvest(Long ownerId, Long farmId, Long id) {
        owned(ownerId, farmId);
        harvests.delete(harvests.findByIdAndFarmId(id, farmId).orElseThrow(this::notFound));
    }

    // ---------- livestock ----------
    @Transactional(readOnly = true)
    public List<LivestockView> livestock(Long ownerId, Long farmId) {
        owned(ownerId, farmId);
        return livestock.findTop300ByFarmIdOrderByEventDateDescIdDesc(farmId).stream()
                .map(e -> new LivestockView(e.getId(), e.getAnimalType(), e.getEventType(), e.getEventDate(),
                        e.getHeadCount(), e.getNote())).toList();
    }

    @Transactional
    public LivestockView addLivestock(Long ownerId, Long farmId, LivestockRequest r) {
        owned(ownerId, farmId);
        String animal = up(r.animalType()), event = up(r.eventType());
        if (!ANIMALS.contains(animal)) throw bad("Animal must be one of " + ANIMALS);
        if (!EVENTS.contains(event)) throw bad("Event must be one of " + EVENTS);
        notFuture(r.eventDate());
        if (LOSS.contains(event)) {
            long have = balance(farmId, animal, r.eventDate());
            if (have < r.headCount())
                throw bad("Only " + Math.max(have, 0) + " " + animal.toLowerCase() + " recorded on that date. Add the animals you already have first (event type OPENING).");
        }
        LivestockEvent e = new LivestockEvent();
        e.setFarmId(farmId);
        e.setAnimalType(animal);
        e.setEventType(event);
        e.setEventDate(r.eventDate());
        e.setHeadCount(r.headCount());
        e.setNote(r.note());
        livestock.save(e);
        return new LivestockView(e.getId(), animal, event, e.getEventDate(), e.getHeadCount(), e.getNote());
    }

    @Transactional
    public void deleteLivestock(Long ownerId, Long farmId, Long id) {
        owned(ownerId, farmId);
        livestock.delete(livestock.findByIdAndFarmId(id, farmId).orElseThrow(this::notFound));
    }

    // ---------- report ----------
    @Transactional(readOnly = true)
    public Summary summary(Long ownerId, Long farmId, LocalDate from, LocalDate to) {
        owned(ownerId, farmId);
        LocalDate[] r = range(from, to);
        LocalDate f = r[0], t = r[1];
        long days = ChronoUnit.DAYS.between(f, t) + 1;
        LocalDate prevTo = f.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1);

        Map<Long, CropRecord> cropMap = new HashMap<>();
        for (CropRecord c : crops.findByFarmIdOrderByIdDesc(farmId)) cropMap.put(c.getId(), c);

        Map<Long, BigDecimal> kgByCrop = new LinkedHashMap<>();
        for (Object[] row : harvests.totals(farmId, f, t)) kgByCrop.put((Long) row[0], (BigDecimal) row[1]);

        Map<Long, Map<String, BigDecimal[]>> linked = new LinkedHashMap<>();
        for (Object[] row : txs.cropTotals(farmId, f, t)) {
            Long cid = (Long) row[0];
            String type = (String) row[1], cur = (String) row[2];
            BigDecimal sum = (BigDecimal) row[3];
            BigDecimal[] pair = linked.computeIfAbsent(cid, k -> new TreeMap<>())
                    .computeIfAbsent(cur, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (type.equals("INCOME")) pair[0] = pair[0].add(sum); else pair[1] = pair[1].add(sum);
        }

        Set<Long> ids = new LinkedHashSet<>(kgByCrop.keySet());
        ids.addAll(linked.keySet());
        List<CropSummary> cropList = new ArrayList<>();
        for (Long id : ids) {
            CropRecord c = cropMap.get(id);
            if (c == null) continue;
            BigDecimal kg = kgByCrop.getOrDefault(id, BigDecimal.ZERO);
            BigDecimal area = c.getAreaHectares();
            BigDecimal yield = (area != null && area.signum() > 0)
                    ? kg.divide(area.multiply(BigDecimal.valueOf(1000)), 2, RoundingMode.HALF_UP) : null;
            List<CurrencyAmount> money = new ArrayList<>();
            linked.getOrDefault(id, Map.of()).forEach((cur, p) -> money.add(new CurrencyAmount(cur, p[0], p[1])));
            cropList.add(new CropSummary(id, c.getCropName(), area, kg, yield, money));
        }

        Map<String, Long> herd = new TreeMap<>();
        for (Object[] row : livestock.balances(farmId, today())) {
            String animal = (String) row[0], ev = (String) row[1];
            long n = ((Number) row[2]).longValue();
            if (GAIN.contains(ev)) herd.merge(animal, n, Long::sum);
            else if (LOSS.contains(ev)) herd.merge(animal, -n, Long::sum);
        }
        List<HerdCount> herdList = herd.entrySet().stream().filter(e -> e.getValue() != 0)
                .map(e -> new HerdCount(e.getKey(), e.getValue())).toList();

        List<String> notes = List.of(
                "Figures only include what has been recorded. Missing records mean missing money or yield.",
                "Money is shown separately for each currency. Currencies are not converted.",
                "Animal counts are as of today and come from the animal events you recorded.");
        return new Summary(new Period(f, t), money(farmId, f, t), cropList, herdList,
                new Period(prevFrom, prevTo), money(farmId, prevFrom, prevTo), notes);
    }

    private List<CurrencyTotals> money(Long farmId, LocalDate from, LocalDate to) {
        Map<String, BigDecimal[]> byCur = new TreeMap<>();
        Map<String, Map<String, BigDecimal>> cats = new TreeMap<>();
        for (Object[] row : txs.totals(farmId, from, to)) {
            String type = (String) row[0], cur = (String) row[1], cat = (String) row[2];
            BigDecimal sum = (BigDecimal) row[3];
            BigDecimal[] pair = byCur.computeIfAbsent(cur, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            if (type.equals("INCOME")) pair[0] = pair[0].add(sum);
            else {
                pair[1] = pair[1].add(sum);
                cats.computeIfAbsent(cur, k -> new TreeMap<>()).merge(cat, sum, BigDecimal::add);
            }
        }
        List<CurrencyTotals> out = new ArrayList<>();
        byCur.forEach((cur, p) -> {
            List<CategoryTotal> list = cats.getOrDefault(cur, Map.of()).entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                    .map(e -> new CategoryTotal(e.getKey(), e.getValue())).toList();
            out.add(new CurrencyTotals(cur, p[0], p[1], p[0].subtract(p[1]), list));
        });
        return out;
    }

    private long balance(Long farmId, String animal, LocalDate asOf) {
        long n = 0;
        for (Object[] row : livestock.balances(farmId, asOf)) {
            if (!animal.equals(row[0])) continue;
            String ev = (String) row[1];
            long c = ((Number) row[2]).longValue();
            if (GAIN.contains(ev)) n += c; else if (LOSS.contains(ev)) n -= c;
        }
        return n;
    }

    private Map<Long, String> cropNames(Long farmId) {
        Map<Long, String> m = new HashMap<>();
        for (CropRecord c : crops.findByFarmIdOrderByIdDesc(farmId)) m.put(c.getId(), c.getCropName());
        return m;
    }

    private void owned(Long ownerId, Long farmId) {
        farms.findByIdAndOwnerId(farmId, ownerId).orElseThrow(this::notFound);
    }

    private LocalDate[] range(LocalDate from, LocalDate to) {
        LocalDate t = to == null ? today() : to;
        LocalDate f = from == null ? t.minusDays(364) : from;
        if (f.isAfter(t)) throw bad("The start date is after the end date");
        if (ChronoUnit.DAYS.between(f, t) > 3660) throw bad("Choose a period of 10 years or less");
        return new LocalDate[]{f, t};
    }

    private void notFuture(LocalDate d) {
        if (d.isAfter(today())) throw bad("The date cannot be in the future");
    }

    private static LocalDate today() { return LocalDate.now(HARARE); }
    private static String up(String s) { return s == null ? "" : s.trim().toUpperCase(); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
}
