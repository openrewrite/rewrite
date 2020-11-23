package org.openrewrite.maven;

import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.cache.InMemoryCache;
import org.openrewrite.maven.cache.MavenCache;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.internal.RawMavenResolver;
import org.openrewrite.maven.internal.RawPomDownloader;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Modules;
import org.openrewrite.maven.tree.Pom;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static java.util.stream.StreamSupport.stream;

public class MavenParser implements Parser<Maven> {
    private final MavenCache mavenCache;
    private final boolean resolveOptional;

    private MavenParser(MavenCache mavenCache, boolean resolveOptional) {
        this.mavenCache = mavenCache;
        this.resolveOptional = resolveOptional;
    }

    @Override
    public List<Maven> parseInputs(Iterable<Input> sources, @Nullable URI relativeTo) {
        Collection<RawMaven> projectPoms = stream(sources.spliterator(), false)
                .map(source -> RawMaven.parse(source, relativeTo))
                .collect(toList());

        RawPomDownloader downloader = new RawPomDownloader(mavenCache,
                projectPoms.stream().collect(toMap(RawMaven::getURI, Function.identity())));

        List<Maven> parsed = projectPoms.stream()
                .map(raw -> new RawMavenResolver(downloader, false, resolveOptional).resolve(raw))
                .filter(Objects::nonNull)
                .map(Maven::new)
                .collect(toCollection(ArrayList::new));

        for (int i = 0; i < parsed.size(); i++) {
            Maven maven = parsed.get(i);
            List<Pom> modules = new ArrayList<>(0);
            for (Maven possibleModule : parsed) {
                Pom parent = possibleModule.getModel().getParent();
                if (parent != null &&
                        parent.getGroupId().equals(maven.getModel().getGroupId()) &&
                        parent.getArtifactId().equals(maven.getModel().getArtifactId()) &&
                        parent.getVersion().equals(maven.getModel().getVersion())) {
                    modules.add(possibleModule.getModel());
                }
            }

            if (!modules.isEmpty()) {
                parsed.set(i, maven.setMetadata(new Modules(modules)));
            }
        }

        return parsed;
    }

    @Override
    public boolean accept(URI path) {
        return path.toString().equals("pom.xml") || path.toString().endsWith(".pom");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean resolveOptional = true;
        private MavenCache mavenCache = new InMemoryCache();

        public Builder resolveOptional(@Nullable Boolean optional) {
            this.resolveOptional = optional == null || optional;
            return this;
        }

        public Builder cache(MavenCache cache) {
            this.mavenCache = cache;
            return this;
        }

        public MavenParser build() {
            return new MavenParser(mavenCache, resolveOptional);
        }
    }
}
