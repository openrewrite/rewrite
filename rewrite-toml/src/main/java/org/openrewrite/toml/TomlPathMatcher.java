/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.toml;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.toml.tree.Toml;
import org.openrewrite.toml.tree.TomlKey;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;

/**
 * A utility for matching TOML paths similar to JsonPathMatcher.
 * Supports paths like:
 * - "package.name" for nested keys
 * - "[section]" for table sections
 * - "[[array]]" for array tables
 * - Wildcards: "*" matches any single key, "**" matches any path
 */
public class TomlPathMatcher {
    private final List<PathSegment> segments;

    public TomlPathMatcher(String path) {
        this.segments = parsePath(path);
    }

    public boolean matches(Cursor cursor) {
        List<String> actualPath = buildPath(cursor);
        return matchesPath(actualPath);
    }

    private boolean matchesPath(List<String> actualPath) {
        if (segments.isEmpty()) {
            return actualPath.isEmpty();
        }

        int segmentIndex = 0;
        int pathIndex = 0;

        while (segmentIndex < segments.size() && pathIndex < actualPath.size()) {
            PathSegment segment = segments.get(segmentIndex);

            if (segment.isDoubleWildcard) {
                if (segmentIndex == segments.size() - 1) {
                    return true; // Terminal ** matches everything
                }

                PathSegment nextSegment = segments.get(segmentIndex + 1);
                boolean foundMatch = false;
                for (int i = pathIndex; i <= actualPath.size(); i++) {
                    if (i < actualPath.size() && nextSegment.matches(actualPath.get(i))) {
                        segmentIndex += 2;
                        pathIndex = i + 1;
                        foundMatch = true;
                        break;
                    }
                }
                if (!foundMatch) {
                    return false;
                }
            } else if (segment.matches(actualPath.get(pathIndex))) {
                segmentIndex++;
                pathIndex++;
            } else {
                return false;
            }
        }

        return segmentIndex == segments.size() && pathIndex == actualPath.size();
    }

    private List<String> buildPath(Cursor cursor) {
        List<String> path = new ArrayList<>();

        Cursor current = cursor;
        while (current != null) {
            Object value = current.getValue();

            if (value instanceof Toml.KeyValue) {
                Toml.KeyValue kv = (Toml.KeyValue) value;
                TomlKey key = kv.getKey();
                if (key instanceof Toml.Identifier) {
                    String keyName = ((Toml.Identifier) key).getName();
                    Cursor parent = current.getParent();
                    while (parent != null) {
                        Object parentValue = parent.getValue();
                        if (parentValue instanceof Toml.Table) {
                            Toml.Table table = (Toml.Table) parentValue;
                            if (table.getName() != null) {
                                String tableName = table.getName().getName();
                                // Split dotted names: [tool.poetry]
                                String[] parts = tableName.split("\\.");
                                for (int i = parts.length - 1; i >= 0; i--) {
                                    path.add(0, parts[i].trim());
                                }
                            }
                            break;
                        }
                        parent = parent.getParent();
                    }
                    path.add(keyName);
                    return path;
                }
            }

            current = current.getParent();
        }

        return path;
    }

    private List<PathSegment> parsePath(String path) {
        path = path.trim();
        if (path.isEmpty()) {
            return emptyList();
        }

        if (path.startsWith("[") && path.endsWith("]")) {
            path = path.substring(1, path.length() - 1);
        }

        List<String> parts = new ArrayList<>();
        int i = 0;
        while (i < path.length()) {
            if (path.charAt(i) == '"' || path.charAt(i) == '\'') {
                char quote = path.charAt(i);
                int end = path.indexOf(quote, i + 1);
                if (end != -1) {
                    parts.add(path.substring(i + 1, end));
                    i = end + 1;
                    if (i < path.length() && path.charAt(i) == '.') {
                        i++; // Skip dot after quote
                    }
                } else {
                    i++;
                }
            } else {
                int nextDot = path.indexOf('.', i);
                if (nextDot == -1) {
                    parts.add(path.substring(i).trim());
                    break;
                } else {
                    parts.add(path.substring(i, nextDot).trim());
                    i = nextDot + 1;
                }
            }
        }

        List<PathSegment> result = new ArrayList<>();
        for (String part : parts) {
            switch (part) {
                case "":
                    continue;
                case "**":
                    result.add(new PathSegment(null, false, true));
                    break;
                case "*":
                    result.add(new PathSegment(null, true, false));
                    break;
                default:
                    result.add(new PathSegment(part, false, false));
                    break;
            }
        }

        return result;
    }

    private static class PathSegment {
        final @Nullable String literal;
        final boolean isSingleWildcard;
        final boolean isDoubleWildcard;
        final @Nullable Pattern pattern;

        PathSegment(@Nullable String literal, boolean isSingleWildcard, boolean isDoubleWildcard) {
            this.literal = literal;
            this.isSingleWildcard = isSingleWildcard;
            this.isDoubleWildcard = isDoubleWildcard;

            if (literal != null && !isSingleWildcard && !isDoubleWildcard) {
                String regex = literal.replace("*", ".*").replace("?", ".");
                this.pattern = Pattern.compile("^" + regex + "$");
            } else {
                this.pattern = null;
            }
        }

        boolean matches(String key) {
            if (isSingleWildcard || isDoubleWildcard) {
                return true;
            }
            if (pattern != null) {
                return pattern.matcher(key).matches();
            }
            return literal != null && literal.equals(key);
        }
    }
}
