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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

@EqualsAndHashCode
@ToString
public class LocalMavenArtifactCache implements MavenArtifactCache {
    private final Path cache;

    public LocalMavenArtifactCache(Path cache) {
        if (!cache.toFile().exists() && !cache.toFile().mkdirs()) {
            throw new IllegalStateException("Unable to find or create maven artifact cache at " + cache);
        }
        this.cache = cache;
    }

    @Override
    @Nullable
    public Path getArtifact(ResolvedDependency dependency) {
        Path path = dependencyPath(dependency);
        return path.toFile().exists() ? path : null;
    }

    @Override
    @Nullable
    public Path putArtifact(ResolvedDependency dependency, InputStream artifactInputStream, Consumer<Throwable> onError) {
        if (artifactInputStream == null) {
            return null;
        }
        Path path = dependencyPath(dependency);
        try (InputStream is = artifactInputStream;
             OutputStream out = Files.newOutputStream(path)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = is.read(buffer, 0, 1024)) >= 0) {
                out.write(buffer, 0, read);
            }
        } catch (Throwable t) {
            onError.accept(t);
            return null;
        }

        return path;
    }

    private Path dependencyPath(ResolvedDependency dependency) {
        Path resolvedPath = cache.resolve(Paths.get(dependency.getGroupId().replace('.', '/'),
                dependency.getArtifactId(),
                dependency.getVersion()));

        try {
            synchronized (cache) {
                if (!Files.exists(resolvedPath)) {
                    Files.createDirectories(resolvedPath);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return resolvedPath.resolve(dependency.getArtifactId() + "-" +
                                    (dependency.getDatedSnapshotVersion() == null ? dependency.getVersion() : dependency.getDatedSnapshotVersion()) +
                                    (dependency.getRequested().getClassifier() == null ? "" : dependency.getRequested().getClassifier()) +
                                    ".jar");
    }
}
