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

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.tree.TypeUtils;

public interface TypeNameMatcher {
    boolean matches(String typeName);
    String getPattern();

    static TypeNameMatcher fromPattern(String pattern) {
        if (pattern.contains("*") || pattern.contains("..")) {
            return new PatternTypeNameMatcher(new AspectJMatcher(pattern, null));
        } else {
            return new ExactTypeNameMatcher(pattern);
        }
    }
}

class ExactTypeNameMatcher implements TypeNameMatcher {
    private final String pattern;

    ExactTypeNameMatcher(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public boolean matches(String typeName) {
        return TypeUtils.fullyQualifiedNamesAreEqual(pattern, typeName);
    }

    @Override
    public String getPattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return pattern;
    }
}

class PatternTypeNameMatcher implements TypeNameMatcher {
    private final AspectJMatcher delegate;

    PatternTypeNameMatcher(AspectJMatcher delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean matches(String typeName) {
        return delegate.matchesType(typeName);
    }

    @Override
    public String getPattern() {
        return delegate.getPattern();
    }

    boolean isFullWildcard() {
        return delegate.getPatternType() == AspectJMatcher.PatternType.FullWildcard;
    }

    AspectJMatcher getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return getPattern();
    }
}

class AspectJMatcher {
    enum PatternType {
        FullWildcard,
        Exact,
        PackagePrefix,
        Wildcard
    }

    private final String pattern;
    private final PatternType patternType;

    AspectJMatcher(String pattern, @Nullable PatternType explicitType) {
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

    String getPattern() {
        return pattern;
    }

    PatternType getPatternType() {
        return patternType;
    }

    boolean matchesType(String text) {
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

    boolean matchesMethod(String methodName) {
        if (patternType == PatternType.Exact) {
            return pattern.equals(methodName);
        } else if (patternType == PatternType.FullWildcard) {
            return true;
        }

        return matchesMethodPattern(pattern, methodName, 0, 0);
    }

    private boolean matchesMethodPattern(String pattern, String text, int pIdx, int tIdx) {
        int pLength = pattern.length(), tLength = text.length();
        while (pIdx < pLength) {
            if (tIdx >= tLength) {
                while (pIdx < pLength) {
                    if (pattern.charAt(pIdx) != '*') {
                        return false;
                    }
                    pIdx++;
                }
                return true;
            }

            char p = pattern.charAt(pIdx);

            if (p == '*') {
                pIdx++;
                if (pIdx >= pLength) {
                    return true;
                }

                if (matchesMethodPattern(pattern, text, pIdx, tIdx)) {
                    return true;
                }
                while (tIdx < tLength) {
                    tIdx++;
                    if (matchesMethodPattern(pattern, text, pIdx, tIdx)) {
                        return true;
                    }
                }
                return false;
            } else {
                if (pattern.charAt(pIdx) != text.charAt(tIdx)) {
                    return false;
                }
                pIdx++;
                tIdx++;
            }
        }

        return tIdx == tLength;
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
        return getPattern();
    }
}