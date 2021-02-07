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

import lombok.Data;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenDownloadingException;
import org.openrewrite.maven.tree.Pom;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

@Data
public class LocalMavenArtifactCache implements MavenArtifactCache {
    private final Path cache;

    @Override
    @Nullable
    public Path getArtifact(Pom.Dependency dependency) {
        Path path = dependencyPath(dependency);
        return path.toFile().exists() ? path : null;
    }

    @Override
    @Nullable
    public Path putArtifact(Pom.Dependency dependency, InputStream artifactInputStream, Consumer<Throwable> onError) {
        Path path = dependencyPath(dependency);
        File folder = path.getParent().toFile();
        if(folder.exists() || folder.mkdirs()) {
            try (InputStream is = artifactInputStream;
                 OutputStream out = Files.newOutputStream(path)) {
                if (is != null) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = is.read(buffer, 0, 1024)) >= 0) {
                        out.write(buffer, 0, read);
                    }
                }
            } catch (Exception e) {
                onError.accept(e);
                return null;
            }
        } else {
            onError.accept(new MavenDownloadingException("Unable to create local folder for artifact"));
            return null;
        }
        return path;
    }

    private Path dependencyPath(Pom.Dependency dependency) {
        return cache.resolve(Paths.get(dependency.getGroupId().replace('.', '/'),
                dependency.getArtifactId(),
                dependency.getVersion(),
                dependency.getArtifactId() + "-" + dependency.getVersion() +
                        (dependency.getClassifier() == null ? "" : dependency.getClassifier()) +
                        ".jar"));
    }
}
