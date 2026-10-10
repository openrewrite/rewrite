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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.Picomatch.Options;
import org.openrewrite.javascript.internal.lock.Picomatch.Release;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each release against the bundle that carries it, row by row: 2.3.1 as Yarn 4.10.3's fast-glob calls it
 * ({@code yarn-berry/generate/globs.js}), 4.0.4 as pnpm 11.2.2's tinyglobby does ({@code pnpm/generate/globs.js}).
 */
class PicomatchTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void compilesAndMatchesAsYarnsBundleDoes() {
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("yarn-berry/differential/glob-picomatch.tsv.gz",
                System.getenv("REWRITE_BERRY_DIFFERENTIAL"))) {
            Options options = new Options(Release.V2_3_1, "1".equals(row[1]), true);
            String actual;
            try {
                actual = Picomatch.source(row[0], options);
            } catch (JsError e) {
                actual = "!" + e;
            }
            if (!actual.equals(row[2])) {
                differences.add(row[0] + " dot=" + options.isDot() + " => " + row[2] + " / " + actual);
                continue;
            }
            if (actual.startsWith("!")) {
                continue;
            }
            Pattern regex = Picomatch.makeRe(row[0], options);
            for (Iterator<Map.Entry<String, JsonNode>> it = json(row[3]).fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> test = it.next();
                if (regex.matcher(test.getKey()).find() != test.getValue().asBoolean()) {
                    differences.add(row[0] + " dot=" + options.isDot() + " test " + test.getKey() + " => " +
                                    test.getValue());
                }
            }
        }
        assertThat(differences).isEmpty();
    }

    @Test
    void compilesAndMatchesAsPnpmsBundleDoes() {
        Options options = new Options(Release.V4_0_4, false, true);
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("pnpm/differential/globs-picomatch.tsv.gz", null)) {
            String pattern = row[0];
            String expected = row[1];
            String source;
            try {
                source = Picomatch.source(pattern, options);
            } catch (JsError e) {
                source = "!" + e.getMessage();
            }
            if (expected.startsWith("!") ? !source.startsWith("!") || !expected.endsWith(source.substring(1)) :
                    !expected.equals(source)) {
                differences.add(pattern + " => " + expected + " / " + source);
                continue;
            }
            if (expected.startsWith("!")) {
                continue;
            }
            Predicate<String> matcher = Picomatch.matcher(pattern, options);
            for (Iterator<Map.Entry<String, JsonNode>> it = json(row[2]).fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> test = it.next();
                if (matcher.test(test.getKey()) != test.getValue().asBoolean()) {
                    differences.add(pattern + " on " + test.getKey() + " => " + test.getValue());
                }
            }
        }
        assertThat(differences).isEmpty();
    }

    private static JsonNode json(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // the committed rows, then those of the same name under another directory
    private static List<String[]> rows(String resource, @Nullable String moreRows) {
        List<String[]> out = new ArrayList<>();
        read(PicomatchTest.class.getClassLoader().getResourceAsStream(resource), out);
        String name = resource.substring(resource.lastIndexOf('/') + 1);
        if (moreRows != null && Files.isRegularFile(Paths.get(moreRows, name))) {
            try {
                read(Files.newInputStream(Paths.get(moreRows, name)), out);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        assertThat(out).as(resource).isNotEmpty();
        return out;
    }

    private static void read(@Nullable InputStream gzipped, List<String[]> out) {
        if (gzipped == null) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(gzipped),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                String[] fields = line.split("\t", -1);
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = unescape(fields[i]);
                }
                out.add(fields);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String unescape(String field) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if (c == '\\' && i + 1 < field.length()) {
                char e = field.charAt(++i);
                out.append(e == 't' ? '\t' : e == 'n' ? '\n' : e == 'r' ? '\r' : e);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
