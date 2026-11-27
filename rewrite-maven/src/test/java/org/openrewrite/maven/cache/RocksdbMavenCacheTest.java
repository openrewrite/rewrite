/*
 * Copyright 2022 the original author or authors.
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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.Scm;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RocksdbMavenCacheTest {

    @Test
    void invalidateCacheOnModelChange(@TempDir Path tempDir) throws Exception {

        String pathString = tempDir.resolve(".rewrite-cache").toString();
        try {
            //Create Cache
            RocksdbMavenPomCache mavenCache = new RocksdbMavenPomCache(tempDir);

            //Add a pom:
            Pom pom = parsePomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.foo</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <name>test</name>
                </project>
                """);

            mavenCache.putPom(pom.getGav(), pom);
            RocksdbMavenPomCache.RocksCache cache = RocksdbMavenPomCache.getCache(pathString);

            // Reset model version to 0, close.
            cache.updateDatabaseModelVersion(0);
            RocksdbMavenPomCache.closeCache(pathString);

            //Re-open, the versions do not match, expect the database to be purged and no entry should be present
            mavenCache = new RocksdbMavenPomCache(tempDir);
            assertThat(mavenCache.getPom(pom.getGav())).isNull();
        } finally {
            RocksdbMavenPomCache.closeCache(pathString);
        }
    }


    @Test
    void scmSurvivesTheCache(@TempDir Path tempDir) throws Exception {
        String pathString = tempDir.resolve(".rewrite-cache").toString();
        try {
            RocksdbMavenPomCache mavenCache = new RocksdbMavenPomCache(tempDir);
            Pom pom = parsePomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.foo</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.3</version>
                    <scm>
                        <connection>scm:git:https://github.com/foo/test.git</connection>
                        <developerConnection>scm:git:git@github.com:foo/test.git</developerConnection>
                        <url>https://github.com/foo/test</url>
                        <tag>v1.0.3</tag>
                    </scm>
                </project>
                """);
            mavenCache.putPom(pom.getGav(), pom);
            RocksdbMavenPomCache.closeCache(pathString);

            //noinspection OptionalAssignedToNull
            assertThat(new RocksdbMavenPomCache(tempDir).getPom(pom.getGav()))
              .isNotNull()
              .get()
              .extracting(Pom::getScm)
              .isEqualTo(new Scm(
                "scm:git:https://github.com/foo/test.git",
                "scm:git:git@github.com:foo/test.git",
                "https://github.com/foo/test",
                "v1.0.3"));
        } finally {
            RocksdbMavenPomCache.closeCache(pathString);
        }
    }

    /**
     * A POM cached before {@code <scm>} was kept has none, and would go on having none for as long as it stayed
     * in the cache, so the model version that guards the cache has to change with the model.
     */
    @Test
    void pomCachedBeforeScmIsDiscarded(@TempDir Path tempDir) throws Exception {
        String pathString = tempDir.resolve(".rewrite-cache").toString();
        try {
            new RocksdbMavenPomCache(tempDir);
            Pom pom = parsePomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.foo</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.4</version>
                </project>
                """);

            RocksdbMavenPomCache.RocksCache cache = RocksdbMavenPomCache.getCache(pathString);
            putRaw(cache,
                    RocksdbMavenPomCache.serialize("org.openrewrite.maven.internal.Pom.version"),
                    RocksdbMavenPomCache.serialize(4));
            putRaw(cache,
                    RocksdbMavenPomCache.serialize(pom.getGav().toString().getBytes(StandardCharsets.UTF_8)),
                    RocksdbMavenPomCache.serialize(pom));
            RocksdbMavenPomCache.closeCache(pathString);

            assertThat(new RocksdbMavenPomCache(tempDir).getPom(pom.getGav())).isNull();
        } finally {
            RocksdbMavenPomCache.closeCache(pathString);
        }
    }

    @Test
    void invalidateCacheWhenReferencedPomTypeSerializationChanges(@TempDir Path tempDir) throws Exception {
        String pathString = tempDir.resolve(".rewrite-cache").toString();

        try {
            new RocksdbMavenPomCache(tempDir);

            Pom pom = parsePomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.foo</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.2</version>
                    <name>test</name>
                </project>
                """);

            ObjectMapper previousPomMapper = RocksdbMavenPomCache.mapper.copy()
                    .addMixIn(GroupArtifactVersion.class, NoObjectId.class)
                    .addMixIn(ResolvedGroupArtifactVersion.class, NoObjectId.class);

            RocksdbMavenPomCache.RocksCache cache = RocksdbMavenPomCache.getCache(pathString);
            putRaw(cache,
                    RocksdbMavenPomCache.serialize("org.openrewrite.maven.internal.Pom.version"),
                    RocksdbMavenPomCache.serialize(2));
            putRaw(cache,
                    RocksdbMavenPomCache.serialize(pom.getGav().toString().getBytes(StandardCharsets.UTF_8)),
                    previousPomMapper.writeValueAsBytes(pom));
            RocksdbMavenPomCache.closeCache(pathString);

            RocksdbMavenPomCache mavenCache = new RocksdbMavenPomCache(tempDir);
            assertThat(mavenCache.getPom(pom.getGav())).isNull();
        } finally {
            RocksdbMavenPomCache.closeCache(pathString);
        }
    }


    @Test
    void entryPersistedInExistingDatabase(@TempDir Path tempDir) throws Exception {

        String pathString = tempDir.resolve(".rewrite-cache").toString();

        try {
            //Create Cache
            RocksdbMavenPomCache mavenCache = new RocksdbMavenPomCache(tempDir);

            //Add a pom:
            Pom pom = parsePomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.foo</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.1</version>
                    <name>test</name>
                </project>
                """);

            mavenCache.putPom(pom.getGav(), pom);
            RocksdbMavenPomCache.closeCache(pathString);

            //Re-open and ensure cached value is correct.
            mavenCache = new RocksdbMavenPomCache(tempDir);

            Optional<Pom> cached = mavenCache.getPom(pom.getGav());
            assertThat(cached).isPresent();
            assertThat(cached.get().getGav()).isEqualTo(pom.getGav());
        } finally {
            RocksdbMavenPomCache.closeCache(pathString);
        }
    }

    private Pom parsePomXml(String pom) {
        return RawPom.parse(new ByteArrayInputStream(pom.getBytes()), null).toPom(null, null);
    }

    private void putRaw(RocksdbMavenPomCache.RocksCache cache, byte[] key, byte[] value) throws Exception {
        Method put = RocksdbMavenPomCache.RocksCache.class.getDeclaredMethod("put", byte[].class, byte[].class);
        put.setAccessible(true);
        put.invoke(cache, key, value);
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.None.class)
    private interface NoObjectId {
    }
}
