package bench;

import java.util.Arrays;

/** Tiny descriptive statistics over nanosecond samples; all display in milliseconds. */
public final class Stat {

    private Stat() {
    }

    public static double ms(long nanos) {
        return nanos / 1_000_000.0;
    }

    public static long median(long[] nanos) {
        long[] c = nanos.clone();
        Arrays.sort(c);
        int n = c.length;
        return n % 2 == 1 ? c[n / 2] : (c[n / 2 - 1] + c[n / 2]) / 2;
    }

    public static long min(long[] nanos) {
        long m = Long.MAX_VALUE;
        for (long v : nanos) {
            m = Math.min(m, v);
        }
        return m;
    }

    public static long max(long[] nanos) {
        long m = Long.MIN_VALUE;
        for (long v : nanos) {
            m = Math.max(m, v);
        }
        return m;
    }

    /** median with [min..max] spread in ms, e.g. "412.3 ms (min 401.0, max 455.8)". */
    public static String summary(long[] nanos) {
        return String.format("%.1f ms (min %.1f, max %.1f, n=%d)",
                ms(median(nanos)), ms(min(nanos)), ms(max(nanos)), nanos.length);
    }
}
