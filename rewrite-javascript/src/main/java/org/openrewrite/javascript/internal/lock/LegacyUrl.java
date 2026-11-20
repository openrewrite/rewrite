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

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.net.IDN;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of Node's legacy {@code url.parse(url)} (lib/url.js, Node 24.21.0), without its {@code parseQueryString} and
 * {@code slashesDenoteHost} arguments: the parts of a URL as npm's older libraries read them (hosted-git-info 3,
 * version-from-tgz before npm 10.5.0). A field Node leaves {@code null} is null.
 */
@Getter
public final class LegacyUrl {

    private static final Pattern PROTOCOL = Pattern.compile("^[a-z0-9.+-]+:", Pattern.CASE_INSENSITIVE);
    private static final Pattern PORT = Pattern.compile(":[0-9]*$");
    private static final Pattern HOST = Pattern.compile("^//[^@/]+@[^@/]+");
    private static final Pattern SIMPLE_PATH = V8RegExp.of("^(\\/\\/?(?!\\/)[^?\\s]*)(\\?[^\\s]*)?$");
    private static final Pattern FORBIDDEN_HOST_CHARS = Pattern.compile("[\\x00\\t\\n\\r #%/:<>?@\\[\\\\\\]^|]");
    private static final Pattern FORBIDDEN_HOST_CHARS_IPV6 = Pattern.compile("[\\x00\\t\\n\\r #%/<>?@\\\\^|]");
    private static final int HOSTNAME_MAX_LEN = 255;
    private static final Set<String> UNSAFE_PROTOCOL = new HashSet<>(Arrays.asList("javascript", "javascript:"));
    private static final Set<String> HOSTLESS_PROTOCOL = new HashSet<>(Arrays.asList("javascript", "javascript:"));
    private static final Set<String> SLASHED_PROTOCOL = new HashSet<>(Arrays.asList("http", "http:", "https",
            "https:", "ftp", "ftp:", "gopher", "gopher:", "file", "file:", "ws", "ws:", "wss", "wss:"));
    private static final String[] ESCAPED_CODES = new String[126];

    static {
        ESCAPED_CODES[9] = "%09";
        ESCAPED_CODES[10] = "%0A";
        ESCAPED_CODES[13] = "%0D";
        ESCAPED_CODES[32] = "%20";
        ESCAPED_CODES[34] = "%22";
        ESCAPED_CODES[39] = "%27";
        ESCAPED_CODES[60] = "%3C";
        ESCAPED_CODES[62] = "%3E";
        ESCAPED_CODES[92] = "%5C";
        ESCAPED_CODES[94] = "%5E";
        ESCAPED_CODES[96] = "%60";
        ESCAPED_CODES[123] = "%7B";
        ESCAPED_CODES[124] = "%7C";
        ESCAPED_CODES[125] = "%7D";
    }

    private @Nullable String protocol;
    private @Nullable Boolean slashes;
    private @Nullable String auth;
    private @Nullable String host;
    private @Nullable String port;
    private @Nullable String hostname;
    private @Nullable String hash;
    private @Nullable String search;
    private @Nullable String query;
    private @Nullable String pathname;
    private @Nullable String path;

    private LegacyUrl() {
    }

    /**
     * @throws JsError the {@code URIError} of an auth {@code decodeURIComponent} refuses, and Node's
     *                 {@code ERR_INVALID_URL} for a host name IDNA turns into one with a forbidden character
     */
    public static LegacyUrl parse(String url) {
        LegacyUrl u = new LegacyUrl();
        u.parseUrl(url);
        return u;
    }

    private static boolean isWs(char code) {
        return code < 33 || code == ' ' || code == '﻿';
    }

