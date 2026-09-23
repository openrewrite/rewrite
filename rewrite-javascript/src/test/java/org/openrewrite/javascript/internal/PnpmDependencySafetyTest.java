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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.javascript.AddDependency;
import org.openrewrite.javascript.ChangeDependency;
import org.openrewrite.javascript.UpgradeDependencyVersion;
import org.openrewrite.javascript.UpgradeTransitiveDependencyVersion;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.JsonParser;
import org.openrewrite.json.tree.Json;
import org.openrewrite.test.RewriteTest;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.javascript.Assertions.*;

@Issue("https://github.com/openrewrite/rewrite/issues/8926")
class PnpmDependencySafetyTest implements RewriteTest {
    private Json.Document parse(String source) {
        return (Json.Document) new JsonParser().parse(source).findFirst().orElseThrow();
    }

    @ParameterizedTest
    @ValueSource(strings = {"catalog:", "catalog:default", "workspace:*", "patch:foo@1#patch", "portal:../foo",
      "npm:bar@^1.0.0", "file:../foo", "link:../foo", "github:org/repo", "future:1", "latest"})
    void refusesToOverwriteReferences(String reference) {
        Json.Document doc = parse("{\"dependencies\":{\"foo\":\"" + reference + "\"}}");
        assertThatThrownBy(() -> PackageJsonHelper.upgradeVersion(doc,
          singletonList(new MatchedDependency("foo", "dependencies", reference)), "^2.0.0"))
          .isInstanceOf(EngineFailure.class).hasMessageContaining(reference);
        assertThatThrownBy(() -> PackageJsonHelper.changeDependency(doc, "foo", "bar", "^2.0.0", null))
          .isInstanceOf(EngineFailure.class).hasMessageContaining(reference);
    }

