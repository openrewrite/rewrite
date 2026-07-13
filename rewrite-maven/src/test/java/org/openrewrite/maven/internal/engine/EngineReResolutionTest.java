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
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.internal.ResolutionEngineSelector;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.maven.tree.ResolvedPom;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Phase-2c gate for {@code UpdateMavenModel}-style re-resolution under the MAVEN engine: mutating a reactor parent's
 * managed version and refreshing the {@link PomXmlRegistry} + bumping the reactor epoch (exactly what
 * {@code UpdateMavenModel} does) makes a child's re-resolved effective pom pick up the mutation — the XML-first
 * bytes-refresh + epoch path, proven at the {@code ResolvedPom.resolve} facade.
 */
class EngineReResolutionTest {

    private static final Path PARENT_PATH = Paths.get("parent/pom.xml");
    private static final Path CHILD_PATH = Paths.get("child/pom.xml");

    private static String parentPom(String managedVersion) {
        return "<project>\n" +
               "  <modelVersion>4.0.0</modelVersion>\n" +
               "  <groupId>org.example</groupId>\n" +
               "  <artifactId>reactor-parent</artifactId>\n" +
               "  <version>1</version>\n" +
               "  <packaging>pom</packaging>\n" +
               "  <dependencyManagement>\n" +
               "    <dependencies>\n" +
               "      <dependency>\n" +
               "        <groupId>org.example</groupId>\n" +
               "        <artifactId>lib</artifactId>\n" +
               "        <version>" + managedVersion + "</version>\n" +
               "      </dependency>\n" +
               "    </dependencies>\n" +
               "  </dependencyManagement>\n" +
               "</project>\n";
    }

    private static final String CHILD_POM = "<project>\n" +
            "  <modelVersion>4.0.0</modelVersion>\n" +
            "  <parent>\n" +
            "    <groupId>org.example</groupId>\n" +
            "    <artifactId>reactor-parent</artifactId>\n" +
            "    <version>1</version>\n" +
            "  </parent>\n" +
            "  <artifactId>reactor-child</artifactId>\n" +
            "</project>\n";

    @Test
    void mutateParentThenReResolveChildUnderMavenPicksUpTheMutation() throws MavenDownloadingException {
        ExecutionContext base = new InMemoryExecutionContext(Throwable::printStackTrace);
        base.putMessage(ResolutionEngineSelector.ENGINE_KEY, "maven");
        MavenExecutionContextView ctx = MavenExecutionContextView.view(base);
        ctx.setPomCache(new InMemoryMavenPomCache());
        ctx.setAddCentralRepository(false);
        ctx.setAddLocalRepository(false);

        // Initial parse of the reactor (parent + child) under the MAVEN engine.
        MavenResolutionResult childResult = null;
        for (SourceFile parsed : MavenParser.builder().build().parseInputs(asList(
                Parser.Input.fromString(PARENT_PATH, parentPom("1.0")),
                Parser.Input.fromString(CHILD_PATH, CHILD_POM)), null, ctx).toArray(SourceFile[]::new)) {
            MavenResolutionResult mrr = parsed.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow(AssertionError::new);
            if ("reactor-child".equals(mrr.getPom().getArtifactId())) {
                childResult = mrr;
            }
        }
        assertThat(childResult).isNotNull();
        assertThat(managedVersion(childResult.getPom())).as("initial managed version inherited from the parent").isEqualTo("1.0");

        // Simulate UpdateMavenModel: the parent document is edited, its printed bytes refresh the registry, the reactor
        // epoch bumps, and projectPoms carries the mutated parent. Then the child re-resolves through the facade.
        byte[] mutatedParentXml = parentPom("2.0").getBytes(StandardCharsets.UTF_8);
        Pom mutatedParent = RawPom.parse(new ByteArrayInputStream(mutatedParentXml), null).toPom(PARENT_PATH, null);
        PomXmlRegistry.put(ctx, mutatedParent, mutatedParentXml);
        PomXmlRegistry.bumpEpoch(ctx);

        Map<Path, Pom> projectPoms = new HashMap<>(childResult.getProjectPoms());
        projectPoms.put(PARENT_PATH, mutatedParent);
        MavenPomDownloader downloader = new MavenPomDownloader(projectPoms, ctx);

        ResolvedPom reResolved = childResult.getPom().resolve(ctx, downloader);
        assertThat(managedVersion(reResolved)).as("re-resolved managed version after the parent mutation").isEqualTo("2.0");
    }