    private void parseUrl(String url) {
        boolean hasHash = false;
        boolean hasAt = false;
        int start = -1;
        int end = -1;
        StringBuilder restBuilder = new StringBuilder();
        int lastPos = 0;
        boolean inWs = false;
        boolean split = false;
        for (int i = 0; i < url.length(); ++i) {
            char code = url.charAt(i);
            boolean isWs = isWs(code);
            if (start == -1) {
                if (isWs) {
                    continue;
                }
                lastPos = start = i;
            } else if (inWs) {
                if (!isWs) {
                    end = -1;
                    inWs = false;
                }
            } else if (isWs) {
                end = i;
                inWs = true;
            }
            if (!split) {
                switch (code) {
                    case '@':
                        hasAt = true;
                        break;
                    case '#':
                        hasHash = true;
                        split = true;
                        break;
                    case '?':
                        split = true;
                        break;
                    case '\\':
                        if (i - lastPos > 0) {
                            restBuilder.append(url, lastPos, i);
                        }
                        restBuilder.append('/');
                        lastPos = i + 1;
                        break;
                    default:
                        break;
                }
            } else if (!hasHash && code == '#') {
                hasHash = true;
            }
        }

        String rest = restBuilder.toString();
        if (start != -1) {
            if (lastPos == start) {
                if (end == -1) {
                    rest = start == 0 ? url : url.substring(start);
                } else {
                    rest = url.substring(start, end);
                }
            } else if (end == -1 && lastPos < url.length()) {
                rest += url.substring(lastPos);
            } else if (end != -1 && lastPos < end) {
                rest += url.substring(lastPos, end);
            }
        }

        if (!hasHash && !hasAt) {
            Matcher simplePath = SIMPLE_PATH.matcher(rest);
            if (simplePath.find()) {
                path = rest;
                pathname = simplePath.group(1);
                if (simplePath.group(2) != null && !simplePath.group(2).isEmpty()) {
                    search = simplePath.group(2);
                    query = search.substring(1);
                }
                return;
            }
        }

        String proto = null;
        String lowerProto = null;
        Matcher protoMatch = PROTOCOL.matcher(rest);
        if (protoMatch.find()) {
            proto = protoMatch.group();
            lowerProto = proto.toLowerCase(Locale.ROOT);
            protocol = lowerProto;
            rest = rest.substring(proto.length());
        }

        boolean hasSlashes = false;
        if (proto != null || HOST.matcher(rest).find()) {
            hasSlashes = rest.length() > 1 && rest.charAt(0) == '/' && rest.charAt(1) == '/';
            if (hasSlashes && !(proto != null && HOSTLESS_PROTOCOL.contains(lowerProto))) {
                rest = rest.substring(2);
                slashes = true;
            }
        }

        if (!(lowerProto != null && HOSTLESS_PROTOCOL.contains(lowerProto)) &&
            (hasSlashes || (proto != null && !SLASHED_PROTOCOL.contains(proto)))) {
            int hostEnd = -1;
            int atSign = -1;
            int nonHost = -1;
            for (int i = 0; i < rest.length(); ++i) {
                switch (rest.charAt(i)) {
                    case '\t':
                    case '\n':
                    case '\r':
                        rest = rest.substring(0, i) + rest.substring(i + 1);
                        i -= 1;
                        break;
                    case ' ':
                    case '"':
                    case '%':
                    case '\'':
                    case ';':
                    case '<':
                    case '>':
                    case '\\':
                    case '^':
                    case '`':
                    case '{':
                    case '|':
                    case '}':
                        if (nonHost == -1) {
                            nonHost = i;
                        }
                        break;
                    case '#':
                    case '/':
                    case '?':
                        if (nonHost == -1) {
                            nonHost = i;
                        }
                        hostEnd = i;
                        break;
                    case '@':
                        atSign = i;
                        nonHost = -1;
                        break;
                    default:
                        break;
                }
                if (hostEnd != -1) {
                    break;
                }
            }
            int hostStart = 0;
            if (atSign != -1) {
                try {
                    auth = UriComponent.decodeURIComponent(rest.substring(0, atSign));
                } catch (IllegalArgumentException e) {
                    throw new JsError(JsError.URI_ERROR, "URI malformed");
                }
                hostStart = atSign + 1;
            }
            if (nonHost == -1) {
                host = rest.substring(hostStart);
                rest = "";
            } else {
                host = rest.substring(hostStart, nonHost);
                rest = rest.substring(nonHost);
            }

            parseHost();

            if (hostname == null) {
                hostname = "";
            }
            String name = hostname;
            boolean ipv6Hostname = name.length() > 1 && name.charAt(0) == '[' && name.charAt(name.length() - 1) == ']';
            if (!ipv6Hostname) {
                rest = getHostname(rest, name);
            }

            if (hostname.length() > HOSTNAME_MAX_LEN) {
                hostname = "";
            } else {
                hostname = hostname.toLowerCase(Locale.ROOT);
            }

            if (!hostname.isEmpty()) {
                if (ipv6Hostname) {
                    if (FORBIDDEN_HOST_CHARS_IPV6.matcher(hostname).find()) {
                        throw invalidUrl();
                    }
                } else {
                    hostname = toAscii(hostname);
                    if (hostname.isEmpty() || FORBIDDEN_HOST_CHARS.matcher(hostname).find()) {
                        throw invalidUrl();
                    }
                }
            }

            host = (hostname.isEmpty() ? "" : hostname) + (port != null && !port.isEmpty() ? ":" + port : "");

            if (ipv6Hostname) {
                hostname = hostname.substring(1, hostname.length() - 1);
                if (rest.isEmpty() || rest.charAt(0) != '/') {
                    rest = "/" + rest;
                }
            }
        }

        if (lowerProto == null || !UNSAFE_PROTOCOL.contains(lowerProto)) {
            rest = autoEscapeStr(rest);
        }

        int questionIdx = -1;
        int hashIdx = -1;
        for (int i = 0; i < rest.length(); ++i) {
            char code = rest.charAt(i);
            if (code == '#') {
                hash = rest.substring(i);
                hashIdx = i;
                break;
            } else if (code == '?' && questionIdx == -1) {
                questionIdx = i;
            }
        }

        if (questionIdx != -1) {
            if (hashIdx == -1) {
                search = rest.substring(questionIdx);
                query = rest.substring(questionIdx + 1);
            } else {
                search = rest.substring(questionIdx, hashIdx);
                query = rest.substring(questionIdx + 1, hashIdx);
            }
        }

        boolean useQuestionIdx = questionIdx != -1 && (hashIdx == -1 || questionIdx < hashIdx);
        int firstIdx = useQuestionIdx ? questionIdx : hashIdx;
        if (firstIdx == -1) {
            if (!rest.isEmpty()) {
                pathname = rest;
            }
        } else if (firstIdx > 0) {
            pathname = rest.substring(0, firstIdx);
        }
        if (lowerProto != null && SLASHED_PROTOCOL.contains(lowerProto) && hostname != null &&
            !hostname.isEmpty() && (pathname == null || pathname.isEmpty())) {
            pathname = "/";
        }

        if ((pathname != null && !pathname.isEmpty()) || (search != null && !search.isEmpty())) {
            path = (pathname == null ? "" : pathname) + (search == null ? "" : search);
        }
    }

