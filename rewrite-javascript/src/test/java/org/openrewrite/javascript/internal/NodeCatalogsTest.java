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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.internal.NodeCatalogs.Pnpm.CatalogMode;
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
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each catalog read and edit against what pnpm 11.2.2, Yarn 4.10.3 and Bun 1.3.10 read or wrote in the recordings
 * under {@code lock/catalogs/}.
 */
class NodeCatalogsTest {

    @Test
    void pnpmEntryEditedInPlace() {
        assertThat(edited(pnpm("catalog-entry-one-importer").withSpecifier("default", "wrappy", "1.0.2")))
          .isEqualTo(file("pnpm/catalog-entry-one-importer/workspace"));
        assertThat(edited(pnpm("catalog-entry-named").withSpecifier("legacy", "is-number", "^7.0.0")))
          .isEqualTo(file("pnpm/catalog-entry-named/workspace"));
    }

    @Test
    void pnpmUpdateKeepsTheQuoting() {
        assertThat(edited(pnpm("catalog-update-shared-latest-quoted").withSpecifier("default", "wrappy", "1.0.2")))
          .isEqualTo(file("pnpm/catalog-update-shared-latest-quoted/workspace"));
        assertThat(edited(requireNonNull(pnpm("catalog-entry-quoted").withSpecifier("default", "is-number",
          ">=7.0.0")).withSpecifier("default", "wrappy", "1.0.2")))
          .isEqualTo(file("pnpm/catalog-entry-quoted/workspace"));
    }

    @Test
    void pnpmDefaultCatalogSpelledInCatalogs() {
        NodeCatalogs catalogs = pnpm("catalog-entry-default-spelling");
        assertThat(catalogs.catalogOf("catalog:")).isEqualTo("default");
        assertThat(catalogs.catalogOf("catalog:default")).isEqualTo("default");
        assertThat(catalogs.catalogOf("catalog: legacy ")).isEqualTo("legacy");
        assertThat(catalogs.catalogOf("^1.0.0")).isNull();
        assertThat(edited(catalogs.withSpecifier("default", "wrappy", "1.0.2")))
          .isEqualTo(file("pnpm/catalog-entry-default-spelling/workspace"));
    }

    @Test
    void bothSpellingsReadAsTheCatalogField() {
        NodeCatalogs catalogs = requireNonNull(NodeCatalogs.read(PackageManager.Pnpm, yaml(
          "catalog:\n  wrappy: 1.0.1\ncatalogs:\n  default:\n    wrappy: 1.0.2\n")));
        assertThat(catalogs.specifier("default", "wrappy")).isEqualTo("1.0.1");
    }

    @Test
    void anEditHasThreeAnswers() {
        NodeCatalogs catalogs = requireNonNull(NodeCatalogs.read(PackageManager.Pnpm, yaml(
          "catalog:\n  a: 1.0.0\n  b: >\n    ~1.4.1\n  c: 'npm:@acme/c@^1.4.1'\n")));
        assertThat(catalogs.withSpecifier("default", "missing", "2.0.0")).isNull();
        assertThat(catalogs.withSpecifier("default", "b", "2.0.0")).as("a block scalar").isNull();
        assertThat(catalogs.withSpecifier("default", "c", "2.0.0")).as("another protocol").isNull();
        assertThat(catalogs.withSpecifier("default", "a", "1.0.0")).isSameAs(catalogs);
        assertThat(requireNonNull(catalogs.withSpecifier("default", "a", "2.0.0")).specifier("default", "a"))
          .isEqualTo("2.0.0");
    }

    @Test
    void pnpmAddSortsTheNewEntryIn() {
        assertThat(edited(pnpm("catalog-add-root-declares-not-in-catalog-prefer").withEntry("default", "is-number",
          "7.0.0"))).isEqualTo(file("pnpm/catalog-add-root-declares-not-in-catalog-prefer/workspace"));
        assertThat(edited(pnpm("catalog-add-not-in-catalog-prefer-cleanup").withEntry("default", "is-number",
          "7.0.0"))).as("a new catalog key")
          .isEqualTo(file("pnpm/catalog-add-not-in-catalog-prefer-cleanup/workspace"));
    }

