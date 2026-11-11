package com.agripulse.backend.prices;

import com.agripulse.backend.prices.PriceDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Any logged-in user can read the price board and history. */
@RestController
@RequestMapping("/api/prices")
@RequiredArgsConstructor
public class BoardController {
    private final BoardService service;

    @GetMapping("/board")
    public List<BoardRow> board(@RequestParam(required = false) String commodity,
                                @RequestParam(required = false) String province) {
        return service.board(commodity, province);
    }

    @GetMapping("/series")
    public List<SeriesPoint> series(@RequestParam String commodity, @RequestParam String market,
                                    @RequestParam String currency, @RequestParam String unit,
                                    @RequestParam(defaultValue = "90") int days) {
        return service.series(commodity, market, currency, unit, days);
    }
}