    private void parseHost() {
        String h = host;
        Matcher portMatch = PORT.matcher(h);
        if (portMatch.find()) {
            String p = portMatch.group();
            if (!":".equals(p)) {
                port = p.substring(1);
            }
            h = h.substring(0, h.length() - p.length());
        }
        if (!h.isEmpty()) {
            hostname = h;
        }
    }

    // getHostname: a host name ends at a character no host name has; what follows it starts the path
    private String getHostname(String rest, String name) {
        for (int i = 0; i < name.length(); ++i) {
            char code = name.charAt(i);
            if (code == '/' || code == '\\' || code == '#' || code == '?' || code == ':') {
                hostname = name.substring(0, i);
                return "/" + name.substring(i) + rest;
            }
        }
        return rest;
    }

    // the encoding binding's toASCII: a name with no character beyond ASCII stays as it is
    private static String toAscii(String name) {
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) >= 0x80) {
                try {
                    return IDN.toASCII(name, IDN.ALLOW_UNASSIGNED).toLowerCase(Locale.ROOT);
                } catch (IllegalArgumentException e) {
                    return "";
                }
            }
        }
        return name;
    }

    private static JsError invalidUrl() {
        return new JsError(JsError.TYPE_ERROR, "Invalid URL", "ERR_INVALID_URL");
    }

    private static String autoEscapeStr(String rest) {
        StringBuilder escaped = null;
        int lastEscapedPos = 0;
        for (int i = 0; i < rest.length(); ++i) {
            char c = rest.charAt(i);
            String escapedChar = c < ESCAPED_CODES.length ? ESCAPED_CODES[c] : null;
            if (escapedChar != null) {
                if (escaped == null) {
                    escaped = new StringBuilder();
                }
                if (i > lastEscapedPos) {
                    escaped.append(rest, lastEscapedPos, i);
                }
                escaped.append(escapedChar);
                lastEscapedPos = i + 1;
            }
        }
        if (escaped == null) {
            return rest;
        }
        if (lastEscapedPos < rest.length()) {
            escaped.append(rest, lastEscapedPos, rest.length());
        }
        return escaped.toString();
    }
}
