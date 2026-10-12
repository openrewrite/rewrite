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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.JsonParser;
import org.openrewrite.yaml.YamlParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void bothSpellingsOfTheDefaultCatalogAreDeclinedAsPnpmRejectsThem() {
        String recorded = "pnpm-rejects/11.2.2/catalog-and-catalogs-default/";
        NodeCatalogs catalogs = requireNonNull(NodeCatalogs.read(PackageManager.Pnpm,
          yaml(file(recorded + "workspace-before"))));
        assertThat(catalogs.declined())
          .isEqualTo("ERR_PNPM_INVALID_CATALOGS_CONFIGURATION " + file(recorded + "pnpm.log")
            .substring("[ERROR] ".length(), file(recorded + "pnpm.log").indexOf('\n')));
        assertThat(catalogs.withEntry("default", "wrappy", "1.0.2")).isNull();
        assertThat(file(recorded + "workspace")).isEqualTo(file(recorded + "workspace-before"));
    }

    // pnpm 10.34.6 writes the document sorted at every level, packages first, for an add and for an update alike
    @Test
    void pnpm10SortsEveryLevelOfTheDocumentItWrites() {
        assertThat(edited(pnpmOfRoot("catalog-add-pnpm10-unsorted").withEntry("default", "is-number", "7.0.0")))
          .isEqualTo(file("pnpm/catalog-add-pnpm10-unsorted/workspace"));
        assertThat(edited(pnpmOfRoot("catalog-update-pnpm10-unsorted").withSpecifier("default", "wrappy", "1.0.2")))
          .isEqualTo(file("pnpm/catalog-update-pnpm10-unsorted/workspace"));
    }

    // pnpm 9.15.9 never writes pnpm-workspace.yaml and has no cleanupUnusedCatalogs (catalog-cleanup-pnpm9)
    @Test
    void pnpm9WritesNoNewEntryAndCleansNothingUp() {
        NodeCatalogs.Pnpm catalogs = pnpmOfRoot("catalog-cleanup-pnpm9");
        assertThat(catalogs.isCleanupUnusedCatalogs()).isTrue();
        assertThat(catalogs.cleansUp()).isFalse();
        assertThat(catalogs.withoutUnused(projects(Arrays.asList(emptyMap(), declares("wrappy", "catalog:")))))
          .isSameAs(catalogs);
        assertThat(catalogs.withEntry("default", "is-number", "7.0.0")).isNull();
        assertThat(edited(catalogs.withSpecifier("default", "wrappy", "1.0.1")))
          .isEqualTo(file("pnpm/catalog-cleanup-pnpm9/workspace").replace("wrappy: 1.0.2", "wrappy: 1.0.1"));
    }

    // each range's first release and the one below it recorded (probes pnpm-releases, pnpm-writes-below); corepack
    // 0.35.0 refuses a range, recorded
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
      {"devEngines":{"packageManager":{"name":"pnpm","version":">=9"}}}     | (pnpm@>=9); expected a semver version
      {"devEngines":{"packageManager":{"name":"pnpm","version":"^10.0.0"}}} | (pnpm@^10.0.0); expected a semver
      {"packageManager":"pnpm@12.0.0"}                                      | pnpm 12.0.0 is not
      {"packageManager":"pnpm@11.0.2"}                                      | pnpm 11.0.2 is not
      {"packageManager":"pnpm@10.31.0"}                                     | pnpm 10.31.0 is not
      {"packageManager":"pnpm@10.0.0"}                                      | pnpm 10.0.0 is not
      {"packageManager":"pnpm@9.4.0"}                                       | pnpm 9.4.0 is not
      {"packageManager":"pnpm@8.15.9"}                                      | pnpm 8.15.9 is not
      """)
    void aReleaseNoRecordingCoversIsDeclined(String root, String why) {
        NodeCatalogs.Pnpm catalogs = (NodeCatalogs.Pnpm) requireNonNull(NodeCatalogs.read(PackageManager.Pnpm,
          yaml("cleanupUnusedCatalogs: true\ncatalog:\n  wrappy: 1.0.1\n"), root));
        assertThat(catalogs.declined()).contains(why);
        assertThat(catalogs.withSpecifier("default", "wrappy", "1.0.2")).isNull();
        assertThat(catalogs.withEntry("default", "ms", "2.1.3")).isNull();
        assertThat(catalogs.withoutUnused(projects(singletonList(emptyMap())))).isNull();
    }

    static Stream<Arguments> catalogWrites() throws IOException, URISyntaxException {
        Path root = Paths.get(requireNonNull(NodeCatalogsTest.class.getResource("/lock/catalogs/pnpm-writes/"))
          .toURI());
        List<Arguments> cases = new ArrayList<>();
        try (Stream<Path> releases = Files.list(root)) {
            for (Path release : (Iterable<Path>) releases.sorted()::iterator) {
                if (!release.getFileName().toString().matches("\\d+\\.\\d+\\.\\d+")) {
                    continue;
                }
                try (Stream<Path> dirs = Files.list(release)) {
                    dirs.sorted().forEach(d -> cases.add(Arguments.of(release.getFileName() + "/" + d.getFileName(),
                      d)));
                }
            }
        }
        return cases.stream();
    }

    // pnpm's own writes of a catalog (config set --location=project --json catalog), each the edit named in `edit`
    @ParameterizedTest(name = "{0}")
    @MethodSource("catalogWrites")
    void writesTheCatalogAsPnpmDoes(String name, Path dir) throws IOException {
        String[] edit = new String(Files.readAllBytes(dir.resolve("edit")), StandardCharsets.UTF_8).split(" ", 4);
        NodeCatalogs catalogs = requireNonNull(NodeCatalogs.read(PackageManager.Pnpm, yaml(new String(
          Files.readAllBytes(dir.resolve("workspace-before")), StandardCharsets.UTF_8)),
          "{\"name\":\"p\",\"packageManager\":\"pnpm@" + name.substring(0, name.indexOf('/')) + "\"}"));
        NodeCatalogs written = "entry".equals(edit[0]) ? catalogs.withEntry(edit[1], edit[2], edit[3]) :
          catalogs.withSpecifier(edit[1], edit[2], edit[3]);
        assertThat(edited(written))
          .isEqualTo(new String(Files.readAllBytes(dir.resolve("workspace")), StandardCharsets.UTF_8));
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
        assertThat(edited(pnpm("catalog-remove-manual-cleanup").withoutUnused(projects(Arrays.asList(emptyMap(),
          declares("is-number", "catalog:legacy")))))).isEqualTo(file("pnpm/catalog-remove-manual-cleanup/workspace"));
        assertThat(edited(pnpm("catalog-remove-named-cleanup").withoutUnused(projects(singletonList(
          declares("wrappy", "catalog:")))))).isEqualTo(file("pnpm/catalog-remove-named-cleanup/workspace"));
        assertThat(edited(pnpm("catalog-update-shared-recursive-cleanup").withoutUnused(projects(Arrays.asList(
          declares("wrappy", "1.0.2"), declares("wrappy", "1.0.2"))))))
          .isEqualTo(file("pnpm/catalog-update-shared-recursive-cleanup/workspace"));
    }

    @Test
    void pnpmCleanupWithProjectsDeclaringNothing() {
        assertThat(edited(pnpm("catalog-remove-last-cleanup").withoutUnused(projects(Arrays.asList(emptyMap(),
          singletonMap("dependencies", emptyMap())))))).isEqualTo(file("pnpm/catalog-remove-last-cleanup/workspace"));
        NodeCatalogs.Pnpm catalogs = pnpm("catalog-remove-last-cleanup");
        assertThat(catalogs.withoutUnused(projects(emptyList()))).as("no projects").isSameAs(catalogs);
    }

    @Test
    void pnpmCleanupKeepsWhatAnOverrideOrTheDefaultReferenceNames() {
        NodeCatalogs.Pnpm catalogs = pnpmOf("overrides:\n  foo>ms: 'catalog:'\ncatalog:\n  ms: 2.1.3\n" +
          "  wrappy: 1.0.2\ncatalogs:\n  legacy:\n    wrappy: 1.0.1\n");
        NodeCatalogs cleaned = requireNonNull(catalogs.withoutUnused(projects(singletonList(declares("wrappy",
                "catalog:")))));
        assertThat(cleaned.specifier("default", "ms")).isEqualTo("2.1.3");
        assertThat(cleaned.specifier("legacy", "wrappy")).as("catalog: keeps every catalog's entry").isEqualTo("1.0.1");
        assertThat(requireNonNull(catalogs.withoutUnused(projects(singletonList(declares("wrappy",
                "catalog:default")))))
          .specifier("default", "wrappy")).as("pnpm counts only catalog:").isNull();
    }

    @Test
    void pnpmCleanupDropsAnAlreadyEmptyNamedCatalog() {
        NodeCatalogs.Pnpm catalogs = pnpmOf("catalog:\n  wrappy: 1.0.2\ncatalogs:\n  legacy: {}\n  tools:\n" +
          "    ms: 2.1.3\n");
        assertThat(edited(catalogs.withoutUnused(projects(singletonList(declares("ms", "catalog:tools"))))))
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
        assertThatThrownBy(() -> catalogs.withoutUnused(projects(singletonList(declares("ms", "catalog:")))))
          .hasMessageContaining("flow-style catalogs");
        assertThat(pnpmOf("catalog:\n  wrappy: 1.0.1\n").isFlowStyle()).isFalse();
    }

    @Test
    void aNewEntryNeverDuplicatesAKey() {
        assertThat(edited(pnpm("catalog-add-into-null-catalog").withEntry("default", "wrappy", "1.0.2")))
          .as("catalog holds null").isEqualTo(file("pnpm/catalog-add-into-null-catalog/workspace"));
        assertThat(pnpmOf("catalog: 1\npackages:\n  - a\n").withEntry("default", "wrappy", "1.0.2"))
          .as("catalog holds no mapping").isNull();
        assertThat(pnpmOf("catalog:\n  wrappy:\n    nested: 1\n").withEntry("default", "wrappy", "1.0.2"))
          .as("the entry holds no scalar").isNull();
    }

    // pnpm's yaml prints the whole document again, ending it with a newline (catalog-update-no-final-newline)
    @Test
    void pnpmEndsTheFileItWritesWithANewline() {
        assertThat(edited(pnpm("catalog-update-no-final-newline").withSpecifier("default", "wrappy", "1.0.2")))
          .isEqualTo(file("pnpm/catalog-update-no-final-newline/workspace"));
    }

    // the entries left keep their comments, as pnpm 11.2.2 and 10.34.6 wrote them (catalog-cleanup-comments-*)
    @ParameterizedTest
    @CsvSource(textBlock = """
      catalog-cleanup-comments-middle-pnpm11, wrappy
      catalog-cleanup-comments-first-pnpm11,  is-number
      catalog-cleanup-comments-last-pnpm11,   wrappy
      catalog-cleanup-comments-middle-pnpm10, wrappy
      catalog-cleanup-comments-first-pnpm10,  is-number
      catalog-cleanup-comments-last-pnpm10,   wrappy
      """)
    void aCleanupKeepsTheCommentsOfTheEntriesItLeaves(String scenario, String removed) {
        Map<String, String> used = new LinkedHashMap<>();
        for (String name : Arrays.asList("is-number", "ms", "wrappy")) {
            if (!name.equals(removed)) {
                used.put(name, "catalog:");
            }
        }
        assertThat(edited(pnpmOfRoot(scenario).withoutUnused(projects(Arrays.asList(emptyMap(),
          singletonMap("dependencies", used))))))
          .isEqualTo(file("pnpm/" + scenario + "/workspace"));
    }

    // pnpm 10's cleanup reads the dependency fields only, pnpm 11's an override's catalog: too
    @Test
    void anOverridesCatalogReferenceKeepsAnEntryForPnpm11Only() {
        Map<String, Map<String, String>> member = declares("wrappy", "catalog:");
        assertThat(edited(pnpmOfRoot("catalog-cleanup-override-reference-pnpm10").withoutUnused(
          projects(Arrays.asList(emptyMap(), member)))))
          .isEqualTo(file("pnpm/catalog-cleanup-override-reference-pnpm10/workspace"));
        NodeCatalogs.Pnpm pnpm11 = pnpmOfRoot("catalog-cleanup-override-reference-pnpm11");
        assertThat(pnpm11.withoutUnused(projects(Arrays.asList(emptyMap(), member)))).isSameAs(pnpm11);
        assertThat(file("pnpm/catalog-cleanup-override-reference-pnpm11/workspace"))
          .isEqualTo(file("pnpm/catalog-cleanup-override-reference-pnpm11/workspace-before"));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
      catalog-add-root-declares-in-catalog-strict-cleanup, true
      catalog-entry-one-importer,                          false
      catalog-add-mode-capitalized-mismatch,               false
      catalog-add-mode-null-mismatch,                      false
      catalog-add-save-name-prefer,                        false
      catalog-remove-cleanup-capitalized-true,             true
      catalog-remove-cleanup-quoted-false,                 true
      catalog-remove-cleanup-no,                           true
      """)
    void pnpmSettingsAsPnpmReadsThem(String scenario, boolean cleanup) {
        assertThat(pnpm(scenario).isCleanupUnusedCatalogs()).isEqualTo(cleanup);
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

    // Yarn writes no catalog entry, so a new one is the recipe's own edit: where pnpm 11 would put it, the text around
    // it as it was
    @Test
    void yarnInsertsAnEntryInPlace() {
        String[][] edits = {
          {"catalog:\n  react: ^18.0.0\n# end\n", "catalog:\n  is-number: 7.0.0\n  react: ^18.0.0\n# end\n"},
          {"catalog:\n  ms: 2.1.3 # pinned\nnodeLinker: pnp\n",
            "catalog:\n  is-number: 7.0.0\n  ms: 2.1.3 # pinned\nnodeLinker: pnp\n"},
          {"catalog:\n  a: 1.0.0 # first\n\n\nnodeLinker: pnp\n",
            "catalog:\n  a: 1.0.0 # first\n  is-number: 7.0.0\n\n\nnodeLinker: pnp\n"},
          {"catalog:\r\n  wrappy: 1.0.1\r\n", "catalog:\r\n  is-number: 7.0.0\r\n  wrappy: 1.0.1\r\n"},
          {"enableTelemetry: false\n", "catalog:\n  is-number: 7.0.0\nenableTelemetry: false\n"}};
        for (String[] edit : edits) {
            NodeCatalogs catalogs = requireNonNull(NodeCatalogs.read(PackageManager.YarnBerry, yaml(edit[0])));
            assertThat(edited(catalogs.withEntry("", "is-number", "7.0.0"))).as(edit[0]).isEqualTo(edit[1]);
        }
    }

    // pnpm 9 never writes pnpm-workspace.yaml (catalog-cleanup-pnpm9), so its catalog upgrade is the recipe's own
    // edit of the one scalar, where pnpm 10 and 11 print the whole document again
    @Test
    void pnpm9EditsTheEntryInPlace() {
        String before = "savePrefix:\ncatalog: # c\n  wrappy: 1.0.1  \n\n\n# end";
        NodeCatalogs catalogs = requireNonNull(NodeCatalogs.read(PackageManager.Pnpm, yaml(before),
          "{\"packageManager\":\"pnpm@9.15.9\"}"));
        assertThat(edited(catalogs.withSpecifier("default", "wrappy", "1.0.2")))
          .isEqualTo(before.replace("1.0.1", "1.0.2"));
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

    // the root first, then one member under packages/ each, as pnpm-workspace.yaml's packages list them
    private static Map<String, @Nullable Map<String, @Nullable Object>> projects(
      List<? extends Map<String, ? extends Map<String, String>>> manifests) {
        Map<String, @Nullable Map<String, @Nullable Object>> projects = new LinkedHashMap<>();
        for (int i = 0; i < manifests.size(); i++) {
            projects.put(i == 0 ? "" : "packages/p" + i, new LinkedHashMap<>(manifests.get(i)));
        }
        return projects;
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

    // the recording's workspace for the release its root names
    private static NodeCatalogs.Pnpm pnpmOfRoot(String scenario) {
        return (NodeCatalogs.Pnpm) requireNonNull(NodeCatalogs.read(PackageManager.Pnpm,
          yaml(file("pnpm/" + scenario + "/workspace-before")), file("pnpm/" + scenario + "/pkg-before")));
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
