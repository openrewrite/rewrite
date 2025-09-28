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
package org.openrewrite.java;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Incubating;
import org.openrewrite.java.tree.TypeUtils;

@Incubating(since = "8.63.0")
public interface TypeNameMatcher {
    boolean matches(String typeName);
    boolean matchesSimpleName(String simpleName);

    static TypeNameMatcher fromPattern(String pattern) {
        if (pattern.contains("*") || pattern.contains("..")) {
            return PatternTypeNameMatcher.fromPattern(pattern);
        } else {
            return new ExactTypeNameMatcher(pattern);
        }
    }
}

@RequiredArgsConstructor
class ExactTypeNameMatcher implements TypeNameMatcher {
    private final String pattern;

    @Override
    public boolean matches(String typeName) {
        return TypeUtils.fullyQualifiedNamesAreEqual(pattern, typeName);
    }

    @Override
    public boolean matchesSimpleName(String simpleName) {
        return pattern.equals(simpleName) || pattern.endsWith('.' + simpleName);
    }

    @Override
    public String toString() {
        return pattern;
    }
}

class PatternTypeNameMatcher implements TypeNameMatcher {
    private enum PatternType {
        FullWildcard,
        Exact,
        PackagePrefix,
        Wildcard
    }

    private final String pattern;
    private final PatternType patternType;

    private PatternTypeNameMatcher(String pattern, @Nullable PatternType explicitType) {
        this.pattern = pattern;

        if (explicitType != null) {
            this.patternType = explicitType;
        } else if ("*".equals(pattern) || "*..*".equals(pattern)) {
            this.patternType = PatternType.FullWildcard;
        } else if (!pattern.contains("*") && !pattern.contains("..")) {
            this.patternType = PatternType.Exact;
        } else {
            this.patternType = PatternType.Wildcard;
        }
    }

    static PatternTypeNameMatcher fromPattern(String pattern) {
        return new PatternTypeNameMatcher(pattern, null);
    }

    static PatternTypeNameMatcher fullWildcard(String pattern) {
        return new PatternTypeNameMatcher(pattern, PatternType.FullWildcard);
    }

    static PatternTypeNameMatcher packagePrefix(String pattern) {
        return new PatternTypeNameMatcher(pattern, PatternType.PackagePrefix);
    }

    static PatternTypeNameMatcher wildcard(String pattern) {
        return new PatternTypeNameMatcher(pattern, PatternType.Wildcard);
    }

    boolean isFullWildcard() {
        return patternType == PatternType.FullWildcard;
    }

    @Override
    public boolean matches(String text) {
        switch (patternType) {
            case FullWildcard:
                return true;
            case Exact:
                return TypeUtils.fullyQualifiedNamesAreEqual(pattern, text);
            case PackagePrefix:
                int prefixLen = pattern.length();
                return text.length() > prefixLen && text.startsWith(pattern) && text.charAt(prefixLen) == '.';
            default:
                return matchesPattern(pattern, text, 0, 0);
        }
    }

    @Override
    public boolean matchesSimpleName(String simpleName) {
        // For patterns like com.*.Bar or com..Bar, we want to match just "Bar"
        int lastDot = pattern.lastIndexOf('.');
        if (lastDot > 0 && lastDot < pattern.length() - 1) {
            int lastPartStart = lastDot + 1;
            int lastPartLength = pattern.length() - lastPartStart;

            // Check if last part is just "*"
            if (lastPartLength == 1 && pattern.charAt(lastPartStart) == '*') {
                return true;
            }

            // Check if last part contains wildcards
            boolean hasWildcard = false;
            for (int i = lastPartStart; i < pattern.length(); i++) {
                if (pattern.charAt(i) == '*') {
                    hasWildcard = true;
                    break;
                }
            }

            if (!hasWildcard) {
                // Simple exact match
                return simpleName.length() == lastPartLength &&
                        pattern.regionMatches(lastPartStart, simpleName, 0, lastPartLength);
            } else {
                // Has wildcards - use pattern matching on the substring
                return simpleName.matches(pattern.substring(lastPartStart).replace("*", ".*"));
            }
        }
        return false;
    }

    private boolean matchesPattern(String pattern, String text, int pIdx, int tIdx) {
        int pLength = pattern.length();
        int tLength = text.length();
        while (pIdx < pLength) {
            char p = pattern.charAt(pIdx);

            if (tIdx >= tLength) {
                while (pIdx < pLength) {
                    if (p == '*') {
                        pIdx++;
                    } else if (p == '.' && pIdx + 1 < pLength && pattern.charAt(pIdx + 1) == '.') {
                        pIdx += 2;
                    } else {
                        return false;
                    }
                }
                return true;
            }

            if (p == '*') {
                pIdx++;
                if (pIdx >= pLength) {
                    while (tIdx < tLength) {
                        if (text.charAt(tIdx) == '.') return false;
                        tIdx++;
                    }
                    return true;
                }

                if (matchesPattern(pattern, text, pIdx, tIdx)) {
                    return true;
                }
                while (tIdx < tLength) {
                    if (text.charAt(tIdx) == '.') {
                        return false;
                    }
                    tIdx++;
                    if (matchesPattern(pattern, text, pIdx, tIdx)) {
                        return true;
                    }
                }
                return false;

            } else if (p == '.' && pIdx + 1 < pLength && pattern.charAt(pIdx + 1) == '.') {
                pIdx += 2;
                if (pIdx >= pLength) {
                    return true;
                }

                if (pattern.charAt(pIdx) == '*') {
                    return true;
                }

                while (tIdx <= tLength) {
                    if (matchesPattern(pattern, text, pIdx, tIdx)) {
                        return true;
                    }
                    tIdx++;
                }
                return false;

            } else if (p == '.') {
                if (text.charAt(tIdx) != '.' && text.charAt(tIdx) != '$') {
                    return false;
                }
                pIdx++;
                tIdx++;

            } else {
                if (p != text.charAt(tIdx)) {
                    return false;
                }
                pIdx++;
                tIdx++;
            }
        }

        return tIdx >= tLength;
    }

    @Override
    public String toString() {
        return pattern;
    }
}
