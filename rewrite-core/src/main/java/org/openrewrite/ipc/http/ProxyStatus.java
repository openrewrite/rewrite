/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.ipc.http;

import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import static java.util.Collections.emptyList;

/**
 * One member of an <a href="https://www.rfc-editor.org/rfc/rfc9209">RFC 9209</a> {@code Proxy-Status} response
 * header: how an intermediary handled the request, and what its next hop answered.
 */
@Value
public class ProxyStatus {
    private static final String HEADER = "Proxy-Status";

    String identifier;

    @Nullable
    String nextHop;

    @Nullable
    String error;

    @Nullable
    Integer receivedStatus;

    @Nullable
    String details;

    /**
     * @return The members of every {@code Proxy-Status} field line, in field order, or none when the field is absent
     * or malformed.
     */
    public static List<ProxyStatus> parse(Map<String, List<String>> headers) {
        StringJoiner field = new StringJoiner(",");
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {
            if (HEADER.equalsIgnoreCase(header.getKey())) {
                for (String line : header.getValue()) {
                    if (line != null && !line.trim().isEmpty()) {
                        field.add(line);
                    }
                }
            }
        }
        try {
            return new Parser(field.toString()).list();
        } catch (IllegalArgumentException malformed) {
            return emptyList();
        }
    }

    /**
     * The members that report an outcome, each on its own line under the response they were proxied for:
     * <pre>
     * , proxying:
     *   https://repo.example.com/maven: HTTP 404
     *   https://mirror.example.com/maven: connection_timeout
     * </pre>
     *
     * @return The block to append to the proxy's own response, or an empty string when no member reports an outcome.
     */
    public static String render(List<ProxyStatus> members) {
        StringJoiner rendered = new StringJoiner("\n  ", ", proxying:\n  ", "").setEmptyValue("");
        for (ProxyStatus member : members) {
            if (member.error != null || member.receivedStatus != null) {
                rendered.add(member.toString());
            }
        }
        return rendered.toString();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(nextHop == null ? identifier : nextHop);
        if (error != null) {
            s.append(": ").append(error);
        } else if (receivedStatus != null) {
            s.append(": HTTP ").append(receivedStatus);
        }
        if (details != null) {
            s.append(" - ").append(details);
        }
        return s.toString();
    }

    /**
     * The subset of Structured Field Values (RFC 9651) that a list of items with parameters needs.
     */
    private static class Parser {
        private final String input;
        private int pos;

        Parser(String input) {
            this.input = input;
        }

        List<ProxyStatus> list() {
            List<ProxyStatus> members = new ArrayList<>();
            skipWhitespace();
            while (pos < input.length()) {
                members.add(member());
                skipWhitespace();
                if (pos == input.length()) {
                    break;
                }
                expect(',');
                skipWhitespace();
                if (pos == input.length()) {
                    throw malformed();
                }
            }
            return members;
        }

        private ProxyStatus member() {
            Object identifier = bareItem();
            if (!(identifier instanceof String)) {
                throw malformed();
            }
            String nextHop = null;
            String error = null;
            Integer receivedStatus = null;
            String details = null;
            while (pos < input.length() && input.charAt(pos) == ';') {
                pos++;
                while (pos < input.length() && input.charAt(pos) == ' ') {
                    pos++;
                }
                String key = key();
                Object value = Boolean.TRUE;
                if (pos < input.length() && input.charAt(pos) == '=') {
                    pos++;
                    value = bareItem();
                }
                switch (key) {
                    case "next-hop":
                        nextHop = value instanceof String ? (String) value : null;
                        break;
                    case "error":
                        error = value instanceof String ? (String) value : null;
                        break;
                    case "received-status":
                        receivedStatus = value instanceof Long ? ((Long) value).intValue() : null;
                        break;
                    case "details":
                        details = value instanceof String ? (String) value : null;
                        break;
                    default:
                        break;
                }
            }
            return new ProxyStatus((String) identifier, nextHop, error, receivedStatus, details);
        }

        private String key() {
            int start = pos;
            if (pos < input.length() && (isLowerAlpha(input.charAt(pos)) || input.charAt(pos) == '*')) {
                pos++;
                while (pos < input.length() && isKeyChar(input.charAt(pos))) {
                    pos++;
                }
            }
            if (start == pos) {
                throw malformed();
            }
            return input.substring(start, pos);
        }

        private Object bareItem() {
            if (pos == input.length()) {
                throw malformed();
            }
            char c = input.charAt(pos);
            if (c == '"') {
                return string();
            } else if (c == '%' && pos + 1 < input.length() && input.charAt(pos + 1) == '"') {
                pos++;
                return string();
            } else if (c == '-' || isDigit(c)) {
                return number();
            } else if (c == '@') {
                pos++;
                return number();
            } else if (c == '?') {
                if (pos + 1 == input.length() || (input.charAt(pos + 1) != '0' && input.charAt(pos + 1) != '1')) {
                    throw malformed();
                }
                pos += 2;
                return input.charAt(pos - 1) == '1';
            } else if (c == ':') {
                int end = input.indexOf(':', pos + 1);
                if (end < 0) {
                    throw malformed();
                }
                String bytes = input.substring(pos + 1, end);
                pos = end + 1;
                return bytes;
            } else if (isAlpha(c) || c == '*') {
                return token();
            }
            throw malformed();
        }

        private String string() {
            StringBuilder s = new StringBuilder();
            pos++;
            while (pos < input.length()) {
                char c = input.charAt(pos++);
                if (c == '\\') {
                    if (pos == input.length()) {
                        throw malformed();
                    }
                    s.append(input.charAt(pos++));
                } else if (c == '"') {
                    return s.toString();
                } else {
                    s.append(c);
                }
            }
            throw malformed();
        }

        private Object number() {
            int start = pos;
            if (input.charAt(pos) == '-') {
                pos++;
            }
            while (pos < input.length() && (isDigit(input.charAt(pos)) || input.charAt(pos) == '.')) {
                pos++;
            }
            String number = input.substring(start, pos);
            try {
                if (number.indexOf('.') < 0) {
                    return Long.parseLong(number);
                }
                return Double.parseDouble(number);
            } catch (NumberFormatException e) {
                throw malformed();
            }
        }

        private String token() {
            int start = pos++;
            while (pos < input.length() && isTokenChar(input.charAt(pos))) {
                pos++;
            }
            return input.substring(start, pos);
        }

        private void expect(char c) {
            if (pos == input.length() || input.charAt(pos) != c) {
                throw malformed();
            }
            pos++;
        }

        private void skipWhitespace() {
            while (pos < input.length() && (input.charAt(pos) == ' ' || input.charAt(pos) == '\t')) {
                pos++;
            }
        }

        private static boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }

        private static boolean isLowerAlpha(char c) {
            return c >= 'a' && c <= 'z';
        }

        private static boolean isAlpha(char c) {
            return isLowerAlpha(c) || (c >= 'A' && c <= 'Z');
        }

        private static boolean isKeyChar(char c) {
            return isLowerAlpha(c) || isDigit(c) || c == '_' || c == '-' || c == '.' || c == '*';
        }

        private static boolean isTokenChar(char c) {
            return isAlpha(c) || isDigit(c) || "!#$%&'*+-.^_`|~:/".indexOf(c) >= 0;
        }

        private static IllegalArgumentException malformed() {
            return new IllegalArgumentException("Malformed " + HEADER + " field");
        }
    }
}
