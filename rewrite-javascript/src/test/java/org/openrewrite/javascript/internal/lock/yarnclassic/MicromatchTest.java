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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * micromatch 2.3.11's own test suite ({@code micromatch-suite.tsv}) and patterns generated over its syntax
 * ({@code micromatch-generated.tsv}), each call as yarn 1.22.22's bundled micromatch answered it.
 */
class MicromatchTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern RANDOM_ID = Pattern.compile("__ID.{5}__");

    static Stream<Arguments> suite() throws IOException {
        return rows("micromatch-suite.tsv");
    }

    static Stream<Arguments> generated() throws IOException {
        return rows("micromatch-generated.tsv");
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("suite")
    void answersItsOwnSuiteAsYarnsBundleDoes(String fn, JsonNode args, JsonNode outcome) {
        assertThat(answer(fn, args)).isEqualTo(outcome);
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("generated")
    void answersGeneratedPatternsAsYarnsBundleDoes(String fn, JsonNode args, JsonNode outcome) {
        assertThat(withoutRandomIds(answer(fn, args))).isEqualTo(withoutRandomIds(outcome));
    }

    @Test
    void cannotKnowWhetherAPathMatchesTheRandomIdBracesLeavesInTheRegex() {
        assertThat(Micromatch.micromatch(singletonList("1b"), "1${a}{b,c}")).isEmpty();
        assertThatThrownBy(() -> Micromatch.micromatch(singletonList("1__ID00000__b"), "1${a}{b,c}"))
                .isInstanceOf(CannotKnow.class);
    }

    // preserve draws its ids from Math.random(), and with makeRe braces leaves one in the regex
    private static String withoutRandomIds(JsonNode node) {
        return RANDOM_ID.matcher(node.toString()).replaceAll("__ID_____");
    }

    private static JsonNode answer(String fn, JsonNode args) {
        try {
            Object out;
            switch (fn) {
                case "micromatch":
                    out = Micromatch.micromatch(files(args.get(0)), value(args.get(1)));
                    break;
                case "match":
                    out = new Micromatch().match(files(args.get(0)), value(args.get(1)));
                    break;
                case "isMatch":
                    out = new Micromatch().matcher(value(args.get(1))).test(args.get(0).asText());
                    break;
                case "makeRe":
                    out = new Micromatch().makeRe(args.get(0).asText());
                    break;
                default:
                    throw new IllegalArgumentException(fn);
            }
            return MAPPER.createObjectNode().set("out", MAPPER.valueToTree(out));
        } catch (JsError e) {
            return MAPPER.createObjectNode().put("threw", e.toString());
        }
    }

    // arrayify of the files argument
    private static List<String> files(JsonNode files) {
        List<String> out = new ArrayList<>();
        if (files.isArray()) {
            files.forEach(f -> out.add(f.asText()));
        } else if (!files.asText().isEmpty()) {
            out.add(files.asText());
        }
        return out;
    }

    private static @Nullable Object value(JsonNode node) {
        if (node.isNull()) {
            return null;
        }
        if (node.isArray()) {
            List<@Nullable Object> list = new ArrayList<>();
            node.forEach(n -> list.add(value(n)));
            return list;
        }
        if (node.isNumber()) {
            return node.asDouble();
        }
        if (node.isBoolean()) {
            return node.asBoolean();
        }
        return node.asText();
    }

    private static Stream<Arguments> rows(String resource) throws IOException {
        List<Arguments> rows = new ArrayList<>();
        try (InputStream is = requireNonNull(MicromatchTest.class.getResourceAsStream(resource));
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("#")) {
                    String[] cells = line.split("\t", -1);
                    rows.add(Arguments.of(cells[0], MAPPER.readTree(cells[1]), MAPPER.readTree(cells[2])));
                }
            }
        }
        return rows.stream();
    }
}
