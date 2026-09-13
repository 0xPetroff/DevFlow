package com.devflow.user.entity;

final class AvatarColors {

    private static final String[] PALETTE = {
            "#6366f1", "#8b5cf6", "#ec4899", "#f43f5e", "#f59e0b",
            "#10b981", "#14b8a6", "#0ea5e9", "#3b82f6", "#84cc16"
    };

    private AvatarColors() {
    }

    static String forSeed(String seed) {
        if (seed == null || seed.isEmpty()) {
            return PALETTE[0];
        }
        return PALETTE[Math.floorMod(seed.hashCode(), PALETTE.length)];
    }
}
