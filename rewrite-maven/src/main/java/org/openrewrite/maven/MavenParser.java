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
package org.openrewrite.maven;

import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.internal.RawMavenResolver;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.MavenModel;
import org.openrewrite.maven.tree.Modules;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toMap;
import static org.openrewrite.Tree.randomId;

public class MavenParser implements Parser<Maven> {
    private final MavenPomCache mavenPomCache;
    private final Collection<String> activeProfiles;
    private final boolean resolveOptional;

    /**
     * @param mavenPomCache   The cache to be used to speed up dependency resolution
     * @param activeProfiles  The maven profile names set to be active. Profiles are typically defined in the settings.xml
     * @param resolveOptional When set to 'true' resolve dependencies marked as optional
     */
    private MavenParser(MavenPomCache mavenPomCache,
                        Collection<String> activeProfiles,
                        boolean resolveOptional) {
        this.mavenPomCache = mavenPomCache;
        this.activeProfiles = activeProfiles;
        this.resolveOptional = resolveOptional;
    }

    @Override
    public List<Maven> parse(@Language("xml") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    private static class MavenXmlParser extends XmlParser {
        @Override
        public boolean accept(Path path) {
            return super.accept(path) || path.toString().endsWith(".pom");
        }
    }

    @Override
    public List<Maven> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo,
                                   ExecutionContext ctx) {
        Map<RawMaven, Xml.Document> projectPoms = new LinkedHashMap<>();
        for (Input source : sources) {
            projectPoms.put(
                    RawMaven.parse(source, relativeTo, null, ctx),
                    new MavenXmlParser()
                            .parseInputs(singletonList(source), relativeTo, ctx)
                            .iterator().next()
            );
        }

        MavenPomDownloader downloader = new MavenPomDownloader(mavenPomCache,
                projectPoms.keySet().stream().collect(toMap(RawMaven::getSourcePath, Function.identity())), ctx);

        List<Maven> parsed = new ArrayList<>();
        Map<String, String> effectiveProperties = new HashMap<>();
        if (relativeTo != null) {
            effectiveProperties.put("project.basedir", relativeTo.toString());
            effectiveProperties.put("basedir", relativeTo.toString());
        }

        for (Map.Entry<RawMaven, Xml.Document> rawToDoc : projectPoms.entrySet()) {
            RawMaven raw = rawToDoc.getKey().withProjectPom(true);
            MavenModel model = new RawMavenResolver(downloader, activeProfiles, resolveOptional, ctx)
                    .resolve(raw, effectiveProperties);
            if (model != null) {
                parsed.add(new Maven(rawToDoc.getValue().withMarkers(rawToDoc.getValue().getMarkers()
                        .compute(model, (old, n) -> n))));
            }
        }

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
                parsed.set(i, maven.withMarkers(maven.getMarkers().compute(new Modules(randomId(), modules), (old, n) -> n)));
            }
        }

        return parsed;
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().equals("pom.xml") || path.toString().endsWith(".pom");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private MavenPomCache mavenPomCache = new InMemoryMavenPomCache();
        private final Collection<String> activeProfiles = new HashSet<>();
        private boolean resolveOptional = false;

        /**
         * @deprecated When we are confident in optional dependency resolution in general, we can remove this flag and
         * all its effects.
         */
        Builder resolveOptional(@Nullable Boolean optional) {
            this.resolveOptional = optional == null || optional;
            return this;
        }

        public Builder activeProfiles(@Nullable String... profiles) {
            //noinspection ConstantConditions
            if (profiles != null) {
                Collections.addAll(this.activeProfiles, profiles);
            }
            return this;
        }

        public Builder mavenConfig(@Nullable Path mavenConfig) {
            if (mavenConfig != null && mavenConfig.toFile().exists()) {
                try {
                    String mavenConfigText = new String(Files.readAllBytes(mavenConfig));
                    Matcher matcher = Pattern.compile("(?:$|\\s)-P\\s+([^\\s]+)").matcher(mavenConfigText);
                    if (matcher.find()) {
                        String[] profiles = matcher.group(1).split(",");
                        return activeProfiles(profiles);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return this;
        }

        public Builder cache(MavenPomCache cache) {
            this.mavenPomCache = cache;
            return this;
        }

        public MavenParser build() {
            return new MavenParser(mavenPomCache, activeProfiles, resolveOptional);
        }
    }
}
