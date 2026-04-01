/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.maven.tree;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.internal.ListUtils.concatAll;

/**
 * The minimum information required about a POM to resolve it.
 * The data model here is the cacheable representation of POMs.
 * <p>
 * A POM serves as a {@link ManagedDependency} when
 * it is used as a BOM import.
 */
@Value
@With
@Builder
@AllArgsConstructor
public class Pom {

    private static final List<String> JAR_PACKAGING_TYPES = Arrays.asList("jar", "bundle");

    /**
     * The model version can be used to verify the structure of the serialized object, in cache, is compatible
     * with the current model loaded by the JVM.
     *
     * @return Model version
     */
    public static int getModelVersion() {
        //NOTE: This value should be incremented if there are any model changes to Pom (or one of its referenced types)
        return 2;
    }

    @Nullable
    Path sourcePath;

    @Nullable
    MavenRepository repository;

    @Nullable
    Parent parent;

    ResolvedGroupArtifactVersion gav;

    /**
     * Old Maven Poms had a "pomVersion" field which was replaced by "modelVersion" in newer versions of Maven.
     * When Maven encounters a pom with this field, it refuses to resolve its dependencies.
     * We keep track of this field so that we can match Maven's behavior.
     */
    @Nullable
    String obsoletePomVersion;

    @Nullable
    String name;

    @Nullable
    Prerequisites prerequisites;

    @Nullable
    String packaging;

    @Builder.Default
    Map<String, String> properties = emptyMap();

    @Builder.Default
    List<ManagedDependency> dependencyManagement = emptyList();

    @Builder.Default
    List<Dependency> dependencies = emptyList();

    @Builder.Default
    List<MavenRepository> repositories = emptyList();

    @Builder.Default
    List<MavenRepository> pluginRepositories = emptyList();

    @Builder.Default
    List<License> licenses = emptyList();

    @Builder.Default
    List<Profile> profiles = emptyList();

    @Builder.Default
    List<Plugin> plugins = emptyList();

    @Builder.Default
    List<Plugin> pluginManagement = emptyList();

    @Builder.Default
    @Nullable
    List<String> subprojects = emptyList();

    public String getGroupId() {
        return gav.getGroupId();
    }

    public String getArtifactId() {
        return gav.getArtifactId();
    }

    public String getVersion() {
        return gav.getVersion();
    }

    public @Nullable String getDatedSnapshotVersion() {
        return gav.getDatedSnapshotVersion();
    }

    /**
     * @return the repositories with any property placeholders resolved.
     */
    public List<MavenRepository> getEffectiveRepositories() {
        return Stream.concat(Stream.of(getRepository()), getRepositories().stream())
                .filter(Objects::nonNull)
                .map(r -> {
                    if (r.getUri().startsWith("~")) {
                        r = r.withUri(Paths.get(System.getProperty("user.home") + r.getUri().substring(1))
                                .toUri()
                                .toString());
                    }
                    if (r.getId() != null && ResolvedPom.placeholderHelper.hasPlaceholders(r.getUri())) {
                        r = r.withId(ResolvedPom.placeholderHelper.replacePlaceholders(
                                r.getId(),
                                this.properties::get));
                    }
                    if (ResolvedPom.placeholderHelper.hasPlaceholders(r.getUri())) {
                        r = r.withUri(ResolvedPom.placeholderHelper.replacePlaceholders(
                                r.getUri(),
                                this.properties::get));
                    }
                    return r;
                })
                .distinct()
                .collect(toList());
    }

    /**
     * "[activeByDefault] profile will automatically be active for all builds unless another profile in the same POM is
     * activated using one of the previously described methods. All profiles that are active by default are automatically
     * deactivated when a profile in the POM is activated on the command line or through its activation config."
     *
     * @param explicitActiveProfiles Any profiles explicitly activated on the command line.
     * @return the effective profiles, given the explicitly active profiles, or failing that those active by default.
     */
    List<Profile> effectiveProfiles(Iterable<String> explicitActiveProfiles) {
        List<Profile> pomActivatedProfiles = profiles.stream()
                .filter(p -> p.isActive(explicitActiveProfiles))
                .collect(toList());
        if (!pomActivatedProfiles.isEmpty()) {
            return pomActivatedProfiles;
        }
        return profiles.stream()
                .filter(p -> p.getActivation() != null && Boolean.TRUE.equals(p.getActivation().getActiveByDefault()))
                .collect(toList());
    }

    /**
     * @param downloader A POM downloader to download dependencies and parents.
     * @param ctx        An execution context containing any maven-specific requirements.
     * @return A new instance with dependencies resolved.
     * @throws MavenDownloadingException When problems are encountered downloading dependencies or parents.
     */
    public ResolvedPom resolve(Iterable<String> activeProfiles,
                               MavenPomDownloader downloader,
                               ExecutionContext ctx) throws MavenDownloadingException {
        return resolve(activeProfiles, downloader, emptyList(), ctx);
    }

    public ResolvedPom resolve(Iterable<String> activeProfiles,
                               MavenPomDownloader downloader,
                               List<MavenRepository> initialRepositories,
                               ExecutionContext ctx) throws MavenDownloadingException {
        return new ResolvedPom(
                this,
                activeProfiles,
                properties,
                emptyList(),
                true,
                concatAll(initialRepositories, getEffectiveRepositories()),
                repositories,
                pluginRepositories,
                dependencies,
                plugins,
                pluginManagement,
                subprojects)
                .resolve(ctx, downloader);
    }

    public @Nullable String getValue(@Nullable String value) {
        if (value == null) {
            return null;
        }
        return ResolvedPom.placeholderHelper.replacePlaceholders(value, this.properties::get);
    }

    public boolean hasJarPackaging() {
        return JAR_PACKAGING_TYPES.contains(packaging);
    }
}
