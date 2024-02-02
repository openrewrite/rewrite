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
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

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

    @Nullable
    String name;

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
    List<License> licenses = emptyList();
    @Builder.Default
    List<Profile> profiles = emptyList();
    @Builder.Default
    List<Plugin> plugins = emptyList();
    @Builder.Default
    List<Plugin> pluginManagement = emptyList();

    public String getGroupId() {
        return gav.getGroupId();
    }

    public String getArtifactId() {
        return gav.getArtifactId();
    }

    public String getVersion() {
        return gav.getVersion();
    }

    @Nullable
    public String getDatedSnapshotVersion() {
        return gav.getDatedSnapshotVersion();
    }

    /**
     * @param downloader A POM downloader to download dependencies and parents.
     * @param ctx        An execution context containing any maven-specific requirements.
     * @return A new instance with dependencies resolved.
     * @throws MavenDownloadingException When problems are encountered downloading dependencies or parents.
     */
    public ResolvedPom resolve(Iterable<String> activeProfiles, MavenPomDownloader downloader, ExecutionContext ctx) throws MavenDownloadingException {
        return new ResolvedPom(this, activeProfiles).resolve(ctx, downloader);
    }

    public ResolvedPom resolve(Iterable<String> activeProfiles, MavenPomDownloader downloader, List<MavenRepository> initialRepositories, ExecutionContext ctx) throws MavenDownloadingException {
        return new ResolvedPom(this, activeProfiles, emptyMap(), emptyList(), initialRepositories, emptyList(), emptyList(), emptyList(), emptyList()).resolve(ctx, downloader);
    }

    @Nullable
    public String getValue(@Nullable String value) {
        if (value == null) {
            return null;
        }
        return ResolvedPom.placeholderHelper.replacePlaceholders(value, this.properties::get);
    }

}
