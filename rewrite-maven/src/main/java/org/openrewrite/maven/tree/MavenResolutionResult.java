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

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.marker.Marker;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
public class MavenResolutionResult implements Marker {
    @EqualsAndHashCode.Include
    @With
    UUID id;

    @With
    ResolvedPom pom;

    /**
     * Resolution results of POMs in this repository that hold this POM as a parent.
     */
    @With
    List<MavenResolutionResult> modules;

    @With
    Map<Scope, List<ResolvedDependency>> dependencies;

    @Incubating(since = "7.18.0")
    @Nullable
    public ResolvedDependency getResolvedDependency(Dependency dependency) {
        for (int i = Scope.values().length - 1; i >= 0; i--) {
            Scope scope = Scope.values()[i];
            if(dependencies.containsKey(scope)) {
                for (ResolvedDependency resolvedDependency : dependencies.get(scope)) {
                    if (resolvedDependency.getRequested() == dependency) {
                        return resolvedDependency;
                    }
                }
            }
        }
        return null;
    }
}
