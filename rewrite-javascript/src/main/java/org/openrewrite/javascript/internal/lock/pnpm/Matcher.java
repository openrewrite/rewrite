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
package org.openrewrite.javascript.internal.lock.pnpm;

import org.openrewrite.javascript.internal.lock.V8RegExp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/config.matcher src/index.ts (pnpm 11.2.2).
 */
final class Matcher {

    private Matcher() {
    }

    static Predicate<String> createMatcher(String pattern) {
        return createMatcher(Collections.singletonList(pattern));
    }

    static Predicate<String> createMatcher(List<String> patterns) {
        ToIntFunction<String> m = createMatcherWithIndex(patterns);
        return input -> m.applyAsInt(input) != -1;
    }

    static ToIntFunction<String> createMatcherWithIndex(List<String> patterns) {
        switch (patterns.size()) {
            case 0:
                return input -> -1;
            case 1:
                return matcherWhenOnlyOnePatternWithIndex(patterns.get(0));
        }
        List<MatcherFunction> matchArr = new ArrayList<>();
        boolean hasIgnore = false;
        boolean hasInclude = false;
        for (String pattern : patterns) {
            if (isIgnorePattern(pattern)) {
                hasIgnore = true;
                matchArr.add(new MatcherFunction(matcherFromPattern(pattern.substring(1)), true));
            } else {
                hasInclude = true;
                matchArr.add(new MatcherFunction(matcherFromPattern(pattern), false));
            }
        }
        if (!hasIgnore) {
            return input -> matchInputWithNonIgnoreMatchers(matchArr, input);
        }
        if (!hasInclude) {
            return input -> matchInputWithoutIgnoreMatchers(matchArr, input);
        }
        return input -> matchInputWithMatchersArray(matchArr, input);
    }

    private static final class MatcherFunction {
        final Predicate<String> match;
        final boolean ignore;

        MatcherFunction(Predicate<String> match, boolean ignore) {
            this.match = match;
            this.ignore = ignore;
        }
    }

    private static int matchInputWithNonIgnoreMatchers(List<MatcherFunction> matchArr, String input) {
        for (int i = 0; i < matchArr.size(); i++) {
            if (matchArr.get(i).match.test(input)) {
                return i;
            }
        }
        return -1;
    }

    private static int matchInputWithoutIgnoreMatchers(List<MatcherFunction> matchArr, String input) {
        for (MatcherFunction m : matchArr) {
            if (m.match.test(input)) {
                return -1;
            }
        }
        return 0;
    }

    private static int matchInputWithMatchersArray(List<MatcherFunction> matchArr, String input) {
        int matchedPatternIndex = -1;
        for (int i = 0; i < matchArr.size(); i++) {
            MatcherFunction m = matchArr.get(i);
            if (m.ignore) {
                if (m.match.test(input)) {
                    matchedPatternIndex = -1;
                }
            } else if (matchedPatternIndex == -1 && m.match.test(input)) {
                matchedPatternIndex = i;
            }
        }
        return matchedPatternIndex;
    }

    private static Predicate<String> matcherFromPattern(String pattern) {
        if ("*".equals(pattern)) {
            return input -> true;
        }
        String escapedPattern = escapeStringRegexp(pattern).replace("\\*", ".*");
        if (escapedPattern.equals(pattern)) {
            return pattern::equals;
        }
        Pattern regexp = V8RegExp.of("^" + escapedPattern + "$");
        return input -> regexp.matcher(input).find();
    }

    // escape-string-regexp 5: | \ { } ( ) [ ] ^ $ + * ? . escaped, - as \x2d
    private static String escapeStringRegexp(String string) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if ("|\\{}()[]^$+*?.".indexOf(c) >= 0) {
                out.append('\\').append(c);
            } else if (c == '-') {
                out.append("\\x2d");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static boolean isIgnorePattern(String pattern) {
        return !pattern.isEmpty() && pattern.charAt(0) == '!';
    }

    private static ToIntFunction<String> matcherWhenOnlyOnePatternWithIndex(String pattern) {
        Predicate<String> m = matcherWhenOnlyOnePattern(pattern);
        return input -> m.test(input) ? 0 : -1;
    }

    private static Predicate<String> matcherWhenOnlyOnePattern(String pattern) {
        if (!isIgnorePattern(pattern)) {
            return matcherFromPattern(pattern);
        }
        Predicate<String> m = matcherFromPattern(pattern.substring(1));
        return input -> !m.test(input);
    }
}
