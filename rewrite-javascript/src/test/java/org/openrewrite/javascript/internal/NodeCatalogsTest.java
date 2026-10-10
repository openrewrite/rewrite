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
package org.openrewrite.javascript.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.JsonParser;
import org.openrewrite.yaml.YamlParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.javascript.internal.NodeCatalogs.DEFAULT;

/**
 * Each catalog read and edit against what pnpm 11.2.2, Yarn 4.10.3 and Bun 1.3.10 read or wrote in the recordings
 * under {@code catalogs/}.
 */
class NodeCatalogsTest {

    @Test
    void pnpmEntryEditedInPlace() {
        assertThat(pnpm("catalog-entry-one-importer").withSpecifier(DEFAULT, "wrappy", "1.0.2").print())
          .isEqualTo(file("pnpm/catalog-entry-one-importer/workspace"));
        assertThat(pnpm("catalog-entry-named").withSpecifier("legacy", "is-number", "^7.0.0").print())
          .isEqualTo(file("pnpm/catalog-entry-named/workspace"));
    }

    @Test
    void pnpmUpdateKeepsTheQuoting() {
        assertThat(pnpm("catalog-update-shared-latest-quoted").withSpecifier(DEFAULT, "wrappy", "1.0.2").print())
          .isEqualTo(file("pnpm/catalog-update-shared-latest-quoted/workspace"));
        assertThat(requireNonNull(pnpm("catalog-entry-quoted").withSpecifier(DEFAULT, "is-number", ">=7.0.0"))
          .withSpecifier(DEFAULT, "wrappy", "1.0.2").print())
          .isEqualTo(file("pnpm/catalog-entry-quoted/workspace"));
    }

    @Test
    void pnpmDefaultCatalogSpelledInCatalogs() {
        NodeCatalogs catalogs = pnpm("catalog-entry-default-spelling");
        assertThat(catalogs.catalogOf("catalog:")).isEqualTo(DEFAULT);
        assertThat(catalogs.catalogOf("catalog:default")).isEqualTo(DEFAULT);
        assertThat(catalogs.catalogOf("catalog: legacy ")).isEqualTo("legacy");
        assertThat(catalogs.catalogOf("^1.0.0")).isNull();
        assertThat(catalogs.withSpecifier(DEFAULT, "wrappy", "1.0.2").print())
          .isEqualTo(file("pnpm/catalog-entry-default-spelling/workspace"));
    }

    @Test
    void pnpmRejectsBothSpellings() {
        NodeCatalogs catalogs = NodeCatalogs.read(PackageManager.Pnpm, yaml(
          "catalog:\n  wrappy: 1.0.1\ncatalogs:\n  default:\n    wrappy: 1.0.2\n"));
        assertThat(requireNonNull(catalogs).getMisconfiguration())
          .startsWith("The 'default' catalog was defined multiple times.");
        assertThat(catalogs.specifier(DEFAULT, "wrappy")).isEqualTo("1.0.1");
    }

    @Test
    void anEditHasThreeAnswers() {
        NodeCatalogs catalogs = requireNonNull(NodeCatalogs.read(PackageManager.Pnpm, yaml(
          "catalog:\n  a: 1.0.0\n  b: >\n    ~1.4.1\n  c: 'npm:@acme/c@^1.4.1'\n")));
        assertThat(catalogs.withSpecifier(DEFAULT, "missing", "2.0.0")).isNull();
        assertThat(catalogs.withSpecifier(DEFAULT, "b", "2.0.0")).as("a block scalar").isNull();
        assertThat(catalogs.withSpecifier(DEFAULT, "c", "2.0.0")).as("another protocol").isNull();
        assertThat(catalogs.withSpecifier(DEFAULT, "a", "1.0.0")).isSameAs(catalogs);
        assertThat(requireNonNull(catalogs.withSpecifier(DEFAULT, "a", "2.0.0")).specifier(DEFAULT, "a"))
          .isEqualTo("2.0.0");
    }

    @Test
    void pnpmAddSortsTheNewEntryIn() {
        assertThat(pnpm("catalog-add-root-declares-not-in-catalog-prefer").withEntry(DEFAULT, "is-number", "7.0.0")
          .print()).isEqualTo(file("pnpm/catalog-add-root-declares-not-in-catalog-prefer/workspace"));
        assertThat(pnpm("catalog-add-not-in-catalog-prefer-cleanup").withEntry(DEFAULT, "is-number", "7.0.0")
          .print()).as("a new catalog key").isEqualTo(file("pnpm/catalog-add-not-in-catalog-prefer-cleanup/workspace"));
    }

