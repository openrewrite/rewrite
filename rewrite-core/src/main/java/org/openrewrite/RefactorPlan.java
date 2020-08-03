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
package org.openrewrite;

import org.openrewrite.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

public class RefactorPlan {
    private static final Logger logger = LoggerFactory.getLogger(RefactorPlan.class);

    private final Map<String, Profile> profilesByName;
    private final Collection<RefactorVisitor<?>> visitors;

    public RefactorPlan(Collection<Profile> profiles, Collection<RefactorVisitor<?>> visitors) {
        this.profilesByName = profiles.stream().collect(toMap(Profile::getName, identity()));
        this.visitors = visitors;
    }

    public <T extends Tree, R extends RefactorVisitor<T>> R configure(R visitor, String... profiles) {
        return configure(visitor, Arrays.asList(profiles));
    }

    public <T extends Tree, R extends RefactorVisitor<T>> R configure(R visitor, Iterable<String> profiles) {
        List<Profile> loadedProfiles = stream(profiles.spliterator(), false)
                .map(profilesByName::get)
                .filter(Objects::nonNull)
                .collect(toList());

        visitor = loadedProfiles.stream().reduce(visitor, (v2, profile) -> profile.configure(v2), (v1, v2) -> v1);

        return visitor;
    }

    public Collection<RefactorVisitor<?>> visitors(String... profiles) {
        return visitors(Arrays.asList(profiles));
    }

    public Collection<RefactorVisitor<?>> visitors(Iterable<String> profiles) {
        List<Profile> loadedProfiles = stream(profiles.spliterator(), false)
                .map(profilesByName::get)
                .filter(Objects::nonNull)
                .collect(toList());

        return visitors.stream()
                .map(v -> loadedProfiles.stream().reduce(v, (v2, profile) -> profile.configure(v2), (v1, v2) -> v1))
                .filter(v -> loadedProfiles.stream().anyMatch(p -> p.accept(v).equals(Profile.FilterReply.ACCEPT)))
                .collect(toList());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, ProfileConfiguration> profileConfigurations = new HashMap<>();
        private final Collection<RefactorVisitor<?>> visitors = new ArrayList<>();
        private Iterable<Path> compileClasspath = emptyList();

        public Builder compileClasspath(Iterable<Path> compileClasspath) {
            this.compileClasspath = emptyList();
            return this;
        }

        public Builder scanResources() {
            ClasspathResourceLoader classpathResourceLoader = new ClasspathResourceLoader(compileClasspath);
            loadVisitors(classpathResourceLoader);
            loadProfiles(classpathResourceLoader);
            return this;
        }

        public Builder scanUserHome() {
            File userHomeRewriteConfig = new File(System.getProperty("user.home") + "/.rewrite/rewrite.yml");
            if (userHomeRewriteConfig.exists()) {
                try (FileInputStream is = new FileInputStream(userHomeRewriteConfig)) {
                    YamlResourceLoader resourceLoader = new YamlResourceLoader(is);
                    loadVisitors(resourceLoader);
                    loadProfiles(resourceLoader);
                } catch (IOException e) {
                    logger.warn("Unable to load ~/.rewrite/rewrite.yml.", e);
                }
            }
            return this;
        }

        public Builder scanVisitors(String... acceptVisitorPackages) {
            visitors.addAll(new AutoConfigureRefactorVisitorLoader(acceptVisitorPackages).loadVisitors());
            return this;
        }

        public Builder loadVisitors(RefactorVisitorLoader refactorVisitorLoader) {
            visitors.addAll(refactorVisitorLoader.loadVisitors());
            return this;
        }

        public Builder loadVisitors(Collection<? extends RefactorVisitor<?>> visitors) {
            this.visitors.addAll(visitors);
            return this;
        }

        public Builder visitor(RefactorVisitor<?> visitor) {
            this.visitors.add(visitor);
            return this;
        }

        public Builder loadProfiles(ProfileConfigurationLoader profileConfigurationLoader) {
            profileConfigurationLoader.loadProfiles().forEach(this::loadProfile);
            return this;
        }

        public Builder loadProfile(ProfileConfiguration profileConfiguration) {
            profileConfigurations.compute(profileConfiguration.getName(),
                    (name, existing) -> profileConfiguration.merge(existing));
            return this;
        }

        public RefactorPlan build() {
            visitors.addAll(new AutoConfigureRefactorVisitorLoader("org.openrewrite").loadVisitors());

            return new RefactorPlan(profileConfigurations.values().stream()
                    .map(pc -> pc.build(profileConfigurations.values()))
                    .collect(toList()),
                    visitors);
        }
    }
}
