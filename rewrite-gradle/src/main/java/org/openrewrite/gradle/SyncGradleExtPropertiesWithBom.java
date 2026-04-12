/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.trait.ExtraProperty;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.semver.LatestRelease;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class SyncGradleExtPropertiesWithBom extends Recipe {

    @Option(displayName = "Group ID",
            description = "The groupId of the BOM to sync with.",
            example = "org.springframework.boot")
    String groupId;

    @Option(displayName = "Artifact ID",
            description = "The artifactId of the BOM to sync with.",
            example = "spring-boot-dependencies")
    String artifactId;

    @Option(displayName = "Version",
            description = "The version of the BOM to sync with.",
            example = "3.4.0")
    String version;

    @Option(displayName = "Remove redundant overrides",
            description = "When enabled, ext properties whose value is lower than or equal to the BOM version " +
                    "will be removed entirely instead of updated, since the BOM default is now sufficient.",
            required = false)
    @Nullable
    Boolean removeRedundantOverrides;

    String displayName = "Sync Gradle ext properties with BOM";

    String description = "Downloads a BOM and compares its properties against Gradle ext properties. " +
            "When the BOM defines a higher version for a property, the ext property is updated to match " +
            "(or removed if `removeRedundantOverrides` is enabled).";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new IsBuildGradle<>(), new JavaIsoVisitor<ExecutionContext>() {
            final ExtraProperty.Matcher matcher = new ExtraProperty.Matcher().matchVariableDeclarations(false);
            final LatestRelease versionComparator = new LatestRelease(null);
            @Nullable
            Map<String, String> bomProperties;

            private Map<String, String> getBomProperties(ExecutionContext ctx) {
                if (bomProperties != null) {
                    return bomProperties;
                }
                SourceFile sf = getCursor().firstEnclosing(SourceFile.class);
                List<MavenRepository> repos = sf != null
                        ? sf.getMarkers().findFirst(GradleProject.class)
                        .map(GradleProject::getMavenRepositories)
                        .orElse(Collections.singletonList(MavenRepository.MAVEN_CENTRAL))
                        : Collections.singletonList(MavenRepository.MAVEN_CENTRAL);
                try {
                    MavenPomDownloader mpd = new MavenPomDownloader(ctx);
                    Pom bom = mpd.download(new GroupArtifactVersion(groupId, artifactId, version), null, null, repos);
                    bomProperties = bom.resolve(Collections.emptyList(), mpd, repos, ctx).getProperties();
                } catch (MavenDownloadingException e) {
                    bomProperties = Collections.emptyMap();
                }
                return bomProperties;
            }

            @Override
            public @Nullable Statement visitStatement(Statement statement, ExecutionContext ctx) {
                if (statement instanceof J.Assignment || statement instanceof J.MethodInvocation) {
                    ExtraProperty prop = matcher.get(getCursor()).orElse(null);
                    if (prop != null) {
                        String bomVersion = getBomProperties(ctx).get(prop.getName());
                        if (bomVersion != null) {
                            int cmp = versionComparator.compare(null, prop.getValue(), bomVersion);
                            if (cmp < 0) {
                                // BOM version is higher
                                if (Boolean.TRUE.equals(removeRedundantOverrides)) {
                                    return null;
                                }
                                return (Statement) prop.withValue(bomVersion).getTree();
                            }
                        }
                    }
                }
                return super.visitStatement(statement, ctx);
            }
        });
    }
}
