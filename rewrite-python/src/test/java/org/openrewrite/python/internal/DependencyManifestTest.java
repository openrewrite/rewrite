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
package org.openrewrite.python.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.python.internal.DependencyManifest.Requirements;
import org.openrewrite.python.internal.DependencyManifest.SetupPyReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.openrewrite.python.internal.DependencyManifest.*;

class DependencyManifestTest {

    private static final SetupPyReader NO_SETUP_PY = (projectDir, extras) -> {
        throw new AssertionError("no setup.py is read");
    };

    @TempDir
    Path dir;

    @Test
    void pyprojectComesFirstOnlyWhenItListsDependencies() throws IOException {
        write("requirements.txt", "six\n");
        write("pyproject.toml", "[tool.poetry.dependencies]\nsix = \"*\"\n");
        write("poetry.lock", "");
        assertThat(choose(dir)).isEqualTo(RequirementsFiles);

        write("pyproject.toml", "[project]\nname = \"demo\"\ndependencies = [\"six\"]\n");
        assertThat(choose(dir)).isEqualTo(PyProjectToml);
    }

    @Test
    void pyprojectWithoutListedDependenciesDefersToTheOtherManifests() throws IOException {
        write("pyproject.toml", "[project]\nname = \"demo\"\ndynamic = [\"dependencies\"]\n");
        write("setup.py", "from setuptools import setup; setup()\n");
        assertThat(choose(dir)).isEqualTo(SetupPy);

        write("requirements-dev.txt", "pytest\n");
        assertThat(choose(dir)).isEqualTo(RequirementsFiles);

        write("Pipfile", "[packages]\nsix = \"*\"\n");
        assertThat(choose(dir)).isEqualTo(Pipfile);
    }

    @Test
    void projectTableAloneGetsAnEnvWithoutRootsButToolSettingsAloneGetNone() throws IOException {
        write("pyproject.toml", "[project]\nname = \"demo\"\n");
        assertThat(choose(dir)).isEqualTo(PyProjectToml);
        assertThat(PyProjectToml.read(dir, null, emptyMap(), NO_SETUP_PY).getRoots()).isEmpty();

        write("pyproject.toml", "[tool.ruff]\nline-length = 100\n");
        assertThat(choose(dir)).isNull();
    }

    @Test
    void pyprojectRootsAreRuntimeAndDefaultGroupsWithoutLocalPackages() throws IOException {
        write("pyproject.toml", """
          [project]
          name = "demo"
          requires-python = ">=3.11"
          dependencies = ["requests[socks]>=2.28", "member", "vendored @ file:///opt/vendored"]

          [project.optional-dependencies]
          docs = ["sphinx"]

          [dependency-groups]
          dev = ["pytest", "torch"]
          typing = ["mypy"]

          [tool.uv.sources]
          member = { workspace = true }
          torch = { index = "pytorch" }

          [[tool.uv.index]]
          url = "https://pkgs.example.com/simple"

          [[tool.uv.index]]
          name = "pytorch"
          url = "https://download.pytorch.org/whl/cpu"
          explicit = true
          """);

        Requirements requirements = PyProjectToml.read(dir, null, emptyMap(), NO_SETUP_PY);
        assertThat(requirements.getRoots()).containsExactly("requests[socks]>=2.28", "pytest");
        assertThat(requirements.getExcludes()).containsExactly("member", "torch");
        assertThat(requirements.getIndexes()).containsExactly("https://pkgs.example.com/simple");
        assertThat(requirements.getRequiresPython()).containsExactly(">=3.11");
    }

    @Test
    void aLockPinsOnlyThePackagesItLocksAtOneVersion() throws IOException {
        write("pyproject.toml", "[project]\nname = \"demo\"\ndependencies = [\"six\"]\n");
        write("uv.lock", """
          version = 1
          revision = 3
          requires-python = ">=3.12"

          [[package]]
          name = "numpy"
          version = "1.26.4"
          source = { registry = "https://pypi.org/simple" }

          [[package]]
          name = "numpy"
          version = "2.1.0"
          source = { registry = "https://pypi.org/simple" }

          [[package]]
          name = "six"
          version = "1.16.0"
          source = { registry = "https://pypi.org/simple" }
          """);

        Requirements requirements = PyProjectToml.read(dir, null, emptyMap(), NO_SETUP_PY);
        assertThat(requirements.getRoots()).containsExactly("six");
        assertThat(requirements.getConstraints()).containsExactly("six==1.16.0");
    }

