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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.StreamSupport.stream;

public class RefactorPlan {
    private static final Logger logger = LoggerFactory.getLogger(RefactorPlan.class);

    private final Map<String, Profile> profilesByName;
    private final Collection<SourceVisitor<?>> visitors;

    public RefactorPlan(Collection<Profile> profiles, Collection<SourceVisitor<?>> visitors) {
        this.profilesByName = profiles.stream().collect(toMap(Profile::getName, identity()));
        this.visitors = visitors;
    }

    public <T extends Tree, S extends SourceVisitor<T>> S configure(S visitor, String... profiles) {
        return configure(visitor, Arrays.asList(profiles));
    }

    public <T extends Tree, S extends SourceVisitor<T>> S configure(S visitor, Iterable<String> profiles) {
        List<Profile> loadedProfiles = stream(profiles.spliterator(), false)
                .map(profilesByName::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        visitor = loadedProfiles.stream().reduce(visitor, (v2, profile) -> profile.configure(v2), (v1, v2) -> v1);

        return visitor;
    }

    public <T extends Tree, S extends SourceVisitor<T>> Collection<S> visitors(
            Class<T> sourceType, String... profiles) {

        return visitors(sourceType, Arrays.asList(profiles));
    }

    public <T extends Tree, S extends SourceVisitor<T>> Collection<S> visitors(
            Class<T> sourceType, Iterable<String> profiles) {
        List<Profile> loadedProfiles = stream(profiles.spliterator(), false)
                .map(profilesByName::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        //noinspection unchecked
        return visitors.stream()
                .filter(v -> {
                    Type genericSuperclass = v.getClass().getGenericSuperclass();

                    // TODO better way to handle this?
                    if(CompositeRefactorVisitor.class.equals(v.getClass())) {
                        genericSuperclass = ((CompositeRefactorVisitor) v).getVisitorType()
                                .getGenericSuperclass();
                    }

                    if (genericSuperclass instanceof ParameterizedType) {
                        Type[] sourceFileType = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
                        return sourceFileType[0].equals(sourceType);
                    }
                    return true;
                })
                .map(v -> (S) v)
                .map(v -> loadedProfiles.stream().reduce(v, (v2, profile) -> profile.configure(v2), (v1, v2) -> v1))
                .filter(v -> loadedProfiles.stream().anyMatch(p -> p.accept(v).equals(Profile.FilterReply.ACCEPT)))
                .collect(Collectors.toList());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, ProfileConfiguration> profileConfigurations = new HashMap<>();
        private final Collection<SourceVisitor<?>> visitors = new ArrayList<>();
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
            if(userHomeRewriteConfig.exists()) {
                try(FileInputStream is = new FileInputStream(userHomeRewriteConfig)) {
                    YamlResourceLoader resourceLoader = new YamlResourceLoader(is);
                    loadVisitors(resourceLoader);
                    loadProfiles(resourceLoader);
                } catch(IOException e) {
                    logger.warn("Unable to load ~/.rewrite/rewrite.yml.", e);
                }
            }
            return this;
        }

        public Builder scanVisitors(String... whitelistVisitorPackages) {
            visitors.addAll(new AutoConfigureSourceVisitorLoader(whitelistVisitorPackages).loadVisitors());
            return this;
        }

        public Builder loadVisitors(SourceVisitorLoader sourceVisitorLoader) {
            visitors.addAll(sourceVisitorLoader.loadVisitors());
            return this;
        }

        public Builder visitor(SourceVisitor<?> visitor) {
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
            visitors.addAll(new AutoConfigureSourceVisitorLoader("org.openrewrite").loadVisitors());

            return new RefactorPlan(profileConfigurations.values().stream()
                    .map(pc -> pc.build(profileConfigurations.values()))
                    .collect(Collectors.toList()),
                    visitors);
        }
    }
}
