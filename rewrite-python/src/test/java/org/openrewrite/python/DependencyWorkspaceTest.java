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
package org.openrewrite.python;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.python.internal.InstalledEnvParser;
import org.openrewrite.python.internal.PackageManagerExecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DependencyWorkspaceTest {

    @TempDir
    Path dir;

    @BeforeEach
    void requireUv() {
        assumeTrue(PackageManagerExecutor.UV.find() != null, "uv is not installed");
    }

    @Test
    void lockPinsDecideTheInstalledVersions() throws IOException {
        write("pyproject.toml", "[project]\nname = \"demo\"\nversion = \"0.1.0\"\ndependencies = [\"six\"]\n");
        write("uv.lock", """
          version = 1
          revision = 3
          requires-python = ">=3.12"

          [[package]]
          name = "demo"
          version = "0.1.0"
          source = { editable = "." }
          dependencies = [
              { name = "six" },
          ]

          [[package]]
          name = "six"
          version = "1.16.0"
          source = { registry = "https://pypi.org/simple" }
          """);

        DependencyWorkspace.Result result = DependencyWorkspace.provision(dir, options());

        assertThat(result.getFailure()).isNull();
        assertThat(result.getManifest()).isEqualTo("pyproject.toml");
        assertThat(result.getPythonVersion()).isEqualTo("3.12");
        assertThat(installed(result)).containsExactly("six==1.16.0");
    }

    @Test
    void envRecordsTheRequirementsASetupPyDeclares() throws IOException {
        write("app/setup.py", "from setuptools import setup\nsetup(name='demo', version='0.1.0', install_requires=['six'])\n");

        DependencyWorkspace.Result result = DependencyWorkspace.provision(dir.resolve("app"), options());

        assertThat(installed(result)).singleElement().asString().startsWith("six==");
        assertThat(DependencyWorkspace.installedRequirements(requireNonNull(result.getEnv())))
          .singleElement().asString().startsWith("six==");
    }

    @Test
    void ancestorProjectSettingsDoNotReachTheWorkspace() throws IOException {
        // An ancestor with a [project] table, the kind a uv.toml in the workspace does not stop
        write("pyproject.toml", "[project]\nname = \"repo\"\nversion = \"0\"\n\n[tool.uv]\nrequired-version = \"==0.0.1\"\n");
        write("app/requirements.txt", "six\n");

        DependencyWorkspace.Result result = DependencyWorkspace.provision(dir.resolve("app"), options());

        assertThat(result.getFailure()).isNull();
        assertThat(installed(result)).singleElement().asString().startsWith("six==");
    }

    @Test
    void reusedWorkspaceIsReconciledWithTheRequirements() throws IOException {
        write("app/requirements.txt", "six\n");
        Path log = dir.resolve("uv.log");
        DependencyWorkspace.Options options = options().withLog(log);

        DependencyWorkspace.Result first = DependencyWorkspace.provision(dir.resolve("app"), options);
        assertThat(installed(first)).singleElement().asString().startsWith("six==");

        // Unchanged requirements and an intact env run no uv command
        long logged = Files.size(log);
        DependencyWorkspace.provision(dir.resolve("app"), options);
        assertThat(Files.size(log)).isEqualTo(logged);

        // An env emptied from outside is reinstalled
        Path sitePackages = requireNonNull(InstalledEnvParser.findSitePackages(requireNonNull(first.getEnv())));
        try (Stream<Path> distInfos = Files.list(sitePackages)) {
            for (Path distInfo : distInfos.filter(p -> p.getFileName().toString().endsWith(".dist-info")).collect(toList())) {
                Files.delete(distInfo.resolve("METADATA"));
            }
        }
        assertThat(installed(DependencyWorkspace.provision(dir.resolve("app"), options)))
          .singleElement().asString().startsWith("six==");

        // A dependency the requirements no longer name is removed
        write("app/requirements.txt", "idna\n");
        assertThat(installed(DependencyWorkspace.provision(dir.resolve("app"), options)))
          .singleElement().asString().startsWith("idna==");
    }

    @Test
    void envThatInstallsNoPackagesIsReused() throws IOException {
        write("app/requirements.txt", "six ; python_version < '3.0'\n");
        Path log = dir.resolve("uv.log");
        DependencyWorkspace.Options options = options().withLog(log);

        assertThat(installed(DependencyWorkspace.provision(dir.resolve("app"), options))).isEmpty();
        long logged = Files.size(log);
        DependencyWorkspace.provision(dir.resolve("app"), options);
        assertThat(Files.size(log)).isEqualTo(logged);
    }

    private DependencyWorkspace.Options options() {
        return DependencyWorkspace.Options.builder()
          .workspace(dir.resolve(".workspace"))
          .relativeTo(dir)
          .build();
    }

    private static List<String> installed(DependencyWorkspace.Result result) {
        assertThat(result.getEnv()).isNotNull();
        return InstalledEnvParser.parse(result.getEnv()).stream()
          .map(r -> r.getName() + "==" + r.getVersion())
          .collect(toList());
    }

    private void write(String path, String content) throws IOException {
        Path file = dir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
