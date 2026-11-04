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
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.tree.ResolvedDependency;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.function.Consumer;

@EqualsAndHashCode
@ToString
public class LocalMavenArtifactCache implements MavenArtifactCache {
    private final Path cache;

    public LocalMavenArtifactCache(Path cache) {
        if (!cache.toFile().mkdirs() && !cache.toFile().exists()) {
            throw new IllegalStateException("Unable to find or create maven artifact cache at " + cache);
        }
        this.cache = cache;
    }

    @Override
    public @Nullable Path getArtifact(ResolvedDependency dependency) {
        Path path = dependencyPath(dependency);
        return path.toFile().exists() ? path : null;
    }

    @Override
    public @Nullable Path putArtifact(ResolvedDependency dependency, InputStream artifactInputStream, Consumer<Throwable> onError) {
        if (artifactInputStream == null) {
            return null;
        }
        Path path = dependencyPath(dependency);
        // Written beside the artifact and moved into place, so that no reader ever sees part of one
        Path partial = path.resolveSibling(path.getFileName() + "." + UUID.randomUUID() + ".part");
        try (InputStream is = artifactInputStream) {
            Files.copy(is, partial);
            Files.move(partial, path, StandardCopyOption.ATOMIC_MOVE);
        } catch (Throwable t) {
            try {
                Files.deleteIfExists(partial);
            } catch (IOException e) {
                t.addSuppressed(e);
            }
            // Another writer may have put the same jar there first, whole, and on Windows the move fails while that
            // jar is open. A non-dated SNAPSHOT's jar may be an older build, though, so the failure is reported.
            if (!isMutable(dependency) && Files.isRegularFile(path)) {
                return path;
            }
            onError.accept(t);
            return null;
        }

        return path;
    }

    /**
     * Whether a newer build can replace the artifact's jar, as {@link MavenArtifactCache#computeArtifact} assumes of
     * non-dated SNAPSHOTs.
     */
    private static boolean isMutable(ResolvedDependency dependency) {
        String datedSnapshotVersion = dependency.getDatedSnapshotVersion();
        return dependency.getVersion().endsWith("-SNAPSHOT") &&
               (datedSnapshotVersion == null || dependency.getVersion().equals(datedSnapshotVersion));
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
                                    (dependency.getRequested().getClassifier() == null ? "" : "-" + dependency.getRequested().getClassifier()) +
                                    ".jar");
    }
}
