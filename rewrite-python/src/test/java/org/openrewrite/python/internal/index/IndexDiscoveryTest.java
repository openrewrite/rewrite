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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IndexDiscoveryTest {

    @TempDir
    Path home;

    private static Environment env(Map<String, String> vars, Path home) {
        return new Environment() {
            @Override
            public String getenv(String name) {
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

    private static Toml.Document pipfile(@Language("toml") String content) {
        return (Toml.Document) new TomlParser().parse(content).findFirst().orElseThrow();
    }

    private static ExecutionContext ctx() {
        return new InMemoryExecutionContext(Throwable::printStackTrace);
    }

    @Test
    void explicitIndexesOnViewWinOutright() {
        ExecutionContext ctx = ctx();
        PythonPackageIndex hostSupplied = new PythonPackageIndex(
          "corp", "https://corp.example.com/simple", true, "u", "p", false);
        PythonExecutionContextView.view(ctx).setPackageIndexes(List.of(hostSupplied));

        Toml.Document doc = pipfile("""
          [[source]]
          name = "pypi"
          url = "https://pypi.org/simple"
          verify_ssl = true
          """);
        List<Map<String, Object>> lockSources = List.of(
          Map.of("name", "lock", "url", "https://lock.example.com/simple", "verify_ssl", true));

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx, doc, lockSources, env(Map.of(), home));
        assertThat(indexes).containsExactly(hostSupplied);
    }

    @Test
    void pipfileSourcesBeatLockSources() {
        Toml.Document doc = pipfile("""
          [[source]]
          name = "corp"
          url = "https://corp.example.com/simple"
          verify_ssl = false

          [[source]]
          name = "mirror"
          url = "https://mirror.example.com/simple"
          verify_ssl = true

          [packages]
          requests = "*"
          """);
        List<Map<String, Object>> lockSources = List.of(
          Map.of("name", "lock", "url", "https://lock.example.com/simple", "verify_ssl", true));

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), doc, lockSources, env(Map.of(), home));
        assertThat(indexes).hasSize(2);
        assertThat(indexes.get(0).getName()).isEqualTo("corp");
        assertThat(indexes.get(0).getUrl()).isEqualTo("https://corp.example.com/simple");
        assertThat(indexes.get(0).isVerifySsl()).isFalse();
        assertThat(indexes.get(1).getName()).isEqualTo("mirror");
    }

    @Test
    void lockSourcesBeatEnvironmentVariables() {
        Toml.Document doc = pipfile("""
          [packages]
          requests = "*"
          """);
        List<Map<String, Object>> lockSources = List.of(
          Map.of("name", "lock", "url", "https://lock.example.com/simple", "verify_ssl", false));

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), doc, lockSources,
          env(Map.of("PIP_INDEX_URL", "https://env.example.com/simple"), home));
        assertThat(indexes).hasSize(1);
        assertThat(indexes.get(0).getName()).isEqualTo("lock");
        assertThat(indexes.get(0).getUrl()).isEqualTo("https://lock.example.com/simple");
        assertThat(indexes.get(0).isVerifySsl()).isFalse();
    }

    @Test
    void environmentVariablesBeatPipConf() throws IOException {
        writeUserPipConf("""
          [global]
          index-url = https://conf.example.com/simple
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), null, null,
          env(Map.of(
            "PIP_INDEX_URL", "https://env.example.com/simple",
            "PIP_EXTRA_INDEX_URL", "https://extra1.example.com/simple https://extra2.example.com/simple"
          ), home));

        assertThat(indexes).extracting(PythonPackageIndex::getUrl).containsExactly(
          "https://env.example.com/simple",
          "https://extra1.example.com/simple",
          "https://extra2.example.com/simple");
        assertThat(indexes.get(0).getName()).isEqualTo("pypi");
    }

    @Test
    void pipConfBeatsDefault() throws IOException {
        writeUserPipConf("""
          [global]
          index-url = https://conf.example.com/simple
          extra-index-url =
              https://extra1.example.com/simple
              https://extra2.example.com/simple
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), null, null, env(Map.of(), home));
        assertThat(indexes).extracting(PythonPackageIndex::getUrl).containsExactly(
          "https://conf.example.com/simple",
          "https://extra1.example.com/simple",
          "https://extra2.example.com/simple");
    }

    @Test
    void installSectionOverridesGlobal() throws IOException {
        writeUserPipConf("""
          [global]
          index-url = https://global.example.com/simple

          [install]
          index-url = https://install.example.com/simple
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), null, null, env(Map.of(), home));
        assertThat(indexes.get(0).getUrl()).isEqualTo("https://install.example.com/simple");
    }

    @Test
    void pipConfigFileLoadsLast() throws IOException {
        writeUserPipConf("""
          [global]
          index-url = https://user.example.com/simple
          """);
        Path explicit = home.resolve("custom-pip.conf");
        Files.writeString(explicit, """
          [global]
          index-url = https://explicit.example.com/simple
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), null, null,
          env(Map.of("PIP_CONFIG_FILE", explicit.toString()), home));
        assertThat(indexes.get(0).getUrl()).isEqualTo("https://explicit.example.com/simple");
    }

    @Test
    void windowsPipIni() throws IOException {
        Path appData = home.resolve("AppData/Roaming");
        Files.createDirectories(appData.resolve("pip"));
        Files.writeString(appData.resolve("pip/pip.ini"), """
          [global]
          index-url = https://win.example.com/simple
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), null, null,
          env(Map.of("os.name", "Windows 11", "APPDATA", appData.toString()), home));
        assertThat(indexes.get(0).getUrl()).isEqualTo("https://win.example.com/simple");
    }

    @Test
    void defaultsToPypi() {
        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), null, null, env(Map.of(), home));
        assertThat(indexes).containsExactly(
          new PythonPackageIndex("pypi", "https://pypi.org/simple", true, null, null, false));
    }

    @Test
    void urlUserinfoExpandedAndPercentEncoded() {
        Toml.Document doc = pipfile("""
          [[source]]
          name = "corp"
          url = "https://${INDEX_USER}:${INDEX_PASS}@corp.example.com/simple"
          verify_ssl = true
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), doc, null,
          env(Map.of("INDEX_USER", "alice", "INDEX_PASS", "p@ss:w0rd"), home));

        assertThat(indexes).hasSize(1);
        PythonPackageIndex index = indexes.get(0);
        // only the userinfo portion is percent-encoded
        assertThat(index.getUrl()).isEqualTo("https://alice:p%40ss%3Aw0rd@corp.example.com/simple");
        assertThat(index.isUnresolvedPlaceholders()).isFalse();
        // URL-embedded credentials are surfaced decoded
        assertThat(index.getUsername()).isEqualTo("alice");
        assertThat(index.getPassword()).isEqualTo("p@ss:w0rd");
    }

    @Test
    void partiallyResolvedUserinfoFlagsIndex() {
        Toml.Document doc = pipfile("""
          [[source]]
          name = "corp"
          url = "https://${INDEX_USER}:${INDEX_PASS}@corp.example.com/simple"
          verify_ssl = true
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), doc, null,
          env(Map.of("INDEX_USER", "alice"), home));

        assertThat(indexes).hasSize(1);
        assertThat(indexes.get(0).getUrl())
          .isEqualTo("https://alice:%24%7BINDEX_PASS%7D@corp.example.com/simple");
        assertThat(indexes.get(0).isUnresolvedPlaceholders()).isTrue();
        // the encoded placeholder must never be surfaced as a literal password
        assertThat(indexes.get(0).getPassword()).isNull();
    }

    @Test
    void unsetVariableStaysLiteralAndFlagsIndex() {
        Toml.Document doc = pipfile("""
          [[source]]
          name = "corp"
          url = "https://${UNSET_TOKEN}@corp.example.com/simple"
          verify_ssl = true
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), doc, null, env(Map.of(), home));
        assertThat(indexes).hasSize(1);
        assertThat(indexes.get(0).getUrl()).isEqualTo("https://${UNSET_TOKEN}@corp.example.com/simple");
        assertThat(indexes.get(0).isUnresolvedPlaceholders()).isTrue();
        assertThat(indexes.get(0).getUsername()).isNull();
    }

    @Test
    void nonUrlFieldsUseExpandvarsSemantics() {
        Toml.Document doc = pipfile("""
          [[source]]
          name = "$SOURCE_NAME"
          url = "https://corp.example.com/simple"
          verify_ssl = true

          [[source]]
          name = "$UNSET_NAME"
          url = "https://other.example.com/simple"
          verify_ssl = true
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), doc, null,
          env(Map.of("SOURCE_NAME", "corp"), home));
        assertThat(indexes.get(0).getName()).isEqualTo("corp");
        // unset variables stay literal, as with os.path.expandvars
        assertThat(indexes.get(1).getName()).isEqualTo("$UNSET_NAME");
    }

    @Test
    void netrcFillsCredentialsByHostname() throws IOException {
        Files.writeString(home.resolve(".netrc"), """
          machine corp.example.com
            login bob
            password sekret

          default login dee password fault
          """);

        Toml.Document doc = pipfile("""
          [[source]]
          name = "corp"
          url = "https://corp.example.com/simple"
          verify_ssl = true

          [[source]]
          name = "other"
          url = "https://other.example.com/simple"
          verify_ssl = true
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), doc, null, env(Map.of(), home));
        assertThat(indexes.get(0).getUsername()).isEqualTo("bob");
        assertThat(indexes.get(0).getPassword()).isEqualTo("sekret");
        // no machine match falls back to the default entry
        assertThat(indexes.get(1).getUsername()).isEqualTo("dee");
        assertThat(indexes.get(1).getPassword()).isEqualTo("fault");
    }

    @Test
    void viewCredentialsFillByHost() {
        ExecutionContext ctx = ctx();
        PythonExecutionContextView.view(ctx).setIndexCredentials(List.of(
          new PythonIndexCredentials("corp.example.com", "alice", "hunter2")));

        Toml.Document doc = pipfile("""
          [[source]]
          name = "corp"
          url = "https://corp.example.com/simple"
          verify_ssl = true

          [[source]]
          name = "other"
          url = "https://other.example.com/simple"
          verify_ssl = true
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx, doc, null, env(Map.of(), home));
        assertThat(indexes.get(0).getUsername()).isEqualTo("alice");
        assertThat(indexes.get(0).getPassword()).isEqualTo("hunter2");
        assertThat(indexes.get(1).getUsername()).isNull();
    }

    @Test
    void urlEmbeddedCredentialsWinOverViewCredentials() {
        ExecutionContext ctx = ctx();
        PythonExecutionContextView.view(ctx).setIndexCredentials(List.of(
          new PythonIndexCredentials("corp.example.com", "carol", "other")));

        Toml.Document doc = pipfile("""
          [[source]]
          name = "corp"
          url = "https://alice:hunter2@corp.example.com/simple"
          verify_ssl = true
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx, doc, null, env(Map.of(), home));
        assertThat(indexes.get(0).getUsername()).isEqualTo("alice");
        assertThat(indexes.get(0).getPassword()).isEqualTo("hunter2");
    }

    @Test
    void viewCredentialsBeatNetrc() throws IOException {
        Files.writeString(home.resolve(".netrc"), """
          machine corp.example.com login bob password sekret
          """);
        ExecutionContext ctx = ctx();
        PythonExecutionContextView.view(ctx).setIndexCredentials(List.of(
          new PythonIndexCredentials("corp.example.com", "alice", "hunter2")));

        Toml.Document doc = pipfile("""
          [[source]]
          name = "corp"
          url = "https://corp.example.com/simple"
          verify_ssl = true
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx, doc, null, env(Map.of(), home));
        assertThat(indexes.get(0).getUsername()).isEqualTo("alice");
        assertThat(indexes.get(0).getPassword()).isEqualTo("hunter2");
    }

    @Test
    void urlEmbeddedCredentialsWinOverNetrc() throws IOException {
        Files.writeString(home.resolve(".netrc"), """
          machine corp.example.com login bob password sekret
          """);

        Toml.Document doc = pipfile("""
          [[source]]
          name = "corp"
          url = "https://alice:hunter2@corp.example.com/simple"
          verify_ssl = true
          """);

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), doc, null, env(Map.of(), home));
        assertThat(indexes.get(0).getUsername()).isEqualTo("alice");
        assertThat(indexes.get(0).getPassword()).isEqualTo("hunter2");
    }

    @Test
    void lockSourcesExpandPlaceholders() {
        List<Map<String, Object>> lockSources = List.of(
          Map.of("name", "corp", "url", "https://${LOCK_USER}@lock.example.com/simple", "verify_ssl", true));

        List<PythonPackageIndex> indexes = IndexDiscovery.discover(ctx(), null, lockSources,
          env(Map.of("LOCK_USER", "carol"), home));
        assertThat(indexes.get(0).getUrl()).isEqualTo("https://carol@lock.example.com/simple");
        assertThat(indexes.get(0).getUsername()).isEqualTo("carol");
    }

    private void writeUserPipConf(String content) throws IOException {
        Path pipDir = home.resolve("Library/Application Support/pip");
        Files.createDirectories(pipDir);
        Files.writeString(pipDir.resolve("pip.conf"), content);
    }
}
