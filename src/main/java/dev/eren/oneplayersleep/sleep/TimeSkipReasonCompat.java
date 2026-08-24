package dev.eren.oneplayersleep.sleep;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Bridges the binary-incompatible TimeSkipEvent change introduced by Paper 26.2.
 *
 * <p>Older Bukkit APIs declare getSkipReason() directly on TimeSkipEvent and return
 * TimeSkipEvent.SkipReason. Paper 26.2 inherits the method from ClockTimeSkipEvent
 * and returns ClockTimeSkipEvent.SkipReason. Calling the old descriptor directly
 * therefore causes a NoSuchMethodError even though the Java method name is unchanged.</p>
 */
final class TimeSkipReasonCompat {
    private static final String NIGHT_SKIP = "NIGHT_SKIP";

    private TimeSkipReasonCompat() {
    }

    static boolean isNightSkip(Object event) {
        Object reason = invokeNoArg(event, "getSkipReason");
        if (reason == null) {
            // Kept as a conservative fallback for distributions that expose a generic reason accessor.
            reason = invokeNoArg(event, "getReason");
        }
        return reason instanceof Enum<?> && NIGHT_SKIP.equals(((Enum<?>) reason).name());
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) return null;
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (NoSuchMethodException ignored) {
            return null;
        } catch (IllegalAccessException ignored) {
            return null;
        } catch (InvocationTargetException ignored) {
            return null;
        } catch (LinkageError ignored) {
            return null;
        }
    }
}
