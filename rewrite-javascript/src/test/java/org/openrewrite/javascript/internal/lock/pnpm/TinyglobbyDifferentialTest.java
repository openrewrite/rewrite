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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PicomatchScan;
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The glob modules pnpm 11.2.2 bundles against the port, row by row, from
 * {@code src/test/resources/pnpm/generate/globs.js}; a pattern outside the project is one the engine cannot know.
 */
class TinyglobbyDifferentialTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void scanSplitsAsPnpmDoes() {
        List<String> differences = new ArrayList<>();
        for (String[] row : rows("globs-scan.tsv.gz")) {
            List<String> parts = PicomatchScan.scan(row[0], true).getParts();
            boolean dynamic = Tinyglobby.isDynamicPattern(row[0]);
            if (!json(row[1]).equals(MAPPER.valueToTree(parts)) || dynamic != "1".equals(row[2])) {
                differences.add(row[0] + " => " + row[1] + " " + row[2] + " / " + parts + " " + dynamic);
            }
        }
        assertThat(differences).isEmpty();
    }

    @Test
    void findWorkspaceProjectsFindsWhatPnpmFinds() {
        List<String> differences = new ArrayList<>();
        int unknowable = 0;
        for (String[] row : rows("globs-workspaces.tsv.gz")) {
            List<String> dirs = new ArrayList<>();
            json(row[0]).forEach(d -> dirs.add(d.asText()));
            List<String> patterns = new ArrayList<>();
            json(row[1]).forEach(p -> patterns.add(p.asText()));
            String actual;
            try {
                actual = MAPPER.writeValueAsString(FindPackages.findWorkspaceProjects(dirs, patterns));
            } catch (CannotKnow e) {
                if (!outside(patterns)) {
                    differences.add(row[0] + " " + row[1] + " => " + row[2] + " / " + e.getMessage());
                }
                unknowable++;
                continue;
            } catch (JsError e) {
                actual = "!" + e.getMessage();
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
            String expected = row[2];
            if (expected.startsWith("!") ? !actual.startsWith("!") || !expected.endsWith(actual.substring(1)) :
                    !json(expected).equals(json(actual))) {
                differences.add(row[0] + " " + row[1] + " => " + expected + " / " + actual);
            }
        }
        assertThat(differences).isEmpty();
        assertThat(unknowable).as("rows with a pattern outside the project").isLessThan(100);
    }

    private static boolean outside(List<String> patterns) {
        for (String pattern : patterns) {
            String p = PosixPath.normalize(pattern.startsWith("!") ? pattern.substring(1) : pattern);
            if (p.startsWith("..") || p.startsWith("/")) {
                return true;
            }
        }
        return false;
    }

    private static JsonNode json(String text) {
        try {
            return MAPPER.readTree(text);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String[]> rows(String name) {
        List<String[]> out = new ArrayList<>();
        InputStream gzipped = TinyglobbyDifferentialTest.class.getClassLoader()
          .getResourceAsStream("pnpm/differential/" + name);
        assertThat(gzipped).as(name).isNotNull();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(gzipped),
          StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t", -1);
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = unescape(fields[i]);
                }
                out.add(fields);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertThat(out).as(name).isNotEmpty();
        return out;
    }

    private static String unescape(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                out.append(n == 't' ? '\t' : n == 'n' ? '\n' : n == 'r' ? '\r' : n);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
