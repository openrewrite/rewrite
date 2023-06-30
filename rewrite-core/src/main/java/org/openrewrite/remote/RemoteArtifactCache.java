package org.openrewrite.remote;

import org.openrewrite.internal.lang.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public interface RemoteArtifactCache {
    RemoteArtifactCache NOOP = new RemoteArtifactCache() {
        @Override
        public Path get(URI uri) {
            return null;
        }

        @Override
        public @Nullable Path put(URI uri, InputStream is, Consumer<Throwable> onError) {
            try {
                is.close();
            } catch (IOException e) {
                onError.accept(e);
            }
            return null;
        }

        @Override
        public boolean containsKey(URI uri) {
            return false;
        }

        @Override
        public void clear() {
        }
    };

    @Nullable
    Path get(URI uri);

    @Nullable
    Path put(URI uri, InputStream is, Consumer<Throwable> onError);

    boolean containsKey(URI uri);

    void clear();

    @Nullable
    default Path compute(URI uri, Callable<@Nullable InputStream> artifactStream, Consumer<Throwable> onError) {
        Path artifact = get(uri);
        if (artifact == null) {
            try {
                InputStream is = artifactStream.call();
                if (is != null) {
                    artifact = put(uri, is, onError);
                }
            } catch (Exception e) {
                onError.accept(e);
            }
        }
        return artifact;
    }

    default RemoteArtifactCache orElse(RemoteArtifactCache other) {
        RemoteArtifactCache me = this;
        return new RemoteArtifactCache() {
            @Override
            @Nullable
            public Path get(URI uri) {
                Path artifact = me.get(uri);
                return artifact == null ? other.get(uri) : artifact;
            }

            @Override
            @Nullable
            public Path put(URI uri, InputStream is, Consumer<Throwable> onError) {
                Path artifact = me.put(uri, is, onError);
                return artifact == null ? other.put(uri, is, onError) : artifact;
            }

            @Override
            public boolean containsKey(URI uri) {
                return me.containsKey(uri) || other.containsKey(uri);
            }

            @Override
            public void clear() {
                me.clear();
                other.clear();
            }
        };
    }
}
