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
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class InstalledEnvParserTest {

    private static void writeDistInfo(Path sitePackages, String name, String version, String metadata) throws IOException {
        Path distInfo = sitePackages.resolve(name + "-" + version + ".dist-info");
        Files.createDirectories(distInfo);
        Files.write(distInfo.resolve("METADATA"), metadata.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void envIsIntactOnlyWhileItsPackagesAreOnDisk(@TempDir Path env) throws IOException {
        Path sitePackages = env.resolve("lib/python3.12/site-packages");
        writeDistInfo(sitePackages, "a", "1.0.0", """
          Metadata-Version: 2.4
          Name: a
          Version: 1.0.0
          """);
        assertThat(InstalledEnvParser.isIntact(env)).isTrue();

        Path distInfo = sitePackages.resolve("a-1.0.0.dist-info");
        Files.delete(distInfo.resolve("METADATA"));
        assertThat(InstalledEnvParser.isIntact(env)).isFalse();

        Files.delete(distInfo);
        assertThat(InstalledEnvParser.isIntact(env)).isFalse();
    }

    @Test
    void linksRequiredDependenciesDeeplyButNotThoseOfExtras(@TempDir Path env) throws IOException {
        Path sitePackages = env.resolve("lib/python3.12/site-packages");
        writeDistInfo(sitePackages, "a", "1.0.0", """
          Metadata-Version: 2.4
          Name: a
          Version: 1.0.0
          Requires-Dist: b>=1.0
          Requires-Dist: c>=1.0; extra == "socks"
          """);
        writeDistInfo(sitePackages, "b", "1.0.0", """
          Metadata-Version: 2.4
          Name: b
          Version: 1.0.0
          Requires-Dist: c>=1.0
          """);
        writeDistInfo(sitePackages, "c", "1.0.0", """
          Metadata-Version: 2.4
          Name: c
          Version: 1.0.0
          """);

        List<ResolvedDependency> resolved = InstalledEnvParser.parse(env);

        assertThat(resolved).hasSize(3);
        ResolvedDependency a = resolved.get(0);
        ResolvedDependency b = resolved.get(1);
        ResolvedDependency c = resolved.get(2);
        assertThat(a.getDependencies()).containsExactly(b);
        assertThat(b.getDependencies().get(0)).isSameAs(c);
        assertThat(a.getDependencies().get(0).getDependencies().get(0)).isSameAs(c);
        assertThat(c.getDependencies()).isNull();
    }

    @Test
    void readsProjectUrlsFromMetadata(@TempDir Path env) throws IOException {
        Path sitePackages = env.resolve("lib/python3.12/site-packages");
        writeDistInfo(sitePackages, "a", "1.0.0", """
          Metadata-Version: 2.4
          Name: a
          Version: 1.0.0
          Home-page: https://a.example.com
          Project-URL: Source, https://github.com/example/a
          Requires-Dist: b>=1.0
          """);
        writeDistInfo(sitePackages, "b", "1.0.0", """
          Metadata-Version: 2.4
          Name: b
          Version: 1.0.0
          """);
        Files.createDirectories(sitePackages.resolve("c-1.0.0.dist-info"));

        List<ResolvedDependency> resolved = InstalledEnvParser.parse(env);

        assertThat(resolved.get(0).getProjectUrls()).containsExactly(
          entry("Source", "https://github.com/example/a"),
          entry("Homepage", "https://a.example.com"));
        assertThat(resolved.get(1).getProjectUrls())
          .as("metadata that declares no URL is read, and has none")
          .isEmpty();
        assertThat(resolved.get(2).getProjectUrls())
          .as("a distribution without METADATA has nothing to read")
          .isNull();
    }

    @Test
    void lockedDependenciesTakeProjectUrlsFromTheEnvTheyAreInstalledIn(@TempDir Path env) throws IOException {
        Path sitePackages = env.resolve("lib/python3.12/site-packages");
        writeDistInfo(sitePackages, "typing_extensions", "4.12.2", """
          Metadata-Version: 2.4
          Name: typing_extensions
          Version: 4.12.2
          Project-URL: Repository, https://github.com/python/typing_extensions
          """);
        writeDistInfo(sitePackages, "b", "2.0.0", """
          Metadata-Version: 2.4
          Name: b
          Version: 2.0.0
          Project-URL: Source, https://github.com/example/b
          """);
        ResolvedDependency typingExtensions = new ResolvedDependency("typing-extensions", "4.12.2", "https://pypi.org/simple", null);
        ResolvedDependency b = new ResolvedDependency("b", "1.0.0", null, new ArrayList<>());
        b.getDependencies().add(typingExtensions);

        List<ResolvedDependency> resolved = InstalledEnvParser.withProjectUrls(List.of(b, typingExtensions), env);

        assertThat(resolved.get(1).getProjectUrls())
          .containsExactly(entry("Repository", "https://github.com/python/typing_extensions"));
        assertThat(resolved.get(1).getSource()).isEqualTo("https://pypi.org/simple");
        assertThat(resolved.get(0).getProjectUrls())
          .as("the env holds another version of b, whose URLs are not this one's")
          .isNull();
        assertThat(resolved.get(0).getDependencies().get(0))
          .as("the graph still shares one instance per package")
          .isSameAs(resolved.get(1));
    }
}
