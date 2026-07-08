/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.maven.internal.engine;

import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.Artifact;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.ArtifactProperties;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.ArtifactType;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.DefaultArtifact;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.artifact.DefaultArtifactType;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.graph.Dependency;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.graph.Exclusion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Maven-model {@code Dependency} → aether {@code Dependency}, a verbatim port of
 * {@code ArtifactDescriptorReaderDelegate.convert} (type→stereotype, {@code systemPath}→{@code local.path} property,
 * exclusions normalized to {@code g:a:*:*}). Shared by {@link EngineDescriptorReader} (transitive descriptors) and
 * {@link EngineDependencyCollector} (the root effective model's seeds), so both read exactly what Maven's collector
 * would.
 */
final class DependencyConversions {

    private DependencyConversions() {
    }

    static List<Dependency> toAether(List<org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency> deps,
                                     ArtifactTypeRegistry stereotypes) {
        List<Dependency> out = new ArrayList<>(deps.size());
        for (org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency d : deps) {
            out.add(toAether(d, stereotypes));
        }
        return out;
    }

    static List<Dependency> managedOf(Model model, ArtifactTypeRegistry stereotypes) {
        return model.getDependencyManagement() == null ? Collections.emptyList() :
                toAether(model.getDependencyManagement().getDependencies(), stereotypes);
    }

    static Dependency toAether(org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency dependency,
                               ArtifactTypeRegistry stereotypes) {
        ArtifactType stereotype = stereotypes.get(dependency.getType());
        if (stereotype == null) {
            stereotype = new DefaultArtifactType(dependency.getType());
        }
        boolean system = dependency.getSystemPath() != null && !dependency.getSystemPath().isEmpty();
        Map<String, String> props = system ?
                Collections.singletonMap(ArtifactProperties.LOCAL_PATH, dependency.getSystemPath()) : null;
        Artifact artifact = new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
                dependency.getClassifier(), null, dependency.getVersion(), props, stereotype);
        List<Exclusion> exclusions = new ArrayList<>(dependency.getExclusions().size());
        for (org.openrewrite.maven.engine.shaded.org.apache.maven.model.Exclusion e : dependency.getExclusions()) {
            exclusions.add(new Exclusion(e.getGroupId(), e.getArtifactId(), "*", "*"));
        }
        return new Dependency(artifact, dependency.getScope(),
                dependency.getOptional() != null ? dependency.isOptional() : null, exclusions);
    }
}
