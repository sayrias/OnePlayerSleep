package dev.eren.oneplayersleep.threshold;

public enum PercentageRounding {
    CEIL,
    FLOOR,
    NEAREST;

    public static PercentageRounding parse(String value) {
        if (value == null) {
            return CEIL;
        }
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return CEIL;
        }
    }

    int apply(double value) {
        switch (this) {
            case FLOOR:
                return (int) Math.floor(value);
            case NEAREST:
                return (int) Math.round(value);
            case CEIL:
            default:
                return (int) Math.ceil(value);
        }
    }
}
