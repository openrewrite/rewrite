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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.semver.NodeSemver.Options;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * node-semver 5.5.0's own tests (which yarn 1.22.22 bundles): each call to a function yarn calls, with 5.5.0's answer
 * ({@code suite-5.5.0.tsv}, written by {@code suite-5.5.0.js}), replayed with {@link Options#V5_5_0}.
 */
class NodeSemverRelease550Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static Stream<Arguments> rows() {
        List<Arguments> rows = new ArrayList<>();
        try (InputStream is = NodeSemverRelease550Test.class.getResourceAsStream("suite-5.5.0.tsv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }
                String[] cells = line.split("\t", -1);
                List<JsonNode> args = new ArrayList<>();
                int i = 1;
                for (; !"=>".equals(cells[i]); i++) {
                    args.add(MAPPER.readTree(cells[i]));
                }
                rows.add(Arguments.of(line.replace('\t', ' '), cells[0], args, MAPPER.readTree(cells[i + 1])));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rows.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rows")
    void answersAs550(String call, String fn, List<JsonNode> args, JsonNode result) {
        if (result.has("threw")) {
            assertThatThrownBy(() -> call(fn, args)).hasMessage(result.get("threw").asText());
            return;
        }
        Object actual = call(fn, args);
        if (result.isBoolean()) {
            assertThat(actual).isEqualTo(result.asBoolean());
        } else if (result.isNull() || result.has("undefined")) {
            assertThat(actual).isNull();
        } else {
            assertThat(actual).isEqualTo(result.asText());
        }
    }

    // Range.test parses a string version and throws on one that does not parse (7.x answers false); run on 5.5.0
    @Test
    void testThrowsOnAVersionThatDoesNotParse() {
        assertThatThrownBy(() -> NodeSemver.satisfies("1.0", "^1.0.0", Options.V5_5_0))
                .hasMessage("Invalid Version: 1.0");
        assertThatThrownBy(() -> NodeSemver.satisfies("1.0", "x", Options.V5_5_0)).hasMessage("Invalid Version: 1.0");
        assertThat(NodeSemver.satisfies("", "^1.0.0", Options.V5_5_0)).isFalse();
        assertThat(NodeSemver.satisfies("1.2.3", "not a range", Options.V5_5_0)).isFalse();
        assertThatThrownBy(() -> NodeSemver.maxSatisfying(Arrays.asList("1.0.0", "bad"), "^1", Options.V5_5_0))
                .hasMessage("Invalid Version: bad");
        assertThatThrownBy(() -> NodeSemver.maxSatisfying(Collections.singletonList("bad"), "^1",
                Options.V5_5_0_LOOSE)).hasMessage("Invalid Version: bad");
        assertThat(NodeSemver.satisfies("1.0", "^1.0.0", Options.DEFAULT)).isFalse();
    }

    // new SemVer of a value that is not a string or of a string over 256 characters, run on 5.5.0 and 7.8.4
    @Test
    void compareThrowsAsEachRelease() {
        String tooLong = "1.0.0-" + String.join("", Collections.nCopies(260, "a"));
        assertThatThrownBy(() -> NodeSemver.compare(5.0, "1.0.0", Options.V5_5_0_LOOSE))
                .hasMessage("Invalid Version: 5");
        assertThatThrownBy(() -> NodeSemver.compare(5.0, "1.0.0", Options.LOOSE))
                .hasMessage("Invalid version. Must be a string. Got type \"number\".");
        assertThatThrownBy(() -> NodeSemver.compare("1.0.0", true, Options.V5_5_0_LOOSE))
                .hasMessage("Invalid Version: true");
        assertThatThrownBy(() -> NodeSemver.compare(Collections.singletonList(1.0), "1.0.0", Options.V5_5_0_LOOSE))
                .hasMessage("Invalid Version: 1");
        assertThatThrownBy(() -> NodeSemver.compare(tooLong, "1.0.0", Options.V5_5_0_LOOSE))
                .hasMessage("version is longer than 256 characters");
        assertThatThrownBy(() -> NodeSemver.compare(tooLong, "1.0.0", Options.LOOSE))
                .hasMessage("version is longer than 256 characters");
        assertThatThrownBy(() -> NodeSemver.maxSatisfying(Arrays.asList("1.0.0", tooLong), "^1.0.0",
                Options.V5_5_0_LOOSE)).hasMessage("version is longer than 256 characters");
    }

    static Stream<Arguments> jsonValueRows() {
        List<Arguments> rows = new ArrayList<>();
        try (InputStream is = NodeSemverRelease550Test.class.getResourceAsStream("satisfies-json-values.tsv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] cells = line.split("\t", -1);
                Object version = MAPPER.readValue(cells[0], Object.class);
                String range = MAPPER.readValue(cells[1], String.class);
                String call = cells[0].length() > 40 ? cells[0].substring(0, 40) + "..." : cells[0];
                rows.add(Arguments.of(call + " " + cells[1], version, range, MAPPER.readTree(cells[2]),
                        MAPPER.readTree(cells[3])));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return rows.stream();
    }

    // satisfies with a version as JSON package data holds it, recorded from both releases
    @ParameterizedTest(name = "{0}")
    @MethodSource("jsonValueRows")
    void satisfiesAJsonValueAsEachRelease(String call, Object version, String range, JsonNode answer550,
                                          JsonNode answer784) {
        assertAnswers(version, range, Options.V5_5_0_LOOSE, answer550);
        assertAnswers(version, range, Options.LOOSE, answer784);
    }

    private static void assertAnswers(Object version, String range, Options options, JsonNode answer) {
        if (answer.has("threw")) {
            assertThatThrownBy(() -> NodeSemver.satisfies(version, range, options))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(answer.get("threw").asText());
        } else {
            assertThat(NodeSemver.satisfies(version, range, options)).isEqualTo(answer.asBoolean());
        }
    }

    private static @Nullable Object call(String fn, List<JsonNode> args) {
        switch (fn) {
            case "valid":
                return NodeSemver.valid(args.get(0).asText(), options(args, 1));
            case "validRange":
                return NodeSemver.validRange(args.get(0).asText(), options(args, 1));
            case "clean":
                return NodeSemver.clean(args.get(0).asText(), options(args, 1));
            case "satisfies":
                return args.get(0).isTextual() ?
                        NodeSemver.satisfies(args.get(0).asText(), args.get(1).asText(), options(args, 2)) :
                        NodeSemver.satisfies(value(args.get(0)), args.get(1).asText(), options(args, 2));
            case "intersects":
                return NodeSemver.intersects(args.get(0).asText(), args.get(1).asText(), options(args, 2));
            case "maxSatisfying": {
                List<String> versions = new ArrayList<>();
                args.get(0).forEach(v -> versions.add(v.asText()));
                return NodeSemver.maxSatisfying(versions, args.get(1).asText(), options(args, 2));
            }
            case "cmp":
                return cmp(args.get(0).asText(), args.get(1).asText(), args.get(2).asText(), options(args, 3));
            default:
                return cmp(args.get(0).asText(), op(fn), args.get(1).asText(), options(args, 2));
        }
    }

    private static @Nullable Object value(JsonNode node) {
        return node.isBoolean() ? (Object) node.asBoolean() : node.isNumber() ? (Object) node.asDouble() : null;
    }

    private static String op(String fn) {
        switch (fn) {
            case "gt":
                return ">";
            case "lt":
                return "<";
            case "eq":
                return "==";
            case "neq":
                return "!=";
            case "gte":
                return ">=";
            default:
                return "<=";
        }
    }

    // semver.js cmp: === and !== compare the strings, the rest compare the versions
    private static boolean cmp(String a, String op, String b, Options options) {
        switch (op) {
            case "===":
                return a.equals(b);
            case "!==":
                return !a.equals(b);
            case "":
            case "=":
            case "==":
                return NodeSemver.compare(a, b, options) == 0;
            case "!=":
                return NodeSemver.compare(a, b, options) != 0;
            case ">":
                return NodeSemver.compare(a, b, options) > 0;
            case ">=":
                return NodeSemver.compare(a, b, options) >= 0;
            case "<":
                return NodeSemver.compare(a, b, options) < 0;
            case "<=":
                return NodeSemver.compare(a, b, options) <= 0;
            default:
                throw new IllegalArgumentException("Invalid operator: " + op);
        }
    }

    private static Options options(List<JsonNode> args, int index) {
        boolean loose = args.size() > index && args.get(index).isBoolean() && args.get(index).asBoolean();
        return loose ? Options.V5_5_0_LOOSE : Options.V5_5_0;
    }
}
