/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.cache;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.*;

import java.net.URI;
import java.util.Optional;

public interface MavenPomCache {

    @Nullable
    ResolvedPom getResolvedDependencyPom(ResolvedGroupArtifactVersion dependency);

    void putResolvedDependencyPom(ResolvedGroupArtifactVersion dependency, ResolvedPom resolved);

    @Nullable
    Optional<MavenMetadata> getMavenMetadata(URI repo, GroupArtifactVersion gav);

    void putMavenMetadata(URI repo, GroupArtifactVersion gav, @Nullable MavenMetadata metadata);

    @Nullable
    Optional<Pom> getPom(ResolvedGroupArtifactVersion gav) throws MavenDownloadingException;

    void putPom(ResolvedGroupArtifactVersion gav, @Nullable Pom pom);

    @Nullable
    Optional<MavenRepository> getNormalizedRepository(MavenRepository repository);

    void putNormalizedRepository(MavenRepository repository, MavenRepository normalized);
}
