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
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.net.URI;

public interface MavenPomCache {
    @Nullable
    CacheResult<MavenMetadata> getMavenMetadata(URI repo, GroupArtifactVersion gav);

    CacheResult<MavenMetadata> putMavenMetadata(URI repo, GroupArtifactVersion gav, MavenMetadata metadata);

    @Nullable
    CacheResult<Pom> getPom(ResolvedGroupArtifactVersion gav);

    CacheResult<Pom> putPom(ResolvedGroupArtifactVersion gav, Pom pom);

    @Nullable
    CacheResult<MavenRepository> getNormalizedRepository(MavenRepository repository);

    CacheResult<MavenRepository> putNormalizedRepository(MavenRepository repository, MavenRepository normalized);
}
