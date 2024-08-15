/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.maven.tree;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * In case multiple profiles are active in the same POM or external file, the ones which are defined
 * later <a href="https://maven.apache.org/guides/introduction/introduction-to-profiles.html#profile-order">take precedence</a>
 * over the ones defined earlier (independent of their profile id and activation order).
 */
@Value
@With
public class Profile {
    @Nullable
    String id;

    @Nullable
    ProfileActivation activation;

    Map<String, String> properties;
    List<Dependency> dependencies;
    List<ManagedDependency> dependencyManagement;
    List<MavenRepository> repositories;

    List<Plugin> plugins;
    List<Plugin> pluginManagement;

    public boolean isActive(Iterable<String> activeProfiles) {
        return ProfileActivation.isActive(id, activeProfiles, activation);
    }
}
