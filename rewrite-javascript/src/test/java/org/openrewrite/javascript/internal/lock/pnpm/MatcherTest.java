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

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * config/matcher/test/index.ts of pnpm 11.2.2.
 */
class MatcherTest {

    @Test
    void matcher() {
        Predicate<String> match = Matcher.createMatcher("*");
        assertThat(match.test("@eslint/plugin-foo")).isTrue();
        assertThat(match.test("express")).isTrue();

        match = Matcher.createMatcher(singletonList("eslint-*"));
        assertThat(match.test("eslint-plugin-foo")).isTrue();
        assertThat(match.test("express")).isFalse();

        match = Matcher.createMatcher(singletonList("*plugin*"));
        assertThat(match.test("@eslint/plugin-foo")).isTrue();
        assertThat(match.test("express")).isFalse();

        match = Matcher.createMatcher(singletonList("a*c"));
        assertThat(match.test("abc")).isTrue();

        match = Matcher.createMatcher(singletonList("*-positive"));
        assertThat(match.test("is-positive")).isTrue();

        match = Matcher.createMatcher(asList("foo", "bar"));
        assertThat(match.test("foo")).isTrue();
        assertThat(match.test("bar")).isTrue();
        assertThat(match.test("express")).isFalse();

        match = Matcher.createMatcher(asList("eslint-*", "!eslint-plugin-bar"));
        assertThat(match.test("eslint-plugin-foo")).isTrue();
        assertThat(match.test("eslint-plugin-bar")).isFalse();

        match = Matcher.createMatcher(asList("!eslint-plugin-bar", "eslint-*"));
        assertThat(match.test("eslint-plugin-foo")).isTrue();
        assertThat(match.test("eslint-plugin-bar")).isTrue();

        match = Matcher.createMatcher(asList("eslint-*", "!eslint-plugin-*", "eslint-plugin-bar"));
        assertThat(match.test("eslint-config-foo")).isTrue();
        assertThat(match.test("eslint-plugin-foo")).isFalse();
        assertThat(match.test("eslint-plugin-bar")).isTrue();
    }

    @Test
    void createMatcherWithIndex() {
        ToIntFunction<String> match = Matcher.createMatcherWithIndex(singletonList("*"));
        assertThat(match.applyAsInt("@eslint/plugin-foo")).isEqualTo(0);
        assertThat(match.applyAsInt("express")).isEqualTo(0);

        match = Matcher.createMatcherWithIndex(singletonList("eslint-*"));
        assertThat(match.applyAsInt("eslint-plugin-foo")).isEqualTo(0);
        assertThat(match.applyAsInt("express")).isEqualTo(-1);

        match = Matcher.createMatcherWithIndex(singletonList("*plugin*"));
        assertThat(match.applyAsInt("@eslint/plugin-foo")).isEqualTo(0);
        assertThat(match.applyAsInt("express")).isEqualTo(-1);

        match = Matcher.createMatcherWithIndex(singletonList("a*c"));
        assertThat(match.applyAsInt("abc")).isEqualTo(0);

        match = Matcher.createMatcherWithIndex(singletonList("*-positive"));
        assertThat(match.applyAsInt("is-positive")).isEqualTo(0);

        match = Matcher.createMatcherWithIndex(asList("foo", "bar"));
        assertThat(match.applyAsInt("foo")).isEqualTo(0);
        assertThat(match.applyAsInt("bar")).isEqualTo(1);
        assertThat(match.applyAsInt("express")).isEqualTo(-1);

        match = Matcher.createMatcherWithIndex(asList("eslint-*", "!eslint-plugin-bar"));
        assertThat(match.applyAsInt("eslint-plugin-foo")).isEqualTo(0);
        assertThat(match.applyAsInt("eslint-plugin-bar")).isEqualTo(-1);

        match = Matcher.createMatcherWithIndex(asList("!eslint-plugin-bar", "eslint-*"));
        assertThat(match.applyAsInt("eslint-plugin-foo")).isEqualTo(1);
        assertThat(match.applyAsInt("eslint-plugin-bar")).isEqualTo(1);

        match = Matcher.createMatcherWithIndex(asList("eslint-*", "!eslint-plugin-*", "eslint-plugin-bar"));
        assertThat(match.applyAsInt("eslint-config-foo")).isEqualTo(0);
        assertThat(match.applyAsInt("eslint-plugin-foo")).isEqualTo(-1);
        assertThat(match.applyAsInt("eslint-plugin-bar")).isEqualTo(2);

        match = Matcher.createMatcherWithIndex(singletonList("!@pnpm.e2e/peer-*"));
        assertThat(match.applyAsInt("@pnpm.e2e/foo")).isEqualTo(0);

        match = Matcher.createMatcherWithIndex(asList("!foo", "!bar"));
        assertThat(match.applyAsInt("foo")).isEqualTo(-1);
        assertThat(match.applyAsInt("bar")).isEqualTo(-1);
        assertThat(match.applyAsInt("baz")).isEqualTo(0);

        match = Matcher.createMatcherWithIndex(asList("!foo", "!bar", "qar"));
        assertThat(match.applyAsInt("foo")).isEqualTo(-1);
        assertThat(match.applyAsInt("bar")).isEqualTo(-1);
        assertThat(match.applyAsInt("baz")).isEqualTo(-1);
    }

    /**
     * A JavaScript regular expression's {@code .} stops at a line terminator, and its {@code $} only at the end.
     */
    @Test
    void aWildcardDoesNotCrossALineTerminator() {
        assertThat(Matcher.createMatcher("a*c").test("a\nc")).isFalse();
        assertThat(Matcher.createMatcher("a*c").test("a\u0085c")).isTrue();
        assertThat(Matcher.createMatcher("a-*").test("a-b\n")).isFalse();
    }
}
