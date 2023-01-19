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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.ConstructorDetector;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.tree.MavenMetadata;
import org.openrewrite.maven.tree.*;
import org.rocksdb.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the maven cache that leverages Rocksdb. The keys and values are serialized to/from byte arrays
 * using jackson. Things to know about this cache implementation:
 * <p>
 * <li> It will create a rocks db in the workspace directory passed to it.</li>
 * <li> If two caches are pointed to the same workspace folder, they will "share" the same underlying rocks database,
 * it is thread-safe.</li>
 * <li> Because multiple caches can share the same database, the close on this cache implementation does nothing.</li>
 * <li> The database is closed via a system shutdown hook registered by this class. Any unexpected process termination
 * is non-fatal, any non-flushed data is lost, but the database will not be corrupted.</li>
 * <li> The database is configured to auto-flush when the in-memory size reaches 1MB.</li>
 * <li> Rocksdb's write ahead log has been disabled because we are using this as a cache and do not need to recover any
 * "lost" data.</li>
 * <li> Rocksdb computes checksums for all of its files, normally it checks those on startup, this has been disabled as
 * well.</li>
 */
@SuppressWarnings("OptionalAssignedToNull")
public class RocksdbMavenPomCache implements MavenPomCache {

    private static final String MODEL_VERSION_KEY = "org.openrewrite.maven.internal.Pom.version";

    static ObjectMapper mapper;

    //The RocksDB instance is thread safe, the first call to create a database for a workspace will open the database
    //subsequent calls will get the same instances back. This cache also registers a shutdown hook to close the
    //databases on shutdown.
    private static final Map<String, RocksCache> cacheMap = new HashMap<>();

    static {
        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.CHECK_SHARED_STRING_VALUES, true);
        ObjectMapper m = JsonMapper.builder(f)
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .build()
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

    static synchronized RocksCache getCache(String pomCacheDir) {
        return cacheMap.computeIfAbsent(pomCacheDir, RocksCache::new);
    }

    static synchronized void closeCache(String pomCacheDir) {
        RocksCache cache = cacheMap.remove(pomCacheDir);
        if (cache != null) {
            cache.close();
        }
    }

    private final RocksCache cache;

    public RocksdbMavenPomCache(Path workspace) {
        File pomCacheDir = new File(workspace.toFile(), ".rewrite-cache");
        if (!pomCacheDir.exists() && !pomCacheDir.mkdirs()) {
            throw new IllegalStateException("Unable to find or create maven pom cache at " + pomCacheDir);
        } else if (!pomCacheDir.isDirectory()) {
            throw new IllegalStateException("The maven pom cache workspace must be a directory at " + pomCacheDir);
        }
        // In case a stale lock file is left over from a previous run that was interrupted
        File lock = new File(pomCacheDir, "LOCK");
        if (lock.exists()) {
            //noinspection ResultOfMethodCallIgnored
            lock.delete();
        }
        cache = getCache(pomCacheDir.getAbsolutePath());
    }

    @Nullable
    @Override
    public ResolvedPom getResolvedDependencyPom(ResolvedGroupArtifactVersion dependency) {
        return null;
    }

    @Override
    public void putResolvedDependencyPom(ResolvedGroupArtifactVersion dependency, ResolvedPom resolved) {
    }

    @Nullable
    @Override
    public Optional<MavenMetadata> getMavenMetadata(URI repo, GroupArtifactVersion gav) {
        //The Maven metadata is not something that should be stored long term, as it will change over time.
        return null;
    }

    @Override
    public void putMavenMetadata(URI repo, GroupArtifactVersion gav, @Nullable MavenMetadata metadata) {
        //The Maven metadata is not something that should be stored long term, as it will change over time.
    }

    @Override
    public Optional<Pom> getPom(ResolvedGroupArtifactVersion gav) throws MavenDownloadingException {
        try {
            return deserializePom(cache.get(serialize(gav.toString().getBytes(StandardCharsets.UTF_8))));
        } catch (RocksDBException e) {
            throw new MavenDownloadingException("Failed to deserialize POM from RocksDB cache", e,
                    new GroupArtifactVersion(gav.getGroupId(), gav.getArtifactId(), gav.getVersion()));
        }
    }

