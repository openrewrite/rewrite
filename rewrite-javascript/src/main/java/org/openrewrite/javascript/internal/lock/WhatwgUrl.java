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

import java.io.ByteArrayOutputStream;
import java.net.IDN;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Port of the WHATWG URL Standard's basic URL parser and serializer, the {@code URL} class that
 * npm-package-arg and hosted-git-info parse specs with.
 */
public final class WhatwgUrl {

    private static final Map<String, Integer> SPECIAL = new HashMap<>();

    static {
        SPECIAL.put("ftp", 21);
        SPECIAL.put("file", -1);
        SPECIAL.put("http", 80);
        SPECIAL.put("https", 443);
        SPECIAL.put("ws", 80);
        SPECIAL.put("wss", 443);
    }

    private enum State {
        SCHEME_START, SCHEME, NO_SCHEME, SPECIAL_RELATIVE_OR_AUTHORITY, PATH_OR_AUTHORITY, RELATIVE,
        RELATIVE_SLASH, SPECIAL_AUTHORITY_SLASHES, SPECIAL_AUTHORITY_IGNORE_SLASHES, AUTHORITY, HOST,
        PORT, FILE, FILE_SLASH, FILE_HOST, PATH_START, PATH, OPAQUE_PATH, QUERY, FRAGMENT
    }

    private String scheme = "";
    private String username = "";
    private String password = "";
    private @Nullable String host;
    private @Nullable Integer port;
    private List<String> path = new ArrayList<>();
    private @Nullable String opaquePath;
    private @Nullable String query;
    private @Nullable String fragment;

    private WhatwgUrl() {
    }

    /**
     * @throws IllegalArgumentException as {@code new URL()} throws {@code TypeError: Invalid URL}.
     */
    public static WhatwgUrl parse(String input) {
        return parse(input, null);
    }

    public static WhatwgUrl parse(String input, @Nullable String base) {
        WhatwgUrl baseUrl = null;
        if (base != null) {
            baseUrl = basicParse(base, null);
            if (baseUrl == null) {
                throw new IllegalArgumentException("Invalid URL: " + base);
            }
        }
        WhatwgUrl url = basicParse(input, baseUrl);
        if (url == null) {
            throw new IllegalArgumentException("Invalid URL: " + input);
        }
        return url;
    }

    public static @Nullable WhatwgUrl tryParse(String input) {
        return basicParse(input, null);
    }

    /**
     * {@code new URL(input, base)}, or null where it throws.
     */
    public static @Nullable WhatwgUrl tryParse(String input, String base) {
        WhatwgUrl baseUrl = basicParse(base, null);
        return baseUrl == null ? null : basicParse(input, baseUrl);
    }

    /**
     * {@code URL.prototype.origin}.
     */
    public String getOrigin() {
        if ("blob".equals(scheme)) {
            WhatwgUrl inner = basicParse(getPathname(), null);
            return inner != null && ("http".equals(inner.scheme) || "https".equals(inner.scheme)) ?
                    inner.getOrigin() : "null";
        }
        return SPECIAL.containsKey(scheme) && !"file".equals(scheme) ? scheme + "://" + getHost() : "null";
    }

