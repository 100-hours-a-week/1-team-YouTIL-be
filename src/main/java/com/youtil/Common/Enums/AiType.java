package com.youtil.Common.Enums;

import java.util.Optional;

public enum AiType {
    TIL, INTERVIEW;

    public static Optional<AiType> from(String name) {
        try {
            return Optional.of(AiType.valueOf(name));
        } catch (IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
