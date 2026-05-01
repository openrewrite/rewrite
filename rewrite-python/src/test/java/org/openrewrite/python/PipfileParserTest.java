/*
 * Copyright 2026 the original author or authors.
 *
 * Moderne Proprietary. Only for use by Moderne customers under the terms of a commercial contract.
 */
package org.openrewrite.python;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.toml.tree.Toml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PipfileParserTest {

    @Test
    void parsesWithResolvedDependencies(@TempDir Path tempDir) throws IOException {
        String pipfile = """
          [[source]]
          url = "https://pypi.org/simple"
          verify_ssl = true
          name = "pypi"

          [packages]
          requests = ">=2.28.0"

          [dev-packages]
          pytest = ">=7.0"

          [requires]
          python_version = "3.10"
          """;

        String pipfileLock = """
          {
              "_meta": {
                  "hash": {"sha256": "abc"},
                  "pipfile-spec": 6,
                  "requires": {"python_version": "3.10"},
                  "sources": [
                      {"name": "pypi", "url": "https://pypi.org/simple", "verify_ssl": true}
                  ]
              },
              "default": {
                  "requests": {"version": "==2.31.0"},
                  "certifi": {"version": "==2024.2.2"}
              },
              "develop": {
                  "pytest": {"version": "==7.4.0"}
              }
          }
          """;

        Files.write(tempDir.resolve("Pipfile"), pipfile.getBytes());
        Files.write(tempDir.resolve("Pipfile.lock"), pipfileLock.getBytes());

        PipfileParser parser = new PipfileParser();
        Parser.Input input = Parser.Input.fromFile(tempDir.resolve("Pipfile"));
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                tempDir,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        Toml.Document doc = (Toml.Document) parsed.get(0);
        PythonResolutionResult marker = doc.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();
        assertThat(marker.getPackageManager()).isEqualTo(PythonResolutionResult.PackageManager.Pipenv);

        assertThat(marker.getResolvedDependencies()).hasSize(3);
        assertThat(marker.getResolvedDependency("requests")).isNotNull();
        assertThat(marker.getResolvedDependency("requests").getVersion()).isEqualTo("2.31.0");
        assertThat(marker.getResolvedDependency("pytest")).isNotNull();
        assertThat(marker.getResolvedDependency("pytest").getVersion()).isEqualTo("7.4.0");

        Dependency requests = marker.getDependencies().get(0);
        assertThat(requests.getName()).isEqualTo("requests");
        assertThat(requests.getResolved()).isNotNull();
        assertThat(requests.getResolved().getVersion()).isEqualTo("2.31.0");

        // dev-packages dependencies are tracked under optionalDependencies and should
        // also be linked to their resolved versions.
        List<Dependency> devDeps = marker.getOptionalDependencies().get("dev-packages");
        assertThat(devDeps).isNotNull();
        assertThat(devDeps).hasSize(1);
        assertThat(devDeps.get(0).getName()).isEqualTo("pytest");
        assertThat(devDeps.get(0).getResolved()).isNotNull();
        assertThat(devDeps.get(0).getResolved().getVersion()).isEqualTo("7.4.0");
    }

    @Test
    void parsesWithoutLockFile(@TempDir Path tempDir) throws IOException {
        // Without a Pipfile.lock and without pipenv on PATH the marker should still
        // be produced — just without resolved dependencies.
        String pipfile = """
          [packages]
          requests = ">=2.28.0"
          """;
        Files.write(tempDir.resolve("Pipfile"), pipfile.getBytes());

        PipfileParser parser = new PipfileParser();
        Parser.Input input = Parser.Input.fromFile(tempDir.resolve("Pipfile"));
        List<SourceFile> parsed = parser.parseInputs(
                Collections.singletonList(input),
                tempDir,
                new InMemoryExecutionContext(Throwable::printStackTrace)
        ).collect(Collectors.toList());

        assertThat(parsed).hasSize(1);
        Toml.Document doc = (Toml.Document) parsed.get(0);
        PythonResolutionResult marker = doc.getMarkers().findFirst(PythonResolutionResult.class).orElse(null);
        assertThat(marker).isNotNull();
        assertThat(marker.getDependencies()).hasSize(1);
        assertThat(marker.getDependencies().get(0).getName()).isEqualTo("requests");
        // resolvedDependencies may be empty when neither Pipfile.lock nor pipenv is available.
    }
}