    @Test
    void anEntryAddedToUnsortedKeysGoesLast() {
        assertThat(pnpm("catalog-rename-named").withEntry("legacy", "is-number", "^7.0.0").print())
          .isEqualTo(file("pnpm/catalog-rename-named/workspace"));
    }

    @Test
    void pnpmCleanupDropsUnusedEntriesAndEmptiedCatalogs() {
        assertThat(pnpm("catalog-remove-manual-cleanup").withoutUnused(Arrays.asList(
          Collections.emptyMap(), Collections.singletonMap("is-number", "catalog:legacy"))).print())
          .isEqualTo(file("pnpm/catalog-remove-manual-cleanup/workspace"));
        assertThat(pnpm("catalog-remove-named-cleanup").withoutUnused(Collections.singletonList(
          Collections.singletonMap("wrappy", "catalog:"))).print())
          .isEqualTo(file("pnpm/catalog-remove-named-cleanup/workspace"));
        assertThat(pnpm("catalog-update-shared-recursive-cleanup").withoutUnused(Arrays.asList(
          Collections.singletonMap("wrappy", "1.0.2"), Collections.singletonMap("wrappy", "1.0.2"))).print())
          .isEqualTo(file("pnpm/catalog-update-shared-recursive-cleanup/workspace"));
    }

    @Test
    void pnpmCleanupKeepsWhatAnOverrideOrTheDefaultReferenceNames() {
        NodeCatalogs catalogs = requireNonNull(NodeCatalogs.read(PackageManager.Pnpm, yaml(
          "overrides:\n  foo>ms: 'catalog:'\ncatalog:\n  ms: 2.1.3\n  wrappy: 1.0.2\n" +
          "catalogs:\n  legacy:\n    wrappy: 1.0.1\n")));
        Map<String, String> declared = new HashMap<>();
        declared.put("wrappy", "catalog:");
        NodeCatalogs cleaned = catalogs.withoutUnused(Collections.singletonList(declared));
        assertThat(cleaned.specifier(DEFAULT, "ms")).isEqualTo("2.1.3");
        assertThat(cleaned.specifier("legacy", "wrappy")).as("catalog: keeps every catalog's entry").isEqualTo("1.0.1");
        assertThat(catalogs.withoutUnused(Collections.singletonList(Collections.singletonMap("wrappy",
          "catalog:default"))).specifier(DEFAULT, "wrappy")).as("pnpm counts only catalog:").isNull();
    }

    @Test
    void pnpmSettings() {
        assertThat(pnpm("catalog-add-root-declares-in-catalog-strict-cleanup").getCatalogMode()).isEqualTo("strict");
        assertThat(pnpm("catalog-add-root-declares-in-catalog-strict-cleanup").isCleanupUnusedCatalogs()).isTrue();
        assertThat(pnpm("catalog-entry-one-importer").getCatalogMode()).isEqualTo("manual");
        assertThat(pnpm("catalog-entry-one-importer").isCleanupUnusedCatalogs()).isFalse();
    }

    @Test
    void yarnReadsCatalogAndCatalogsApart() {
        NodeCatalogs catalogs = yarn("catalog-yarnrc-default-name-edit");
        assertThat(catalogs.catalogOf("catalog:")).isEqualTo(DEFAULT);
        assertThat(catalogs.catalogOf("catalog:default")).isEqualTo("default");
        assertThat(catalogs.resolve("ms", "catalog:")).isEqualTo("2.0.0");
        assertThat(catalogs.resolve("ms", "catalog:default")).isEqualTo("2.1.2");
        assertThat(catalogs.withSpecifier("default", "ms", "2.1.3").print())
          .isEqualTo(file("yarn-berry/catalog-yarnrc-default-name-edit/yarnrc-after"));
    }

    @Test
    void yarnEntryEditedInPlace() {
        assertThat(yarn("catalog-yarnrc-default-edit").withSpecifier(DEFAULT, "ms", "2.1.3").print())
          .isEqualTo(file("yarn-berry/catalog-yarnrc-default-edit/yarnrc-after"));
        assertThat(yarn("catalog-yarnrc-named-edit").withSpecifier("legacy", "is-number", "^7.0.0").print())
          .isEqualTo(file("yarn-berry/catalog-yarnrc-named-edit/yarnrc-after"));
        assertThat(requireNonNull(yarn("catalog-yarnrc-quoted-edit").withSpecifier(DEFAULT, "is-number", ">=7.0.0"))
          .withSpecifier(DEFAULT, "ms", "2.1.3").print())
          .isEqualTo(file("yarn-berry/catalog-yarnrc-quoted-edit/yarnrc-after"));
    }

