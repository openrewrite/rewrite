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
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.internal.ResolutionEngineSelector;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

/**
 * Pins the {@link EffectivePomMapper} / {@link EngineModelResolver} / {@link BomGavAttributor} Phase-2-D fixes against
 * the real engine (MAVEN mode), independent of the shadow differential. Hermetic — effective-pom construction only, so
 * dependency resolution is skipped and every referenced pom is a reactor sibling.
 */
class EngineEffectivePomProjectionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        ExecutionContext ctx = new InMemoryExecutionContext();
        ctx.putMessage(ResolutionEngineSelector.ENGINE_KEY, "maven");
        MavenExecutionContextView.view(ctx).setPomCache(new InMemoryMavenPomCache());
        spec.executionContext(ctx)
                .parser(MavenParser.builder().skipDependencyResolution(true));
    }

    // L-P2-D-007: a ${...} gav is interpolated from the merged properties, mirroring legacy's projected gav.
    @Test
    void ciFriendlyGavInterpolated() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>net.sample</groupId>
                <artifactId>sample</artifactId>
                <version>${revision}</version>
                <properties>
                  <revision>1.2.3-SNAPSHOT</revision>
                </properties>
              </project>
              """,
            spec -> spec.afterRecipe(pom -> {
                ResolvedPom resolved = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
                assertThat(resolved.getGav().getVersion()).isEqualTo("1.2.3-SNAPSHOT");
                assertThat(resolved.getVersion()).isEqualTo("1.2.3-SNAPSHOT");
            })
          )
        );
    }

    // L-P2-D-008: a parser/config-injected property overrides a POM-declared property of the same name.
    @Test
    void builderPropertyOverridesPomProperty() {
        rewriteRun(
          spec -> spec.parser(MavenParser.builder()
            .property("project.version", "9.9.9")
            .skipDependencyResolution(true)),
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>root</artifactId>
                <version>${project.version}</version>
                <packaging>pom</packaging>
                <properties>
                  <project.version>1.2.3</project.version>
                </properties>
              </project>
              """,
            spec -> spec.afterRecipe(pom -> {
                ResolvedPom resolved = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
                assertThat(resolved.getProperties().get("project.version")).isEqualTo("9.9.9");
                assertThat(resolved.getVersion()).isEqualTo("9.9.9");
            })
          )
        );
    }

    // L-P2-D-006: dependencyManagement collapses to one entry per g:a:classifier:type (first-wins); Maven keeps all raw.
    @Test
    void dependencyManagementDedupedByGroupArtifactClassifierType() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.example</groupId>
                <artifactId>bom</artifactId>
                <version>1</version>
                <packaging>pom</packaging>
                <dependencyManagement>
                  <dependencies>
                    <dependency><groupId>io.x</groupId><artifactId>c</artifactId><version>1.0</version></dependency>
                    <dependency><groupId>io.x</groupId><artifactId>c</artifactId><version>1.0</version><type>jar</type></dependency>
                    <dependency><groupId>io.x</groupId><artifactId>c</artifactId><version>1.0</version><scope>compile</scope></dependency>
                    <dependency><groupId>io.x</groupId><artifactId>c</artifactId><version>1.0</version><type>pom</type></dependency>
                    <dependency><groupId>io.x</groupId><artifactId>c</artifactId><version>1.0</version><classifier>sources</classifier></dependency>
                    <dependency><groupId>io.x</groupId><artifactId>c</artifactId><version>1.0</version><type>test-jar</type><scope>test</scope></dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            spec -> spec.afterRecipe(pom -> {
                ResolvedPom resolved = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
                List<ResolvedManagedDependency> dm = resolved.getDependencyManagement();
                // jar, pom, sources:jar, test-jar — the three jar duplicates collapse to one.
                assertThat(dm).hasSize(4);
                assertThat(dm).allMatch(d -> "io.x".equals(d.getGroupId()) && "c".equals(d.getArtifactId()));
                assertThat(resolved.getManagedVersion("io.x", "c", "test-jar", null)).isEqualTo("1.0");
                assertThat(resolved.getManagedVersion("io.x", "c", "jar", "sources")).isEqualTo("1.0");
            })
          )
        );
    }

    // Reactor-sibling BOM import: the imported BOM is served from the workspace, its managed entry is threaded and its
    // gav is attributed (bomGav), mirroring parent resolution.
    @Test
    void reactorSiblingBomImportResolvesAndAttributes() {
        rewriteRun(
          mavenProject("bom",
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>bom</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <dependencyManagement>
                    <dependencies>
                      <dependency><groupId>io.x</groupId><artifactId>lib</artifactId><version>2.0</version></dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """
            )
          ),
          mavenProject("app",
            pomXml(
              """
                <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1</version>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>bom</artifactId>
                        <version>1</version>
                        <type>pom</type>
                        <scope>import</scope>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """,
              spec -> spec.afterRecipe(pom -> {
                  ResolvedPom resolved = pom.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow().getPom();
                  assertThat(resolved.getManagedVersion("io.x", "lib", null, null)).isEqualTo("2.0");
                  ResolvedManagedDependency lib = resolved.getManagedDependency("io.x", "lib", null, null);
                  assertThat(lib).isNotNull();
                  assertThat(lib.getBomGav()).isNotNull();
                  assertThat(lib.getBomGav().getArtifactId()).isEqualTo("bom");
              })
            )
          )
        );
    }
}
