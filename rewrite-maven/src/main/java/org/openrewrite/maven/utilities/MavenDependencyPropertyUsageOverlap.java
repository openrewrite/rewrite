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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.openrewrite.internal.StringUtils.matchesGlob;

public class MavenDependencyPropertyUsageOverlap {
    private static final Pattern PROPERTY_REFERENCE = Pattern.compile("\\$\\{([^}]+)}");
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
        // Pom fields default to emptyList() via @Builder.Default, but deserialization can leave them null
        for (ManagedDependency md : requestedPom.getDependencyManagement() == null ?
                java.util.Collections.<ManagedDependency>emptyList() : requestedPom.getDependencyManagement()) {
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
        for (Dependency d : requestedPom.getDependencies() == null ?
                java.util.Collections.<Dependency>emptyList() : requestedPom.getDependencies()) {
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
        // A dependency relocation must not repurpose versions also used by the build.
        // Inspect inactive profiles too: their plugins still need the original version
        // when the profile is activated in a later build.
        Map<String, String> propertyValues = new HashMap<>();
        if (resolvedPom != null) {
            propertyValues.putAll(resolvedPom.getProperties());
        }
        if (requestedPom.getProperties() != null) {
            propertyValues.putAll(requestedPom.getProperties());
        }
        filterPropertiesUsedByPlugins(remainingProperties, requestedPom.getPlugins(), propertyValues);
        filterPropertiesUsedByPlugins(remainingProperties, requestedPom.getPluginManagement(), propertyValues);
        if (requestedPom.getProfiles() != null) {
            for (Profile profile : requestedPom.getProfiles()) {
                Map<String, String> profileProperties = new HashMap<>(propertyValues);
                profileProperties.putAll(profile.getProperties());
                filterPropertiesUsedByPlugins(remainingProperties, profile.getPlugins(), profileProperties);
                filterPropertiesUsedByPlugins(remainingProperties, profile.getPluginManagement(), profileProperties);
            }
        }
        return remainingProperties;
    }

    private static void filterPropertiesUsedByPlugins(Set<String> properties, @Nullable List<Plugin> plugins, Map<String, String> propertyValues) {
        if (plugins == null) {
            return;
        }
        for (Plugin plugin : plugins) {
            protectReferencedProperties(properties, plugin.getVersion(), propertyValues, new HashSet<>());
            if (plugin.getDependencies() != null) {
                for (Dependency dependency : plugin.getDependencies()) {
                    protectReferencedProperties(properties, dependency.getVersion(), propertyValues, new HashSet<>());
                }
            }
        }
    }

    private static void protectReferencedProperties(Set<String> properties, @Nullable String value,
                                                    Map<String, String> propertyValues, Set<String> visited) {
        if (value == null) {
            return;
        }
        Matcher matcher = PROPERTY_REFERENCE.matcher(value);
        while (matcher.find()) {
            String name = matcher.group(1);
            properties.remove(matcher.group());
            if (visited.add(name)) {
                protectReferencedProperties(properties, propertyValues.get(name), propertyValues, visited);
            }
        }
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
                currentResolved = downloadedParent.resolve(emptyList(), downloader, ctx);
            } catch (MavenDownloadingException e) {
                // Give up
                return remainingProperties;
            }
        }
        return remainingProperties;
    }
}