    @ParameterizedTest
    @ValueSource(strings = {"qar@1>zoo", "bar@^2.1.0", "form-data@", "x>y", "@scope/pkg@1", "foo/bar", "future:foo"})
    void refusesUnsupportedOverrideSelectors(String selector) {
        Json.Document doc = parse("{\"pnpm\":{\"overrides\":{\"" + selector + "\":\"1.0.0\"}}}");
        assertThatThrownBy(() -> PackageJsonOverrides.applyOverride(doc, PackageManager.Pnpm, "foo", "2.0.0", null))
          .isInstanceOf(EngineFailure.class).hasMessageContaining(selector);
        assertThatThrownBy(() -> PackageJsonOverrides.applyOverride(parse("{}"), PackageManager.Pnpm, selector, "2.0.0", null))
          .isInstanceOf(EngineFailure.class).hasMessageContaining(selector);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-", "catalog:", "npm:foo@1", "$foo", "latest", "latest.release", "future:1"})
    void refusesUnsupportedOverrideValues(String value) {
        Json.Document doc = parse("{\"pnpm\":{\"overrides\":{\"foo\":\"" + value + "\"}}}");
        assertThatThrownBy(() -> PackageJsonOverrides.applyOverride(doc, PackageManager.Pnpm, "foo", "2.0.0", null))
          .isInstanceOf(EngineFailure.class).hasMessageContaining(value);
        assertThatThrownBy(() -> PackageJsonOverrides.applyOverride(parse("{}"), PackageManager.Pnpm, "foo", value, null))
          .isInstanceOf(EngineFailure.class).hasMessageContaining(value);
    }

    @Test
    void refusesScopedPath() {
        assertThatThrownBy(() -> PackageJsonOverrides.applyOverride(parse("{}"), PackageManager.Pnpm, "foo", "2.0.0",
          PackageJsonOverrides.parsePath("parent@1")))
          .isInstanceOf(EngineFailure.class).hasMessageContaining("path");
    }

    @Test
    void preservesFormattingAndConverges() {
        String before = "{\n\t\"name\" : \"example\",\n\t\"pnpm\": {\n\t\t\"overrides\" : { \"@scope/pkg\" : \"^1.0.0\" },\n\t\t\"other\": true\n\t}\n}\n";
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("@scope/pkg", "^2.0.0", null)),
          packageJson(before, before.replace("^1.0.0", "^2.0.0"), nodeResolutionResult(PackageManager.Pnpm))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"pnpm\":{}}", "{\"pnpm\":{\"overrides\":{}}}",
      "{\"pnpm\":{\"overrides\":{\"other\":\"1\"}}}"})
    void insertsOnce(String before) {
        Json.Document doc = parse(before);
        Json.Document after = PackageJsonOverrides.applyOverride(doc, PackageManager.Pnpm, "foo", "^2.0.0", null);
        assertThat(after.printAll()).contains("\"foo\": \"^2.0.0\"");
        assertThat(PackageJsonOverrides.applyOverride(after, PackageManager.Pnpm, "foo", "^2.0.0", null)).isSameAs(after);
        assertThat(parse(after.printAll()).printAll()).isEqualTo(after.printAll());
    }

    @Test
    void preservesCommentsAndTrailingCommasWhenInserting() {
        Json.Document doc = parse("{\n  // keep once\n  \"pnpm\": {\n    \"overrides\": {\n      // existing\n      \"other\": \"1\",\n    },\n  },\n}");
        Json.Document after = PackageJsonOverrides.applyOverride(doc, PackageManager.Pnpm, "foo", "2", null);
        assertThat(after.printAll()).isEqualTo("{\n  // keep once\n  \"pnpm\": {\n    \"overrides\": {\n      // existing\n      \"other\": \"1\",\n      \"foo\": \"2\",\n    },\n  },\n}");
        assertThat(PackageJsonOverrides.applyOverride(after, PackageManager.Pnpm, "foo", "2", null)).isSameAs(after);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pnpm@11.0.0", "pnpm@12.4.0+sha512.abc"})
    void refusesIgnoredPackageJsonOverrides(String manager) {
        assertThatThrownBy(() -> PackageJsonOverrides.applyOverride(
          parse("{\"packageManager\":\"" + manager + "\"}"), PackageManager.Pnpm, "foo", "2.0.0", null))
          .isInstanceOf(EngineFailure.class).hasMessageContaining("pnpm-workspace.yaml");
    }

    @Test
    void insertsWithExistingIndentation() {
        String before = "{\n    \"name\": \"x\"\n}";
        String after = "{\n    \"name\": \"x\",\n    \"pnpm\": {\n        \"overrides\": {\n            \"foo\": \"2\"\n        }\n    }\n}";
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("foo", "2", null)),
          packageJson(before, after, nodeResolutionResult(PackageManager.Pnpm))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.0.0", "^1.0.0", "~1.2", ">=1 <2 || ^3.0.0", "*", "1.0.0-beta.1"})
    void acceptsPlainSemverRanges(String range) {
        Json.Document doc = parse("{\"dependencies\":{\"foo\":\"" + range + "\"}}");
        assertThat(PackageJsonHelper.upgradeVersion(doc,
          singletonList(new MatchedDependency("foo", "dependencies", range)), "2.0.0").printAll())
          .isEqualTo("{\"dependencies\":{\"foo\":\"2.0.0\"}}");
        assertThat(PackageJsonOverrides.applyOverride(parse("{\"packageManager\":\"pnpm@10.0.0\"}"),
          PackageManager.Pnpm, "@scope/foo", range, null).printAll()).contains("\"@scope/foo\": \"" + range + "\"");
    }

    @Test
    void upgradeReportsUnsupportedReferenceWithoutChangingManifest() {
        String before = "{\"dependencies\":{\"foo\":\"catalog:\"}}";
        rewriteRun(
          spec -> spec.recipe(new UpgradeDependencyVersion("foo", null, "2.0.0")),
          packageJson(before, "/*~~(lock regeneration failed: UNSUPPORTED_ENTRY_TYPE [foo]: Unsupported dependency version 'catalog:'; only protocol-free semver ranges are supported)~~>*/" + before,
            nodeResolutionResult(PackageManager.Pnpm, dependency("foo", "catalog:")))
        );
    }

    @Test
    void transitiveUpgradeReportsUnsupportedSelectorWithoutAddingOverride() {
        String before = "{\"pnpm\":{\"overrides\":{\"bar@^2\":\"2.0.0\"}}}";
        rewriteRun(
          spec -> spec.recipe(new UpgradeTransitiveDependencyVersion("foo", "2.0.0", null)),
          packageJson(before, "/*~~(lock regeneration failed: UNSUPPORTED_ENTRY_TYPE [bar@^2]: Unsupported pnpm override selector 'bar@^2'; only bare package names are supported)~~>*/" + before,
            nodeResolutionResult(PackageManager.Pnpm))
        );
    }

    @Test
    void unsupportedMatchRejectsEntireEdit() {
        Json.Document doc = parse("{\"dependencies\":{\"safe\":\"1\",\"foo\":\"catalog:\"}}");
        PackageJsonHelper.EditAndRegenerateResult result = PackageJsonHelper.editAndRegenerate(doc,
          source -> PackageJsonHelper.upgradeVersion(source, java.util.Arrays.asList(
            new MatchedDependency("safe", "dependencies", "1"),
            new MatchedDependency("foo", "dependencies", "catalog:")), "2"), null, new InMemoryExecutionContext());
        assertThat(result.getModifiedPackageJson()).isSameAs(doc);
        assertThat(result.getRegenResult()).isNotNull();
        assertThat(result.getRegenResult().getFailure().getReason())
          .isEqualTo(LockFileRegeneration.Reason.UNSUPPORTED_ENTRY_TYPE);
    }

    @Test
    void addThenUpgradeStillSharesEditsWithinTheCycle() {
        rewriteRun(
          spec -> spec.recipes(new AddDependency("foo", "1", null), new UpgradeDependencyVersion("foo", null, "2")),
          packageJson("{\"dependencies\":{\"keep\":\"1\"}}", "{\"dependencies\":{\"keep\":\"1\",\"foo\": \"2\"}}",
            nodeResolutionResult(PackageManager.Pnpm, dependency("keep", "1")))
        );
    }

    @Test
    void renameWithoutVersionPreservesReference() {
        Json.Document doc = parse("{\"dependencies\":{\"foo\":\"workspace:*\"}}");
        assertThat(PackageJsonHelper.changeDependency(doc, "foo", "bar", null, null).printAll())
          .isEqualTo("{\"dependencies\":{\"bar\":\"workspace:*\"}}");
    }

    @Test
    void changeReportsUnsupportedReferenceWithoutRenaming() {
        String before = "{\"dependencies\":{\"foo\":\"workspace:*\"}}";
        rewriteRun(
          spec -> spec.recipe(new ChangeDependency("foo", "bar", "2.0.0", null)),
          packageJson(before, "/*~~(lock regeneration failed: UNSUPPORTED_ENTRY_TYPE [foo]: Unsupported dependency version 'workspace:*'; only protocol-free semver ranges are supported)~~>*/" + before,
            nodeResolutionResult(PackageManager.Pnpm, dependency("foo", "workspace:*")))
        );
    }
}
