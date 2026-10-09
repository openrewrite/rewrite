/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code Date.parse} as V8 reads the ECMAScript date-time string format.
 */
public final class V8Date {

    private static final Pattern DATE_TIME_STRING = Pattern.compile(
            "^([+-]\\d{6}|\\d{4})(?:-(\\d{2})(?:-(\\d{2}))?)?" +
            "(?:[T ](\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.(\\d{1,3})\\d*)?)?(Z|[+-]\\d{2}:\\d{2})?)?$");

    private V8Date() {
    }

    public static boolean isDateTimeString(String text) {
        return DATE_TIME_STRING.matcher(text).matches();
    }

    /**
     * The time value in milliseconds; NaN for text V8 reads otherwise; null for a date-time without an offset, which
     * JavaScript reads in the host's time zone. A date-only form is UTC.
     */
    public static @Nullable Double parse(String text) {
        Matcher m = DATE_TIME_STRING.matcher(text);
        if (!m.matches()) {
            return Double.NaN;
        }
        if (m.group(4) != null && m.group(8) == null) {
            return null;
        }
        int year = Integer.parseInt(m.group(1));
        int month = m.group(2) == null ? 1 : Integer.parseInt(m.group(2));
        int day = m.group(3) == null ? 1 : Integer.parseInt(m.group(3));
        int hour = m.group(4) == null ? 0 : Integer.parseInt(m.group(4));
        int minute = m.group(5) == null ? 0 : Integer.parseInt(m.group(5));
        int second = m.group(6) == null ? 0 : Integer.parseInt(m.group(6));
        int millis = m.group(7) == null ? 0 : Integer.parseInt((m.group(7) + "00").substring(0, 3));
        if ("-000000".equals(m.group(1)) || month < 1 || month > 12 || day < 1 || day > 31 || hour > 24 ||
            minute > 59 || second > 59 || hour == 24 && (minute != 0 || second != 0 || millis != 0)) {
            return Double.NaN;
        }
        long offset = 0;
        String zone = m.group(8);
        if (zone != null && !"Z".equals(zone)) {
            int hours = Integer.parseInt(zone.substring(1, 3));
            int minutes = Integer.parseInt(zone.substring(4, 6));
            if (hours > 23 || minutes > 59) {
                return Double.NaN;
            }
            offset = (zone.charAt(0) == '-' ? -1 : 1) * (hours * 60L + minutes) * 60000L;
        }
        // a day past the month's end rolls into the next, as V8's MakeDay does
        long days = LocalDate.of(year, month, 1).toEpochDay() + day - 1;
        double time = days * 86400000.0 + hour * 3600000L + minute * 60000L + second * 1000L + millis - offset;
        return Math.abs(time) > 8.64e15 ? Double.NaN : time;
    }
}
