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
package org.openrewrite.python.internal.index;

import org.intellij.lang.annotations.Language;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.python.PythonExecutionContextView;
import org.openrewrite.python.PythonIndexCredentials;
import org.openrewrite.python.PythonPackageIndex;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UvIndexDiscoveryTest {

    @TempDir
    Path home;

    @TempDir
    Path projectDir;

    private static Environment env(Map<String, String> vars, Path home) {
        return new Environment() {
            @Override
            public String getenv(String name) {
                // keep tests hermetic against a machine-level /etc/uv/uv.toml
                if ("UV_NO_SYSTEM_CONFIG".equals(name) && !vars.containsKey(name)) {
                    return "1";
                }
                return vars.get(name);
            }

            @Override
            public Path userHome() {
                return home;
            }

            @Override
            public String osName() {
                return vars.getOrDefault("os.name", "Mac OS X");
            }
        };
    }

    private static Toml.Document toml(@Language("toml") String content) {
        return (Toml.Document) new TomlParser().parse(content).findFirst().orElseThrow();
    }

    private static ExecutionContext ctx() {
        return new InMemoryExecutionContext(Throwable::printStackTrace);
    }

    private List<UvIndex> discover(Toml.@Nullable Document pyproject, Map<String, String> vars) {
        return UvIndexDiscovery.discover(ctx(), pyproject, projectDir, env(vars, home));
    }

    @Test
    void viewIndexesWinOutright() {
        ExecutionContext ctx = ctx();
        PythonPackageIndex hostSupplied = new PythonPackageIndex(
          "corp", "https://corp.example.com/simple", true, "u", "p", false);
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(hostSupplied));

        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "other"
          url = "https://other.example.com/simple"
          """);

        List<UvIndex> indexes = UvIndexDiscovery.discover(ctx, doc, projectDir, env(Map.of(), home));
        assertThat(indexes).hasSize(1);
        assertThat(indexes.get(0).getIndex()).isEqualTo(hostSupplied);
    }

    @Test
    void toolUvIndexEntriesThenPypiDefault() {
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "first"
          url = "https://first.example.com/simple"

          [[tool.uv.index]]
          name = "second"
          url = "https://second.example.com/simple"
          """);

        List<UvIndex> indexes = discover(doc, Map.of());
        assertThat(indexes).extracting(i -> i.getIndex().getUrl()).containsExactly(
          "https://first.example.com/simple",
          "https://second.example.com/simple",
          "https://pypi.org/simple");
        assertThat(indexes.get(2).isDefaultIndex()).isTrue();
        assertThat(indexes.get(2).getIndex().getName()).isEqualTo("pypi");
        assertThat(indexes.get(0).isDefaultIndex()).isFalse();
        assertThat(indexes.get(0).isNamed()).isTrue();
    }

    @Test
    void defaultTrueReplacesPypiAndSortsLast() {
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "corp"
          url = "https://corp.example.com/simple"
          default = true

          [[tool.uv.index]]
          name = "extra"
          url = "https://extra.example.com/simple"
          """);

        List<UvIndex> indexes = discover(doc, Map.of());
        assertThat(indexes).extracting(i -> i.getIndex().getName()).containsExactly("extra", "corp");
        assertThat(indexes.get(1).isDefaultIndex()).isTrue();
    }

    @Test
    void toolUvIndexBeatsLegacyKeys() {
        Toml.Document doc = toml("""
          [tool.uv]
          index-url = "https://legacy.example.com/simple"
          extra-index-url = ["https://extra1.example.com/simple", "https://extra2.example.com/simple"]

          [[tool.uv.index]]
          name = "corp"
          url = "https://corp.example.com/simple"
          """);

        List<UvIndex> indexes = discover(doc, Map.of());
        assertThat(indexes).extracting(i -> i.getIndex().getUrl()).containsExactly(
          "https://corp.example.com/simple",
          "https://extra1.example.com/simple",
          "https://extra2.example.com/simple",
          "https://legacy.example.com/simple");
        // legacy index-url acts as the default index, lowest priority
        assertThat(indexes.get(3).isDefaultIndex()).isTrue();
    }

    @Test
    void envIndexesPrecedeConfigEntries() {
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "config"
          url = "https://config.example.com/simple"
          """);

        List<UvIndex> indexes = discover(doc,
          Map.of("UV_INDEX", "https://env1.example.com/simple corp=https://env2.example.com/simple"));
        assertThat(indexes).extracting(i -> i.getIndex().getUrl()).containsExactly(
          "https://env1.example.com/simple",
          "https://env2.example.com/simple",
          "https://config.example.com/simple",
          "https://pypi.org/simple");
        // UV_INDEX supports the name=url form
        assertThat(indexes.get(1).getIndex().getName()).isEqualTo("corp");
        assertThat(indexes.get(1).isNamed()).isTrue();
    }

    @Test
    void envDefaultIndexBeatsConfigDefault() {
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "corp"
          url = "https://corp.example.com/simple"
          default = true
          """);

        List<UvIndex> indexes = discover(doc,
          Map.of("UV_DEFAULT_INDEX", "https://env.example.com/simple"));
        // the first default wins; the shadowed config default is dropped entirely
        assertThat(indexes).extracting(i -> i.getIndex().getUrl())
          .containsExactly("https://env.example.com/simple");
        assertThat(indexes.get(0).isDefaultIndex()).isTrue();
    }

    @Test
    void legacyEnvVars() {
        List<UvIndex> indexes = discover(null, Map.of(
          "UV_INDEX_URL", "https://legacy.example.com/simple",
          "UV_EXTRA_INDEX_URL", "https://extra1.example.com/simple https://extra2.example.com/simple"));
        assertThat(indexes).extracting(i -> i.getIndex().getUrl()).containsExactly(
          "https://extra1.example.com/simple",
          "https://extra2.example.com/simple",
          "https://legacy.example.com/simple");
        assertThat(indexes.get(2).isDefaultIndex()).isTrue();
    }

    @Test
    void uvTomlMasksPyprojectToolUv() throws IOException {
        Files.writeString(projectDir.resolve("uv.toml"), """
          [[index]]
          name = "from-uv-toml"
          url = "https://uvtoml.example.com/simple"
          """);
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "from-pyproject"
          url = "https://pyproject.example.com/simple"
          """);

        List<UvIndex> indexes = discover(doc, Map.of());
        assertThat(indexes).extracting(i -> i.getIndex().getName()).containsExactly("from-uv-toml", "pypi");
    }

    @Test
    void userUvTomlEntriesAppendAfterProject() throws IOException {
        Path configHome = home.resolve("xdg");
        Files.createDirectories(configHome.resolve("uv"));
        Files.writeString(configHome.resolve("uv/uv.toml"), """
          [[index]]
          name = "user"
          url = "https://user.example.com/simple"
          """);
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "project"
          url = "https://project.example.com/simple"
          """);

        List<UvIndex> indexes = discover(doc, Map.of("XDG_CONFIG_HOME", configHome.toString()));
        assertThat(indexes).extracting(i -> i.getIndex().getName())
          .containsExactly("project", "user", "pypi");
    }

    @Test
    void userUvTomlDefaultsToDotConfigUnderHome() throws IOException {
        Files.createDirectories(home.resolve(".config/uv"));
        Files.writeString(home.resolve(".config/uv/uv.toml"), """
          index-url = "https://user.example.com/simple"
          """);

        List<UvIndex> indexes = discover(null, Map.of());
        assertThat(indexes).extracting(i -> i.getIndex().getUrl())
          .containsExactly("https://user.example.com/simple");
        assertThat(indexes.get(0).isDefaultIndex()).isTrue();
    }

    @Test
    void uvConfigFileReplacesDiscoveredChain() throws IOException {
        Files.writeString(projectDir.resolve("uv.toml"), """
          [[index]]
          name = "project"
          url = "https://project.example.com/simple"
          """);
        Path explicit = home.resolve("custom-uv.toml");
        Files.writeString(explicit, """
          [[index]]
          name = "explicit-file"
          url = "https://explicit.example.com/simple"
          """);

        List<UvIndex> indexes = discover(null, Map.of("UV_CONFIG_FILE", explicit.toString()));
        assertThat(indexes).extracting(i -> i.getIndex().getName()).containsExactly("explicit-file", "pypi");
    }

    @Test
    void uvNoConfigIgnoresConfigFiles() throws IOException {
        Files.writeString(projectDir.resolve("uv.toml"), """
          [[index]]
          name = "project"
          url = "https://project.example.com/simple"
          """);

        List<UvIndex> indexes = discover(null, Map.of("UV_NO_CONFIG", "1"));
        assertThat(indexes).extracting(i -> i.getIndex().getName()).containsExactly("pypi");
    }

    @Test
    void namedIndexCredentialEnvPattern() {
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "internal-proxy"
          url = "https://proxy.example.com/simple"

          [[tool.uv.index]]
          name = "corp.pkg-repo"
          url = "https://corp.example.com/simple"
          """);

        List<UvIndex> indexes = discover(doc, Map.of(
          "UV_INDEX_INTERNAL_PROXY_USERNAME", "alice",
          "UV_INDEX_INTERNAL_PROXY_PASSWORD", "hunter2",
          "UV_INDEX_CORP_PKG_REPO_USERNAME", "bob",
          "UV_INDEX_CORP_PKG_REPO_PASSWORD", "sekret"));
        assertThat(indexes.get(0).getIndex().getUsername()).isEqualTo("alice");
        assertThat(indexes.get(0).getIndex().getPassword()).isEqualTo("hunter2");
        // periods and dashes both map to underscores in the env var name
        assertThat(indexes.get(1).getIndex().getUsername()).isEqualTo("bob");
        assertThat(indexes.get(1).getIndex().getPassword()).isEqualTo("sekret");
    }

    @Test
    void credentialEnvPatternBeatsUrlEmbedded() {
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "corp"
          url = "https://urluser:urlpass@corp.example.com/simple"
          """);

        List<UvIndex> indexes = discover(doc, Map.of(
          "UV_INDEX_CORP_USERNAME", "envuser",
          "UV_INDEX_CORP_PASSWORD", "envpass"));
        // uv prefers env credentials over URL-embedded ones for named indexes
        assertThat(indexes.get(0).getIndex().getUsername()).isEqualTo("envuser");
        assertThat(indexes.get(0).getIndex().getPassword()).isEqualTo("envpass");
    }

    @Test
    void urlEmbeddedCredentialsDecoded() {
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "corp"
          url = "https://alice:p%40ss@corp.example.com/simple"
          """);

        List<UvIndex> indexes = discover(doc, Map.of());
        assertThat(indexes.get(0).getIndex().getUsername()).isEqualTo("alice");
        assertThat(indexes.get(0).getIndex().getPassword()).isEqualTo("p@ss");
    }

    @Test
    void viewCredentialsFillByHostThenNetrc() throws IOException {
        Files.writeString(home.resolve(".netrc"), """
          machine other.example.com login bob password sekret
          """);
        ExecutionContext ctx = ctx();
        PythonExecutionContextView.view(ctx).setIndexCredentials(List.of(
          new PythonIndexCredentials("corp.example.com", "alice", "hunter2")));

        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "corp"
          url = "https://corp.example.com/simple"

          [[tool.uv.index]]
          name = "other"
          url = "https://other.example.com/simple"
          """);

        List<UvIndex> indexes = UvIndexDiscovery.discover(ctx, doc, projectDir, env(Map.of(), home));
        assertThat(indexes.get(0).getIndex().getUsername()).isEqualTo("alice");
        assertThat(indexes.get(0).getIndex().getPassword()).isEqualTo("hunter2");
        assertThat(indexes.get(1).getIndex().getUsername()).isEqualTo("bob");
        assertThat(indexes.get(1).getIndex().getPassword()).isEqualTo("sekret");
    }

    @Test
    void noEnvExpansionInConfigUrls() {
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "corp"
          url = "https://${INDEX_USER}@corp.example.com/simple"
          """);

        List<UvIndex> indexes = discover(doc, Map.of("INDEX_USER", "alice"));
        // uv takes config URLs literally; no placeholder expansion
        assertThat(indexes.get(0).getIndex().getUrl())
          .isEqualTo("https://${INDEX_USER}@corp.example.com/simple");
        assertThat(indexes.get(0).getIndex().isUnresolvedPlaceholders()).isFalse();
        assertThat(indexes.get(0).getIndex().getUsername()).isEqualTo("${INDEX_USER}");
    }

    @Test
    void explicitIndexUsableOnlyForPinnedPackages() {
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "pytorch"
          url = "https://download.pytorch.org/whl/cpu"
          explicit = true

          [[tool.uv.index]]
          name = "corp"
          url = "https://corp.example.com/simple"
          """);

        List<UvIndex> indexes = discover(doc, Map.of());
        assertThat(indexes).extracting(i -> i.getIndex().getName()).containsExactly("pytorch", "corp", "pypi");
        UvIndex pytorch = indexes.get(0);
        assertThat(pytorch.isExplicit()).isTrue();
        assertThat(pytorch.usableFor(null)).isFalse();
        assertThat(pytorch.usableFor("pytorch")).isTrue();
        UvIndex corp = indexes.get(1);
        assertThat(corp.usableFor(null)).isTrue();
        // a pinned package resolves exclusively from its pinned index
        assertThat(corp.usableFor("pytorch")).isFalse();
        assertThat(indexes.get(2).usableFor("pytorch")).isFalse();
    }

    @Test
    void sourceIndexPins() {
        Toml.Document doc = toml("""
          [tool.uv.sources]
          torch = { index = "pytorch" }
          torchvision = [
            { index = "pytorch-cpu", marker = "platform_system != 'Darwin'" },
            { index = "pytorch-gpu", marker = "platform_system == 'Darwin'" },
          ]
          My_Package = { index = "corp" }
          local-dep = { path = "../local" }
          """);

        Map<String, List<String>> pins = UvIndexDiscovery.sourceIndexPins(doc);
        assertThat(pins).containsOnlyKeys("torch", "torchvision", "my-package");
        assertThat(pins.get("torch")).containsExactly("pytorch");
        assertThat(pins.get("torchvision")).containsExactly("pytorch-cpu", "pytorch-gpu");
        assertThat(pins.get("my-package")).containsExactly("corp");
    }

    @Test
    void flatFormatRelativePathResolvesAgainstProjectDir() {
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "shared"
          url = "./assets/shared"
          format = "flat"
          """);

        List<UvIndex> indexes = discover(doc, Map.of());
        UvIndex shared = indexes.get(0);
        assertThat(shared.isFlat()).isTrue();
        assertThat(shared.getIndex().getUrl())
          .isEqualTo(projectDir.resolve("assets/shared").normalize().toString());
    }

    @Test
    void relativePathInUvTomlResolvesAgainstItsOwnDirectory() throws IOException {
        Path configHome = home.resolve("xdg");
        Files.createDirectories(configHome.resolve("uv"));
        Files.writeString(configHome.resolve("uv/uv.toml"), """
          [[index]]
          name = "wheels"
          url = "./wheels"
          format = "flat"
          """);

        List<UvIndex> indexes = discover(null, Map.of("XDG_CONFIG_HOME", configHome.toString()));
        assertThat(indexes.get(0).getIndex().getUrl())
          .isEqualTo(configHome.resolve("uv/wheels").toString());
    }

    @Test
    void findLinksAppendLastAsFlat() {
        Toml.Document doc = toml("""
          [tool.uv]
          find-links = ["https://links.example.com/", "./local-wheels"]
          """);

        List<UvIndex> indexes = discover(doc, Map.of("UV_FIND_LINKS", "/opt/wheels,https://env.example.com/links/"));
        assertThat(indexes).extracting(i -> i.getIndex().getUrl()).containsExactly(
          "https://pypi.org/simple",
          "/opt/wheels",
          "https://env.example.com/links/",
          "https://links.example.com/",
          projectDir.resolve("local-wheels").normalize().toString());
        assertThat(indexes.get(1).isFlat()).isTrue();
        assertThat(indexes.get(4).isFlat()).isTrue();
    }

    @Test
    void namedDuplicatesFirstOccurrenceWins() {
        Toml.Document doc = toml("""
          [[tool.uv.index]]
          name = "corp"
          url = "https://config.example.com/simple"
          """);

        List<UvIndex> indexes = discover(doc, Map.of("UV_INDEX", "corp=https://env.example.com/simple"));
        assertThat(indexes).extracting(i -> i.getIndex().getUrl())
          .containsExactly("https://env.example.com/simple", "https://pypi.org/simple");
    }

    @Test
    void noConfigurationDefaultsToPypi() {
        List<UvIndex> indexes = discover(null, new HashMap<>());
        assertThat(indexes).hasSize(1);
        assertThat(indexes.get(0).getIndex()).isEqualTo(
          new PythonPackageIndex("pypi", "https://pypi.org/simple", true, null, null, false));
        assertThat(indexes.get(0).isDefaultIndex()).isTrue();
        assertThat(indexes.get(0).isNamed()).isFalse();
    }
}