    @Override
    public void putPom(ResolvedGroupArtifactVersion gav, Pom pom) {
        if (pom == null) {
            return;
        }

        try {
            cache.put(serialize(gav.toString().getBytes(StandardCharsets.UTF_8)), serialize(pom));
        } catch (RocksDBException e) {
            throw new IllegalStateException("Failed to save POM into RocksDB cache", e);
        }
    }

    @Override
    @Nullable
    public Optional<MavenRepository> getNormalizedRepository(MavenRepository repository) {
        return null;
    }

    @Override
    public void putNormalizedRepository(MavenRepository repository, MavenRepository normalized) {
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

    @Nullable
    static Optional<Pom> deserializePom(byte[] bytes) {
        try {
            return bytes == null ? null : Optional.of(mapper.readValue(bytes, Pom.class));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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

        private RocksDB database;
        private final Options options;
        private final WriteOptions writeOptions;
        private final String cacheFolder;
        RocksCache(String pomCacheDir) {
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
                database = RocksDB.open(options, pomCacheDir);
                cacheFolder = pomCacheDir;
            } catch (RocksDBException exception) {
                throw new IllegalStateException("Unable to create cache database." + exception.getMessage(), exception);
            }

            try {
                cleanCacheIfCorrupt(pomCacheDir);
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to clear corrupt maven pom cache.", ex);
            }
        }

        private void cleanCacheIfCorrupt(String pomCacheDir) {
            boolean clearCache = false;
            try {
                database.verifyChecksum();
            } catch (RocksDBException ex) {
                clearCache = true;
            }

            if (!clearCache) {

                // Validate the model version used by the cache matches the version of RawMaven
                clearCache = !modelVersionMatches();
            }

            if (clearCache) {
                database.close();
                try {
                    //Delete the contents of the database.
                    RocksDB.destroyDB(pomCacheDir, options);
                } catch (RocksDBException exception) {
                    throw new IllegalStateException("Unable to clear maven pom cache at " + pomCacheDir, exception);
                }
                // Re-open the database anew
                try {
                    database = RocksDB.open(options, pomCacheDir);
                } catch (RocksDBException exception) {
                    throw new IllegalStateException("Unable to clear maven pom cache at " + pomCacheDir, exception);
                }
            }
            // Update the model version key
            updateDatabaseModelVersion(Pom.getModelVersion());
        }

        private boolean modelVersionMatches() {

            // Check if there are any keys in the database, there is no need to check the model version if
            // this is a new database.
            RocksIterator iterator = database.newIterator();
            iterator.seekToFirst();

            try {
                if (iterator.isValid()) {
                    // Check the model version used by the cache against the in-memory version
                    try {
                        byte[] rawVersion = database.get(serialize(MODEL_VERSION_KEY));
                        Integer databaseVersion = rawVersion == null ? null : mapper.readValue(rawVersion, Integer.class);
                        if (databaseVersion == null || Pom.getModelVersion() != databaseVersion) {
                            return false;
                        }
                    } catch (IOException exception) {
                        throw new IllegalStateException("Unable to deserialize database model version.", exception);
                    }
                }
            } catch (RocksDBException rocksException) {
                throw new IllegalStateException("Unable verify database model version.", rocksException);
            }
            return true;
        }

        protected void updateDatabaseModelVersion(Integer newVersion) {
            try {
                put(serialize(MODEL_VERSION_KEY), serialize(newVersion));
            } catch (RocksDBException e) {
                throw new IllegalStateException("Unable to update database model version.");
            }
        }

        private void put(byte[] key, byte[] value) throws RocksDBException {
            database.put(writeOptions, key, value);
        }

        private byte[] get(byte[] key) throws RocksDBException {
            return database.get(key);
        }

        private void close() {
            // This will flush any in-memory memtables to disk and free up resources held
            // by the underlying C++ code. The worse case scenario is that this is not called because the system exits
            // abnormally, in which case, the data in-memory is simply not saved to the cache.
            if (Files.exists(Paths.get(cacheFolder))) {
                //Attempting to close the database if the file has been deleted underneath it prevents the process
                //from exiting.
                try(FlushOptions flushOptions = new FlushOptions()) {
                    database.flush(flushOptions);
                } catch (RocksDBException e) {
                    //Error while flushing the database (before close), this means that any interim changes in memory
                    //will not be persisted. This is non-fatal.
                } finally {
                    database.close();
                }
            }
            writeOptions.close();
            options.close();
        }
    }
}
