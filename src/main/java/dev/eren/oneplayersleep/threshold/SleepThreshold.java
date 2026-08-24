package dev.eren.oneplayersleep.threshold;

import java.math.BigDecimal;

public final class SleepThreshold {
    public enum Type {
        ABSOLUTE,
        PERCENTAGE
    }

    private final Type type;
    private final double value;
    private final PercentageRounding rounding;

    private SleepThreshold(Type type, double value, PercentageRounding rounding) {
        this.type = type;
        this.value = value;
        this.rounding = rounding;
    }

    public static SleepThreshold parse(Object raw, PercentageRounding rounding) {
        if (raw == null) {
            throw new IllegalArgumentException("Sleep threshold cannot be empty");
        }

        if (raw instanceof Number) {
            double number = ((Number) raw).doubleValue();
            if (!Double.isFinite(number) || number < 1 || number != Math.rint(number)) {
                throw new IllegalArgumentException("Absolute sleep threshold must be a whole number greater than zero");
            }
            return new SleepThreshold(Type.ABSOLUTE, number, rounding);
        }

        String text = String.valueOf(raw).trim().replace(" ", "");
        boolean percentage = text.startsWith("%") || text.endsWith("%");
        if (percentage) {
            text = text.replace("%", "");
            double percent = parseFinite(text);
            if (percent <= 0 || percent > 100) {
                throw new IllegalArgumentException("Percentage must be greater than 0 and at most 100");
            }
            return new SleepThreshold(Type.PERCENTAGE, percent, rounding);
        }

        double absolute = parseFinite(text);
        if (absolute < 1 || absolute != Math.rint(absolute)) {
            throw new IllegalArgumentException("Absolute sleep threshold must be a whole number greater than zero");
        }
        return new SleepThreshold(Type.ABSOLUTE, absolute, rounding);
    }

    private static double parseFinite(String value) {
        try {
            double parsed = Double.parseDouble(value.replace(',', '.'));
            if (!Double.isFinite(parsed)) {
                throw new IllegalArgumentException("Threshold must be finite");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Threshold is not a number", exception);
        }
    }

    public int requiredPlayers(int eligiblePlayers) {
        if (eligiblePlayers <= 0) {
            return 0;
        }

        int requested;
        if (type == Type.ABSOLUTE) {
            requested = value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
        } else {
            requested = rounding.apply(eligiblePlayers * value / 100.0D);
        }

        // This protects both low populations and thresholds configured above the population.
        return Math.max(1, Math.min(requested, eligiblePlayers));
    }

    public boolean isClamped(int eligiblePlayers) {
        return eligiblePlayers > 0 && type == Type.ABSOLUTE && value > eligiblePlayers;
    }

    public Type getType() {
        return type;
    }

    public double getValue() {
        return value;
    }

    public String asConfigValue() {
        String normalized = BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
        return type == Type.PERCENTAGE ? normalized + "%" : normalized;
    }
}
