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
package org.openrewrite.javascript.semver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.semver.NodeSemver.Options;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * node-semver 7.8.4's own fixtures (test/fixtures), dumped by generate/dump-fixtures.js.
 */
class NodeSemverTest {

    private static final JsonNode FIXTURES = load();

    private static JsonNode load() {
        try (InputStream is = NodeSemverTest.class.getResourceAsStream("fixtures-7.8.4")) {
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
                    args[i] = options(cell.get("loose").asBoolean(), cell.get("includePrerelease").asBoolean());
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
        assertThat(NodeSemver.intersects(c0, c1, options(false, includePrerelease))).isEqualTo(expect);
        assertThat(NodeSemver.intersects(c1, c0, options(false, includePrerelease))).isEqualTo(expect);
        assertThat(NodeSemver.intersects(c0, c1, options(true, includePrerelease))).isEqualTo(expect);
        assertThat(NodeSemver.intersects(c1, c0, options(true, includePrerelease))).isEqualTo(expect);
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

    // test/functions/clean.js
    @Test
    void clean() {
        String[][] rows = {
                {"1.2.3", "1.2.3"}, {" 1.2.3 ", "1.2.3"}, {" 1.2.3-4 ", "1.2.3-4"}, {" 1.2.3-pre ", "1.2.3-pre"},
                {"  =v1.2.3   ", "1.2.3"}, {"v1.2.3", "1.2.3"}, {" v1.2.3 ", "1.2.3"}, {"\t1.2.3", "1.2.3"},
                {">1.2.3", null}, {"~1.2.3", null}, {"<=1.2.3", null}, {"1.2.x", null},
                {"0.12.0-dev.1150+3c22cecee", "0.12.0-dev.1150"},
        };
        for (String[] row : rows) {
            assertThat(NodeSemver.clean(row[0], Options.DEFAULT)).as("clean(%s)", row[0]).isEqualTo(row[1]);
        }
    }

    // test/functions/prerelease.js
    @Test
    void prerelease() {
        assertThat(NodeSemver.prerelease("1.2.2-alpha.1", Options.DEFAULT)).containsExactly("alpha", 1L);
        assertThat(NodeSemver.prerelease("0.6.1-1", Options.DEFAULT)).containsExactly(1L);
        assertThat(NodeSemver.prerelease("1.0.0-beta.2", Options.DEFAULT)).containsExactly("beta", 2L);
        assertThat(NodeSemver.prerelease("v0.5.4-pre", Options.DEFAULT)).containsExactly("pre");
        assertThat(NodeSemver.prerelease("1.2.2-alpha.1", Options.DEFAULT)).containsExactly("alpha", 1L);
        assertThat(NodeSemver.prerelease("0.6.1beta", Options.LOOSE)).containsExactly("beta");
        assertThat(NodeSemver.prerelease("1.0.0", Options.LOOSE)).isNull();
        assertThat(NodeSemver.prerelease("~2.0.0-alpha.1", Options.DEFAULT)).isNull();
        assertThat(NodeSemver.prerelease("invalid version", Options.DEFAULT)).isNull();
    }

    // test/ranges/max-satisfying.js
    @Test
    void maxSatisfying() {
        assertThat(NodeSemver.maxSatisfying(asList("1.2.3", "1.2.4"), "1.2", Options.DEFAULT)).isEqualTo("1.2.4");
        assertThat(NodeSemver.maxSatisfying(asList("1.2.4", "1.2.3"), "1.2", Options.DEFAULT)).isEqualTo("1.2.4");
        assertThat(NodeSemver.maxSatisfying(asList("1.2.3", "1.2.4", "1.2.5", "1.2.6"), "~1.2.3", Options.DEFAULT))
                .isEqualTo("1.2.6");
        assertThat(NodeSemver.maxSatisfying(asList("1.1.0", "1.2.0", "1.2.1", "1.3.0", "2.0.0b1", "2.0.0b2", "2.0.0b3",
                "2.0.0", "2.1.0"), "~2.0.0", Options.LOOSE)).isEqualTo("2.0.0");
        assertThat(NodeSemver.maxSatisfying(emptyList(), "some frogs and sneks-v2.5.6", Options.DEFAULT)).isNull();
    }

    // test/ranges/min-satisfying.js
    @Test
    void minSatisfying() {
        assertThat(NodeSemver.minSatisfying(asList("1.2.3", "1.2.4"), "1.2", Options.DEFAULT)).isEqualTo("1.2.3");
        assertThat(NodeSemver.minSatisfying(asList("1.2.4", "1.2.3"), "1.2", Options.DEFAULT)).isEqualTo("1.2.3");
        assertThat(NodeSemver.minSatisfying(asList("1.2.3", "1.2.4", "1.2.5", "1.2.6"), "~1.2.3", Options.DEFAULT))
                .isEqualTo("1.2.3");
        assertThat(NodeSemver.minSatisfying(asList("1.1.0", "1.2.0", "1.2.1", "1.3.0", "2.0.0b1", "2.0.0b2", "2.0.0b3",
                "2.0.0", "2.1.0"), "~2.0.0", Options.LOOSE)).isEqualTo("2.0.0");
        assertThat(NodeSemver.minSatisfying(emptyList(), "some frogs and sneks-v2.5.6", Options.DEFAULT)).isNull();
    }

    // test/functions/compare-build.js
    @Test
    void compareBuild() {
        assertThat(NodeSemver.compareBuild("1.0.0", "1.0.0+0", Options.DEFAULT)).isEqualTo(-1);
        assertThat(NodeSemver.compareBuild("1.0.0+0", "1.0.0+0", Options.DEFAULT)).isEqualTo(0);
        assertThat(NodeSemver.compareBuild("1.0.0+0", "1.0.0", Options.DEFAULT)).isEqualTo(1);
        assertThat(NodeSemver.compareBuild("1.0.0+0", "1.0.0+0.0", Options.DEFAULT)).isEqualTo(-1);
        assertThat(NodeSemver.compareBuild("1.0.0+0", "1.0.0+1", Options.DEFAULT)).isEqualTo(-1);
        assertThat(NodeSemver.compareBuild("1.0.0+1", "1.0.0+0", Options.DEFAULT)).isEqualTo(1);
        assertThat(NodeSemver.compareBuild("1.0.0+1.0", "1.0.0+1", Options.DEFAULT)).isEqualTo(1);
    }

    // test/functions/rsort.js
    @Test
    void rsort() {
        List<String> list = new ArrayList<>(asList("1.2.3+1", "1.2.3+0", "1.2.3", "5.9.6", "0.1.2"));
        NodeSemver.rsort(list, Options.DEFAULT);
        assertThat(list).containsExactly("5.9.6", "1.2.3+1", "1.2.3+0", "1.2.3", "0.1.2");
    }

    // test/ranges/subset.js: sub, dom, expect, includePrerelease
    static Stream<Arguments> subsets() {
        Object[][] cases = {
          {"1.2.3", "1.2.3", true, false},
          {"1.2.3", "1.x", true, false},
          {"1.2.3 1.2.4", "1.2.3", true, false},
          {"1.2.3 1.2.4", "1.2.9", true, false},
          {"1.2.3", ">1.2.0", true, false},
          {"1.2.3 2.3.4 || 2.3.4", "3", false, false},
          {"^1.2.3-pre.0", "1.x", false, false},
          {"^1.2.3-pre.0", "1.x", true, true},
          {">2 <1", "3", true, false},
          {"1 || 2 || 3", ">=1.0.0", true, false},
          {"1.2.3", "*", true, false},
          {"^1.2.3", "*", true, false},
          {"^1.2.3-pre.0", "*", false, false},
          {"^1.2.3-pre.0", "*", true, true},
          {"1 || 2 || 3", "*", true, false},
          {"^1.2.3-pre.0", ">=1.0.0", false, false},
          {"^1.2.3-pre.0", ">=1.0.0", true, true},
          {"^1.2.3-pre.0", ">=1.2.3-pre.0", true, false},
          {"^1.2.3-pre.0", ">=1.2.3-pre.0", true, true},
          {"^10.2.0-beta.2", "^10.2.0-beta.1", true, false},
          {">1.2.3-pre.0", ">=1.2.3-pre.0", true, false},
          {">1.2.3-pre.0", ">1.2.3-pre.0 || 2", true, false},
          {"1 >1.2.3-pre.0", ">1.2.3-pre.0", true, false},
          {"1 <=1.2.3-pre.0", ">=1.0.0-0", false, false},
          {"1 <=1.2.3-pre.0", ">=1.0.0-0", true, true},
          {"1 <=1.2.3-pre.0", "<=1.2.3-pre.0", true, false},
          {"1 <=1.2.3-pre.0", "<=1.2.3-pre.0", true, true},
          {"<1.2.3-pre.0", "<=1.2.3-pre.0", true, false},
          {"<1.2.3-pre.0", "<1.2.3-pre.0 || 2", true, false},
          {"1 <1.2.3-pre.0", "<1.2.3-pre.0", true, false},
          {"*", "*", true, false},
          {"", "*", true, false},
          {"*", "", true, false},
          {"", "", true, false},
          {"*", ">=0.0.0-0", true, true},
          {"*", ">=0.0.0", true, false},
          {"*", ">=0.0.0", false, true},
          {"*", ">=0.0.0-0", true, false},
          {"^2 || ^3 || ^4", ">=1", true, false},
          {"^2 || ^3 || ^4", ">1", true, false},
          {"^2 || ^3 || ^4", ">=2", true, false},
          {"^2 || ^3 || ^4", ">=3", false, false},
          {">=1", "^2 || ^3 || ^4", false, false},
          {">1", "^2 || ^3 || ^4", false, false},
          {">=2", "^2 || ^3 || ^4", false, false},
          {">=3", "^2 || ^3 || ^4", false, false},
          {"^1", "^2 || ^3 || ^4", false, false},
          {"^2", "^2 || ^3 || ^4", true, false},
          {"^3", "^2 || ^3 || ^4", true, false},
          {"^4", "^2 || ^3 || ^4", true, false},
          {"1.x", "^2 || ^3 || ^4", false, false},
          {"2.x", "^2 || ^3 || ^4", true, false},
          {"3.x", "^2 || ^3 || ^4", true, false},
          {"4.x", "^2 || ^3 || ^4", true, false},
          {">=1.0.0 <=1.0.0 || 2.0.0", "1.0.0 || 2.0.0", true, false},
          {"<=1.0.0 >=1.0.0 || 2.0.0", "1.0.0 || 2.0.0", true, false},
          {">=1.0.0", "1.0.0", false, false},
          {">=1.0.0 <2.0.0", "<2.0.0", true, false},
          {">=1.0.0 <2.0.0", ">0.0.0", true, false},
          {">=1.0.0 <=1.0.0", "1.0.0", true, false},
          {">=1.0.0 <=1.0.0", "2.0.0", false, false},
          {"<2.0.0", ">=1.0.0 <2.0.0", false, false},
          {">=1.0.0", ">=1.0.0 <2.0.0", false, false},
          {">=1.0.0 <2.0.0", "<2.0.0", true, false},
          {">=1.0.0 <2.0.0", ">=1.0.0", true, false},
          {">=1.0.0 <2.0.0", ">1.0.0", false, false},
          {">=1.0.0 <=2.0.0", "<2.0.0", false, false},
          {">=1.0.0", "<1.0.0", false, false},
          {"<=1.0.0", ">1.0.0", false, false},
          {"<=1.0.0 >1.0.0", ">1.0.0", true, false},
          {"1.0.0 >1.0.0", ">1.0.0", true, false},
          {"1.0.0 <1.0.0", ">1.0.0", true, false},
          {"<1 <2 <3", "<4", true, false},
          {"<3 <2 <1", "<4", true, false},
          {">1 >2 >3", ">0", true, false},
          {">3 >2 >1", ">0", true, false},
          {"<=1 <=2 <=3", "<4", true, false},
          {"<=3 <=2 <=1", "<4", true, false},
          {">=1 >=2 >=3", ">0", true, false},
          {">=3 >=2 >=1", ">0", true, false},
          {">=3 >=2 >=1", ">=3 >=2 >=1", true, false},
          {">2.0.0", ">=2.0.0", true, false},
        };
        List<Arguments> rows = new ArrayList<>();
        for (Object[] row : cases) {
            rows.add(Arguments.of(row));
        }
        return rows.stream();
    }

    @ParameterizedTest
    @MethodSource("subsets")
    void subset(String sub, String dom, boolean expect, boolean includePrerelease) {
        assertThat(NodeSemver.subset(sub, dom, options(false, includePrerelease)))
          .as("%s subset of %s", sub, dom).isEqualTo(expect);
    }

    // test/ranges/min-version.js: range, minimum
    static Stream<Arguments> minVersions() {
        Object[][] cases = {
          {"*", "0.0.0"},
          {"* || >=2", "0.0.0"},
          {">=2 || *", "0.0.0"},
          {">2 || *", "0.0.0"},
          {"1.0.0", "1.0.0"},
          {"1.0", "1.0.0"},
          {"1.0.x", "1.0.0"},
          {"1.0.*", "1.0.0"},
          {"1", "1.0.0"},
          {"1.x.x", "1.0.0"},
          {"1.x.x", "1.0.0"},
          {"1.*.x", "1.0.0"},
          {"1.x.*", "1.0.0"},
          {"1.x", "1.0.0"},
          {"1.*", "1.0.0"},
          {"=1.0.0", "1.0.0"},
          {"~1.1.1", "1.1.1"},
          {"~1.1.1-beta", "1.1.1-beta"},
          {"~1.1.1 || >=2", "1.1.1"},
          {"^1.1.1", "1.1.1"},
          {"^1.1.1-beta", "1.1.1-beta"},
          {"^1.1.1 || >=2", "1.1.1"},
          {"^2.16.2 ^2.16", "2.16.2"},
          {"1.1.1 - 1.8.0", "1.1.1"},
          {"1.1 - 1.8.0", "1.1.0"},
          {"<2", "0.0.0"},
          {"<0.0.0-beta", "0.0.0-0"},
          {"<0.0.1-beta", "0.0.0"},
          {"<2 || >4", "0.0.0"},
          {">4 || <2", "0.0.0"},
          {"<=2 || >=4", "0.0.0"},
          {">=4 || <=2", "0.0.0"},
          {"<0.0.0-beta >0.0.0-alpha", "0.0.0-alpha.0"},
          {">0.0.0-alpha <0.0.0-beta", "0.0.0-alpha.0"},
          {">=1.1.1 <2 || >=2.2.2 <2", "1.1.1"},
          {">=2.2.2 <2 || >=1.1.1 <2", "1.1.1"},
          {">1.0.0", "1.0.1"},
          {">1.0.0-0", "1.0.0-0.0"},
          {">1.0.0-beta", "1.0.0-beta.0"},
          {">2 || >1.0.0", "1.0.1"},
          {">2 || >1.0.0-0", "1.0.0-0.0"},
          {">2 || >1.0.0-beta", "1.0.0-beta.0"},
          {">4 <3", null},
        };
        List<Arguments> rows = new ArrayList<>();
        for (Object[] row : cases) {
            rows.add(Arguments.of(row));
        }
        return rows.stream();
    }

    @ParameterizedTest
    @MethodSource("minVersions")
    void minVersion(String range, String expected) {
        assertThat(NodeSemver.minVersion(range, Options.DEFAULT)).as("minVersion(%s)", range).isEqualTo(expected);
    }

    // node-semver 7.8.4's TypeError, as minVersion threw it under node
    @ParameterizedTest
    @CsvSource(delimiterString = " -> ", value = {
      "not a range -> Invalid comparator: not",
      "foo -> Invalid comparator: foo",
      ">=x.y -> Invalid comparator: >=x.y",
      "^1.2.3.4 -> Invalid comparator: ^1.2.3.4",
      "abc def -> Invalid comparator: abc",
      "1.2.3 || foo -> Invalid comparator: foo",
      ">=1 <x.y.z.w -> Invalid comparator: <x.y.z.w",
      "1.x || >=2.0.0-beta.1 || bar -> Invalid comparator: bar"
    })
    void minVersionOfAnInvalidRange(String range, String message) {
        assertThatThrownBy(() -> NodeSemver.minVersion(range, Options.DEFAULT))
          .isInstanceOf(IllegalArgumentException.class).hasMessage(message);
    }

    @Test
    void rangeIsASubsetOfItself() {
        assertThat(NodeSemver.subset("^1", "^1", Options.DEFAULT)).isTrue();
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

    // the preset for the options a fixture row names
    private static Options options(boolean loose, boolean includePrerelease) {
        return loose ? (includePrerelease ? Options.LOOSE_INCLUDE_PRERELEASE : Options.LOOSE) :
                (includePrerelease ? Options.INCLUDE_PRERELEASE : Options.DEFAULT);
    }
}
