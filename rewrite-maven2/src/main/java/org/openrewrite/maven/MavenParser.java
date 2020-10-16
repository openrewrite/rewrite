package org.openrewrite.maven;

import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.internal.RawMavenResolver;
import org.openrewrite.maven.internal.RawPomDownloader;
import org.openrewrite.maven.tree.Maven;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

public class MavenParser implements Parser<Maven> {
    private final RawPomDownloader downloader;
    private final boolean resolveOptional;

//    private static final Serializer<RawPom.Parent> PARENT_SERIALIZER = new JacksonMapdbSerializer<>(RawPom.Parent.class);
//    private static final Serializer<Optional<Maven>> OPTIONAL_MAVEN_SERIALIZER = new OptionalJacksonMapdbSerializer<>(Maven.class);
//    private static final Serializer<Pom.ModuleVersionId> MVID_SERIALIZER = new JacksonMapdbSerializer<>(Pom.ModuleVersionId.class);
//    private static final Serializer<Maven> MAVEN_SERIALIZER = new JacksonMapdbSerializer<>(Maven.class);
//
//    private final Map<RawPom.Parent, Optional<Maven>> parentCache;
//    private final Map<Pom.ModuleVersionId, Maven> resolveCache;

    private MavenParser(@Nullable File workspace, @Nullable Long maxCacheStoreSize, boolean resolveOptional) {
        this.downloader = new RawPomDownloader(workspace, maxCacheStoreSize);
        this.resolveOptional = resolveOptional;

//        DB inMemoryDb = DBMaker
//                .heapDB()
//                .make();

//        parentCache = inMemoryDb
//                .hashMap("parent.inmem")
//                .keySerializer(PARENT_SERIALIZER)
//                .valueSerializer(OPTIONAL_MAVEN_SERIALIZER)
//                .create();
//
//        resolveCache = inMemoryDb
//                .hashMap("resolve.inmem")
//                .keySerializer(MVID_SERIALIZER)
//                .valueSerializer(MAVEN_SERIALIZER)
//                .create();
    }

    @Override
    public List<Maven> parseInputs(Iterable<Input> sources, @Nullable URI relativeTo) {
        Collection<RawMaven> projectPoms = StreamSupport.stream(sources.spliterator(), false)
                .map(source -> RawMaven.parse(source, relativeTo))
                .collect(toList());

        RawPomDownloader downloaderWithProjectPoms = downloader.withProjectPoms(projectPoms);

        return projectPoms.stream()
                .map(raw -> new RawMavenResolver(downloaderWithProjectPoms, false, resolveOptional).resolve(raw))
                .collect(toList());
    }

    @Override
    public boolean accept(URI path) {
        return path.toString().equals("pom.xml") || path.toString().endsWith(".pom");
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Parser<Maven> reset() {
//        parentCache.clear();
//        resolveCache.clear();
        return this;
    }

    public static class Builder {
        boolean resolveOptional = true;

        @Nullable
        File workspace;

        @Nullable
        Long maxInMemCacheStoreSize = 2L * 1024 * 1024 * 1024;

        public Builder workspace(@Nullable File workspace) {
            this.workspace = workspace;
            return this;
        }

        public Builder maxInMemCacheStoreSize(@Nullable Long bytes) {
            this.maxInMemCacheStoreSize = bytes;
            return this;
        }

        public Builder resolveOptional(@Nullable Boolean optional) {
            this.resolveOptional = optional == null || optional;
            return this;
        }

        public MavenParser build() {
            return new MavenParser(workspace, maxInMemCacheStoreSize, resolveOptional);
        }
    }
}
