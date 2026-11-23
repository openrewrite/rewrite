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
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.semver.NodeSemver.Options;
import org.openrewrite.javascript.semver.NodeSemver.Release;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The node-semver releases npm 11 bundled before 7.8.4, 7.5.2 (npm 9.7.2 to 9.8.0) and 7.3.2 (npm 7.0.0 to 7.0.15):
 * each one's own fixtures (test/fixtures at its tag, dumped by generate/dump-fixtures.js), and every question of
 * 7.8.4's fixtures it answers another way (generate/release-differences.js).
 */
class NodeSemverReleasesTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static Release release(String version) {
        return Release.valueOf("V" + version.replace('.', '_'));
    }

    private static JsonNode fixtures(Release release) {
        String name = "fixtures-" + release.name().substring(1).replace('_', '.');
        try (InputStream is = Objects.requireNonNull(NodeSemverReleasesTest.class.getResourceAsStream(name), name)) {
            return JSON.readTree(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Options options(Release release, JsonNode cell) {
        return options(release, cell.get("loose").asBoolean(), cell.get("includePrerelease").asBoolean());
    }

    private static Options options(Release release, boolean loose, boolean includePrerelease) {
        return (loose ? includePrerelease ? Options.LOOSE_INCLUDE_PRERELEASE : Options.LOOSE :
                includePrerelease ? Options.INCLUDE_PRERELEASE : Options.DEFAULT).release(release);
    }

    static Stream<Arguments> ownFixtures() {
        List<Arguments> rows = new ArrayList<>();
        for (Release release : new Release[]{Release.V7_3_2, Release.V7_5_2, Release.V7_7_2, Release.V7_8_0,
                Release.V7_8_1}) {
            JsonNode fixtures = fixtures(release);
            for (String kind : new String[]{"rangeInclude", "rangeExclude", "rangeIntersection",
                    "comparatorIntersection", "rangeParse", "validVersions", "invalidVersions", "comparisons",
                    "equality"}) {
                for (JsonNode row : fixtures.get(kind)) {
                    rows.add(Arguments.of(release, kind, row));
                }
            }
        }
        return rows.stream();
    }

    @ParameterizedTest(name = "{0} {1} {2}")
    @MethodSource("ownFixtures")
    void answersItsOwnFixtures(Release release, String kind, JsonNode row) {
        String a = row.get(0).isNull() ? null : row.get(0).asText();
        String b = row.size() > 1 && row.get(1).isTextual() ? row.get(1).asText() : null;
        switch (kind) {
            case "rangeInclude":
                assertThat(NodeSemver.satisfies(b, a, options(release, row.get(2)))).isTrue();
                break;
            case "rangeExclude":
                assertThat(NodeSemver.satisfies(b, a, options(release, row.get(2)))).isFalse();
                break;
            case "rangeIntersection":
                for (boolean loose : new boolean[]{false, true}) {
                    Options options = options(release, loose, false);
                    assertThat(NodeSemver.intersects(a, b, options)).isEqualTo(row.get(2).asBoolean());
                    assertThat(NodeSemver.intersects(b, a, options)).isEqualTo(row.get(2).asBoolean());
                }
                break;
            case "comparatorIntersection":
                for (boolean loose : new boolean[]{false, true}) {
                    Options options = options(release, loose, row.get(3).asBoolean());
                    assertThat(NodeSemver.intersects(a, b, options)).isEqualTo(row.get(2).asBoolean());
                    assertThat(NodeSemver.intersects(b, a, options)).isEqualTo(row.get(2).asBoolean());
                }
                break;
            case "rangeParse":
                assertThat(NodeSemver.validRange(a, options(release, row.get(2))))
                        .isEqualTo(row.get(1).isNull() ? null : row.get(1).asText());
                break;
            case "validVersions":
                assertThat(NodeSemver.valid(a, options(release, false, false)))
                        .isEqualTo(row.get(1).isNull() ? null : row.get(1).asText());
                break;
            case "invalidVersions":
                assertThat(NodeSemver.valid(a, options(release, row.get(1)))).isNull();
                break;
            case "comparisons":
                assertThat(NodeSemver.compare(a, b, options(release, row.get(2)))).isPositive();
                assertThat(NodeSemver.compare(b, a, options(release, row.get(2)))).isNegative();
                break;
            default:
                assertThat(NodeSemver.compare(a, b, options(release, row.get(2)))).isZero();
                break;
        }
    }

    // re.js bounds a build identifier at 250 characters, which the port's patterns do not: before 7.8.1 strips build
    // metadata from a range first, a longer one makes the range another thing
    private static final Pattern BEYOND_SAFE_BUILD = Pattern.compile("[0-9A-Za-z-]{251}");

    private static List<JsonNode> differenceRows(boolean beyondSafeBuild) {
        List<JsonNode> rows = new ArrayList<>();
        try (InputStream is = Objects.requireNonNull(
                NodeSemverReleasesTest.class.getResourceAsStream("release-differences"));
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    JsonNode row = JSON.readTree(line);
                    JsonNode args = row.get(2);
                    if (BEYOND_SAFE_BUILD.matcher(args.get(args.size() - 1).asText()).find() == beyondSafeBuild) {
                        rows.add(row);
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rows;
    }

    static Stream<Arguments> differences() {
        return differenceRows(false).stream().map(row -> Arguments.of(release(row.get(0).asText()), row));
    }

    // classes/range.js: 7.3.2 keeps a group of any version beside the others, which admits no prerelease, so that a
    // prerelease another group admits satisfies the range; 7.3.3 takes such a range for any (npm 7.0.15 locks a
    // 2.0.0-beta.1 for `x || 2.0.0-beta.1` where npm 7.1.0 locks 1.0.0)
    @Test
    void groupOfAnyKeptBesideOthersIn7_3_2() {
        Options v732 = Options.DEFAULT.release(Release.V7_3_2);
        assertThat(NodeSemver.satisfies("1.0.0-beta.1", "* || 1.0.0-beta.1", v732)).isTrue();
        assertThat(NodeSemver.satisfies("1.0.0-beta.1", "* || 1.0.0-beta.1", Options.DEFAULT)).isFalse();
        assertThat(NodeSemver.satisfies("1.0.0-beta.1", "x || >=1.0.0-beta.0 <1.0.0", v732)).isTrue();
        assertThat(NodeSemver.satisfies("1.0.0-beta.1", "x || >=1.0.0-beta.0 <1.0.0", Options.DEFAULT)).isFalse();
        assertThat(NodeSemver.maxSatisfying(Arrays.asList("1.0.0", "2.0.0-beta.1"), "x || 2.0.0-beta.1",
          Options.LOOSE.release(Release.V7_3_2))).isEqualTo("2.0.0-beta.1");
        assertThat(NodeSemver.maxSatisfying(Arrays.asList("1.0.0", "2.0.0-beta.1"), "x || 2.0.0-beta.1",
          Options.LOOSE)).isEqualTo("1.0.0");
        assertThat(NodeSemver.validRange("* || 1.0.0-beta.1", v732)).isEqualTo("||1.0.0-beta.1");
    }

    // ranges/min-version.js: 7.3.2 takes the least lower bound of any set, so that one set's bounds can leave it below
    // the set, and reads the version of an any comparator; 7.3.3 takes the greatest of each set, then the least set
    @Test
    void minVersionOf7_3_2() {
        Options v732 = Options.DEFAULT.release(Release.V7_3_2);
        assertThat(NodeSemver.minVersion(">=1.0.0 >=2.0.0", v732)).isNull();
        assertThat(NodeSemver.minVersion(">=1.0.0 >=2.0.0", Options.DEFAULT)).isEqualTo("2.0.0");
        assertThat(NodeSemver.minVersion(">1.2.3 >=1.2.4 <3", v732)).isEqualTo("1.2.4");
        assertThat(NodeSemver.minVersion(">=1.0.0 || >=0.5.0 <0.6.0", v732)).isEqualTo("0.5.0");
        assertThatThrownBy(() -> NodeSemver.minVersion("* 1.2.3", v732)).hasMessage("Invalid Version: undefined");
        assertThat(NodeSemver.minVersion("* 1.2.3", Options.DEFAULT)).isEqualTo("1.2.3");
    }

    @Test
    void namesTheDifferencesItDoesNotAnswer() {
        assertThat(differenceRows(true)).as("ranges with a build identifier longer than re.js's bound").hasSize(288);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("differences")
    void answersWhereItDiffersFromTheNewest(Release release, JsonNode row) {
        JsonNode args = row.get(2);
        Options options = options(release, row.get(3).asBoolean(), row.get(4).asBoolean());
        JsonNode answer = row.get(5);
        if ("validRange".equals(row.get(1).asText())) {
            assertThat(NodeSemver.validRange(args.get(0).asText(), options))
                    .isEqualTo(answer.isNull() ? null : answer.asText());
            // and the newest release answers another thing
            assertThat(NodeSemver.validRange(args.get(0).asText(),
                    options(Release.V7_8_4, row.get(3).asBoolean(), row.get(4).asBoolean())))
                    .isNotEqualTo(answer.isNull() ? null : answer.asText());
        } else {
            assertThat(NodeSemver.satisfies(args.get(0).asText(), args.get(1).asText(), options))
                    .isEqualTo(answer.asBoolean());
            assertThat(NodeSemver.satisfies(args.get(0).asText(), args.get(1).asText(),
                    options(Release.V7_8_4, row.get(3).asBoolean(), row.get(4).asBoolean())))
                    .isNotEqualTo(answer.asBoolean());
        }
    }
}
