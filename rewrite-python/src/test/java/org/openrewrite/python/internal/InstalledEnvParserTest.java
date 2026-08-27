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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InstalledEnvParserTest {

    private static void writeDistInfo(Path sitePackages, String name, String version, String metadata) throws IOException {
        Path distInfo = sitePackages.resolve(name + "-" + version + ".dist-info");
        Files.createDirectories(distInfo);
        Files.write(distInfo.resolve("METADATA"), metadata.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void linksTransitiveDependenciesDeeply(@TempDir Path env) throws IOException {
        Path sitePackages = env.resolve("lib/python3.12/site-packages");
        writeDistInfo(sitePackages, "a", "1.0.0", """
          Metadata-Version: 2.4
          Name: a
          Version: 1.0.0
          Requires-Dist: b>=1.0
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
        assertThat(a.getDependencies().get(0)).isSameAs(b);
        assertThat(b.getDependencies().get(0)).isSameAs(c);
        assertThat(a.getDependencies().get(0).getDependencies().get(0)).isSameAs(c);
        assertThat(c.getDependencies()).isNull();
    }
}
