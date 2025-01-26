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

import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

public class ReadOnlyLocalMavenArtifactCache extends LocalMavenArtifactCache {
    public ReadOnlyLocalMavenArtifactCache(Path cache) {
        super(cache);
    }

    public static ReadOnlyLocalMavenArtifactCache mavenLocal() {
        return new ReadOnlyLocalMavenArtifactCache(
                Paths.get(System.getProperty("user.home"), ".m2", "repository"));
    }

    @Override
    public @Nullable Path getArtifact(ResolvedDependency dependency) {
        return super.getArtifact(dependency);
    }

    @Override
    public @Nullable Path putArtifact(ResolvedDependency dependency, InputStream artifactInputStream, Consumer<Throwable> onError) {
        return null;
    }
}
