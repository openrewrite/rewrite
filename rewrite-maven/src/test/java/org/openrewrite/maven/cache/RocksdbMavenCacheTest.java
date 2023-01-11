package org.openrewrite.maven.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.Pom;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class RocksdbMavenCacheTest {

    @Test
    void invalidateCacheOnModelChange(@TempDir Path tempDir) throws Exception {

        String pathString = tempDir.resolve(".rewrite-cache").toString();

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
    }


    @Test
    void entryPersistedInExistingDatabase(@TempDir Path tempDir) throws Exception {

        String pathString = tempDir.resolve(".rewrite-cache").toString();

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

        //Re-open, the versions do not match, expect the database to be purged and no entry should be present
        mavenCache = new RocksdbMavenPomCache(tempDir);

        Optional<Pom> cached = mavenCache.getPom(pom.getGav());
        assertThat(cached).isPresent();
        assertThat(cached.get().getGav()).isEqualTo(pom.getGav());
    }

    private Pom parsePomXml(String pom) {
        return RawPom.parse(new ByteArrayInputStream(pom.getBytes()), null).toPom(null, null);
    }
}
