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
package org.openrewrite.maven.utilities;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.openrewrite.internal.StringUtils.matchesGlob;

public class MavenDependencyPropertyUsageOverlap {
    public static Set<String> filterPropertiesWithOverlapInDependencies(
            Set<String> relevantProperties,
            String groupId,
            String artifactId,
            Pom requestedPom,
            @Nullable ResolvedPom resolvedPom,
            boolean configuredToChangeManagedDependency
    ) {
        // resolvedPom being `null` is an indicator of dealing with a remote parent that we can't change
        Set<String> remainingProperties = new HashSet<>(relevantProperties);
        for (ManagedDependency md : requestedPom.getDependencyManagement()) {
            if (remainingProperties.isEmpty()) {
                break;
            }
            if (remainingProperties.contains(md.getVersion()) &&
                    (!configuredToChangeManagedDependency ||
                            resolvedPom == null ||
                            !matchesGlob(resolvedPom.getValue(md.getGroupId()), groupId) ||
                            !matchesGlob(resolvedPom.getValue(md.getArtifactId()), artifactId))
            ) {
                remainingProperties.remove(md.getVersion());
            }
        }
        for (Dependency d : requestedPom.getDependencies()) {
            if (remainingProperties.isEmpty()) {
                break;
            }
            if (remainingProperties.contains(d.getVersion()) &&
                    (resolvedPom == null ||
                            !matchesGlob(resolvedPom.getValue(d.getGroupId()), groupId) ||
                            !matchesGlob(resolvedPom.getValue(d.getArtifactId()), artifactId))
            ) {
                remainingProperties.remove(d.getVersion());
            }
        }
        return remainingProperties;
    }

    public static Set<String> filterPropertiesWithOverlapInChildren(
            Set<String> relevantProperties,
            String groupId,
            String artifactId,
            MavenResolutionResult result,
            boolean configuredToChangeManagedDependency
    ) {
        Set<String> remainingProperties = new HashSet<>(relevantProperties);
        for (MavenResolutionResult child : result.getModules()) {
            if (remainingProperties.isEmpty()) {
                return remainingProperties;
            }
            ResolvedPom childResolvedPom = child.getPom();
            Pom childRequestedPom = childResolvedPom.getRequested();
            remainingProperties = remainingProperties.stream()
                    .filter(p -> !childRequestedPom.getProperties().containsKey(p.substring(2, p.length() - 1)))
                    .collect(toSet());
            remainingProperties = filterPropertiesWithOverlapInDependencies(remainingProperties, groupId, artifactId, childRequestedPom, childResolvedPom, configuredToChangeManagedDependency);
        }
        return remainingProperties;
    }

    public static Set<String> filterPropertiesWithOverlapInParents(
            Set<String> relevantProperties,
            String groupId,
            String artifactId,
            MavenResolutionResult result,
            boolean configuredToChangeManagedDependency,
            ExecutionContext ctx
    ) {
        Set<String> remainingProperties = new HashSet<>(relevantProperties);
        MavenResolutionResult current = result;
        while (current.parentPomIsProjectPom()) {
            if (remainingProperties.isEmpty()) {
                return remainingProperties;
            }
            current = requireNonNull(current.getParent());
            ResolvedPom currentResolved = current.getPom();
            remainingProperties = filterPropertiesWithOverlapInDependencies(remainingProperties, groupId, artifactId, currentResolved.getRequested(), currentResolved, configuredToChangeManagedDependency);
        }
        MavenPomDownloader downloader = new MavenPomDownloader(current.getProjectPoms(), ctx);
        ResolvedPom currentResolved = current.getPom();
        while (currentResolved.getRequested().getParent() != null) {
            if (remainingProperties.isEmpty()) {
                return remainingProperties;
            }
            try {
                Parent remoteParent = currentResolved.getRequested().getParent();
                Pom downloadedParent = downloader.download(
                        remoteParent.getGav(),
                        remoteParent.getRelativePath(),
                        currentResolved,
                        currentResolved.getRepositories()
                );
                remainingProperties = remainingProperties.stream()
                        .filter(p -> !downloadedParent.getProperties().containsKey(p.substring(2, p.length() - 1)))
                        .collect(toSet());
                remainingProperties = filterPropertiesWithOverlapInDependencies(remainingProperties, groupId, artifactId, downloadedParent, null, configuredToChangeManagedDependency);
                currentResolved = downloadedParent.resolve(Collections.emptyList(), downloader, ctx);
            } catch (MavenDownloadingException e) {
                // Give up
                return remainingProperties;
            }
        }
        return remainingProperties;
    }
}
