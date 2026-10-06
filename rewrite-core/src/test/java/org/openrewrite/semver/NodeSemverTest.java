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
package org.openrewrite.semver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.semver.NodeSemver.Options;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * node-semver 7.8.4's own fixtures (test/fixtures), dumped by node-semver/dump-fixtures.js.
 */
class NodeSemverTest {

    private static final JsonNode FIXTURES = load();

    private static JsonNode load() {
        try (InputStream is = NodeSemverTest.class.getResourceAsStream("/node-semver/fixtures-7.8.4")) {
            return new ObjectMapper().readTree(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Stream<Arguments> rows(String name) {
        List<Arguments> rows = new ArrayList<>();
        for (JsonNode row : FIXTURES.get(name)) {
            Object[] args = new Object[row.size()];
            for (int i = 0; i < row.size(); i++) {
                JsonNode cell = row.get(i);
                if (cell.isObject()) {
                    args[i] = Options.of(cell.get("loose").asBoolean(), cell.get("includePrerelease").asBoolean());
                } else if (cell.isBoolean()) {
                    args[i] = cell.asBoolean();
                } else if (cell.isNull()) {
                    args[i] = null;
                } else {
                    args[i] = cell.asText();
                }
            }
            rows.add(Arguments.of(args));
        }
        return rows.stream();
    }

    static Stream<Arguments> rangeInclude() {
        return rows("rangeInclude");
    }

    @ParameterizedTest
    @MethodSource("rangeInclude")
    void satisfies(String range, String version, Options options) {
        assertThat(NodeSemver.satisfies(version, range, options)).as("%s in %s", version, range).isTrue();
    }

    static Stream<Arguments> rangeExclude() {
        return rows("rangeExclude");
    }

    @ParameterizedTest
    @MethodSource("rangeExclude")
    void notSatisfies(String range, String version, Options options) {
        assertThat(NodeSemver.satisfies(version, range, options)).as("%s not in %s", version, range).isFalse();
    }

    static Stream<Arguments> rangeIntersection() {
        return rows("rangeIntersection");
    }

    @ParameterizedTest
    @MethodSource("rangeIntersection")
    void rangesIntersect(String r0, String r1, boolean expect) {
        assertThat(NodeSemver.intersects(r1, r0, Options.DEFAULT)).isEqualTo(expect);
        assertThat(NodeSemver.intersects(r0, r1, Options.DEFAULT)).isEqualTo(expect);
        assertThat(NodeSemver.intersects(r1, r0, Options.LOOSE)).isEqualTo(expect);
        assertThat(NodeSemver.intersects(r0, r1, Options.LOOSE)).isEqualTo(expect);
    }

    static Stream<Arguments> comparatorIntersection() {
        return rows("comparatorIntersection");
    }

    @ParameterizedTest
    @MethodSource("comparatorIntersection")
    void comparatorsIntersect(String c0, String c1, boolean expect, boolean includePrerelease) {
        assertThat(NodeSemver.intersects(c0, c1, Options.of(false, includePrerelease))).isEqualTo(expect);
        assertThat(NodeSemver.intersects(c1, c0, Options.of(false, includePrerelease))).isEqualTo(expect);
        assertThat(NodeSemver.intersects(c0, c1, Options.of(true, includePrerelease))).isEqualTo(expect);
        assertThat(NodeSemver.intersects(c1, c0, Options.of(true, includePrerelease))).isEqualTo(expect);
    }

    @Test
    void intersectsRejectsAnInvalidRange() {
        assertThatThrownBy(() -> NodeSemver.intersects("not a range", "1.x", Options.DEFAULT))
          .isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<Arguments> rangeParse() {
        return rows("rangeParse");
    }

    @ParameterizedTest
    @MethodSource("rangeParse")
    void validRange(String range, String expected, Options options) {
        assertThat(NodeSemver.validRange(range, options)).as(range).isEqualTo(expected);
    }

    static Stream<Arguments> validVersions() {
        return rows("validVersions");
    }

    @ParameterizedTest
    @MethodSource("validVersions")
    void validVersion(String version, String expected) {
        assertThat(NodeSemver.valid(version, Options.DEFAULT)).isEqualTo(expected);
    }

    static Stream<Arguments> invalidVersions() {
        return rows("invalidVersions");
    }

    @ParameterizedTest
    @MethodSource("invalidVersions")
    void invalidVersion(String version, Options options) {
        assertThat(NodeSemver.valid(version, options)).isNull();
    }

    @Test
    void validLooseness() {
        assertThat(NodeSemver.valid("1.2.3", Options.DEFAULT)).isEqualTo("1.2.3");
        assertThat(NodeSemver.valid("4.2.0foo", Options.LOOSE)).isEqualTo("4.2.0-foo");
    }

    @Test
    void longBuildId() {
        String longBuild = "-928490632884417731e7af463c92b034d6a78268fc993bcb88a57944";
        String longVersion = "9007199254740991.9007199254740991.9007199254740991";
        assertThat(NodeSemver.valid("1.1.1" + longBuild, Options.DEFAULT)).isEqualTo("1.1.1" + longBuild);
        assertThat(NodeSemver.valid(longVersion + longBuild, Options.DEFAULT)).isEqualTo(longVersion + longBuild);
    }

    static Stream<Arguments> comparisons() {
        return rows("comparisons");
    }

    @ParameterizedTest
    @MethodSource("comparisons")
    void greaterThan(String v0, String v1, Options options) {
        assertThat(NodeSemver.compare(v0, v1, options)).isPositive();
        assertThat(NodeSemver.compare(v1, v0, options)).isNegative();
    }

    static Stream<Arguments> equality() {
        return rows("equality");
    }

    @ParameterizedTest
    @MethodSource("equality")
    void equal(String v0, String v1, Options options) {
        assertThat(NodeSemver.compare(v0, v1, options)).isZero();
    }

    // node-semver test/classes/semver.js "invalid version numbers" and test/functions/parse.js, with what
    // node-semver 7.8.4 answers for the other types package data can hold
    @Test
    void versionThatIsNotAString() {
        String number = "Invalid version. Must be a string. Got type \"number\".";
        String object = "Invalid version. Must be a string. Got type \"object\".";
        String bool = "Invalid version. Must be a string. Got type \"boolean\".";
        String length = "Cannot read properties of undefined (reading 'length')";

        assertThatThrownBy(() -> NodeSemver.compare(1.2, "1.0.0", Options.DEFAULT)).hasMessage(number);
        assertThatThrownBy(() -> NodeSemver.compare((Object) null, "1.0.0", Options.DEFAULT)).hasMessage(object);
        assertThatThrownBy(() -> NodeSemver.compare(new ArrayList<>(), "1.0.0", Options.DEFAULT)).hasMessage(object);
        assertThatThrownBy(() -> NodeSemver.compare("1.0.0", true, Options.DEFAULT)).hasMessage(bool);
        assertThatThrownBy(() -> NodeSemver.compare("bad", 2, Options.DEFAULT)).hasMessage("Invalid Version: bad");
        assertThatThrownBy(() -> NodeSemver.compare(2, "bad", Options.DEFAULT)).hasMessage(number);

        assertThat(NodeSemver.satisfies((Object) 0, "^1.0.0", Options.DEFAULT)).isFalse();
        assertThat(NodeSemver.satisfies((Object) false, "*", Options.DEFAULT)).isFalse();
        assertThat(NodeSemver.satisfies((Object) null, "*", Options.DEFAULT)).isFalse();
        assertThat(NodeSemver.satisfies((Object) Double.NaN, "*", Options.DEFAULT)).isFalse();
        assertThat(NodeSemver.satisfies((Object) "", "*", Options.DEFAULT)).isFalse();
        assertThat(NodeSemver.satisfies((Object) 2, "not a range", Options.DEFAULT)).isFalse();
        assertThatThrownBy(() -> NodeSemver.satisfies((Object) 2, "^2.0.0", Options.DEFAULT)).hasMessage(number);
        assertThatThrownBy(() -> NodeSemver.satisfies((Object) 2, "<0.0.0-0", Options.DEFAULT)).hasMessage(number);
        assertThatThrownBy(() -> NodeSemver.satisfies((Object) 2, "1.x", Options.LOOSE)).hasMessage(number);
        assertThatThrownBy(() -> NodeSemver.satisfies(new HashMap<>(), "1.x", Options.DEFAULT)).hasMessage(object);
        assertThatThrownBy(() -> NodeSemver.satisfies((Object) true, ">=0.0.0-0", Options.DEFAULT)).hasMessage(bool);
        assertThatThrownBy(() -> NodeSemver.satisfies((Object) 2, ">=0.0.0", Options.INCLUDE_PRERELEASE))
                .hasMessage(number);
        // `*` passes without reading the version, then Range.test reads its prerelease
        assertThatThrownBy(() -> NodeSemver.satisfies((Object) 2, "x", Options.DEFAULT)).hasMessage(length);
        assertThatThrownBy(() -> NodeSemver.satisfies((Object) 2, "*", Options.LOOSE)).hasMessage(length);
        assertThatThrownBy(() -> NodeSemver.satisfies((Object) 1e21, "*", Options.DEFAULT)).hasMessage(length);
        assertThatThrownBy(() -> NodeSemver.satisfies(new ArrayList<>(), "*", Options.DEFAULT)).hasMessage(length);
        assertThatThrownBy(() -> NodeSemver.satisfies((Object) true, ">=0.0.0", Options.DEFAULT)).hasMessage(length);
        assertThatThrownBy(() -> NodeSemver.satisfies((Object) 2, ">=0.0.0-0", Options.INCLUDE_PRERELEASE))
                .hasMessage(length);
        assertThatThrownBy(() -> NodeSemver.satisfies((Object) 2, ">=1 || x", Options.DEFAULT)).hasMessage(length);
        assertThatThrownBy(() -> NodeSemver.satisfies((Object) 2, "x || 1", Options.DEFAULT)).hasMessage(length);
    }

    // satisfies catches what the Range constructor throws: `range.trim is not a function` for these
    @Test
    void rangeThatIsNotAString() {
        assertThat(NodeSemver.satisfies("1.0.0", (Object) 5, Options.DEFAULT)).isFalse();
        assertThat(NodeSemver.satisfies("1.0.0", (Object) true, Options.DEFAULT)).isFalse();
        assertThat(NodeSemver.satisfies("1.0.0", (Object) null, Options.DEFAULT)).isFalse();
        assertThat(NodeSemver.satisfies("1.0.0", new ArrayList<>(), Options.DEFAULT)).isFalse();
        assertThat(NodeSemver.satisfies("1.0.0", new HashMap<>(), Options.INCLUDE_PRERELEASE)).isFalse();
        assertThat(NodeSemver.satisfies("1.0.0", (Object) "", Options.DEFAULT)).isTrue();
        assertThat(NodeSemver.satisfies("1.0.0", (Object) "^1", Options.DEFAULT)).isTrue();
    }
}
