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
package org.openrewrite.javascript.internal.lock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Parity with Node's path.posix run from {@code /}, as dumped by npm-arborist/generate/path-cases.js.
 */
class PosixPathTest {

    static Stream<Arguments> calls() {
        try (InputStream is = PosixPathTest.class.getResourceAsStream("/npm-arborist/path-cases")) {
            List<Arguments> rows = new ArrayList<>();
            for (JsonNode row : new ObjectMapper().readTree(is)) {
                List<String> args = new ArrayList<>();
                row.get("args").forEach(a -> args.add(a.asText()));
                rows.add(Arguments.of(row.get("fn").asText(), args, row.get("result")));
            }
            return rows.stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ParameterizedTest(name = "{0}{1}")
    @MethodSource("calls")
    void matchesNode(String fn, List<String> args, JsonNode result) {
        String[] a = args.toArray(new String[0]);
        Object actual;
        switch (fn) {
            case "resolve":
                actual = PosixPath.resolve(a);
                break;
            case "relative":
                actual = PosixPath.relative(a[0], a[1]);
                break;
            case "dirname":
                actual = PosixPath.dirname(a[0]);
                break;
            case "basename":
                actual = PosixPath.basename(a[0]);
                break;
            case "normalize":
                actual = PosixPath.normalize(a[0]);
                break;
            case "join":
                actual = PosixPath.join(a);
                break;
            default:
                actual = PosixPath.isAbsolute(a[0]);
                break;
        }
        assertThat(actual).isEqualTo(result.isBoolean() ? result.asBoolean() : result.asText());
    }

    @Test
    void walkUp() {
        assertThat(PosixPath.walkUp("/a/b/c")).containsExactly("/a/b/c", "/a/b", "/a", "/");
    }
}