    @Test
    void anEntryAddedToUnsortedKeysGoesLast() {
        assertThat(edited(pnpm("catalog-rename-named").withEntry("legacy", "is-number", "^7.0.0")))
          .isEqualTo(file("pnpm/catalog-rename-named/workspace"));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      catalog-add-save-name-manual,          tools
      catalog-add-save-name-prefer,          tools
      catalog-add-save-name-prefer-mismatch, tools
      catalog-add-save-name-default,         default
      catalog-add-save-name-empty,           ''
      """)
    void pnpmAddWritesTheSavedCatalogsEntry(String scenario, String catalog) {
        assertThat(edited(pnpm(scenario).withEntry(catalog, "wrappy", "1.0.2")))
          .isEqualTo(file("pnpm/" + scenario + "/workspace"));
    }

    @Test
    void pnpmCleanupDropsUnusedEntriesAndEmptiedCatalogs() {
        assertThat(edited(pnpm("catalog-remove-manual-cleanup").withoutUnused(Arrays.asList(emptyMap(),
          declares("is-number", "catalog:legacy"))))).isEqualTo(file("pnpm/catalog-remove-manual-cleanup/workspace"));
        assertThat(edited(pnpm("catalog-remove-named-cleanup").withoutUnused(singletonList(
          declares("wrappy", "catalog:"))))).isEqualTo(file("pnpm/catalog-remove-named-cleanup/workspace"));
        assertThat(edited(pnpm("catalog-update-shared-recursive-cleanup").withoutUnused(Arrays.asList(
          declares("wrappy", "1.0.2"), declares("wrappy", "1.0.2")))))
          .isEqualTo(file("pnpm/catalog-update-shared-recursive-cleanup/workspace"));
    }

    @Test
    void pnpmCleanupWithProjectsDeclaringNothing() {
        assertThat(edited(pnpm("catalog-remove-last-cleanup").withoutUnused(Arrays.asList(emptyMap(),
          singletonMap("dependencies", emptyMap()))))).isEqualTo(file("pnpm/catalog-remove-last-cleanup/workspace"));
        NodeCatalogs.Pnpm catalogs = pnpm("catalog-remove-last-cleanup");
        assertThat(catalogs.withoutUnused(emptyList())).as("no projects").isSameAs(catalogs);
    }

    @Test
    void pnpmCleanupKeepsWhatAnOverrideOrTheDefaultReferenceNames() {
        NodeCatalogs.Pnpm catalogs = pnpmOf("overrides:\n  foo>ms: 'catalog:'\ncatalog:\n  ms: 2.1.3\n" +
          "  wrappy: 1.0.2\ncatalogs:\n  legacy:\n    wrappy: 1.0.1\n");
        NodeCatalogs cleaned = requireNonNull(catalogs.withoutUnused(singletonList(declares("wrappy", "catalog:"))));
        assertThat(cleaned.specifier("default", "ms")).isEqualTo("2.1.3");
        assertThat(cleaned.specifier("legacy", "wrappy")).as("catalog: keeps every catalog's entry").isEqualTo("1.0.1");
        assertThat(requireNonNull(catalogs.withoutUnused(singletonList(declares("wrappy", "catalog:default"))))
          .specifier("default", "wrappy")).as("pnpm counts only catalog:").isNull();
    }

    @Test
    void pnpmCleanupDropsAnAlreadyEmptyNamedCatalog() {
        NodeCatalogs.Pnpm catalogs = pnpmOf("catalog:\n  wrappy: 1.0.2\ncatalogs:\n  legacy: {}\n  tools:\n" +
          "    ms: 2.1.3\n");
        assertThat(edited(catalogs.withoutUnused(singletonList(declares("ms", "catalog:tools")))))
          .isEqualTo("catalogs:\n  tools:\n    ms: 2.1.3\n");
    }

    @ParameterizedTest
    @ValueSource(strings = {"catalog: {wrappy: 1.0.1, ms: 2.1.3}\n",
      "catalogs: {legacy: {wrappy: 1.0.1}}\n", "catalogs:\n  legacy: {wrappy: 1.0.1}\n"})
    void flowStyleCatalogsAreNeverEdited(String workspace) {
        NodeCatalogs.Pnpm catalogs = pnpmOf(workspace);
        assertThat(catalogs.isFlowStyle()).isTrue();
        assertThat(catalogs.withSpecifier("default", "wrappy", "1.0.2")).isNull();
        assertThat(catalogs.withSpecifier("legacy", "wrappy", "1.0.2")).isNull();
        assertThat(catalogs.withEntry("default", "is-number", "7.0.0")).isNull();
        assertThat(catalogs.withoutUnused(singletonList(declares("ms", "catalog:")))).isNull();
        assertThat(pnpmOf("catalog:\n  wrappy: 1.0.1\n").isFlowStyle()).isFalse();
    }

    @Test
    void aNewEntryNeverDuplicatesAKey() {
        assertThat(pnpmOf("catalog:\npackages:\n  - a\n").withEntry("default", "wrappy", "1.0.2"))
          .as("catalog holds no mapping").isNull();
        assertThat(pnpmOf("catalog:\n  wrappy:\n    nested: 1\n").withEntry("default", "wrappy", "1.0.2"))
          .as("the entry holds no scalar").isNull();
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      catalog-add-root-declares-in-catalog-strict-cleanup, STRICT,  true
      catalog-entry-one-importer,                          MANUAL,  false
      catalog-add-mode-capitalized-mismatch,               PREFER,  false
      catalog-add-mode-null-mismatch,                      PREFER,  false
      catalog-add-save-name-prefer,                        PREFER,  false
      catalog-remove-cleanup-capitalized-true,             MANUAL,  true
      catalog-remove-cleanup-quoted-false,                 MANUAL,  true
      catalog-remove-cleanup-no,                           MANUAL,  true
      """)
    void pnpmSettingsAsPnpmReadsThem(String scenario, CatalogMode mode, boolean cleanup) {
        NodeCatalogs.Pnpm catalogs = pnpm(scenario);
        assertThat(catalogs.getCatalogMode()).isEqualTo(mode);
        assertThat(catalogs.isCleanupUnusedCatalogs()).isEqualTo(cleanup);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", textBlock = """
      'cleanupUnusedCatalogs: false',   false
      'cleanupUnusedCatalogs: FALSE',   false
      'cleanupUnusedCatalogs: ~',       false
      'cleanupUnusedCatalogs:',         false
      'cleanupUnusedCatalogs: 0',       false
      'cleanupUnusedCatalogs: 0x0',     false
      'cleanupUnusedCatalogs: .nan',    false
      'cleanupUnusedCatalogs: ''''',    false
      'cleanupUnusedCatalogs: 1',       true
      'cleanupUnusedCatalogs: 0_',      true
      'cleanupUnusedCatalogs: {}',      true
      """)
    void cleanupIsWhatJavaScriptHoldsTrue(String setting, boolean cleanup) {
        assertThat(pnpmOf(setting + "\n").isCleanupUnusedCatalogs()).isEqualTo(cleanup);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", textBlock = """
      catalog-add-save-name-manual,  tools
      catalog-add-save-name-default, default
      catalog-add-save-name-empty,   ''
      catalog-add-save-name-null,    null
      catalog-entry-one-importer,    null
      """)
    void saveCatalogNameAsPnpmReadsIt(String scenario, @Nullable String name) {
        assertThat(pnpm(scenario).getSaveCatalogName()).isEqualTo(name);
    }

    @Test
    void yarnReadsCatalogAndCatalogsApart() {
        NodeCatalogs catalogs = yarn("catalog-yarnrc-default-name-edit");
        assertThat(catalogs.catalogOf("catalog:")).isEqualTo("");
        assertThat(catalogs.catalogOf("catalog:default")).isEqualTo("default");
        assertThat(catalogs.resolve("ms", "catalog:")).isEqualTo("2.0.0");
        assertThat(catalogs.resolve("ms", "catalog:default")).isEqualTo("2.1.2");
        assertThat(edited(catalogs.withSpecifier("default", "ms", "2.1.3")))
          .isEqualTo(file("yarn-berry/catalog-yarnrc-default-name-edit/yarnrc-after"));
    }

    @Test
    void yarnEntryEditedInPlace() {
        assertThat(edited(yarn("catalog-yarnrc-default-edit").withSpecifier("", "ms", "2.1.3")))
          .isEqualTo(file("yarn-berry/catalog-yarnrc-default-edit/yarnrc-after"));
        assertThat(edited(yarn("catalog-yarnrc-named-edit").withSpecifier("legacy", "is-number", "^7.0.0")))
          .isEqualTo(file("yarn-berry/catalog-yarnrc-named-edit/yarnrc-after"));
        assertThat(edited(requireNonNull(yarn("catalog-yarnrc-quoted-edit").withSpecifier("", "is-number", ">=7.0.0"))
          .withSpecifier("", "ms", "2.1.3"))).isEqualTo(file("yarn-berry/catalog-yarnrc-quoted-edit/yarnrc-after"));
    }

    @Test
    void bunReadsWorkspacesThenTheTopLevel() {
        assertThat(bun("catalog-both-places-edit-workspaces").specifier("", "ms")).isEqualTo("2.0.0");
        assertThat(bun("catalog-top-level-fallback-edit").specifier("", "ms")).isEqualTo("2.0.0");
        assertThat(bun("catalog-workspaces-object-top-level-edit").specifier("", "ms")).isEqualTo("2.0.0");
        assertThat(bun("catalog-top-level-named-fallback-edit").specifier("legacy", "is-number")).isEqualTo("^6.0.0");
        assertThat(bun("catalog-workspaces-catalog-edit").catalogOf("catalog:default")).isEqualTo("default");
        assertThat(NodeCatalogs.read(PackageManager.Bun, json("{\"catalog\":{\"ms\":\"2.0.0\"}}")))
          .as("no workspaces").isNull();
    }

    @Test
    void bunEntryEditedInThePackageJson() {
        List<String[]> edits = Arrays.asList(
          new String[]{"catalog-workspaces-catalog-edit", "", "ms", "2.1.3"},
          new String[]{"catalog-workspaces-catalogs-named-edit", "legacy", "is-number", "^7.0.0"},
          new String[]{"catalog-top-level-fallback-edit", "", "ms", "2.1.3"},
          new String[]{"catalog-top-level-named-fallback-edit", "legacy", "is-number", "^7.0.0"},
          new String[]{"catalog-workspaces-object-top-level-edit", "", "ms", "2.1.3"},
          new String[]{"catalog-both-places-edit-workspaces", "", "ms", "2.1.2"});
        for (String[] edit : edits) {
            assertThat(edited(bun(edit[0]).withSpecifier(edit[1], edit[2], edit[3]))).as(edit[0])
              .isEqualTo(file("bun/" + edit[0] + "/pkg"));
        }
        assertThat(edited(bun("catalog-rename").withEntry("", "is-number", "7.0.0")))
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

    private static Map<String, Map<String, String>> declares(String name, String value) {
        return singletonMap("dependencies", singletonMap(name, value));
    }

    private static String edited(@Nullable NodeCatalogs catalogs) {
        return requireNonNull(catalogs).print();
    }

    private static NodeCatalogs.Pnpm pnpm(String scenario) {
        return pnpmOf(file("pnpm/" + scenario + "/workspace-before"));
    }

    private static NodeCatalogs.Pnpm pnpmOf(String text) {
        return (NodeCatalogs.Pnpm) requireNonNull(NodeCatalogs.read(PackageManager.Pnpm, yaml(text)));
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
        try (InputStream in = NodeCatalogsTest.class.getResourceAsStream("/lock/catalogs/" + path)) {
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
