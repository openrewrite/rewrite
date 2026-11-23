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

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code Date.parse} as V8 reads the ECMAScript date-time string format, and {@code Date.prototype.toISOString}.
 */
public final class V8Date {

    private static final Pattern DATE_TIME_STRING = Pattern.compile(
            "^([+-]\\d{6}|\\d{4})(?:-(\\d{2})(?:-(\\d{2}))?)?" +
            "(?:[T ](\\d{2}):(\\d{2})(?::(\\d{2})(?:\\.(\\d{1,3})\\d*)?)?(Z|[+-]\\d{2}:\\d{2})?)?$");
    private static final double MAX_TIME = 8.64e15;
    private static final DateTimeFormatter ISO_REST =
            DateTimeFormatter.ofPattern("-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private V8Date() {
    }

    public static boolean isDateTimeString(String text) {
        return DATE_TIME_STRING.matcher(text).matches();
    }

    /**
     * {@code new Date(time).toISOString()}, a year outside 0 to 9999 in six digits with its sign.
     * @throws JsError V8's RangeError for NaN or a time beyond the range of a Date
     */
    public static String toISOString(double time) {
        double clipped = timeClip(time);
        if (Double.isNaN(clipped)) {
            throw new JsError(JsError.RANGE_ERROR, "Invalid time value");
        }
        Instant instant = Instant.ofEpochMilli((long) clipped);
        int year = instant.atZone(ZoneOffset.UTC).getYear();
        String yearText = year >= 0 && year <= 9999 ? String.format("%04d", year) :
                (year < 0 ? "-" : "+") + String.format("%06d", Math.abs(year));
        return yearText + ISO_REST.format(instant);
    }

    /**
     * {@code Date.parse(text)}: NaN for text V8 reads otherwise; cannot-know for a date-time without an offset, which
     * JavaScript reads in the host's time zone.
     */
    public static double dateParse(String text) {
        Double time = parse(text);
        if (time == null) {
            throw new CannotKnow("the host's time zone (\"" + text + "\" names no offset)");
        }
        return time;
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
        return timeClip(makeDay(year, month - 1, day) * 86400000.0 + hour * 3600000L + minute * 60000L +
                        second * 1000L + millis - offset);
    }

    /**
     * {@code Date.UTC(year, month, day, hours, minutes, seconds, ms)} of integers, a year from 0 to 99 read as 1900
     * plus it.
     */
    public static double utc(double year, double month, double day, double hour, double minute, double second,
                             double ms) {
        double fullYear = 0 <= year && year <= 99 ? 1900 + year : year;
        return timeClip(makeDay(fullYear, month, day) * 86400000.0 + hour * 3600000 + minute * 60000 + second * 1000 +
                        ms);
    }

    /**
     * TimeClip, {@code new Date(time).getTime()}: NaN beyond the range of a Date, else the integer toward zero.
     */
    public static double timeClip(double time) {
        if (Double.isNaN(time) || Math.abs(time) > MAX_TIME) {
            return Double.NaN;
        }
        return (time < 0 ? Math.ceil(time) : Math.floor(time)) + 0.0;
    }

    // MakeDay: a day past the month's end rolls into the next
    private static double makeDay(double year, double month, double day) {
        double ym = year + Math.floor(month / 12);
        double mn = (month % 12 + 12) % 12;
        return LocalDate.of((int) ym, (int) mn + 1, 1).toEpochDay() + day - 1;
    }
}
