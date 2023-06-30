package org.openrewrite.remote;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.openrewrite.internal.lang.Nullable;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@EqualsAndHashCode
@ToString
public class LocalRemoteArtifactCache implements RemoteArtifactCache {
    private final Path cacheDir;
    private final Map<URI, Path> cache;

    public LocalRemoteArtifactCache(Path cacheDir) {
        if (!cacheDir.toFile().exists() && !cacheDir.toFile().mkdirs()) {
            throw new IllegalStateException("Unable to find or create remote archive cache at " + cacheDir);
        }
        this.cacheDir = cacheDir;
        this.cache = new HashMap<>();
    }

    @Override
    @Nullable
    public Path get(URI uri) {
        return cache.get(uri);
    }

    @Override
    @Nullable
    public Path put(URI uri, InputStream artifactInputStream, Consumer<Throwable> onError) {
        String artifactUri = uri.toString();
        String artifactName = artifactUri.substring(artifactUri.lastIndexOf("/") + 1);
        try {
            Path artifact = Files.createTempFile(cacheDir, artifactName, ".tmp");
            try (InputStream is = artifactInputStream) {
                Files.copy(is, artifact, StandardCopyOption.REPLACE_EXISTING);
            }
            return artifact;
        } catch (Exception e) {
            onError.accept(e);
            return null;
        }
    }

    @Override
    public boolean containsKey(URI uri) {
        return cache.containsKey(uri);
    }

    @Override
    public void clear() {
        cache.forEach((uri, artifact) -> {
            try {
                Files.deleteIfExists(artifact);
            } catch (Exception ignored) {
            }
        });
        cache.clear();
    }
}
