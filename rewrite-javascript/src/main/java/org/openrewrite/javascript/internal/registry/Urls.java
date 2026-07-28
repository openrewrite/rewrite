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
     * Registry path encoding of a package name: a scoped name's {@code /} separator becomes
     * {@code %2F} ({@code @angular/core} → {@code @angular%2Fcore}); unscoped names are unchanged.
     */
    static String encodeName(String packageName) {
        String scope = scopeOf(packageName);
        if (scope != null) {
            return scope + "%2F" + packageName.substring(scope.length() + 1);
        }
        return packageName;
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
