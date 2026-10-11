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
import org.openrewrite.*;
import org.openrewrite.binary.Binary;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.quark.Quark;
import org.openrewrite.remote.Remote;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextParser;
import org.openrewrite.text.PlainTextVisitor;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveUnusedProperties extends ScanningRecipe<RemoveUnusedProperties.Accumulator> {
    @Option(displayName = "Property pattern",
            description = "A pattern to filter properties to remove. Defaults to `.+?` to match anything",
            required = false,
            example = ".+\\.version")
    @Nullable
    String propertyPattern;

    String displayName = "Remove unused properties";

    String description = "Detect and remove Maven property declarations which do not have any usage within the project.";

    private static final Pattern DOLLAR_PROPERTY_USAGE = Pattern.compile("(?<![$\\\\])\\$\\{([^${}\\s]+)}");
    private static final Pattern AT_PROPERTY_USAGE = Pattern.compile("@([^@\\s]+)@");

    public static class Accumulator {
        public Map<String, Set<MavenResolutionResult>> propertiesToUsingPoms = new HashMap<>();
        public Map<Path, MavenResolutionResult> filteredResourcePathsToDeclaringPoms = new HashMap<>();
        public Map<Path, Set<String>> nonPomPathsToUsages = new HashMap<>();

        public Map<String, Set<MavenResolutionResult>> getFilteredResourceUsages() {
            Map<String, Set<MavenResolutionResult>> result = new HashMap<>();
            filteredResourcePathsToDeclaringPoms.forEach((filteredResourcePath, mrr) ->
                    nonPomPathsToUsages.forEach((usagePath, properties) -> {
                                if (usagePath.startsWith(filteredResourcePath)) {
                                    properties.forEach(property -> {
                                        result.putIfAbsent(property, new HashSet<>());
                                        result.get(property).add(mrr);
                                    });
                                }
                            }
                    ));
            return result;
        }
    }

    @Override
    public RemoveUnusedProperties.Accumulator getInitialValue(ExecutionContext ctx) {
        return new RemoveUnusedProperties.Accumulator();
    }

    private String getPropertyPattern() {
        return propertyPattern != null ? propertyPattern : ".+?";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(RemoveUnusedProperties.Accumulator acc) {
        Pattern propertyMatcher = Pattern.compile(getPropertyPattern());
        MavenIsoVisitor<ExecutionContext> findPomUsagesVisitor = new FindPomUsagesVisitor(propertyMatcher, acc);
        MavenIsoVisitor<ExecutionContext> findFilteredResourcePathsVisitor = new FindFilteredResourcePathsVisitor(acc);
        PlainTextVisitor<ExecutionContext> findResourceUsagesVisitor = new FindResourceUsagesVisitor(propertyMatcher, acc);

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof Quark || tree instanceof Remote || tree instanceof Binary) {
                    return tree;
                }
                if (tree instanceof SourceFile) {
                    SourceFile sf = (SourceFile) tree;
                    if (findPomUsagesVisitor.isAcceptable(sf, ctx)) { // ie: is a pom
                        findPomUsagesVisitor.visit(sf, ctx);
                        findFilteredResourcePathsVisitor.visit(sf, ctx);
                    } else if (!(tree instanceof JavaSourceFile)) { // optimization: avoid visiting code files which are almost always not filtered resources
                        findResourceUsagesVisitor.visit(PlainTextParser.convert(sf), ctx);
                    }
                }
                return tree;
            }
        };
    }

    private static void forEachPropertyUsage(Pattern usagePattern, Pattern propertyMatcher, String text, Consumer<String> action) {
        Matcher matcher = usagePattern.matcher(text);
        while (matcher.find()) {
            String propertyName = matcher.group(1);
            if (propertyMatcher.matcher(propertyName).matches()) {
                action.accept(propertyName);
            }
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(RemoveUnusedProperties.Accumulator acc) {
        Pattern propertyMatcher = Pattern.compile(getPropertyPattern());
        Map<String, Set<MavenResolutionResult>> filteredResourceUsages = acc.getFilteredResourceUsages();
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

                    if (acc.propertiesToUsingPoms.containsKey(propertyName)) {
                        for (MavenResolutionResult pomWhereUsed : acc.propertiesToUsingPoms.get(propertyName)) {
                            if (isAncestor(pomWhereUsed, getResolutionResult().getPom().getGav())) {
                                return t;
                            }
                        }
                    }

                    if (filteredResourceUsages.containsKey(propertyName)) {
                        for (MavenResolutionResult pomWhereUsed : filteredResourceUsages.get(propertyName)) {
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
                        MavenExecutionContextView.view(ctx).effectiveSettings(resolutionResult),
                        resolutionResult.getActiveProfiles());
                try {
                    ResolvedPom resolvedBarePom = resolutionResult.getPom().getRequested()
                            .withProperties(emptyMap())
                            .withDependencies(emptyList())
                            .withDependencyManagement(emptyList())
                            .withPlugins(emptyList())
                            .withPluginManagement(emptyList())
                            .resolve(resolutionResult.getActiveProfiles(), downloader, ctx);
                    return resolvedBarePom.getProperties().containsKey(propertyName);
                } catch (MavenDownloadingException e) {
                    // assume parent *does* have property if error to do no harm
                    return true;
                }
            }
        };
    }

    private static class FindPomUsagesVisitor extends MavenIsoVisitor<ExecutionContext> {
        private final Pattern propertyMatcher;
        private final Accumulator acc;

        public FindPomUsagesVisitor(Pattern propertyMatcher, Accumulator acc) {
            this.propertyMatcher = propertyMatcher;
            this.acc = acc;
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            t.getValue().ifPresent(value -> forEachPropertyUsage(DOLLAR_PROPERTY_USAGE, propertyMatcher, value, propertyName ->
                    acc.propertiesToUsingPoms.computeIfAbsent(propertyName, k -> new HashSet<>()).add(getResolutionResult())));
            return t;
        }
    }

    private static class FindFilteredResourcePathsVisitor extends MavenIsoVisitor<ExecutionContext> {
        private final XPathMatcher resourceMatcher = new XPathMatcher("/project/build/resources/resource");
        private final Accumulator acc;

        public FindFilteredResourcePathsVisitor(Accumulator acc) {
            this.acc = acc;
        }

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (resourceMatcher.matches(getCursor())) {
                String directory = tag.getChildValue("directory").orElse(null);
                if (tag.getChildValue("filtering").map(Boolean::valueOf).orElse(false) &&
                        directory != null) {
                    Path path = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
                    try {
                        acc.filteredResourcePathsToDeclaringPoms.put(path.resolveSibling(directory), getResolutionResult());
                    } catch (InvalidPathException ignored) {
                    } // fail quietly
                }
                return tag;
            } else {
                return super.visitTag(tag, ctx);
            }
        }
    }

    private static class FindResourceUsagesVisitor extends PlainTextVisitor<ExecutionContext> {
        private final Pattern propertyMatcher;
        private final Accumulator acc;

        public FindResourceUsagesVisitor(Pattern propertyMatcher, Accumulator acc) {
            this.propertyMatcher = propertyMatcher;
            this.acc = acc;
        }

        @Override
        public PlainText visitText(PlainText text, ExecutionContext ctx) {
            Consumer<String> addUsage = propertyName ->
                    acc.nonPomPathsToUsages.computeIfAbsent(text.getSourcePath(), k -> new HashSet<>()).add(propertyName);
            forEachPropertyUsage(DOLLAR_PROPERTY_USAGE, propertyMatcher, text.getText(), addUsage);
            forEachPropertyUsage(AT_PROPERTY_USAGE, propertyMatcher, text.getText(), addUsage);
            return text;
        }
    }
}
