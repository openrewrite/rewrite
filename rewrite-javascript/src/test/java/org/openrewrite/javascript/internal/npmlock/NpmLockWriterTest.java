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
package org.openrewrite.javascript.internal.npmlock;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.internal.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NpmLockWriterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String resource(String name) {
        try (InputStream is = NpmLockWriterTest.class.getResourceAsStream(name)) {
            assertThat(is).as(name).isNotNull();
            return StringUtils.readFully(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * npm's emission is a deterministic full-document re-serialization, so parsing
     * any npm-written lock and re-emitting it must reproduce the input byte-for-byte.
     */
    @ParameterizedTest
    @ValueSource(strings = {
      "/npmlock/upgrade-leaf/package-lock.before.json",
      "/npmlock/upgrade-leaf/package-lock.after.json",
      "/npmlock/range-satisfied/package-lock.before.json",
      "/npmlock/cascade-fails/package-lock.before.json",
      "/npmlock/cascade-fails/package-lock.after.json",
      "/npmlock/remove-orphans/package-lock.before.json",
      "/npmlock/remove-orphans/package-lock.after.json",
      "/npmlock/upgrade-orphans/package-lock.before.json",
      "/npmlock/upgrade-orphans/package-lock.after.json",
      "/npmlock/add-leaf/package-lock.before.json",
      "/npmlock/add-leaf/package-lock.after.json",
      "/npmlock/override/package-lock.before.json",
      "/npmlock/override/package-lock.after.json",
      "/npmlock/dev-recolor/package-lock.before.json",
      "/npmlock/dev-recolor/package-lock.after.json",
      "/npmlock/dev-peer-overlap/package-lock.before.json",
      "/npmlock/dev-peer-overlap/package-lock.after.json",
      "/npmlock/scoped/package-lock.before.json",
      "/npmlock/scoped/package-lock.after.json"
    })
    void roundTripByteIdentity(String fixture) throws IOException {
        String content = resource(fixture);
        String out = NpmLockWriter.write(MAPPER.readTree(content),
          NpmLockWriter.detectIndent(content), NpmLockWriter.detectEol(content));
        assertThat(out).isEqualTo(content);
    }

    /**
     * Key ordering must match {@code localeCompare('en')} exactly; the vectors were
     * sorted by Node's own Intl collator (see record provenance in the README).
     */
    @Test
    void collationMatchesNodeIntl() {
        List<String> sorted = new ArrayList<>();
        for (String line : resource("/npmlock/locale-sorted-keys.txt").split("\n", -1)) {
            sorted.add(line);
        }
        if (!sorted.isEmpty() && sorted.get(sorted.size() - 1).isEmpty()) {
            sorted.remove(sorted.size() - 1);
        }
        for (int i = 1; i < sorted.size(); i++) {
            String prev = sorted.get(i - 1);
            String next = sorted.get(i);
            assertThat(NpmLockWriter.localeCompareEn(prev, next))
              .as("expected %s <= %s", prev, next)
              .isLessThanOrEqualTo(0);
        }
    }

    @Test
    void crlfPreserved() throws IOException {
        String content = resource("/npmlock/upgrade-leaf/package-lock.before.json");
        String crlf = content.replace("\n", "\r\n");
        String out = NpmLockWriter.write(MAPPER.readTree(crlf), "  ", NpmLockWriter.detectEol(crlf));
        assertThat(out).isEqualTo(crlf);
    }

    @Test
    void indentDetection() {
        assertThat(NpmLockWriter.detectIndent("{\n    \"a\": 1\n}")).isEqualTo("    ");
        assertThat(NpmLockWriter.detectIndent("{\n\t\"a\": 1\n}")).isEqualTo("\t");
        assertThat(NpmLockWriter.detectIndent("{}")).isEqualTo("  ");
    }
}
