package com.tikzy.event.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CategoryNameNormalizerTest {

    @Test
    void normalize_ignoresCaseAndRepeatedWhitespace() {
        assertEquals(
                "hội chợ",
                CategoryNameNormalizer.normalize("  HỘI   CHỢ  "));
    }

    @Test
    void normalize_usesCanonicalUnicodeForm() {
        assertEquals(
                CategoryNameNormalizer.normalize("Hội chợ"),
                CategoryNameNormalizer.normalize("Hội chợ"));
    }
}