    /**
     * The effective-model memo (DESIGN §6) perf-correctness gate: a mutated parent's fresh value stays visible (the epoch
     * bump invalidates the memo), while an unmutated sibling re-resolved at a stable epoch is served from the memo instead
     * of rebuilt. Proven at the {@code ResolvedPom.resolve} facade with the memo's own hit/miss stats.
     */
    @Test
    void memoServesUnmutatedSiblingsAndInvalidatesTheMutatedParent() throws MavenDownloadingException {
        ExecutionContext base = new InMemoryExecutionContext(Throwable::printStackTrace);
        base.putMessage(ResolutionEngineSelector.ENGINE_KEY, "maven");
        MavenExecutionContextView ctx = MavenExecutionContextView.view(base);
        ctx.setPomCache(new InMemoryMavenPomCache());
        ctx.setAddCentralRepository(false);
        ctx.setAddLocalRepository(false);

        Path childBPath = Paths.get("child-b/pom.xml");
        String childBPom = CHILD_POM.replace("reactor-child", "reactor-child-b");
        MavenResolutionResult childA = null;
        MavenResolutionResult childB = null;
        for (SourceFile parsed : MavenParser.builder().build().parseInputs(asList(
                Parser.Input.fromString(PARENT_PATH, parentPom("1.0")),
                Parser.Input.fromString(CHILD_PATH, CHILD_POM),
                Parser.Input.fromString(childBPath, childBPom)), null, ctx).toArray(SourceFile[]::new)) {
            MavenResolutionResult mrr = parsed.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow(AssertionError::new);
            if ("reactor-child".equals(mrr.getPom().getArtifactId())) {
                childA = mrr;
            } else if ("reactor-child-b".equals(mrr.getPom().getArtifactId())) {
                childB = mrr;
            }
        }
        assertThat(childA).isNotNull();
        assertThat(childB).isNotNull();

        // Re-resolving an unmutated sibling at a stable epoch is served from the memo, not rebuilt.
        MavenPomDownloader downloader = new MavenPomDownloader(childB.getProjectPoms(), ctx);
        long hitsBefore = MavenEngineResolution.effectiveMemoStats(ctx).hitCount();
        childB.getPom().resolve(ctx, downloader);
        assertThat(MavenEngineResolution.effectiveMemoStats(ctx).hitCount())
                .as("unmutated sibling served from the effective-model memo").isGreaterThan(hitsBefore);

        // Mutating the parent + bumping the epoch invalidates the memo: the dependent picks up the fresh value.
        byte[] mutatedParentXml = parentPom("2.0").getBytes(StandardCharsets.UTF_8);
        Pom mutatedParent = RawPom.parse(new ByteArrayInputStream(mutatedParentXml), null).toPom(PARENT_PATH, null);
        PomXmlRegistry.put(ctx, mutatedParent, mutatedParentXml);
        PomXmlRegistry.bumpEpoch(ctx);
        Map<Path, Pom> projectPoms = new HashMap<>(childA.getProjectPoms());
        projectPoms.put(PARENT_PATH, mutatedParent);
        MavenPomDownloader mutatedDownloader = new MavenPomDownloader(projectPoms, ctx);
        ResolvedPom reResolvedA = childA.getPom().resolve(ctx, mutatedDownloader);
        assertThat(managedVersion(reResolvedA)).as("mutated value visible after epoch bump").isEqualTo("2.0");
    }

    private static String managedVersion(ResolvedPom pom) {
        for (ResolvedManagedDependency dm : pom.getDependencyManagement()) {
            if ("lib".equals(dm.getArtifactId())) {
                return dm.getVersion();
            }
        }
        throw new AssertionError("no managed 'lib' entry in " + pom.getGav());
    }
}
