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
package org.openrewrite.javascript.internal.registry;

import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;

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

    /**
     * npm's "nerf dart" key for a registry URL: scheme and userinfo dropped, then the
     * final non-directory path segment removed (as {@code url.resolve(".")} does), leaving
     * a {@code //host[:port]/path/} prefix that auth keys ({@code :_authToken}, …) hang off.
     */
    static String nerfDart(String url) {
        String noUserinfo = stripUserinfo(url);
        int schemeEnd = noUserinfo.indexOf("://");
        String rest = schemeEnd < 0 ? noUserinfo : noUserinfo.substring(schemeEnd + 3);
        int cut = rest.length();
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c == '?' || c == '#') {
                cut = i;
                break;
            }
        }
        rest = rest.substring(0, cut);
        int lastSlash = rest.lastIndexOf('/');
        rest = lastSlash < 0 ? rest + "/" : rest.substring(0, lastSlash + 1);
        return "//" + rest;
    }

    /**
     * The {@code @scope} of a package name ({@code @angular} for {@code @angular/core}), or
     * null for an unscoped name.
     */
    static @Nullable String scopeOf(String packageName) {
        if (packageName.startsWith("@")) {
            int slash = packageName.indexOf('/');
            if (slash > 0) {
                return packageName.substring(0, slash);
            }
        }
        return null;
    }

    /**
     * Registry path encoding of a package name: a scoped name's single {@code /} separator becomes
     * {@code %2F} ({@code @angular/core} → {@code @angular%2Fcore}) and every other path-significant
     * character is percent-encoded, so a crafted name cannot escape the registry path. A {@code ..}
     * segment or a stray {@code /} in the (single-segment) remainder is rejected outright.
     */
    static String encodeName(String packageName) {
        if (packageName.isEmpty() || packageName.startsWith("/") || hasDotDotSegment(packageName)) {
            throw new IllegalArgumentException("unsafe package name: " + packageName);
        }
        String scope = scopeOf(packageName);
        String remainder = scope == null ? packageName : packageName.substring(scope.length() + 1);
        if (remainder.indexOf('/') >= 0) {
            throw new IllegalArgumentException("unsafe package name: " + packageName);
        }
        String prefix = scope == null ? "" : scope + "%2F";
        return prefix + percentEncodeSegment(remainder);
    }

    private static boolean hasDotDotSegment(String name) {
        for (String segment : name.split("/", -1)) {
            if ("..".equals(segment)) {
                return true;
            }
        }
        return false;
    }

    private static String percentEncodeSegment(String segment) {
        StringBuilder sb = new StringBuilder(segment.length());
        for (byte b : segment.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') ||
                    c == '-' || c == '.' || c == '_' || c == '~') {
                sb.append((char) c);
            } else {
                sb.append('%').append(hex(c >> 4)).append(hex(c & 0xF));
            }
        }
        return sb.toString();
    }

    private static char hex(int nibble) {
        return (char) (nibble < 10 ? '0' + nibble : 'A' + (nibble - 10));
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
