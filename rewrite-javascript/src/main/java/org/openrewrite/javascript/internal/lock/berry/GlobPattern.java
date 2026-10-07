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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A workspace pattern as fast-glob 3.3.3 matches a directory relative to the workspace ({@code dot: false}) and
 * spells it in its results: literal segments, {@code *}, {@code ?}, a {@code **} segment, a bracket class, a brace
 * list and one leading {@code ./}. Any other glob syntax (extglobs, escapes, brace ranges, {@code .} and {@code ..}
 * segments, absolute patterns) is not ported and is reported as something the engine cannot know.
 */
final class GlobPattern {

    private static final String SEGMENT_CHAR = "[^/]";
    private static final Pattern DOUBLE_SLASH = Pattern.compile("(?!^)/{2,}");

    private GlobPattern() {
    }

    /**
     * fast-glob's processing of a pattern before it makes tasks: repeated slashes collapsed and brace lists
     * expanded as {@code micromatch.braces} with {@code expand} does, duplicates and empty results dropped.
     */
    static List<String> process(String glob) {
        Set<String> out = new LinkedHashSet<>();
        for (String expanded : expandBraces(DOUBLE_SLASH.matcher(glob).replaceAll("/"), glob)) {
            String pattern = DOUBLE_SLASH.matcher(expanded).replaceAll("/");
            if (!pattern.isEmpty()) {
                out.add(pattern);
            }
        }
        return new ArrayList<>(out);
    }

    private static List<String> expandBraces(String pattern, String glob) {
        int open = -1;
        int depth = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '{' && depth++ == 0) {
                open = i;
            } else if (c == '}' && depth > 0 && --depth == 0) {
                List<String> alternatives = splitTopLevel(pattern.substring(open + 1, i));
                if (alternatives.size() < 2) {
                    throw new CannotKnow("the directories the workspace pattern " + glob + " matches (a brace " +
                                         "without a list)");
                }
                List<String> out = new ArrayList<>();
                for (String alternative : alternatives) {
                    out.addAll(expandBraces(pattern.substring(0, open) + alternative + pattern.substring(i + 1),
                            glob));
                }
                return out;
            }
        }
        return Collections.singletonList(pattern);
    }

    private static List<String> splitTopLevel(String body) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            } else if (c == ',' && depth == 0) {
                out.add(body.substring(start, i));
                start = i + 1;
            }
        }
        out.add(body.substring(start));
        return out;
    }

    /**
     * A processed pattern with glob syntax, which fast-glob walks for; any other it stats as written.
     */
    static boolean isDynamic(String pattern) {
        return pattern.indexOf('*') >= 0 || pattern.indexOf('?') >= 0 || pattern.indexOf('{') >= 0 ||
               pattern.matches(".*\\[[^\\[]*].*");
    }

    /**
     * glob-parent's base directory of a processed pattern: the leading segments without glob syntax, never the
     * last one; {@code .} when there are none.
     */
    static String base(String pattern) {
        String[] segments = pattern.split("/", -1);
        StringBuilder base = new StringBuilder();
        for (int s = 0; s < segments.length - 1 && !isDynamic(segments[s]); s++) {
            base.append(s > 0 ? "/" : "").append(segments[s]);
        }
        return base.length() == 0 ? "." : base.toString();
    }

    static Pattern compile(String glob) {
        String pattern = DOUBLE_SLASH.matcher(glob).replaceAll("/");
        if (pattern.startsWith("./")) {
            pattern = pattern.substring(2);
        }
        while (pattern.endsWith("/") && pattern.length() > 1) {
            pattern = pattern.substring(0, pattern.length() - 1);
        }
        if (pattern.isEmpty() || pattern.startsWith("/") || pattern.contains("\\") ||
            pattern.matches("(?:.*/)?\\.\\.?(?:/.*)?") || pattern.matches(".*[@!+*?]\\(.*")) {
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
