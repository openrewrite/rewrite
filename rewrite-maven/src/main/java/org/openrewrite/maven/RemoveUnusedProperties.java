/*
 * Copyright 2024 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveUnusedProperties extends ScanningRecipe<Map<String, Set<MavenResolutionResult>>> {
    @Option(displayName = "Property pattern",
            description = "A pattern to filter properties to remove. Defaults to `.+` to match anything",
            required = false,
            example = ".+\\.version")
    @Nullable
    String propertyPattern;

    @Override
    public String getDisplayName() {
        return "Remove unused properties";
    }

    @Override
    public String getDescription() {
        return "Detect and remove Maven property declarations which do not have any usage within the project.";
    }

    @Override
    public Map<String, Set<MavenResolutionResult>> getInitialValue(ExecutionContext ctx) {
        return new HashMap<>();
    }

    private String getPropertyPattern() {
        return propertyPattern != null ? propertyPattern : ".+";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<String, Set<MavenResolutionResult>> acc) {
        Pattern propertyMatcher = Pattern.compile(getPropertyPattern());
        Pattern propertyUsageMatcher = Pattern.compile(".*\\$\\{(" + propertyMatcher.pattern() + ")}.*");
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                Optional<String> value = t.getValue();
                if (value.isPresent()) {
                    Matcher matcher = propertyUsageMatcher.matcher(value.get());
                    if (matcher.matches()) {
                        acc.putIfAbsent(matcher.group(1), new HashSet<>());
                        acc.get(matcher.group(1)).add(getResolutionResult());
                    }
                }
                return t;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<String, Set<MavenResolutionResult>> acc) {
        Pattern propertyMatcher = Pattern.compile(getPropertyPattern());
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                String propertyName = t.getName();
                if (isPropertyTag() && propertyMatcher.matcher(propertyName).matches()) {
                    if (isMavenBuiltinProperty(propertyName)) {
                        return t;
                    }

                    if (parentHasProperty(getResolutionResult(), propertyName, ctx)) {
                        return t;
                    }

                    if (acc.containsKey(propertyName)) {
                        for (MavenResolutionResult pomWhereUsed : acc.get(propertyName)) {
                            if (isAncestor(pomWhereUsed, getResolutionResult().getPom().getGav())) {
                                return t;
                            }
                        }
                    }

                    doAfterVisit(new RemoveContentVisitor<>(tag, true, true));
                    maybeUpdateModel();
                }
                return t;
            }

            private boolean isMavenBuiltinProperty(String propertyName) {
                return propertyName.startsWith("project.") || propertyName.startsWith("maven.");
            }

            private boolean isAncestor(MavenResolutionResult project, ResolvedGroupArtifactVersion possibleAncestorGav) {
                MavenResolutionResult projectAncestor = project;
                while (projectAncestor != null) {
                    if (projectAncestor.getPom().getGav().equals(possibleAncestorGav)) {
                        return true;
                    }
                    projectAncestor = projectAncestor.getParent();
                }
                return false;
            }

            private boolean parentHasProperty(MavenResolutionResult resolutionResult, String propertyName,
                    ExecutionContext ctx) {
                MavenPomDownloader downloader = new MavenPomDownloader(resolutionResult.getProjectPoms(), ctx,
                        resolutionResult.getMavenSettings(), resolutionResult.getActiveProfiles());
                try {
                    ResolvedPom resolvedBarePom = resolutionResult.getPom().getRequested()
                            .withProperties(Collections.emptyMap())
                            .withDependencies(Collections.emptyList())
                            .withDependencyManagement(Collections.emptyList())
                            .withPlugins(Collections.emptyList())
                            .withPluginManagement(Collections.emptyList())
                            .resolve(resolutionResult.getActiveProfiles(), downloader, ctx);
                    return resolvedBarePom.getProperties().containsKey(propertyName);
                } catch (MavenDownloadingException e) {
                    // assume parent *does* have property if error to do no harm
                    return true;
                }
            }
        };
    }
}