    public String getProtocol() {
        return scheme + ":";
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHost() {
        if (host == null) {
            return "";
        }
        return port == null ? host : host + ":" + port;
    }

    public String getHostname() {
        return host == null ? "" : host;
    }

    public String getPort() {
        return port == null ? "" : Integer.toString(port);
    }

    public String getPathname() {
        if (opaquePath != null) {
            return opaquePath;
        }
        StringBuilder sb = new StringBuilder();
        for (String segment : path) {
            sb.append('/').append(segment);
        }
        return sb.toString();
    }

    public String getSearch() {
        return query == null || query.isEmpty() ? "" : "?" + query;
    }

    public String getHash() {
        return fragment == null || fragment.isEmpty() ? "" : "#" + fragment;
    }

    public void setHash(String value) {
        if (value.isEmpty()) {
            fragment = null;
            return;
        }
        String input = value.startsWith("#") ? value.substring(1) : value;
        StringBuilder sb = new StringBuilder();
        input.codePoints().forEach(c -> percentEncode(sb, c, FRAGMENT_SET));
        fragment = sb.toString();
    }

    public String getHref() {
        StringBuilder out = new StringBuilder(scheme).append(':');
        if (host != null) {
            out.append("//");
            if (!username.isEmpty() || !password.isEmpty()) {
                out.append(username);
                if (!password.isEmpty()) {
                    out.append(':').append(password);
                }
                out.append('@');
            }
            out.append(host);
            if (port != null) {
                out.append(':').append(port);
            }
        }
        if (host == null && opaquePath == null && path.size() > 1 && path.get(0).isEmpty()) {
            out.append("/.");
        }
        out.append(getPathname());
        if (query != null) {
            out.append('?').append(query);
        }
        if (fragment != null) {
            out.append('#').append(fragment);
        }
        return out.toString();
    }

    @Override
    public String toString() {
        return getHref();
    }

    private boolean isSpecial() {
        return SPECIAL.containsKey(scheme);
    }

    private boolean includesCredentials() {
        return !username.isEmpty() || !password.isEmpty();
    }

    private static @Nullable WhatwgUrl basicParse(String rawInput, @Nullable WhatwgUrl base) {
        int start = 0;
        int end = rawInput.length();
        while (start < end && rawInput.charAt(start) <= 0x20) {
            start++;
        }
        while (end > start && rawInput.charAt(end - 1) <= 0x20) {
            end--;
        }
        StringBuilder stripped = new StringBuilder();
        for (int i = start; i < end; i++) {
            char c = rawInput.charAt(i);
            if (c != '\t' && c != '\n' && c != '\r') {
                stripped.append(c);
            }
        }
        int[] input = stripped.toString().codePoints().toArray();

        WhatwgUrl url = new WhatwgUrl();
        State state = State.SCHEME_START;
        StringBuilder buffer = new StringBuilder();
        boolean atSignSeen = false;
        boolean insideBrackets = false;
        boolean passwordTokenSeen = false;
        int pointer = 0;

        while (true) {
            int c = pointer < input.length ? input[pointer] : -1;
            switch (state) {
                case SCHEME_START:
                    if (isAsciiAlpha(c)) {
                        buffer.appendCodePoint(Character.toLowerCase(c));
                        state = State.SCHEME;
                    } else {
                        state = State.NO_SCHEME;
                        pointer--;
                    }
                    break;
                case SCHEME:
                    if (isAsciiAlphanumeric(c) || c == '+' || c == '-' || c == '.') {
                        buffer.appendCodePoint(Character.toLowerCase(c));
                    } else if (c == ':') {
                        url.scheme = buffer.toString();
                        buffer.setLength(0);
                        if ("file".equals(url.scheme)) {
                            state = State.FILE;
                        } else if (url.isSpecial() && base != null && base.scheme.equals(url.scheme)) {
                            state = State.SPECIAL_RELATIVE_OR_AUTHORITY;
                        } else if (url.isSpecial()) {
                            state = State.SPECIAL_AUTHORITY_SLASHES;
                        } else if (remainingStartsWith(input, pointer, '/')) {
                            state = State.PATH_OR_AUTHORITY;
                            pointer++;
                        } else {
                            url.opaquePath = "";
                            state = State.OPAQUE_PATH;
                        }
                    } else {
                        buffer.setLength(0);
                        state = State.NO_SCHEME;
                        pointer = -1;
                    }
                    break;
                case NO_SCHEME:
                    if (base == null || (base.opaquePath != null && c != '#')) {
                        return null;
                    } else if (base.opaquePath != null) {
                        url.scheme = base.scheme;
                        url.opaquePath = base.opaquePath;
                        url.query = base.query;
                        url.fragment = "";
                        state = State.FRAGMENT;
                    } else if (!"file".equals(base.scheme)) {
                        state = State.RELATIVE;
                        pointer--;
                    } else {
                        state = State.FILE;
                        pointer--;
                    }
                    break;
                case SPECIAL_RELATIVE_OR_AUTHORITY:
                    if (c == '/' && remainingStartsWith(input, pointer, '/')) {
                        state = State.SPECIAL_AUTHORITY_IGNORE_SLASHES;
                        pointer++;
                    } else {
                        state = State.RELATIVE;
                        pointer--;
                    }
                    break;
                case PATH_OR_AUTHORITY:
                    if (c == '/') {
                        state = State.AUTHORITY;
                    } else {
                        state = State.PATH;
                        pointer--;
                    }
                    break;
                case RELATIVE:
                    url.scheme = base.scheme;
                    if (c == '/') {
                        state = State.RELATIVE_SLASH;
                    } else if (url.isSpecial() && c == '\\') {
                        state = State.RELATIVE_SLASH;
                    } else {
                        url.username = base.username;
                        url.password = base.password;
                        url.host = base.host;
                        url.port = base.port;
                        url.path = new ArrayList<>(base.path);
                        url.query = base.query;
                        if (c == '?') {
                            url.query = "";
                            state = State.QUERY;
                        } else if (c == '#') {
                            url.fragment = "";
                            state = State.FRAGMENT;
                        } else if (c != -1) {
                            url.query = null;
                            url.shortenPath();
                            state = State.PATH;
                            pointer--;
                        }
                    }
                    break;
                case RELATIVE_SLASH:
                    if (url.isSpecial() && (c == '/' || c == '\\')) {
                        state = State.SPECIAL_AUTHORITY_IGNORE_SLASHES;
                    } else if (c == '/') {
                        state = State.AUTHORITY;
                    } else {
                        url.username = base.username;
                        url.password = base.password;
                        url.host = base.host;
                        url.port = base.port;
                        state = State.PATH;
                        pointer--;
                    }
                    break;
                case SPECIAL_AUTHORITY_SLASHES:
                    if (c == '/' && remainingStartsWith(input, pointer, '/')) {
                        state = State.SPECIAL_AUTHORITY_IGNORE_SLASHES;
                        pointer++;
                    } else {
                        state = State.SPECIAL_AUTHORITY_IGNORE_SLASHES;
                        pointer--;
                    }
                    break;
                case SPECIAL_AUTHORITY_IGNORE_SLASHES:
                    if (c != '/' && c != '\\') {
                        state = State.AUTHORITY;
                        pointer--;
                    }
                    break;
                case AUTHORITY:
                    if (c == '@') {
                        if (atSignSeen) {
                            buffer.insert(0, "%40");
                        }
                        atSignSeen = true;
                        int[] cps = buffer.toString().codePoints().toArray();
                        for (int cp : cps) {
                            if (cp == ':' && !passwordTokenSeen) {
                                passwordTokenSeen = true;
                                continue;
                            }
                            StringBuilder encoded = new StringBuilder();
                            percentEncode(encoded, cp, USERINFO_SET);
                            if (passwordTokenSeen) {
                                url.password += encoded;
                            } else {
                                url.username += encoded;
                            }
                        }
                        buffer.setLength(0);
                    } else if (c == -1 || c == '/' || c == '?' || c == '#' || (url.isSpecial() && c == '\\')) {
                        if (atSignSeen && buffer.length() == 0) {
                            return null;
                        }
                        pointer -= buffer.codePointCount(0, buffer.length()) + 1;
                        buffer.setLength(0);
                        state = State.HOST;
                    } else {
                        buffer.appendCodePoint(c);
                    }
                    break;
                case HOST:
                    if (c == ':' && !insideBrackets) {
                        if (buffer.length() == 0) {
                            return null;
                        }
                        String parsedHost = parseHost(buffer.toString(), !url.isSpecial());
                        if (parsedHost == null) {
                            return null;
                        }
                        url.host = parsedHost;
                        buffer.setLength(0);
                        state = State.PORT;
                    } else if (c == -1 || c == '/' || c == '?' || c == '#' || (url.isSpecial() && c == '\\')) {
                        pointer--;
                        if (url.isSpecial() && buffer.length() == 0) {
                            return null;
                        }
                        String parsedHost = parseHost(buffer.toString(), !url.isSpecial());
                        if (parsedHost == null) {
                            return null;
                        }
                        url.host = parsedHost;
                        buffer.setLength(0);
                        state = State.PATH_START;
                    } else {
                        if (c == '[') {
                            insideBrackets = true;
                        }
                        if (c == ']') {
                            insideBrackets = false;
                        }
                        buffer.appendCodePoint(c);
                    }
                    break;
                case PORT:
                    if (isAsciiDigit(c)) {
                        buffer.appendCodePoint(c);
                    } else if (c == -1 || c == '/' || c == '?' || c == '#' || (url.isSpecial() && c == '\\')) {
                        if (buffer.length() != 0) {
                            int value;
                            try {
                                value = Integer.parseInt(buffer.toString());
                            } catch (NumberFormatException e) {
                                return null;
                            }
                            if (value > 65535) {
                                return null;
                            }
                            Integer defaultPort = SPECIAL.get(url.scheme);
                            url.port = defaultPort != null && defaultPort == value ? null : value;
                            buffer.setLength(0);
                        }
                        state = State.PATH_START;
                        pointer--;
                    } else {
                        return null;
                    }
                    break;
                case FILE:
                    url.scheme = "file";
                    url.host = "";
                    if (c == '/' || c == '\\') {
                        state = State.FILE_SLASH;
                    } else if (base != null && "file".equals(base.scheme)) {
                        url.host = base.host;
                        url.path = new ArrayList<>(base.path);
                        url.query = base.query;
                        if (c == '?') {
                            url.query = "";
                            state = State.QUERY;
                        } else if (c == '#') {
                            url.fragment = "";
                            state = State.FRAGMENT;
                        } else if (c != -1) {
                            url.query = null;
                            if (!startsWithWindowsDriveLetter(input, pointer)) {
                                url.shortenPath();
                            } else {
                                url.path = new ArrayList<>();
                            }
                            state = State.PATH;
                            pointer--;
                        }
                    } else {
                        state = State.PATH;
                        pointer--;
                    }
                    break;
                case FILE_SLASH:
                    if (c == '/' || c == '\\') {
                        state = State.FILE_HOST;
                    } else {
                        if (base != null && "file".equals(base.scheme)) {
                            url.host = base.host;
                            if (!startsWithWindowsDriveLetter(input, pointer) && !base.path.isEmpty() &&
                                isNormalizedWindowsDriveLetter(base.path.get(0))) {
                                url.path.add(base.path.get(0));
                            }
                        }
                        state = State.PATH;
                        pointer--;
                    }
                    break;
                case FILE_HOST:
                    if (c == -1 || c == '/' || c == '\\' || c == '?' || c == '#') {
                        pointer--;
                        if (isWindowsDriveLetter(buffer.toString(), false)) {
                            state = State.PATH;
                        } else if (buffer.length() == 0) {
                            url.host = "";
                            state = State.PATH_START;
                        } else {
                            String parsedHost = parseHost(buffer.toString(), false);
                            if (parsedHost == null) {
                                return null;
                            }
                            url.host = "localhost".equals(parsedHost) ? "" : parsedHost;
                            buffer.setLength(0);
                            state = State.PATH_START;
                        }
                    } else {
                        buffer.appendCodePoint(c);
                    }
                    break;
                case PATH_START:
                    if (url.isSpecial()) {
                        state = State.PATH;
                        if (c != '/' && c != '\\') {
                            pointer--;
                        }
                    } else if (c == '?') {
                        url.query = "";
                        state = State.QUERY;
                    } else if (c == '#') {
                        url.fragment = "";
                        state = State.FRAGMENT;
                    } else if (c != -1) {
                        state = State.PATH;
                        if (c != '/') {
                            pointer--;
                        }
                    }
                    break;
                case PATH:
                    if (c == -1 || c == '/' || (url.isSpecial() && c == '\\') || c == '?' || c == '#') {
                        String segment = buffer.toString();
                        if (isDoubleDotSegment(segment)) {
                            url.shortenPath();
                            if (c != '/' && !(url.isSpecial() && c == '\\')) {
                                url.path.add("");
                            }
                        } else if (isSingleDotSegment(segment) && c != '/' && !(url.isSpecial() && c == '\\')) {
                            url.path.add("");
                        } else if (!isSingleDotSegment(segment)) {
                            if ("file".equals(url.scheme) && url.path.isEmpty() &&
                                isWindowsDriveLetter(segment, false)) {
                                segment = segment.charAt(0) + ":";
                            }
                            url.path.add(segment);
                        }
                        buffer.setLength(0);
                        if (c == '?') {
                            url.query = "";
                            state = State.QUERY;
                        }
                        if (c == '#') {
                            url.fragment = "";
                            state = State.FRAGMENT;
                        }
                    } else {
                        percentEncode(buffer, c, PATH_SET);
                    }
                    break;
                case OPAQUE_PATH:
                    if (c == '?') {
                        url.query = "";
                        state = State.QUERY;
                    } else if (c == '#') {
                        url.fragment = "";
                        state = State.FRAGMENT;
                    } else if (c != -1) {
                        StringBuilder sb = new StringBuilder();
                        percentEncode(sb, c, C0_CONTROL_SET);
                        url.opaquePath += sb;
                    }
                    break;
                case QUERY:
                    if (c == '#' || c == -1) {
                        StringBuilder sb = new StringBuilder();
                        boolean[] set = url.isSpecial() ? SPECIAL_QUERY_SET : QUERY_SET;
                        buffer.toString().codePoints().forEach(cp -> percentEncode(sb, cp, set));
                        url.query += sb;
                        buffer.setLength(0);
                        if (c == '#') {
                            url.fragment = "";
                            state = State.FRAGMENT;
                        }
                    } else {
                        buffer.appendCodePoint(c);
                    }
                    break;
                case FRAGMENT:
                    if (c != -1) {
                        StringBuilder sb = new StringBuilder();
                        percentEncode(sb, c, FRAGMENT_SET);
                        url.fragment += sb;
                    }
                    break;
            }
            // a state that stepped the pointer back from the end runs again on the end
            if (pointer >= input.length) {
                break;
            }
            pointer++;
        }
        return url;
    }

    private void shortenPath() {
        if ("file".equals(scheme) && path.size() == 1 && isNormalizedWindowsDriveLetter(path.get(0))) {
            return;
        }
        if (!path.isEmpty()) {
            path.remove(path.size() - 1);
        }
    }

    private static boolean remainingStartsWith(int[] input, int pointer, char c) {
        return pointer + 1 < input.length && input[pointer + 1] == c;
    }

    private static boolean isSingleDotSegment(String s) {
        return ".".equals(s) || "%2e".equalsIgnoreCase(s);
    }

    private static boolean isDoubleDotSegment(String s) {
        String lower = s.toLowerCase(Locale.ROOT);
        return "..".equals(lower) || ".%2e".equals(lower) || "%2e.".equals(lower) || "%2e%2e".equals(lower);
    }

    private static boolean isWindowsDriveLetter(String s, boolean normalized) {
        return s.length() == 2 && isAsciiAlpha(s.charAt(0)) &&
               (s.charAt(1) == ':' || (!normalized && s.charAt(1) == '|'));
    }

    private static boolean isNormalizedWindowsDriveLetter(String s) {
        return isWindowsDriveLetter(s, true);
    }

    private static boolean startsWithWindowsDriveLetter(int[] input, int pointer) {
        if (input.length - pointer < 2) {
            return false;
        }
        if (!isAsciiAlpha(input[pointer]) || (input[pointer + 1] != ':' && input[pointer + 1] != '|')) {
            return false;
        }
        if (input.length - pointer == 2) {
            return true;
        }
        int third = input[pointer + 2];
        return third == '/' || third == '\\' || third == '?' || third == '#';
    }

    private static @Nullable String parseHost(String input, boolean isOpaque) {
        if (input.startsWith("[")) {
            if (!input.endsWith("]")) {
                return null;
            }
            String ipv6 = parseIpv6(input.substring(1, input.length() - 1));
            return ipv6 == null ? null : "[" + ipv6 + "]";
        }
        if (isOpaque) {
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (isForbiddenHostCodePoint(c)) {
                    return null;
                }
            }
            StringBuilder sb = new StringBuilder();
            input.codePoints().forEach(cp -> percentEncode(sb, cp, C0_CONTROL_SET));
            return sb.toString();
        }
        String domain = new String(percentDecode(input), StandardCharsets.UTF_8);
        String asciiDomain = domain;
        if (!domain.chars().allMatch(c -> c < 0x80)) {
            try {
                asciiDomain = IDN.toASCII(domain, IDN.ALLOW_UNASSIGNED);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        asciiDomain = asciiDomain.toLowerCase(Locale.ROOT);
        if (asciiDomain.isEmpty()) {
            return null;
        }
        for (int i = 0; i < asciiDomain.length(); i++) {
            char c = asciiDomain.charAt(i);
            if (isForbiddenHostCodePoint(c) || c == '%' || c <= 0x1F || c == 0x7F) {
                return null;
            }
        }
        if (endsInANumber(asciiDomain)) {
            Long ipv4 = parseIpv4(asciiDomain);
            if (ipv4 == null) {
                return null;
            }
            return (ipv4 >> 24 & 0xFF) + "." + (ipv4 >> 16 & 0xFF) + "." + (ipv4 >> 8 & 0xFF) + "." + (ipv4 & 0xFF);
        }
        return asciiDomain;
    }

    private static boolean isForbiddenHostCodePoint(char c) {
        return c == 0 || c == '\t' || c == '\n' || c == '\r' || c == ' ' || c == '#' || c == '/' ||
               c == ':' || c == '<' || c == '>' || c == '?' || c == '@' || c == '[' || c == '\\' ||
               c == ']' || c == '^' || c == '|';
    }

    private static boolean endsInANumber(String input) {
        List<String> parts = new ArrayList<>(Arrays.asList(input.split("\\.", -1)));
        if (parts.get(parts.size() - 1).isEmpty()) {
            if (parts.size() == 1) {
                return false;
            }
            parts.remove(parts.size() - 1);
        }
        String last = parts.get(parts.size() - 1);
        if (!last.isEmpty() && last.chars().allMatch(WhatwgUrl::isAsciiDigit)) {
            return true;
        }
        return parseIpv4Number(last) != null;
    }

    private static @Nullable Long parseIpv4(String input) {
        List<String> parts = new ArrayList<>(Arrays.asList(input.split("\\.", -1)));
        if (parts.get(parts.size() - 1).isEmpty() && parts.size() > 1) {
            parts.remove(parts.size() - 1);
        }
        if (parts.size() > 4) {
            return null;
        }
        List<Long> numbers = new ArrayList<>();
        for (String part : parts) {
            Long n = parseIpv4Number(part);
            if (n == null) {
                return null;
            }
            numbers.add(n);
        }
        for (int i = 0; i < numbers.size() - 1; i++) {
            if (numbers.get(i) > 255) {
                return null;
            }
        }
        if (numbers.get(numbers.size() - 1) >= Math.pow(256, 5 - numbers.size())) {
            return null;
        }
        long ipv4 = numbers.get(numbers.size() - 1);
        for (int i = 0; i < numbers.size() - 1; i++) {
            ipv4 += numbers.get(i) * (long) Math.pow(256, 3 - i);
        }
        return ipv4;
    }

    private static @Nullable Long parseIpv4Number(String input) {
        if (input.isEmpty()) {
            return null;
        }
        int radix = 10;
        if (input.length() >= 2 && (input.startsWith("0x") || input.startsWith("0X"))) {
            input = input.substring(2);
            radix = 16;
        } else if (input.length() >= 2 && input.startsWith("0")) {
            input = input.substring(1);
            radix = 8;
        }
        if (input.isEmpty()) {
            return 0L;
        }
        try {
            long value = Long.parseLong(input, radix);
            return value < 0 ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static @Nullable String parseIpv6(String input) {
        int[] address = new int[8];
        int pieceIndex = 0;
        int compress = -1;
        int pointer = 0;
        int len = input.length();
        if (pointer < len && input.charAt(pointer) == ':') {
            if (pointer + 1 >= len || input.charAt(pointer + 1) != ':') {
                return null;
            }
            pointer += 2;
            pieceIndex++;
            compress = pieceIndex;
        }
        while (pointer < len) {
            if (pieceIndex == 8) {
                return null;
            }
            if (input.charAt(pointer) == ':') {
                if (compress != -1) {
                    return null;
                }
                pointer++;
                pieceIndex++;
                compress = pieceIndex;
                continue;
            }
            int value = 0;
            int length = 0;
            while (length < 4 && pointer < len && Character.digit(input.charAt(pointer), 16) >= 0) {
                value = value * 0x10 + Character.digit(input.charAt(pointer), 16);
                pointer++;
                length++;
            }
            if (pointer < len && input.charAt(pointer) == '.') {
                if (length == 0) {
                    return null;
                }
                pointer -= length;
                if (pieceIndex > 6) {
                    return null;
                }
                int numbersSeen = 0;
                while (pointer < len) {
                    int ipv4Piece = -1;
                    if (numbersSeen > 0) {
                        if (input.charAt(pointer) == '.' && numbersSeen < 4) {
                            pointer++;
                        } else {
                            return null;
                        }
                    }
                    if (pointer >= len || !isAsciiDigit(input.charAt(pointer))) {
                        return null;
                    }
                    while (pointer < len && isAsciiDigit(input.charAt(pointer))) {
                        int number = input.charAt(pointer) - '0';
                        if (ipv4Piece == -1) {
                            ipv4Piece = number;
                        } else if (ipv4Piece == 0) {
                            return null;
                        } else {
                            ipv4Piece = ipv4Piece * 10 + number;
                        }
                        if (ipv4Piece > 255) {
                            return null;
                        }
                        pointer++;
                    }
                    address[pieceIndex] = address[pieceIndex] * 0x100 + ipv4Piece;
                    numbersSeen++;
                    if (numbersSeen == 2 || numbersSeen == 4) {
                        pieceIndex++;
                    }
                }
                if (numbersSeen != 4) {
                    return null;
                }
                break;
            } else if (pointer < len && input.charAt(pointer) == ':') {
                pointer++;
                if (pointer >= len) {
                    return null;
                }
            } else if (pointer < len) {
                return null;
            }
            address[pieceIndex] = value;
            pieceIndex++;
        }
        if (compress != -1) {
            int swaps = pieceIndex - compress;
            pieceIndex = 7;
            while (pieceIndex != 0 && swaps > 0) {
                int tmp = address[compress + swaps - 1];
                address[compress + swaps - 1] = address[pieceIndex];
                address[pieceIndex] = tmp;
                pieceIndex--;
                swaps--;
            }
        } else if (pieceIndex != 8) {
            return null;
        }
        int longestStart = -1;
        int longestLength = 1;
        for (int i = 0; i < 8; ) {
            if (address[i] != 0) {
                i++;
                continue;
            }
            int runStart = i;
            while (i < 8 && address[i] == 0) {
                i++;
            }
            if (i - runStart > longestLength) {
                longestStart = runStart;
                longestLength = i - runStart;
            }
        }
        StringBuilder out = new StringBuilder();
        boolean ignore0 = false;
        for (int i = 0; i < 8; i++) {
            if (ignore0 && address[i] == 0) {
                continue;
            }
            ignore0 = false;
            if (longestStart == i) {
                out.append(i == 0 ? "::" : ":");
                ignore0 = true;
                continue;
            }
            out.append(Integer.toHexString(address[i]));
            if (i != 7) {
                out.append(':');
            }
        }
        return out.toString();
    }

    private static boolean isAsciiAlpha(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isAsciiDigit(int c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isAsciiAlphanumeric(int c) {
        return isAsciiAlpha(c) || isAsciiDigit(c);
    }

    private static final boolean[] C0_CONTROL_SET = new boolean[128];
    private static final boolean[] FRAGMENT_SET = new boolean[128];
    private static final boolean[] QUERY_SET = new boolean[128];
    private static final boolean[] SPECIAL_QUERY_SET = new boolean[128];
    private static final boolean[] PATH_SET = new boolean[128];
    private static final boolean[] USERINFO_SET = new boolean[128];

    static {
        for (int i = 0; i <= 0x1F; i++) {
            C0_CONTROL_SET[i] = true;
        }
        C0_CONTROL_SET[0x7F] = true;
        System.arraycopy(C0_CONTROL_SET, 0, FRAGMENT_SET, 0, 128);
        for (char c : " \"<>`".toCharArray()) {
            FRAGMENT_SET[c] = true;
        }
        System.arraycopy(C0_CONTROL_SET, 0, QUERY_SET, 0, 128);
        for (char c : " \"#<>".toCharArray()) {
            QUERY_SET[c] = true;
        }
        System.arraycopy(QUERY_SET, 0, SPECIAL_QUERY_SET, 0, 128);
        SPECIAL_QUERY_SET['\''] = true;
        System.arraycopy(QUERY_SET, 0, PATH_SET, 0, 128);
        for (char c : "?^`{}".toCharArray()) {
            PATH_SET[c] = true;
        }
        System.arraycopy(PATH_SET, 0, USERINFO_SET, 0, 128);
        for (char c : "/:;=@[\\]|".toCharArray()) {
            USERINFO_SET[c] = true;
        }
    }

    private static void percentEncode(StringBuilder out, int codePoint, boolean[] set) {
        if (codePoint < 128 && !set[codePoint]) {
            out.append((char) codePoint);
            return;
        }
        if (codePoint < 128) {
            out.append(String.format("%%%02X", codePoint));
            return;
        }
        byte[] bytes = new String(Character.toChars(codePoint)).getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            out.append(String.format("%%%02X", b & 0xFF));
        }
    }

    public static byte[] percentDecode(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            if (b == '%' && i + 2 < bytes.length && Character.digit(bytes[i + 1], 16) >= 0 &&
                Character.digit(bytes[i + 2], 16) >= 0) {
                out.write(Character.digit(bytes[i + 1], 16) * 16 + Character.digit(bytes[i + 2], 16));
                i += 2;
            } else {
                out.write(b);
            }
        }
        return out.toByteArray();
    }
}
