package com.agripulse.backend.market;

import java.util.Optional;
import java.util.Set;

final class FarmProvinces {
    static final Set<String> ALL = Set.of(
            "Harare", "Bulawayo", "Manicaland", "Mashonaland Central", "Mashonaland East",
            "Mashonaland West", "Masvingo", "Matabeleland North", "Matabeleland South", "Midlands");

    static Optional<String> match(String input) {
        return ALL.stream().filter(p -> p.equalsIgnoreCase(input.trim())).findFirst();
    }

    private FarmProvinces() {}
}
