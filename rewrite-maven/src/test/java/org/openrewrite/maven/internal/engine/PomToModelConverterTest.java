/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven.internal.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.engine.MavenEngine;
import org.openrewrite.maven.engine.SessionConfig;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession.CloseableSession;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.*;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PomToModelConverter}: a synthetic {@code Pom.builder()} graph carries into the shaded {@link Model} exactly
 * what it holds, and — through {@link ReactorWorkspace}'s null-bytes seam — a synthetic reactor parent is inherited by
 * a real child driven through the whole model-building pipeline.
 */
class PomToModelConverterTest {

    @Test
    void convertCarriesPomFields() {
        Pom pom = Pom.builder()
                .gav(new ResolvedGroupArtifactVersion(null, "com.example", "app", "1.0", null))
                .parent(new Parent(new GroupArtifactVersion("com.example", "parent", "1"), "../pom.xml"))
                .packaging("jar")
                .properties(singletonMap("k", "v"))
                .dependencies(List.of(Dependency("dep", "2.0", "test")))
                .dependencyManagement(List.of(
                        new ManagedDependency.Defined(new GroupArtifactVersion("com.example", "managed", "3.0"), null, null, null, null),
                        new ManagedDependency.Imported(new GroupArtifactVersion("com.example", "bom", "4.0"))))
                .repositories(List.of(MavenRepository.builder().id("r").uri("http://example.invalid").build()))
                .build();

        Model model = new PomToModelConverter().convert(pom);

        assertEquals("com.example", model.getGroupId());
        assertEquals("app", model.getArtifactId());
        assertEquals("1.0", model.getVersion());
        assertEquals("jar", model.getPackaging());
        assertEquals("parent", model.getParent().getArtifactId());
        assertEquals("v", model.getProperties().getProperty("k"));
        assertEquals(1, model.getDependencies().size());
        assertEquals("dep", model.getDependencies().get(0).getArtifactId());
        assertEquals("test", model.getDependencies().get(0).getScope());
        assertEquals(2, model.getDependencyManagement().getDependencies().size());
        Dependency imported = model.getDependencyManagement().getDependencies().stream()
                .filter(d -> "bom".equals(d.getArtifactId())).findFirst().orElseThrow(AssertionError::new);
        assertEquals("pom", imported.getType());
        assertEquals("import", imported.getScope());
        assertEquals("r", model.getRepositories().get(0).getId());
    }

    @Test
    void syntheticParentResolvesThroughReactorSeam(@TempDir Path tmp) throws Exception {
        Path parentPath = Paths.get("parent", "pom.xml");
        Pom syntheticParent = Pom.builder()
                .sourcePath(parentPath)
                .gav(new ResolvedGroupArtifactVersion(null, "com.example", "synthparent", "1", null))
                .packaging("pom")
                .properties(singletonMap("sp.prop", "SP"))
                .dependencyManagement(List.of(new ManagedDependency.Defined(
                        new GroupArtifactVersion("com.example", "synthlib", "1.2.3"), null, null, null, null)))
                .build();
        // null-bytes source: the reactor must fill the parent through PomToModelConverter.
        ReactorWorkspace reactor = new ReactorWorkspace(singletonMap(parentPath, syntheticParent), p -> null);

        String childXml = "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "<modelVersion>4.0.0</modelVersion>" +
                "<parent><groupId>com.example</groupId><artifactId>synthparent</artifactId><version>1</version></parent>" +
                "<artifactId>synthchild</artifactId><packaging>jar</packaging></project>";
        Pom requested = RawPom.parse(new ByteArrayInputStream(childXml.getBytes(StandardCharsets.UTF_8)), null)
                .toPom(Paths.get("pom.xml"), null);

        ExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        MavenExecutionContextView.view(ctx).setPomCache(new InMemoryMavenPomCache());
        EffectiveSettings settings = new EffectiveSettings(emptyList(), emptyList(), Map.of());

        try (MavenEngine engine = new MavenEngine();
             CloseableSession session = engine.newSession(tmp.resolve("lrm"),
                     SessionConfig.forSender(new HttpUrlConnectionSender(Duration.ofSeconds(10), Duration.ofSeconds(10))))) {
            EngineEffectivePom service = new EngineEffectivePom(
                    engine.getRepositorySystem(), session, emptyList(), tmp.resolve("materialize"));
            EngineModelBuildingOutcome outcome = service.build(
                    childXml.getBytes(StandardCharsets.UTF_8), requested, settings, reactor, ctx);

            assertTrue(outcome.isSuccess(), () -> "expected success, got " + outcome.getFailure());
            Model effective = outcome.getResult().getEffectiveModel();
            assertEquals("SP", effective.getProperties().getProperty("sp.prop"));
            assertEquals("1.2.3", effective.getDependencyManagement().getDependencies().stream()
                    .filter(d -> "synthlib".equals(d.getArtifactId())).findFirst().orElseThrow(AssertionError::new)
                    .getVersion());
        }
    }

    private static org.openrewrite.maven.tree.Dependency Dependency(String artifactId, String version, String scope) {
        return org.openrewrite.maven.tree.Dependency.builder()
                .gav(new GroupArtifactVersion("com.example", artifactId, version))
                .scope(scope)
                .build();
    }
}
