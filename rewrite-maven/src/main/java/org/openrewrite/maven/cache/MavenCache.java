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

import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.internal.RawRepositories;

import java.net.URI;
import java.util.concurrent.Callable;

public interface MavenCache extends AutoCloseable {
    CacheResult<MavenMetadata> computeMavenMetadata(URI repo,
                                                    String groupId,
                                                    String artifactId,
                                                    Callable<MavenMetadata> orElseGet) throws Exception;

    CacheResult<RawMaven> computeMaven(URI repo,
                                       String groupId,
                                       String artifactId,
                                       String version,
                                       Callable<RawMaven> orElseGet) throws Exception;

    /**
     * Store a normalized repository given an input repository. Normalization takes, for example,
     * an http:// scheme repository URL for a repository that now requires https and changes the scheme.
     * @return
     */
    CacheResult<RawRepositories.Repository> computeRepository(RawRepositories.Repository repository,
                                                              Callable<RawRepositories.Repository> orElseGet) throws Exception;

    @Override
    default void close() {
    }
}
