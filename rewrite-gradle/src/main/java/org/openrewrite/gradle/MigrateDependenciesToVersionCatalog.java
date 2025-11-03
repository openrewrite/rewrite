/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.trait.GradleDependency;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.OmitParentheses;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.internal.ListUtils.mapFirst;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateDependenciesToVersionCatalog extends ScanningRecipe<MigrateDependenciesToVersionCatalog.DependencyAccumulator> {

    private static final MethodMatcher DEPENDENCY_DSL_MATCHER = new MethodMatcher("DependencyHandlerSpec *(..)");
    private static final MethodMatcher PROJECT_MATCHER = new MethodMatcher("DependencyHandlerSpec project(..)");
    private static final Pattern DEPENDENCY_STRING_PATTERN = Pattern.compile("([^:]+):([^:]+):([^:@]+)(@.+)?");
    private static final Pattern DEPENDENCY_MAP_PATTERN = Pattern.compile("group:\\s*['\"]([^'\"]+)['\"],\\s*name:\\s*['\"]([^'\"]+)['\"],\\s*version:\\s*['\"]([^'\"]+)['\"]");
    private static final String CATALOG_PATH = "gradle/libs.versions.toml";
    private static final String GRADLE_PROPERTIES = "gradle.properties";

    @Override
    public String getDisplayName() {
        return "Migrate Gradle project dependencies to version catalog";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Migrates Gradle project dependencies to use the [version catalog](https://docs.gradle.org/current/userguide/platforms.html) feature. " +
                "Supports migrating dependency declarations of various forms:\n" +
                " * `String` notation: `\"group:artifact:version\"`\n" +
                " * `Map` notation: `group: 'group', name: 'artifact', version: 'version'`\n" +
                " * Property references: `\"group:artifact:$version\"` or `\"group:artifact:${version}\"`\n\n" +
                "The recipe will:\n" +
                " * Create a `gradle/libs.versions.toml` file with version declarations\n" +
                " * Replace dependency declarations with catalog references (e.g., `libs.springCore`)\n" +
                " * Migrate version properties from `gradle.properties` to the version catalog\n" +
                " * Preserve project dependencies unchanged\n\n" +
                "**Note:** If a version catalog already exists, the recipe will not modify it.";
    }

    static class DependencyAccumulator {
        final Map<String, DependencyInfo> dependencies = new LinkedHashMap<>();
        final Set<String> configurations = new LinkedHashSet<>();
        final Set<String> propertyNamesToRemove = new LinkedHashSet<>();
        final Map<String, String> propertyValues = new LinkedHashMap<>();
        boolean catalogExists = false;
    }

    static class DependencyCoordinates {
        String group;
        String artifact;
        String version;

        boolean isComplete() {
            return group != null && artifact != null && version != null;
        }
    }

    static class DependencyInfo {
        final GroupArtifactVersion gav;
        final String configuration;

        DependencyInfo(String group, String artifact, String version, String configuration) {
            this.gav = new GroupArtifactVersion(group, artifact, version);
            this.configuration = configuration;
        }

        String getTomlAliasName() {
            // Keep hyphenated names for TOML file
            return gav.getArtifactId().toLowerCase()
                    .replaceAll("[^a-z0-9-]", "-")
                    .replaceAll("-+", "-")
                    .replaceAll("^-|-$", "");
        }

        String getAliasName() {
            // Convert to camelCase for use in build.gradle
            String cleaned = getTomlAliasName();
            String[] parts = cleaned.split("-");
            StringBuilder result = new StringBuilder(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                if (parts[i].length() > 0) {
                    result.append(Character.toUpperCase(parts[i].charAt(0)));
                    if (parts[i].length() > 1) {
                        result.append(parts[i].substring(1));
                    }
                }
            }
            return result.toString();
        }
    }

    @Override
    public DependencyAccumulator getInitialValue(ExecutionContext ctx) {
        return new DependencyAccumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(DependencyAccumulator acc) {
        return Preconditions.check(
            Preconditions.or(
                new IsBuildGradle<>(),
                new TreeVisitor<Tree, ExecutionContext>() {
                    @Override
                    public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                        if (tree instanceof SourceFile) {
                            SourceFile sourceFile = (SourceFile) tree;
                            String path = sourceFile.getSourcePath().toString();
                            if (path.endsWith(GRADLE_PROPERTIES) || path.endsWith(CATALOG_PATH)) {
                                return SearchResult.found(tree);
                            }
                        }
                        return tree;
                    }
                }
            ),
            new TreeVisitor<Tree, ExecutionContext>() {
                @Override
                public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                    if (!(tree instanceof SourceFile)) {
                        return tree;
                    }

                    SourceFile sourceFile = (SourceFile) tree;

                // Check if version catalog already exists
                if (sourceFile.getSourcePath().toString().endsWith(CATALOG_PATH)) {
                    acc.catalogExists = true;
                    return tree;
                }

                // Parse gradle.properties to extract property values
                if (sourceFile.getSourcePath().toString().endsWith(".properties") && sourceFile instanceof Properties.File) {
                    Properties.File propertiesFile = (Properties.File) sourceFile;
                    for (Properties.Content content : propertiesFile.getContent()) {
                        if (content instanceof Properties.Entry) {
                            Properties.Entry entry = (Properties.Entry) content;
                            acc.propertyValues.put(entry.getKey(), entry.getValue().getText());
                        }
                    }
                    return tree;
                }

                // Scan Gradle files for dependencies
                if (sourceFile.getSourcePath().toString().endsWith(".gradle")) {
                    // Extract property references from source text (handles GStrings)
                    String sourceText = sourceFile.printAll();
                    Pattern propRefPattern = Pattern.compile("\\$\\{?([a-zA-Z][a-zA-Z0-9_]*)\\}?");
                    Matcher propMatcher = propRefPattern.matcher(sourceText);
                    while (propMatcher.find()) {
                        String propName = propMatcher.group(1);
                        acc.propertyNamesToRemove.add(propName);
                    }

                    GradleDependency.Matcher matcher = new GradleDependency.Matcher();
                    return matcher.asVisitor((dep, ctx2) -> {
                        if (!PROJECT_MATCHER.matches(dep.getTree())) {
                            String groupId = dep.getDeclaredGroupId();
                            String artifactId = dep.getDeclaredArtifactId();
                            String version = dep.getDeclaredVersion();
                            String configurationName = dep.getConfigurationName();

                            if (groupId != null && artifactId != null && version != null) {
                                acc.configurations.add(configurationName);

                                String versionVariable = dep.getVersionVariable();
                                if (versionVariable != null) {
                                    acc.propertyNamesToRemove.add(versionVariable);
                                }

                                String depKey = groupId + ":" + artifactId + ":" + version;
                                DependencyInfo depInfo = new DependencyInfo(groupId, artifactId, version, configurationName);
                                acc.dependencies.put(depKey, depInfo);
                            }
                        }
                        return dep.getTree();
                    }).visitNonNull(sourceFile, ctx);
                }

                // Scan Kotlin Gradle files for dependencies
                if (sourceFile.getSourcePath().toString().endsWith(".gradle.kts")) {
                    // Extract property references from source text (handles Kotlin string templates)
                    String sourceText = sourceFile.printAll();
                    Pattern propRefPattern = Pattern.compile("\\$\\{?([a-zA-Z][a-zA-Z0-9_]*)\\}?");
                    Matcher propMatcher = propRefPattern.matcher(sourceText);
                    while (propMatcher.find()) {
                        String propName = propMatcher.group(1);
                        acc.propertyNamesToRemove.add(propName);
                    }

                    GradleDependency.Matcher matcher = new GradleDependency.Matcher();
                    return matcher.asVisitor((dep, ctx2) -> {
                        if (!PROJECT_MATCHER.matches(dep.getTree()) && !"project".equals(dep.getTree().getSimpleName())) {
                            String groupId = dep.getDeclaredGroupId();
                            String artifactId = dep.getDeclaredArtifactId();
                            String version = dep.getDeclaredVersion();
                            String configurationName = dep.getConfigurationName();

                            if (groupId != null && artifactId != null && version != null) {
                                acc.configurations.add(configurationName);

                                String versionVariable = dep.getVersionVariable();
                                if (versionVariable != null) {
                                    acc.propertyNamesToRemove.add(versionVariable);
                                }

                                String depKey = groupId + ":" + artifactId + ":" + version;
                                DependencyInfo depInfo = new DependencyInfo(groupId, artifactId, version, configurationName);
                                acc.dependencies.put(depKey, depInfo);
                            }
                        }
                        return dep.getTree();
                    }).visitNonNull(sourceFile, ctx);
                }

                return tree;
            }
        });
    }

    private static String extractStringValue(Expression expr) {
        if (expr instanceof J.Literal) {
            J.Literal literal = (J.Literal) expr;
            if (literal.getValue() instanceof String) {
                return (String) literal.getValue();
            }
        } else if (expr instanceof J.Identifier) {
            return ((J.Identifier) expr).getSimpleName();
        }
        return null;
    }

    private static void extractDependencyCoordinate(String key, String value, DependencyCoordinates coords) {
        if ("group".equals(key)) {
            coords.group = value;
        } else if ("name".equals(key)) {
            coords.artifact = value;
        } else if ("version".equals(key)) {
            coords.version = value;
        }
    }

    @Override
    public Collection<? extends SourceFile> generate(DependencyAccumulator acc, ExecutionContext ctx) {
        // Check if we should skip generation
        if (acc.dependencies.isEmpty() || acc.catalogExists) {
            return emptyList();
        }

        String tomlContent = generateVersionCatalogContent(acc);

        TomlParser parser = TomlParser.builder().build();
        Toml.Document versionCatalog = parser.parse(ctx, tomlContent)
                .findFirst()
                .map(sourceFile -> (Toml.Document) sourceFile)
                .map(doc -> doc.withSourcePath(Paths.get(CATALOG_PATH)))
                .orElseThrow(() -> new IllegalStateException("Failed to create version catalog file"));

        return singletonList(versionCatalog);
    }

    private String generateVersionCatalogContent(DependencyAccumulator acc) {
        StringBuilder sb = new StringBuilder();

        sb.append("[versions]\n");
        Map<String, String> versionRefs = new LinkedHashMap<>();
        Map<String, DependencyInfo> uniqueDeps = new LinkedHashMap<>();

        // Collect unique dependencies preserving order
        for (Map.Entry<String, DependencyInfo> entry : acc.dependencies.entrySet()) {
            DependencyInfo dep = entry.getValue();
            String key = dep.gav.getGroupId() + ":" + dep.gav.getArtifactId();
            if (!uniqueDeps.containsKey(key)) {
                // Resolve version from properties if it's a property reference
                String resolvedVersion = dep.gav.getVersion();
                if (acc.propertyValues.containsKey(dep.gav.getVersion())) {
                    resolvedVersion = acc.propertyValues.get(dep.gav.getVersion());
                }

                // Create a new DependencyInfo with resolved version
                DependencyInfo resolvedDep = new DependencyInfo(dep.gav.getGroupId(), dep.gav.getArtifactId(), resolvedVersion, dep.configuration);
                uniqueDeps.put(key, resolvedDep);
                String versionKey = resolvedDep.getTomlAliasName();
                versionRefs.put(key, versionKey);
                sb.append(versionKey).append(" = \"").append(resolvedVersion).append("\"\n");
            }
        }

        sb.append("\n[libraries]\n");

        // Generate libraries in same order
        for (Map.Entry<String, DependencyInfo> entry : uniqueDeps.entrySet()) {
            DependencyInfo dep = entry.getValue();
            String alias = dep.getTomlAliasName(); // Use hyphenated name in TOML
            String versionRef = versionRefs.get(entry.getKey());

            sb.append(alias).append(" = { ");
            sb.append("group = \"").append(dep.gav.getGroupId()).append("\", ");
            sb.append("name = \"").append(dep.gav.getArtifactId()).append("\", ");
            sb.append("version.ref = \"").append(versionRef).append("\"");
            sb.append(" }\n");
        }

        return sb.toString();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(DependencyAccumulator acc) {
        if (acc.dependencies.isEmpty()) {
            return TreeVisitor.noop();
        }

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile sourceFile = (SourceFile) tree;
                    if (sourceFile.getSourcePath().toString().endsWith(GRADLE_PROPERTIES) && sourceFile instanceof Properties.File) {
                        return new PropertiesFileVisitor(acc).visitNonNull(sourceFile, ctx);
                    } else if (sourceFile.getSourcePath().toString().endsWith(".gradle")) {
                        return new GradleFileVisitor(acc).visitNonNull(sourceFile, ctx);
                    } else if (sourceFile.getSourcePath().toString().endsWith(".gradle.kts")) {
                        return new KotlinGradleFileVisitor(acc).visitNonNull(sourceFile, ctx);
                    }
                }
                return tree;
            }
        };
    }

    private class GradleFileVisitor extends GroovyIsoVisitor<ExecutionContext> {
        private final DependencyAccumulator acc;

        GradleFileVisitor(DependencyAccumulator acc) {
            this.acc = acc;
        }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                // Skip project dependencies
                if (PROJECT_MATCHER.matches(m)) {
                    return m;
                }

                // Check if this is a dependency DSL method call
                if (!DEPENDENCY_DSL_MATCHER.matches(m)) {
                    return m;
                }

                // Process method invocations that have arguments and are in our accumulator
                if (m.getArguments() != null && !m.getArguments().isEmpty()) {
                    String methodName = m.getSimpleName();

                    // Check if this method was identified as a dependency configuration
                    if (acc.configurations.contains(methodName)) {
                        // Check for map notation dependency
                        DependencyCoordinates coords = new DependencyCoordinates();
                        boolean isMapNotation = false;

                        for (Expression arg : m.getArguments()) {
                            if (arg instanceof G.MapEntry) {
                                isMapNotation = true;
                                G.MapEntry entry = (G.MapEntry) arg;
                                String key = extractStringValue(entry.getKey());
                                String value = extractStringValue(entry.getValue());
                                extractDependencyCoordinate(key, value, coords);
                            }
                        }

                        if (isMapNotation && coords.isComplete()) {
                            String depKey = coords.group + ":" + coords.artifact + ":" + coords.version;
                            DependencyInfo dep = acc.dependencies.get(depKey);

                            // If not found by exact version, search by group:artifact only
                            if (dep == null) {
                                for (DependencyInfo candidate : acc.dependencies.values()) {
                                    if (candidate.gav.getGroupId().equals(coords.group) && candidate.gav.getArtifactId().equals(coords.artifact)) {
                                        dep = candidate;
                                        break;
                                    }
                                }
                            }

                            if (dep != null) {
                                // Replace all map entries with a single field access for the catalog with OmitParentheses marker
                                // Preserve the prefix space from the first argument
                                Space prefixSpace = m.getArguments().isEmpty() ? Space.EMPTY : m.getArguments().get(0).getPrefix();

                                J.Identifier libs = new J.Identifier(
                                        Tree.randomId(),
                                        Space.EMPTY,
                                        Markers.EMPTY,
                                        emptyList(),
                                        "libs",
                                        null,
                                        null
                                );

                                J.FieldAccess catalogRef = new J.FieldAccess(
                                        Tree.randomId(),
                                        prefixSpace,  // Preserve the space before the first argument
                                        new Markers(Tree.randomId(), singletonList(new OmitParentheses(Tree.randomId()))),
                                        libs,
                                        new JLeftPadded<>(
                                                Space.EMPTY,
                                                new J.Identifier(
                                                        Tree.randomId(),
                                                        Space.EMPTY,
                                                        Markers.EMPTY,
                                                        emptyList(),
                                                        dep.getAliasName(),
                                                        null,
                                                        null
                                                ),
                                                Markers.EMPTY
                                        ),
                                        null
                                );

                                m = m.withArguments(singletonList(catalogRef));
                            }
                        } else {
                            // Handle regular string dependencies
                            List<Expression> newArgs = new ArrayList<>();
                            boolean changed = false;

                            for (Expression arg : m.getArguments()) {
                                Expression newArg = transformDependencyToVersionCatalog(arg, m, acc);
                                if (newArg != arg) {
                                    changed = true;
                                }
                                newArgs.add(newArg);
                            }

                            if (changed) {
                                m = m.withArguments(newArgs);
                            }
                        }
                    }
                }

                return m;
            }

            private Expression transformDependencyToVersionCatalog(Expression arg, J.MethodInvocation methodInvocation, DependencyAccumulator acc) {
                if (arg instanceof J.Literal) {
                    J.Literal literal = (J.Literal) arg;
                    if (literal.getValue() instanceof String) {
                        String depString = (String) literal.getValue();
                        DependencyInfo dep = acc.dependencies.get(depString);

                        if (dep != null) {
                            boolean hasMultipleArgs = methodInvocation.getArguments().size() > 1;

                            Expression catalogRef = JavaTemplate.builder("libs.#{}")
                                    .build()
                                    .apply(
                                        new Cursor(getCursor(), arg),
                                        arg.getCoordinates().replace(),
                                        dep.getAliasName()
                                    );

                            catalogRef = catalogRef.withPrefix(literal.getPrefix());

                            if (!hasMultipleArgs) {
                                catalogRef = catalogRef.withMarkers(
                                    catalogRef.getMarkers().add(new OmitParentheses(Tree.randomId()))
                                );
                            }

                            return catalogRef;
                        }
                    }
                } else if (arg instanceof G.GString) {
                    // Handle GString dependencies (e.g., "group:artifact:$version")
                    G.GString gstring = (G.GString) arg;
                    List<J> strings = gstring.getStrings();
                    if (strings.size() >= 2 && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                        String firstPart = (String) ((J.Literal) strings.get(0)).getValue();
                        String[] parts = firstPart.split(":", -1);
                        if (parts.length >= 2) {
                            String group = parts[0];
                            String artifact = parts[1];

                            // Look for this dependency in our accumulator by group:artifact
                            for (DependencyInfo dep : acc.dependencies.values()) {
                                if (dep.gav.getGroupId().equals(group) && dep.gav.getArtifactId().equals(artifact)) {
                                    boolean hasMultipleArgs = methodInvocation.getArguments().size() > 1;

                                    Expression catalogRef = JavaTemplate.builder("libs.#{}")
                                            .build()
                                            .apply(
                                                new Cursor(getCursor(), arg),
                                                arg.getCoordinates().replace(),
                                                dep.getAliasName()
                                            );

                                    catalogRef = catalogRef.withPrefix(gstring.getPrefix());

                                    if (!hasMultipleArgs) {
                                        catalogRef = catalogRef.withMarkers(
                                            catalogRef.getMarkers().add(new OmitParentheses(Tree.randomId()))
                                        );
                                    }

                                    return catalogRef;
                                }
                            }
                        }
                    }
                } else if (arg instanceof G.MapLiteral) {
                    // Handle map notation - transform to catalog reference
                    G.MapLiteral map = (G.MapLiteral) arg;
                    DependencyCoordinates coords = new DependencyCoordinates();

                    for (G.MapEntry entry : map.getElements()) {
                        String key = extractStringValue(entry.getKey());
                        String value = extractStringValue(entry.getValue());
                        extractDependencyCoordinate(key, value, coords);
                    }

                    if (coords.isComplete()) {
                        String depKey = coords.group + ":" + coords.artifact + ":" + coords.version;
                        DependencyInfo dep = acc.dependencies.get(depKey);

                        // If not found by exact version, search by group:artifact only
                        if (dep == null) {
                            for (DependencyInfo candidate : acc.dependencies.values()) {
                                if (candidate.gav.getGroupId().equals(coords.group) && candidate.gav.getArtifactId().equals(coords.artifact)) {
                                    dep = candidate;
                                    break;
                                }
                            }
                        }

                        if (dep != null) {
                            // Create a field access for the catalog reference with OmitParentheses marker
                            // Keep the original argument's prefix space for proper formatting
                            // Only omit parentheses if this is the only argument (no closure/lambda following)
                            boolean hasMultipleArgs = methodInvocation.getArguments().size() > 1;

                            J.Identifier libs = new J.Identifier(
                                    Tree.randomId(),
                                    Space.EMPTY,
                                    Markers.EMPTY,
                                    emptyList(),
                                    "libs",
                                    null,
                                    null
                            );

                            Markers markers = hasMultipleArgs ?
                                    Markers.EMPTY :
                                    new Markers(Tree.randomId(), singletonList(new OmitParentheses(Tree.randomId())));

                            return new J.FieldAccess(
                                    Tree.randomId(),
                                    map.getPrefix(),  // Preserve the space before the argument
                                    markers,
                                    libs,
                                    new JLeftPadded<>(
                                            Space.EMPTY,
                                            new J.Identifier(
                                                    Tree.randomId(),
                                                    Space.EMPTY,
                                                    Markers.EMPTY,
                                                    emptyList(),
                                                    dep.getAliasName(),
                                                    null,
                                                    null
                                            ),
                                            Markers.EMPTY
                                    ),
                                    null
                            );
                        }
                    }
                }

                return arg;
            }
    }

    private class KotlinGradleFileVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final DependencyAccumulator acc;
        private boolean removedPropertyDelegations = false;

        KotlinGradleFileVisitor(DependencyAccumulator acc) {
            this.acc = acc;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
            J.VariableDeclarations vd = super.visitVariableDeclarations(multiVariable, ctx);

            // Check if this is a Kotlin property delegation (val propertyName: String by project)
            // The property name should be in our list of properties to remove
            for (J.VariableDeclarations.NamedVariable variable : vd.getVariables()) {
                String propertyName = variable.getSimpleName();
                if (acc.propertyNamesToRemove.contains(propertyName)) {
                    // Check if this looks like a property delegation by checking the initializer
                    // For Kotlin property delegations, the initializer is typically an identifier (e.g., "project")
                    if (variable.getInitializer() instanceof J.Identifier) {
                        J.Identifier init = (J.Identifier) variable.getInitializer();
                        // If it's "project", this is likely a "by project" delegation
                        if ("project".equals(init.getSimpleName())) {
                            // Mark that we removed a property delegation
                            removedPropertyDelegations = true;
                            // Return null to remove this variable declaration
                            return null;
                        }
                    }
                }
            }

            return vd;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

            // Clean up excessive whitespace if property delegations were removed above this statement
            // When property delegations are removed, they leave behind their trailing newlines
            if (removedPropertyDelegations) {
                Space prefix = m.getPrefix();
                String whitespace = prefix.getWhitespace();

                // Count leading newlines
                int newlineCount = 0;
                int lastNewlineIndex = -1;
                for (int i = 0; i < whitespace.length(); i++) {
                    if (whitespace.charAt(i) == '\n') {
                        newlineCount++;
                        lastNewlineIndex = i;
                    } else if (whitespace.charAt(i) != '\r') {
                        break;
                    }
                }

                // If we have 2+ newlines, reduce to 1 newline (no blank lines)
                // This ensures consistent formatting after removing property delegations
                if (newlineCount >= 2) {
                    String remainingWhitespace = lastNewlineIndex + 1 < whitespace.length() ? whitespace.substring(lastNewlineIndex + 1) : "";
                    String normalized = "\n" + remainingWhitespace;
                    m = m.withPrefix(prefix.withWhitespace(normalized));
                    // Reset the flag after cleanup
                    removedPropertyDelegations = false;
                }
            }

            // Skip project dependencies
            // Use MethodMatcher if type attribution is available, otherwise check by name
            if (m.getMethodType() != null && PROJECT_MATCHER.matches(m)) {
                return m;
            } else if (m.getMethodType() == null && "project".equals(m.getSimpleName())) {
                // Without type attribution, check method name directly
                return m;
            }

            // Only check DEPENDENCY_DSL_MATCHER if type attribution is available
            if (m.getMethodType() != null && !DEPENDENCY_DSL_MATCHER.matches(m)) {
                return m;
            }

            // Process method invocations that have arguments and are in our accumulator
            if (m.getArguments() != null && !m.getArguments().isEmpty()) {
                String methodName = m.getSimpleName();

                // Check if this method was identified as a dependency configuration
                if (acc.configurations.contains(methodName)) {
                    // Check for map notation dependency (Kotlin named arguments)
                    DependencyCoordinates coords = new DependencyCoordinates();
                    boolean isMapNotation = false;

                    for (Expression arg : m.getArguments()) {
                        if (arg instanceof J.Assignment) {
                            isMapNotation = true;
                            J.Assignment assignment = (J.Assignment) arg;
                            if (assignment.getVariable() instanceof J.Identifier) {
                                String key = ((J.Identifier) assignment.getVariable()).getSimpleName();
                                String value = extractStringValue(assignment.getAssignment());
                                extractDependencyCoordinate(key, value, coords);
                            }
                        }
                    }

                    if (isMapNotation && coords.isComplete()) {
                        String depKey = coords.group + ":" + coords.artifact + ":" + coords.version;
                        DependencyInfo dep = acc.dependencies.get(depKey);

                        // If not found by exact version, search by group:artifact only
                        if (dep == null) {
                            for (DependencyInfo candidate : acc.dependencies.values()) {
                                if (candidate.gav.getGroupId().equals(coords.group) && candidate.gav.getArtifactId().equals(coords.artifact)) {
                                    dep = candidate;
                                    break;
                                }
                            }
                        }

                        if (dep != null) {
                            // Replace all map entries with a single field access for the catalog
                            // Preserve the prefix space from the first argument
                            Space prefixSpace = m.getArguments().isEmpty() ? Space.EMPTY : m.getArguments().get(0).getPrefix();

                            J.Identifier libs = new J.Identifier(
                                    Tree.randomId(),
                                    Space.EMPTY,
                                    Markers.EMPTY,
                                    emptyList(),
                                    "libs",
                                    null,
                                    null
                            );

                            J.FieldAccess catalogRef = new J.FieldAccess(
                                    Tree.randomId(),
                                    prefixSpace,
                                    Markers.EMPTY,
                                    libs,
                                    new JLeftPadded<>(
                                            Space.EMPTY,
                                            new J.Identifier(
                                                    Tree.randomId(),
                                                    Space.EMPTY,
                                                    Markers.EMPTY,
                                                    emptyList(),
                                                    dep.getAliasName(),
                                                    null,
                                                    null
                                            ),
                                            Markers.EMPTY
                                    ),
                                    null
                            );

                            m = m.withArguments(singletonList(catalogRef));
                        }
                    } else {
                        // Handle regular string dependencies
                        List<Expression> newArgs = new ArrayList<>();
                        boolean changed = false;

                        for (Expression arg : m.getArguments()) {
                            Expression newArg = transformDependencyToVersionCatalog(arg, m, acc);
                            if (newArg != arg) {
                                changed = true;
                            }
                            newArgs.add(newArg);
                        }

                        if (changed) {
                            m = m.withArguments(newArgs);
                        }
                    }
                }
            }

            return m;
        }

        private Expression transformDependencyToVersionCatalog(Expression arg, J.MethodInvocation methodInvocation, DependencyAccumulator acc) {
            if (arg instanceof J.Literal) {
                J.Literal literal = (J.Literal) arg;
                if (literal.getValue() instanceof String) {
                    String depString = (String) literal.getValue();
                    DependencyInfo dep = acc.dependencies.get(depString);

                    if (dep != null) {
                        J.Identifier libs = new J.Identifier(
                                Tree.randomId(),
                                Space.EMPTY,
                                Markers.EMPTY,
                                emptyList(),
                                "libs",
                                null,
                                null
                        );

                        return new J.FieldAccess(
                                Tree.randomId(),
                                literal.getPrefix(),
                                Markers.EMPTY,
                                libs,
                                new JLeftPadded<>(
                                        Space.EMPTY,
                                        new J.Identifier(
                                                Tree.randomId(),
                                                Space.EMPTY,
                                                Markers.EMPTY,
                                                emptyList(),
                                                dep.getAliasName(),
                                                null,
                                                null
                                        ),
                                        Markers.EMPTY
                                ),
                                null
                        );
                    }
                }
            } else if (arg instanceof K.StringTemplate) {
                // Handle Kotlin string template dependencies
                K.StringTemplate template = (K.StringTemplate) arg;
                List<J> strings = template.getStrings();
                if (strings.size() >= 2 && strings.get(0) instanceof J.Literal && ((J.Literal) strings.get(0)).getValue() != null) {
                    String firstPart = (String) ((J.Literal) strings.get(0)).getValue();
                    String[] parts = firstPart.split(":", -1);
                    if (parts.length >= 2) {
                        String group = parts[0];
                        String artifact = parts[1];

                        // Look for this dependency in our accumulator by group:artifact
                        for (DependencyInfo dep : acc.dependencies.values()) {
                            if (dep.gav.getGroupId().equals(group) && dep.gav.getArtifactId().equals(artifact)) {
                                J.Identifier libs = new J.Identifier(
                                        Tree.randomId(),
                                        Space.EMPTY,
                                        Markers.EMPTY,
                                        emptyList(),
                                        "libs",
                                        null,
                                        null
                                );

                                return new J.FieldAccess(
                                        Tree.randomId(),
                                        template.getPrefix(),
                                        Markers.EMPTY,
                                        libs,
                                        new JLeftPadded<>(
                                                Space.EMPTY,
                                                new J.Identifier(
                                                        Tree.randomId(),
                                                        Space.EMPTY,
                                                        Markers.EMPTY,
                                                        emptyList(),
                                                        dep.getAliasName(),
                                                        null,
                                                        null
                                                ),
                                                Markers.EMPTY
                                        ),
                                        null
                                );
                            }
                        }
                    }
                }
            }

            return arg;
        }
    }

    private class PropertiesFileVisitor extends PropertiesVisitor<ExecutionContext> {
        private final DependencyAccumulator acc;

        PropertiesFileVisitor(DependencyAccumulator acc) {
            this.acc = acc;
        }

        @Override
        public Properties visitFile(Properties.File file, ExecutionContext ctx) {
            Properties.File f = (Properties.File) super.visitFile(file, ctx);
            if (acc.propertyNamesToRemove.isEmpty()) {
                return f;
            }
            Properties.File mapped = f.withContent(ListUtils.map(f.getContent(), content -> {
                if (content instanceof Properties.Entry) {
                    Properties.Entry entry = (Properties.Entry) content;
                    if (acc.propertyNamesToRemove.contains(entry.getKey())) {
                        return null; // Remove this entry
                    }
                }
                return content;
            }));
            if (f != mapped) {
                return mapped.withContent(mapFirst(mapped.getContent(), c -> (Properties.Content) c.withPrefix("")));
            }
            return mapped;
        }
    }
}
