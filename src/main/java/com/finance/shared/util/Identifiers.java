package com.finance.shared.util;

import java.util.UUID;

public final class Identifiers {

    private Identifiers() {
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}
