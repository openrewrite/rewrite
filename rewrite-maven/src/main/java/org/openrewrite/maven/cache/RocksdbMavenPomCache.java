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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.MavenRepository;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

/**
 * Implementation of the maven cache that leverages Rocksdb. The keys and values are serialized to/from byte arrays
 * using jackson.
 */
public class RocksdbMavenPomCache implements MavenPomCache {

    static ObjectMapper mapper;

    static {
        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
        ObjectMapper m = new ObjectMapper(f)
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper = m.setVisibility(m.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY));

        //Init the rockdb native jni library
        RocksDB.loadLibrary();
    }

    //While the RocksDB instance is thread-safe, attempts to create two instances of the database to the same
    //folder will fail.
    private static final Map<String, RocksDB> cacheMap = new HashMap<>();
    static synchronized RocksDB getCache(String workspace) {
        return cacheMap.computeIfAbsent(workspace, k -> {
            final Options options = new Options();
            options.setCreateIfMissing(true);
            try {
                return RocksDB.open(options, k);
            } catch (RocksDBException exception) {
                throw new IllegalStateException(("Unable to create cache database." + exception.getMessage()));
            }
        });
    }


    private final RocksDB cache;
    private final Set<String> unresolvablePoms = new HashSet<>();

    CacheResult<RawMaven> UNAVAILABLE_POM = new CacheResult<>(CacheResult.State.Unavailable, null);
    CacheResult<MavenMetadata> UNAVAILABLE_METADATA = new CacheResult<>(CacheResult.State.Unavailable, null);
    CacheResult<MavenRepository> UNAVAILABLE_REPOSITORY = new CacheResult<>(CacheResult.State.Unavailable, null);

    public RocksdbMavenPomCache(@Nullable File workspace) {

        assert workspace != null;

        if(!workspace.exists() && !workspace.mkdirs()) {
            throw new IllegalStateException("Unable to find or create maven pom cache at " + workspace);
        } else if (!workspace.isDirectory()) {
            throw new IllegalStateException("The maven cache workspace must be a directory");
        }
        cache = getCache(workspace.getAbsolutePath());
        fillUnresolvablePoms();
    }

    @Override
    public CacheResult<MavenMetadata> computeMavenMetadata(URI repo, String groupId, String artifactId, Callable<MavenMetadata> orElseGet) throws Exception {
        byte[] key = serialize(new GroupArtifactRepository(repo, new GroupArtifact(groupId, artifactId)));
        Optional<MavenMetadata> rawMavenMetadata = deserializeMavenMetadata(cache.get(key));

        //noinspection OptionalAssignedToNull
        if (rawMavenMetadata == null) {
            //a null is a cache miss.
            try {
                MavenMetadata metadata = orElseGet.call();
                //Note: we store an empty optional in the cache if not resolved.
                cache.put(key, serialize(Optional.ofNullable(metadata)));
                return new CacheResult<>(CacheResult.State.Updated, metadata);
            } catch (Exception e) {
                cache.put(key, serialize(Optional.empty()));
                throw e;
            }
        }

        return rawMavenMetadata
                .map(metadata -> new CacheResult<>(CacheResult.State.Cached, metadata))
                .orElse(UNAVAILABLE_METADATA);
    }

    @Override
    public CacheResult<RawMaven> computeMaven(URI repo, String groupId, String artifactId, String version, Callable<RawMaven> orElseGet) throws Exception {

        //There are a few exceptional artifacts that will never be resolved by the repositories. This will always
        //result in an Unavailable response from the cache.
        String artifactCoordinates = groupId + ':' + artifactId + ':' + version;
        if (unresolvablePoms.contains(artifactCoordinates)) {
            return UNAVAILABLE_POM;
        }

        byte[] key = serialize(repo.toString() + ":" + artifactCoordinates);
        Optional<RawMaven> rawMavenEntry = deserializeRawMaven(cache.get(key));

        //noinspection OptionalAssignedToNull
        if (rawMavenEntry == null) {
            //a null is a cache miss
            try {
                RawMaven rawMaven = orElseGet.call();
                //Note: we store an empty optional in the cache if not resolved.
                cache.put(key, serialize(Optional.ofNullable(rawMaven)));
                return new CacheResult<>(CacheResult.State.Updated, rawMaven);
            } catch (Exception e) {
                cache.put(key, serialize(Optional.empty()));
                throw e;
            }
        }

        return rawMavenEntry
                .map(rawMaven -> new CacheResult<>(CacheResult.State.Cached, rawMaven))
                .orElse(UNAVAILABLE_POM);
    }

    @Override
    public CacheResult<MavenRepository> computeRepository(MavenRepository repository, Callable<MavenRepository> orElseGet) throws Exception {
        byte[] key = serialize(repository);
        Optional<MavenRepository> cacheEntry = deserializeMavenRepository(cache.get(key));

        //noinspection OptionalAssignedToNull
        if (cacheEntry == null) {
            //a null is a cache miss
            try {
                MavenRepository mavenRepository = orElseGet.call();
                //Note: we store an empty optional in the cahce if not resolved
                cache.put(key, serialize(Optional.ofNullable(mavenRepository)));
                return new CacheResult<>(CacheResult.State.Updated, mavenRepository);
            } catch (Exception e) {
                cache.put(key, serialize(Optional.empty()));
                throw e;
            }
        }

        return cacheEntry
                .map(mavenRepository -> new CacheResult<>(CacheResult.State.Cached, mavenRepository))
                .orElse(UNAVAILABLE_REPOSITORY);
    }

    @Override
    public void close() {
        cache.close();
    }

    private void fillUnresolvablePoms() {
        new BufferedReader(new InputStreamReader(MavenPomDownloader.class.getResourceAsStream("/unresolvable.txt"), StandardCharsets.UTF_8))
                .lines()
                .filter(line -> !line.isEmpty())
                .forEach(unresolvablePoms::add);
    }

    static <T> byte[] serialize(T object) {
        if (object == null) {
            return null;
        }
        try {
            return mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize object to byte array.");
        }
    }

    // Note: these methods intentionally return a null optional, which is used as a "cache miss".
    @SuppressWarnings("OptionalAssignedToNull")
    static Optional<MavenRepository> deserializeMavenRepository(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return mapper.readValue(bytes, new TypeReference<Optional<MavenRepository>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to deserialize object to byte array.");
        } catch (IOException e) {
            throw new IllegalArgumentException("IO exception while deserializing object to byte array.");
        }
    }

    @SuppressWarnings("OptionalAssignedToNull")
    static Optional<RawMaven> deserializeRawMaven(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return mapper.readValue(bytes, new TypeReference<Optional<RawMaven>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to deserialize object to byte array.");
        } catch (IOException e) {
            throw new IllegalArgumentException("IO exception while deserializing object to byte array.");
        }
    }

    @SuppressWarnings("OptionalAssignedToNull")
    static Optional<MavenMetadata> deserializeMavenMetadata(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return mapper.readValue(bytes, new TypeReference<Optional<MavenMetadata>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to deserialize object to byte array.");
        } catch (IOException e) {
            throw new IllegalArgumentException("IO exception while deserializing object to byte array.");
        }
    }
}
