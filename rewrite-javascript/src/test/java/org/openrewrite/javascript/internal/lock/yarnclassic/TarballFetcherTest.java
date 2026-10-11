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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.RegistrySource;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * yarn's __tests__/fetchers.js tarball cases and archives for each branch of gunzip-maybe, tar-stream, tar-fs and the
 * manifest read ({@code generate/tarball-rows.js}), as yarn 1.22.22's TarballFetcher read each over http.
 */
class TarballFetcherTest {

    private static final String URL = "<url>";

    static Stream<Arguments> rows() throws IOException {
        List<Arguments> out = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(requireNonNull(
                TarballFetcherTest.class.getResourceAsStream("tarball-rows.tsv")), StandardCharsets.UTF_8))) {
            for (String line; (line = in.readLine()) != null; ) {
                String[] cells = line.split("\t", 2);
                out.add(Arguments.of(cells[0], cells[1]));
            }
        }
        return out.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("rows")
    void readsWhatYarnReads(String archive, String outcome) throws IOException {
        String actual = read(bytes("tarballs/" + archive + ".tgz"));
        if (outcome.startsWith("SyntaxError ")) {
            assertThat(actual).startsWith(outcome.substring(0, outcome.indexOf(':') + 1));
        } else {
            assertThat(actual).isEqualTo(outcome);
        }
    }

    /**
     * yarn creates a hash for every algorithm the integrity names; beyond these six, Node's OpenSSL build answers.
     */
    @Test
    void cannotKnowAnIntegrityAlgorithmBeyondTheSixEveryNodeDigests() {
        PackageRemote remote = new PackageRemote();
        remote.type = "tarball";
        remote.registry = "npm";
        remote.reference = "https://registry.npmjs.org/x/-/x-1.0.0.tgz";
        remote.integrity = "sha512-AAAA whirlpool-AAAA";
        RegistrySource tarballs = new RegistrySource() {
            @Override
            public @Nullable String packument(String r, String name, boolean full, @Nullable String authorization) {
                return null;
            }

            @Override
            public byte[] tarball(String url, String name, String version, @Nullable String authorization) {
                return new byte[0];
            }
        };
        assertThatThrownBy(() -> new TarballFetcher(remote, "x", "1.0.0", "dest", tarballs,
                YarnClassicScenarios.defaults()).fetch())
                .isInstanceOf(CannotKnow.class)
                .hasMessageContaining("the whirlpool digest the integrity of x@1.0.0 names");
    }

    private static String read(byte[] tarball) {
        try {
            Object pkg = TarballFetcher.manifest(TarballFetcher.untar(URL, tarball), "", "npm", "fallback", "9.9.9",
                    true);
            Map<String, @Nullable Object> data = pkg instanceof Manifest ? ((Manifest) pkg).data : null;
            Map<String, @Nullable Object> shown = new LinkedHashMap<>();
            if (data != null) {
                for (String key : Arrays.asList("name", "version", "dependencies", "optionalDependencies")) {
                    if (data.get(key) != null) {
                        shown.put(key, data.get(key));
                    }
                }
            }
            return "ok " + (data == null ? Js.jsonStringify(pkg) : Js.jsonStringify(shown));
        } catch (MessageError e) {
            return "MessageError " + e.getMessage();
        } catch (JsError e) {
            return e.getName().replaceFirst(" \\[.*", "") + " " + e.getMessage();
        }
    }

    private static byte[] bytes(String resource) throws IOException {
        try (InputStream in = requireNonNull(TarballFetcherTest.class.getResourceAsStream(resource))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int n; (n = in.read(buffer)) > 0; ) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }
}