    @Test
    void bunReadsWorkspacesThenTheTopLevel() {
        assertThat(bun("catalog-both-places-edit-workspaces").specifier(DEFAULT, "ms")).isEqualTo("2.0.0");
        assertThat(bun("catalog-top-level-fallback-edit").specifier(DEFAULT, "ms")).isEqualTo("2.0.0");
        assertThat(bun("catalog-workspaces-object-top-level-edit").specifier(DEFAULT, "ms")).isEqualTo("2.0.0");
        assertThat(bun("catalog-top-level-named-fallback-edit").specifier("legacy", "is-number")).isEqualTo("^6.0.0");
        assertThat(bun("catalog-workspaces-catalog-edit").catalogOf("catalog:default")).isEqualTo("default");
        assertThat(NodeCatalogs.read(PackageManager.Bun, json("{\"catalog\":{\"ms\":\"2.0.0\"}}")))
          .as("no workspaces").isNull();
    }

    @Test
    void bunEntryEditedInThePackageJson() {
        List<String[]> edits = Arrays.asList(
          new String[]{"catalog-workspaces-catalog-edit", DEFAULT, "ms", "2.1.3"},
          new String[]{"catalog-workspaces-catalogs-named-edit", "legacy", "is-number", "^7.0.0"},
          new String[]{"catalog-top-level-fallback-edit", DEFAULT, "ms", "2.1.3"},
          new String[]{"catalog-top-level-named-fallback-edit", "legacy", "is-number", "^7.0.0"},
          new String[]{"catalog-workspaces-object-top-level-edit", DEFAULT, "ms", "2.1.3"},
          new String[]{"catalog-both-places-edit-workspaces", DEFAULT, "ms", "2.1.2"});
        for (String[] edit : edits) {
            assertThat(requireNonNull(bun(edit[0]).withSpecifier(edit[1], edit[2], edit[3])).print()).as(edit[0])
              .isEqualTo(file("bun/" + edit[0] + "/pkg"));
        }
        assertThat(bun("catalog-rename").withEntry(DEFAULT, "is-number", "7.0.0").print())
          .isEqualTo(file("bun/catalog-rename/pkg"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"~1.5.0", "^1.2.3", "1.4.1", "10.1.0-rc.1", "1.4.2-beta.00001", "1.x", "v2", "latest",
      "next", "1.0.0-alpha.1+build.7"})
    void constraintsThatCanStandUnquoted(String constraint) {
        assertThat(NodeCatalogs.canBePlainScalar(constraint)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {">=2.0.0", "*", "-1.0.0", "?1", "|1", ">1", "2", "2.0", "1_0", "0x1F", "0b11", ".inf",
      "true", "False", "yes", "NO", "on", "off", "null", "~", "a: b", "a #c", "---", " 1.0.0", "1.0.0 "})
    void constraintsThatMustBeQuoted(String constraint) {
        assertThat(NodeCatalogs.canBePlainScalar(constraint)).isFalse();
    }

    private static NodeCatalogs pnpm(String scenario) {
        return requireNonNull(NodeCatalogs.read(PackageManager.Pnpm,
          yaml(file("pnpm/" + scenario + "/workspace-before"))));
    }

    private static NodeCatalogs yarn(String scenario) {
        return requireNonNull(NodeCatalogs.read(PackageManager.YarnBerry,
          yaml(file("yarn-berry/" + scenario + "/yarnrc"))));
    }

    private static NodeCatalogs bun(String scenario) {
        return requireNonNull(NodeCatalogs.read(PackageManager.Bun, json(file("bun/" + scenario + "/pkg-before"))));
    }

    private static SourceFile yaml(String text) {
        return YamlParser.builder().build().parse(text).findFirst().orElseThrow(IllegalStateException::new)
          .withSourcePath(Paths.get("pnpm-workspace.yaml"));
    }

    private static SourceFile json(String text) {
        return JsonParser.builder().build().parse(text).findFirst().orElseThrow(IllegalStateException::new)
          .withSourcePath(Paths.get("package.json"));
    }

    static String file(String path) {
        try (InputStream in = NodeCatalogsTest.class.getResourceAsStream("/catalogs/" + path)) {
            if (in == null) {
                throw new IllegalArgumentException(path);
            }
            return new String(readAll(in), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }
}
