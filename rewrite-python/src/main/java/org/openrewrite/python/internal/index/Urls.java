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
package org.openrewrite.python.internal.index;

import org.jspecify.annotations.Nullable;

final class Urls {

    private Urls() {
    }

    /**
     * Range {@code [start, end)} of the userinfo within {@code url}'s authority
     * ({@code end} is the index of the {@code @}), or null when there is none.
     */
    static int @Nullable [] userinfoRange(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return null;
        }
        int start = schemeEnd + 3;
        int end = authorityEnd(url, start);
        int at = url.lastIndexOf('@', end - 1);
        if (at < start) {
            return null;
        }
        return new int[]{start, at};
    }

    static String stripUserinfo(String url) {
        int[] range = userinfoRange(url);
        if (range == null) {
            return url;
        }
        return url.substring(0, range[0]) + url.substring(range[1] + 1);
    }

    static @Nullable String host(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return null;
        }
        int start = schemeEnd + 3;
        int end = authorityEnd(url, start);
        int at = url.lastIndexOf('@', end - 1);
        if (at >= start) {
            start = at + 1;
        }
        String hostPort = url.substring(start, end);
        if (hostPort.startsWith("[")) {
            int close = hostPort.indexOf(']');
            return close > 0 ? hostPort.substring(1, close) : null;
        }
        int colon = hostPort.lastIndexOf(':');
        if (colon >= 0 && hostPort.indexOf(':') == colon) {
            hostPort = hostPort.substring(0, colon);
        }
        return hostPort.isEmpty() ? null : hostPort;
    }

    private static int authorityEnd(String url, int start) {
        for (int i = start; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                return i;
            }
        }
        return url.length();
    }
}
