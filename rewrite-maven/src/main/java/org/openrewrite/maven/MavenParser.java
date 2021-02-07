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

import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.internal.RawMavenResolver;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Modules;
import org.openrewrite.maven.tree.Pom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.*;
import static java.util.stream.StreamSupport.stream;

public class MavenParser implements Parser<Maven> {
    private static final Logger logger = LoggerFactory.getLogger(MavenParser.class);

    private final MavenPomCache mavenPomCache;
    private final Collection<String> activeProfiles;

    @Nullable
    private final MavenSettings mavenSettings;

    private final boolean resolveOptional;
    private final Consumer<Throwable> onError;

    /**
     * @param mavenPomCache   The cache to be used to speed up dependency resolution
     * @param activeProfiles  The maven profile names set to be active. Profiles are typically defined in the settings.xml
     * @param mavenSettings   The parsed settings.xml file to retrieve profiles, credentials, mirrors, etc. from
     * @param resolveOptional When set to 'true' resolve dependencies marked as optional
     * @param onError         An optional, user supplied error handler. If a supplied onError handler does not re-throw exceptions they will be suppressed and execution will continue where possible.
     */
    private MavenParser(MavenPomCache mavenPomCache, Collection<String> activeProfiles,
                        @Nullable MavenSettings mavenSettings, boolean resolveOptional, Consumer<Throwable> onError) {
        this.mavenPomCache = mavenPomCache;
        this.activeProfiles = activeProfiles;
        this.mavenSettings = mavenSettings;
        this.resolveOptional = resolveOptional;
        this.onError = onError;
    }

    @Override
    public List<Maven> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo) {
        Collection<RawMaven> projectPoms = stream(sources.spliterator(), false)
                .map(source -> RawMaven.parse(source, relativeTo, null))
                .collect(toList());

        MavenPomDownloader downloader = new MavenPomDownloader(mavenPomCache,
                projectPoms.stream().collect(toMap(RawMaven::getSourcePath, Function.identity())),
                mavenSettings, onError);

        List<Maven> parsed = projectPoms.stream()
                .map(raw -> new RawMavenResolver(downloader, activeProfiles,
                        mavenSettings, resolveOptional, onError).resolve(raw))
                .filter(Objects::nonNull)
                .map(xmlDoc -> new Maven(xmlDoc, mavenSettings))
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
                parsed.set(i, maven.withMarkers(maven.getMarkers().compute(new Modules(modules), (old, n) -> n)));
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
        private boolean resolveOptional = true;

        @Nullable
        private MavenSettings mavenSettings;

        private Consumer<Throwable> onError = t -> {};

        public Builder resolveOptional(@Nullable Boolean optional) {
            this.resolveOptional = optional == null || optional;
            return this;
        }

        public Builder activeProfiles(@Nullable String... profiles) {
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

        public Builder mavenSettings(Parser.Input source) {
            this.mavenSettings = MavenSettings.parse(source);
            return this;
        }

        public Builder pomCache(MavenPomCache cache) {
            this.mavenPomCache = cache;
            return this;
        }

        public Builder doOnError(@Nullable Consumer<Throwable> onError) {
            this.onError = onError == null ? t -> {} : onError;
            return this;
        }

        public MavenParser build() {
            return new MavenParser(mavenPomCache, activeProfiles, mavenSettings, resolveOptional, onError);
        }
    }
}
