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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

public class RefactorPlan {
    private final Map<String, Profile> profilesByName;
    private final Collection<SourceVisitor<?>> visitors;

    public RefactorPlan(Collection<Profile> profiles, Collection<SourceVisitor<?>> visitors) {
        this.profilesByName = profiles.stream().collect(toMap(Profile::getName, identity()));
        this.visitors = visitors;
    }

    public <S extends SourceFile, T extends SourceVisitor<S>> Collection<T> visitors(
            Class<S> sourceType, String... profiles) {
        List<Profile> loadedProfiles = stream(profiles)
                .map(profilesByName::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        //noinspection unchecked
        return visitors.stream()
                .filter(v -> {
                    Type genericSuperclass = v.getClass().getGenericSuperclass();

                    // TODO better way to handle this?
                    if(YamlSourceVisitorLoader.CompositeSourceVisitor.class.equals(v.getClass())) {
                        genericSuperclass = ((YamlSourceVisitorLoader.CompositeSourceVisitor) v).getVisitorType()
                                .getGenericSuperclass();
                    }

                    if (genericSuperclass instanceof ParameterizedType) {
                        Type[] sourceFileType = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
                        return sourceFileType[0].equals(sourceType);
                    }
                    return true;
                })
                .map(v -> (T) v)
                .filter(v -> loadedProfiles.stream().anyMatch(p -> p.accept(v).equals(Profile.FilterReply.ACCEPT)))
                .map(v -> loadedProfiles.stream().reduce(v, (v2, profile) -> profile.configure(v2), (v1, v2) -> v1))
                .collect(Collectors.toList());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, ProfileConfiguration> profileConfigurations = new HashMap<>();
        private final Collection<SourceVisitor<?>> visitors = new ArrayList<>();

        public Builder() {
            visitors.addAll(new AutoConfigureSourceVisitorLoader("org.openrewrite").load());
            visitors.addAll(new ClasspathSourceVisitorLoader().load());
        }

        public Builder scanProfiles() {
            new ClasspathProfileConfigurationLoader().load().forEach(this::loadProfile);
            return this;
        }

        public Builder scanVisitors(String... whitelistVisitorPackages) {
            visitors.addAll(new AutoConfigureSourceVisitorLoader(whitelistVisitorPackages).load());
            return this;
        }

        public Builder loadVisitors(SourceVisitorLoader sourceVisitorLoader) {
            visitors.addAll(sourceVisitorLoader.load());
            return this;
        }

        public Builder visitor(SourceVisitor<?> visitor) {
            this.visitors.add(visitor);
            return this;
        }

        public Builder loadProfiles(ProfileConfigurationLoader profileConfigurationLoader) {
            profileConfigurationLoader.load().forEach(this::loadProfile);
            return this;
        }

        public Builder loadProfile(ProfileConfiguration profileConfiguration) {
            profileConfigurations.compute(profileConfiguration.getName(),
                    (name, existing) -> profileConfiguration.merge(existing));
            return this;
        }

        public RefactorPlan build() {
            return new RefactorPlan(profileConfigurations.values().stream()
                    .map(pc -> pc.build(profileConfigurations.values()))
                    .collect(Collectors.toList()),
                    visitors);
        }
    }
}
