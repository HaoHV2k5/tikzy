package com.tikzy.event.util;

import java.text.Normalizer;
import java.util.Locale;

public final class CategoryNameNormalizer {

    private CategoryNameNormalizer() {
    }

    public static String clean(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFC)
                .strip()
                .replaceAll("\\s+", " ");
    }

    public static String normalize(String name) {
        return clean(name).toLowerCase(Locale.ROOT);
    }
}