    @Test
    void pipfileRootsLockPinsAndIndexCredentials() throws IOException {
        write("Pipfile", """
          [[source]]
          url = "https://pypi.org/simple"
          name = "pypi"

          [[source]]
          url = "https://${PRIVATE_TOKEN}@pkgs.example.com/simple"
          name = "private"

          [packages]
          requests = "*"
          django = ">=4.2"
          mylib = { path = ".", editable = true }
          pywin32 = { version = "*", sys_platform = "== 'win32'" }
          uvloop = { version = "*", markers = "python_version < '3.9' or sys_platform == 'linux'", platform_machine = "== 'x86_64'" }

          [dev-packages]
          pytest = { version = "==8.0.0", extras = ["testing"] }

          [requires]
          python_version = "3.11"
          """);
        write("Pipfile.lock", """
          {
            "_meta": {"sources": [{"name": "pypi", "url": "https://pypi.org/simple"}]},
            "default": {
              "django": {"version": "==4.2.1"},
              "requests": {"version": "==2.31.0"}
            },
            "develop": {
              "pytest": {"version": "==8.0.0"}
            }
          }
          """);

        Requirements requirements = Pipfile.read(dir, null, singletonMap("PRIVATE_TOKEN", "secret"), NO_SETUP_PY);
        assertThat(requirements.getRoots()).containsExactly(
          "requests", "django>=4.2", "pywin32 ; sys_platform == 'win32'",
          "uvloop ; (python_version < '3.9' or sys_platform == 'linux') and (platform_machine == 'x86_64')",
          "pytest[testing]==8.0.0");
        assertThat(requirements.getConstraints()).containsExactly("django==4.2.1", "requests==2.31.0", "pytest==8.0.0");
        assertThat(requirements.getIndexes()).containsExactly("private=https://secret@pkgs.example.com/simple");
        assertThat(requirements.getRequiresPython()).containsExactly("==3.11.*");

        assertThatThrownBy(() -> Pipfile.read(dir, null, emptyMap(), NO_SETUP_PY))
          .hasMessageContaining("${PRIVATE_TOKEN}")
          .hasMessageNotContaining("pkgs.example.com");
    }

    @Test
    void requirementsFilesFlattenIntoOneSetThatTakesInLocalProjectsDependencies() throws IOException {
        write("constraints.txt", "urllib3<2\n");
        write("requirements/base.txt", "requests==2.31.0\n");
        write("requirements.txt", "-rrequirements/base.txt\n-c constraints.txt\n" +
                                  "--extra-index-url https://pkgs.example.com/simple\n-f ./wheels\n");
        Files.createDirectories(dir.resolve("wheels"));
        write("requirements-dev.txt", "-e ./lib[test]\n-e git+https://github.com/org/tool.git#egg=tool\n./vendor/tool.whl\npytest\n");
        write("lib/pyproject.toml", """
          [project]
          name = "lib"
          dependencies = ["click"]

          [project.optional-dependencies]
          test = ["pytest-mock"]
          """);

        Requirements requirements = RequirementsFiles.read(dir, null, emptyMap(), NO_SETUP_PY);
        assertThat(requirements.getRoots()).containsExactly(
          "click",
          "pytest-mock",
          "git+https://github.com/org/tool.git#egg=tool",
          "pytest",
          "requests==2.31.0",
          "--extra-index-url https://pkgs.example.com/simple",
          "--find-links " + dir.resolve("wheels"));
        assertThat(requirements.getConstraints()).containsExactly("urllib3<2");
    }

    @Test
    void setupCfgWithoutSetupPyIsReadWithoutRunningIt() throws IOException {
        write("setup.cfg", """
          [metadata]
          name = demo

          [options]
          python_requires = >=3.9
          install_requires =
              requests>=2.28
              colorama; sys_platform == "win32"

          [options.extras_require]
          docs = sphinx
          """);

        Requirements requirements = SetupCfg.read(dir, null, emptyMap(), NO_SETUP_PY);
        assertThat(requirements.getRoots()).containsExactly("requests>=2.28", "colorama; sys_platform == \"win32\"");
        assertThat(requirements.getRequiresPython()).containsExactly(">=3.9");
    }

    @Test
    void envPythonIsTheLowestFrom312ThatTheProjectAllows() {
        assertThat(pythonVersion(emptyList())).isEqualTo("3.12");
        assertThat(pythonVersion(asList(">=3.9", ">=3.13"))).isEqualTo("3.13");
        assertThat(pythonVersion(singletonList(">=3.8,<3.12"))).isEqualTo("3.11");
        assertThat(pythonVersion(singletonList("==3.11.*"))).isEqualTo("3.11");
        assertThat(pythonVersion(singletonList("<3.9"))).isEqualTo("3.8");
        assertThat(pythonVersion(singletonList("<3.8"))).isNull();
    }

    private void write(String path, String content) throws IOException {
        Path file = dir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes());
    }
}
