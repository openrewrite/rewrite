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
import org.rocksdb.WriteOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Implementation of the maven cache that leverages Rocksdb. The keys and values are serialized to/from byte arrays
 * using jackson. Things to know about this cache implementation:
 * <P>
 * <li> It will create a rocks db in the workspace directory passed to it.</li>
 * <li> If two caches are pointed to the same workspace folder, they will "share" the same underlying rocks database,
 *      it is thread-safe.</li>
 * <li> Because multiple caches can share the same database, the close on this cache implementation does nothing.</li>
 * <li> The database is closed via a system shutdown hook registered by this class. Any unexpected process termination
 *      is non-fatal, any non-flushed data is lost, but the database will not be corrupted.</li>
 * <li> The database is configured to auto-flush when the in-memory size reaches 1MB.</li>
 * <li> Rocksdb's write ahead log has been disabled because we are using this as a cache and do not need to recover any
 *      "lost" data.</li>
 * <li> Rocksdb computes checksums for all of its files, normally it checks those on startup, this has been disabled as
 *      well.</li>
 */
public class RocksdbMavenPomCache implements MavenPomCache {

    static ObjectMapper mapper;

    //The RocksDB instance is thread safe, the first call to create a database for a workspace will open the database
    //subsequent calls will get the same instances back. This cache also registers a shutdown hook to close the
    //databases on shutdown.
    private static final Map<String, RocksCache> cacheMap = new HashMap<>();

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

        //Register a shutdown hook to close things down on exit.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> cacheMap.values().forEach(RocksCache::close)));
    }

    static synchronized RocksCache getCache(String workspace) {
        return cacheMap.computeIfAbsent(workspace, RocksCache::new);
    }

    private final RocksCache cache;
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
        // In case a stale lock file is left over from a previous run that was interrupted
        File lock = new File(workspace, "LOCK");
        if(lock.exists()) {
            //noinspection ResultOfMethodCallIgnored
            lock.delete();
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
        Optional<RawMaven> rawMavenEntry = null;
        rawMavenEntry = deserializeRawMaven(cache.get(key));

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
        } catch (Exception e) {
            //Treat deserialization errors as a cache miss, this will force rewrite to re-download and re-cache the
            //results.
            return null;
        }
    }

    @SuppressWarnings("OptionalAssignedToNull")
    static Optional<RawMaven> deserializeRawMaven(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return mapper.readValue(bytes, new TypeReference<Optional<RawMaven>>() {});
        } catch (Exception e) {
            //Treat deserialization errors as a cache miss, this will force rewrite to re-download and re-cache the
            //results.
            return null;
        }
    }

    @SuppressWarnings("OptionalAssignedToNull")
    static Optional<MavenMetadata> deserializeMavenMetadata(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            return mapper.readValue(bytes, new TypeReference<Optional<MavenMetadata>>() {});
        } catch (Exception e) {
            //Treat deserialization errors as a cache miss, this will force rewrite to re-download and re-cache the
            //results.
            return null;
        }
    }

    /**
     * Wrapper class around the rocksdb. The database and options are all backed by C++ data structures that
     * must be explicitly closed to ensure proper memory management. Note, if the same database is being used
     * by multiple threads, close should only be executed once all threads are done using the database. This class
     * registers a shutdown hook to close all open databases, so there should be no need to explicitly close the
     * databases.
     */
    static class RocksCache {

        private final RocksDB database;
        private final Options options;
        private final WriteOptions writeOptions;

        RocksCache(String workspace) {
            try {
                options = new Options();
                options.setCreateIfMissing(true);
                //Default memtable buffer size is 64MB, changing this to 1MB because we are only caching pom.xml files
                //When the memtable exceeds 1MB, rocks will write the contents to disk. Note, closing the database
                //also forces a flush to occur.
                options.setWriteBufferSize(1_000_000);
                //since we are only using rocks db as a cache, turning off checksum verification when opening.
                options.setParanoidChecks(false);
                options.setParanoidFileChecks(false);

                //Turn off write ahead log, there is no needs to record the data in both memory and in a log (from which
                //rocks can recover in the case of a system failure).
                writeOptions = new WriteOptions();
                writeOptions.setDisableWAL(true);
                database = RocksDB.open(options, workspace);
            } catch (RocksDBException exception) {
                throw new IllegalStateException("Unable to create cache database." + exception.getMessage(), exception);
            }
        }

        private void put(byte[] key, byte[] value) throws RocksDBException {
            database.put(writeOptions, key, value);
        }

        private byte[] get(byte[] key) throws RocksDBException {
            return database.get(key);
        }
        private void close() {
            //Called by a shutdown hook, this will flush any in-memory memtables to disk and free up resources held
            //by the underlying C++ code. The worse case scenario is that this is not called because the system exits
            //abnormally, in which case, the data in-memory is simply not saved to the cache.
            database.close();
            writeOptions.close();
            options.close();
        }
    }

}
