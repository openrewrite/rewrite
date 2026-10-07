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
package org.openrewrite.javascript.internal.lock.berry;

import java.util.regex.Pattern;

/**
 * A workspace pattern as fast-glob matches a directory relative to the workspace ({@code dot: false}): literal
 * segments, {@code *}, {@code ?}, a {@code **} segment, a bracket class and a brace list. Any other glob syntax
 * (extglobs, escapes, brace ranges) is not ported and is reported as something the engine cannot know.
 */
final class GlobPattern {

    private static final String SEGMENT_CHAR = "[^/]";

    private GlobPattern() {
    }

    static Pattern compile(String glob) {
        String pattern = glob;
        while (pattern.startsWith("./")) {
            pattern = pattern.substring(2);
        }
        while (pattern.endsWith("/") && pattern.length() > 1) {
            pattern = pattern.substring(0, pattern.length() - 1);
        }
        if (pattern.isEmpty() || ".".equals(pattern) || pattern.contains("\\") ||
            pattern.matches(".*[@!+*?]\\(.*")) {
            throw new CannotKnow("the directories the workspace pattern " + glob + " matches (fast-glob is not " +
                                 "ported for this syntax)");
        }
        StringBuilder regex = new StringBuilder();
        String[] segments = pattern.split("/", -1);
        for (int s = 0; s < segments.length; s++) {
            String segment = segments[s];
            boolean last = s == segments.length - 1;
            if ("**".equals(segment)) {
                regex.append(last ? "(?:[^/.][^/]*)(?:/[^/.][^/]*)*" : "(?:[^/.][^/]*/)*");
                continue;
            }
            regex.append(segment(segment, glob));
            if (!last) {
                regex.append('/');
            }
        }
        return Pattern.compile(regex.toString());
    }

    private static String segment(String segment, String glob) {
        StringBuilder out = new StringBuilder();
        if (!segment.startsWith(".")) {
            out.append("(?!\\.)");
        }
        int braces = 0;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            switch (c) {
                case '*':
                    if (i + 1 < segment.length() && segment.charAt(i + 1) == '*') {
                        throw new CannotKnow("the directories the workspace pattern " + glob + " matches (a ** " +
                                             "inside a segment)");
                    }
                    out.append(SEGMENT_CHAR).append('*');
                    break;
                case '?':
                    out.append(SEGMENT_CHAR);
                    break;
                case '[': {
                    int end = segment.indexOf(']', i + 2);
                    if (end == -1) {
                        out.append("\\[");
                        break;
                    }
                    String body = segment.substring(i + 1, end);
                    if (body.startsWith("!") || body.startsWith("^")) {
                        body = "^" + body.substring(1);
                    }
                    out.append('[').append(body.replace("\\", "\\\\").replace("[", "\\[")).append(']');
                    i = end;
                    break;
                }
                case '{':
                    braces++;
                    out.append("(?:");
                    break;
                case '}':
                    if (braces == 0) {
                        out.append("\\}");
                    } else {
                        braces--;
                        out.append(')');
                    }
                    break;
                case ',':
                    out.append(braces > 0 ? "|" : ",");
                    break;
                default:
                    if (braces > 0 && c == '.' && i + 1 < segment.length() && segment.charAt(i + 1) == '.') {
                        throw new CannotKnow("the directories the workspace pattern " + glob + " matches (a brace " +
                                             "range)");
                    }
                    out.append(Pattern.quote(String.valueOf(c)));
            }
        }
        if (braces != 0) {
            throw new CannotKnow("the directories the workspace pattern " + glob + " matches (an unbalanced brace)");
        }
        return out.toString();
    }
}
