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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.javascript.internal.lock.YamlDocument;
import org.openrewrite.yaml.YamlParser;
import org.openrewrite.yaml.tree.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Where pnpm's workspace manifest writer puts a new key and how it writes a value, the values from pnpm 11.2.2's own
 * write ({@code lock/catalogs/pnpm-writes/catalog-scalars}).
 */
class WorkspaceManifestWriterTest {

    private static final String SCALARS = "/lock/catalogs/pnpm-writes/catalog-scalars/";

    @Test
    void eachValueIsWrittenAsPnpmWritesIt() throws IOException {
        Map<String, String> set = new ObjectMapper().readValue(read("config-set"),
          new TypeReference<LinkedHashMap<String, String>>() {
          });
        StringBuilder written = new StringBuilder("packages:\n  - .\ncatalog:\n");
        for (Map.Entry<String, String> entry : set.entrySet()) {
            written.append("  ").append(entry.getKey()).append(": ")
              .append(YamlDocument.scalar(entry.getValue())).append('\n');
        }
        assertThat(written.toString()).isEqualTo(read("workspace"));
    }

    @Test
    void aValueThatWouldNeedAnEscapeIsRefused() {
        assertThatThrownBy(() -> YamlDocument.scalar("1.0.0\u0007"))
          .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> YamlDocument.scalar("1.0.0\u2028"))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pnpm11SortsANewKeyIntoASortedLevelAndAppendsItToAnother() {
        WorkspaceManifestWriter pnpm11 = new WorkspaceManifestWriter(11);
        assertThat(pnpm11.insertionIndex(Arrays.asList("packages", "catalog", "overrides"), "minimumReleaseAge"))
          .isEqualTo(2);
        assertThat(pnpm11.insertionIndex(Arrays.asList("catalog", "overrides", "packages"), "minimumReleaseAge"))
          .isEqualTo(1);
        assertThat(pnpm11.insertionIndex(Arrays.asList("packages", "overrides", "catalog"), "minimumReleaseAge"))
          .isEqualTo(3);
        assertThat(pnpm11.insertionIndex(Arrays.asList(), "packages")).isEqualTo(0);
    }

    @Test
    void pnpm10SortsEveryLevelPackagesFirst() {
        WorkspaceManifestWriter pnpm10 = new WorkspaceManifestWriter(10);
        assertThat(pnpm10.insertionIndex(Arrays.asList("packages", "overrides", "catalog"), "minimumReleaseAge"))
          .isEqualTo(2);
        assertThat(pnpm10.insertionIndex(Arrays.asList("catalog", "packages"), "overrides")).isEqualTo(2);
    }

    @Test
    void aNewTopLevelPairFollowsTheDocumentsBlankLines() {
        String separated = "packages:\n  - a\n\ncatalog:\n  x: 1.0.0\n\noverrides:\n  y: 1.0.0\n";
        String packed = "packages:\n  - a\ncatalog:\n  x: 1.0.0\n\noverrides:\n  y: 1.0.0\n";
        assertThat(blankBeforeNew(11, separated, "nodeLinker")).isTrue();
        assertThat(blankBeforeNew(11, packed, "nodeLinker")).isTrue();
        assertThat(blankBeforeNew(11, packed, "allowBuilds")).isFalse();
        assertThat(blankBeforeNew(11, packed, "publicHoistPattern")).isTrue();
        assertThat(blankBeforeNew(11, "packages:\n  - a\ncatalog:\n  x: 1.0.0\n", "nodeLinker")).isFalse();
        assertThat(blankBeforeNew(10, separated, "nodeLinker")).isFalse();
    }

    // the blank line before key once the release's writer added it to the document yaml read
    private static boolean blankBeforeNew(int release, String yaml, String key) {
        YamlDocument document = YamlDocument.parse((Yaml.Documents) YamlParser.builder().build()
          .parse(yaml).findFirst().orElseThrow(IllegalStateException::new));
        @SuppressWarnings("unchecked")
        Map<String, @Nullable Object> manifest = new LinkedHashMap<>((Map<String, @Nullable Object>) document.toJs());
        Map<String, @Nullable Object> original = new LinkedHashMap<>();
        for (String existing : manifest.keySet()) {
            original.put(existing, null);
        }
        manifest.put(key, "hoisted");
        new WorkspaceManifestWriter(release).write(document, original, manifest);
        return document.blankBefore(document.keys().indexOf(key));
    }

    private static String read(String file) throws IOException {
        try (InputStream in = requireNonNull(WorkspaceManifestWriterTest.class.getResourceAsStream(SCALARS + file))) {
            return StringUtils.readFully(in, StandardCharsets.UTF_8);
        }
    }
}
