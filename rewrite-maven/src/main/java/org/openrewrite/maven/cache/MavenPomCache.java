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

import lombok.Value;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.tree.MavenRepository;

import java.net.URI;

/**
 * A cache used to store the pom, it's metadata, and normalized maven repositories. A cache implementation can decide
 * to use "time-to-live" semantics on the cache results and should expire keys once they have aged past their TTL.
 */
public interface MavenPomCache extends AutoCloseable {

    CacheResult<MavenMetadata> getMavenMetadata(MetadataKey key);
    CacheResult<MavenMetadata> setMavenMetadata(MetadataKey key, MavenMetadata metadata, boolean isSnapshot);

    CacheResult<RawMaven> getMaven(PomKey key);
    CacheResult<RawMaven> setMaven(PomKey key, RawMaven maven, boolean isSnapshot);

    CacheResult<MavenRepository> getNormalizedRepository(MavenRepository repository);
    CacheResult<MavenRepository> setNormalizedRepository(MavenRepository repository, MavenRepository normalized);

    void clear();

    /**
     * Given a time-to-live value in milliseconds, this method will calculate the time at which a cache entry should
     * be expired. A negative ttl indicates that the entry should never expire.
     *
     * @param ttl Time-to-live in milliseconds.
     * @return The current time in milliseconds plus the TTL value.
     */
    default long calculateExpiration(long ttl) {
        return ttl < 0 ? -1 : System.currentTimeMillis() + ttl;
    }

    /**
     * Checks a cache result's ttl value and if the result is expired, returns null.
     *
     * @param result A cache result with a TTL value.
     * @param <T> The tye of cached data.
     * @return result or null depending on if the cache result has expired.
     */
    @Nullable
    default <T> CacheResult<T> filterExpired(CacheResult<T> result) {
        if (result != null && result.getTtl() > 0 && result.getTtl() < System.currentTimeMillis()) {
            return null;
        }
        return result;
    }

    MavenPomCache NOOP = new MavenPomCache() {

        @Override
        @Nullable
        public CacheResult<MavenMetadata> getMavenMetadata(MetadataKey key) {
            return null;
        }

        @Override
        public CacheResult<MavenMetadata> setMavenMetadata(MetadataKey key, MavenMetadata metadata, boolean isSnapshot) {
            return new CacheResult<>(CacheResult.State.Updated, metadata, -1);
        }

        @Override
        @Nullable
        public CacheResult<RawMaven> getMaven(PomKey key) {
            return null;
        }

        @Override
        public CacheResult<RawMaven> setMaven(PomKey key, RawMaven maven, boolean isSnapshot) {
            return new CacheResult<>(CacheResult.State.Updated, maven, -1);
        }

        @Override
        @Nullable
        public CacheResult<MavenRepository> getNormalizedRepository(MavenRepository repository) {
            return null;
        }

        @Override
        public CacheResult<MavenRepository> setNormalizedRepository(MavenRepository repository, MavenRepository normalized) {
            return new CacheResult<>(CacheResult.State.Updated, normalized, -1);
        }

        @Override
        public void clear() {
        }

        @Override
        public void close() {
        }
    };

    @Value
    class PomKey {
        URI repo;
        String groupId;
        String artifactId;
        String version;
    }

    @Value
    class MetadataKey {
        URI repo;
        String groupId;
        String artifactId;
        @Nullable
        String version;
    }
}
