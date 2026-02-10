/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.python.marker.PythonResolutionResult;
import org.openrewrite.python.marker.PythonResolutionResult.Dependency;
import org.openrewrite.python.marker.PythonResolutionResult.ResolvedDependency;
import org.openrewrite.python.table.PythonDependenciesInUse;
import org.openrewrite.toml.TomlIsoVisitor;
import org.openrewrite.toml.tree.Toml;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

/**
 * Find direct and transitive Python dependencies matching a package name pattern.
 * Searches pyproject.toml files that have a {@link PythonResolutionResult} marker attached.
 * Search result markers are placed on the dependency string literals in pyproject.toml.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class DependencyInsight extends Recipe {
    transient PythonDependenciesInUse dependenciesInUse = new PythonDependenciesInUse(this);

    @Option(displayName = "Package name pattern",
            description = "A glob pattern to match Python package names (PEP 503 normalized). Use `*` as a wildcard.",
            example = "requests*")
    String packageNamePattern;

    @Option(displayName = "Scope",
            description = "Match dependencies in the specified scope. All scopes are searched by default.",
            valid = {"dependencies", "buildRequires", "optionalDependencies", "dependencyGroups"},
            example = "dependencies",
            required = false)
    @Nullable
    String scope;

    @Option(displayName = "Only direct",
            description = "If enabled, transitive dependencies will not be considered. All dependencies are searched by default.",
            required = false,
            example = "true")
    @Nullable
    Boolean onlyDirect;

    String displayName = "Python dependency insight";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", packageNamePattern);
    }

    String description = "Find direct and transitive Python dependencies matching a package name pattern. " +
            "Results include dependencies that either directly match or transitively include a matching dependency.";

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate();
        if (scope != null) {
            Set<String> validScopes = new HashSet<>(asList("dependencies", "buildRequires",
                    "optionalDependencies", "dependencyGroups"));
            v = v.and(Validated.test("scope", "scope is a valid Python dependency scope", scope, validScopes::contains));
        }
        return v;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern packageNameMatcher = compileGlobPattern(packageNamePattern);

        return new TomlIsoVisitor<ExecutionContext>() {
            private @Nullable PythonResolutionResult resolution;
            private final Map<String, MatchInfo> matchedPackages = new HashMap<>();

            @Override
            public Toml.Document visitDocument(Toml.Document document, ExecutionContext ctx) {
                if (!document.getSourcePath().toString().endsWith("pyproject.toml")) {
                    return document;
                }

                Optional<PythonResolutionResult> pyResolution = document.getMarkers()
                        .findFirst(PythonResolutionResult.class);

                if (!pyResolution.isPresent()) {
                    return document;
                }

                resolution = pyResolution.get();
                matchedPackages.clear();

                // Collect matching dependencies from the marker
                collectMatches(packageNameMatcher);

                // Visit the document to mark matching dependency literals
                Toml.Document result = super.visitDocument(document, ctx);

                // Insert data table rows
                for (MatchInfo match : matchedPackages.values()) {
                    dependenciesInUse.insertRow(ctx, new PythonDependenciesInUse.Row(
                            resolution.getName(),
                            resolution.getPath(),
                            match.packageName,
                            match.version,
                            match.versionConstraint,
                            match.scope,
                            match.direct,
                            match.count,
                            resolution.getLicense()
                    ));
                }

                return result;
            }

            @Override
            public Toml.Literal visitLiteral(Toml.Literal literal, ExecutionContext ctx) {
                Toml.Literal l = super.visitLiteral(literal, ctx);
                if (resolution == null) {
                    return l;
                }

                Object val = l.getValue();
                if (!(val instanceof String)) {
                    return l;
                }

                // Check if this literal is a PEP 508 dependency string that matches
                String spec = (String) val;
                String depName = extractPackageName(spec);
                if (depName != null) {
                    String normalized = PythonResolutionResult.normalizeName(depName);
                    if (matchedPackages.containsKey(normalized)) {
                        return SearchResult.found(l);
                    }
                }

                return l;
            }

            private void collectMatches(Pattern matcher) {
                if (resolution == null) {
                    return;
                }

                // Collect declared dependencies by scope
                if (scope == null || "dependencies".equals(scope)) {
                    collectFromDeclared(resolution.getDependencies(), "dependencies", matcher);
                }
                if (scope == null || "buildRequires".equals(scope)) {
                    collectFromDeclared(resolution.getBuildRequires(), "buildRequires", matcher);
                }
                if (scope == null || "optionalDependencies".equals(scope)) {
                    for (Map.Entry<String, List<Dependency>> entry : resolution.getOptionalDependencies().entrySet()) {
                        collectFromDeclared(entry.getValue(), "optionalDependencies/" + entry.getKey(), matcher);
                    }
                }
                if (scope == null || "dependencyGroups".equals(scope)) {
                    for (Map.Entry<String, List<Dependency>> entry : resolution.getDependencyGroups().entrySet()) {
                        collectFromDeclared(entry.getValue(), "dependencyGroups/" + entry.getKey(), matcher);
                    }
                }
            }

            private void collectFromDeclared(List<Dependency> deps, String scopeName, Pattern matcher) {
                for (Dependency dep : deps) {
                    String normalized = PythonResolutionResult.normalizeName(dep.getName());

                    if (matcher.matcher(normalized).matches()) {
                        recordMatch(dep, scopeName, true);
                    } else if (!Boolean.TRUE.equals(onlyDirect) && dep.getResolved() != null) {
                        // Check transitive dependencies
                        Set<String> transitiveMatches = findTransitiveMatches(dep.getResolved(), matcher, new HashSet<>());
                        if (!transitiveMatches.isEmpty()) {
                            // Record the direct dep as a match (it leads to transitive match)
                            recordMatch(dep, scopeName, true);
                            for (String transitiveName : transitiveMatches) {
                                recordTransitiveMatch(transitiveName);
                            }
                        }
                    }
                }
            }

            private Set<String> findTransitiveMatches(ResolvedDependency resolved, Pattern matcher, Set<String> visited) {
                String key = resolved.getName() + "@" + resolved.getVersion();
                if (visited.contains(key)) {
                    return Collections.emptySet();
                }
                visited.add(key);

                Set<String> matches = new HashSet<>();

                if (resolved.getDependencies() == null) {
                    return matches;
                }

                for (ResolvedDependency dep : resolved.getDependencies()) {
                    String normalized = PythonResolutionResult.normalizeName(dep.getName());
                    if (matcher.matcher(normalized).matches()) {
                        matches.add(dep.getName());
                    }
                    matches.addAll(findTransitiveMatches(dep, matcher, visited));
                }

                return matches;
            }

            private void recordMatch(Dependency dep, String scopeName, boolean direct) {
                String normalized = PythonResolutionResult.normalizeName(dep.getName());
                MatchInfo existing = matchedPackages.get(normalized);
                if (existing != null) {
                    existing.incrementCount();
                    return;
                }

                ResolvedDependency resolved = dep.getResolved();
                String resolvedVersion = resolved != null ? resolved.getVersion() : null;

                matchedPackages.put(normalized, new MatchInfo(
                        dep.getName(),
                        resolvedVersion,
                        dep.getVersionConstraint(),
                        scopeName,
                        direct
                ));
            }

            private void recordTransitiveMatch(String packageName) {
                String normalized = PythonResolutionResult.normalizeName(packageName);
                MatchInfo existing = matchedPackages.get(normalized);
                if (existing != null) {
                    existing.incrementCount();
                    return;
                }

                if (resolution != null) {
                    ResolvedDependency resolved = resolution.getResolvedDependency(packageName);
                    if (resolved != null) {
                        matchedPackages.put(normalized, new MatchInfo(
                                packageName,
                                resolved.getVersion(),
                                null,
                                "transitive",
                                false
                        ));
                        return;
                    }
                }

                matchedPackages.put(normalized, new MatchInfo(
                        packageName,
                        null,
                        null,
                        "transitive",
                        false
                ));
            }
        };
    }

    private static @Nullable String extractPackageName(String pep508Spec) {
        // Extract the package name from a PEP 508 string
        // The name is the first token before any version specifier, extras, or marker
        String trimmed = pep508Spec.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int end = 0;
        while (end < trimmed.length()) {
            char c = trimmed.charAt(end);
            if (c == '[' || c == '>' || c == '<' || c == '=' || c == '!' || c == '~' || c == ';' || c == ' ') {
                break;
            }
            end++;
        }
        String name = trimmed.substring(0, end).trim();
        return name.isEmpty() ? null : name;
    }

    private static class MatchInfo {
        final String packageName;
        final @Nullable String version;
        final @Nullable String versionConstraint;
        final String scope;
        final boolean direct;
        int count;

        MatchInfo(String packageName, @Nullable String version, @Nullable String versionConstraint,
                  String scope, boolean direct) {
            this.packageName = packageName;
            this.version = version;
            this.versionConstraint = versionConstraint;
            this.scope = scope;
            this.direct = direct;
            this.count = 1;
        }

        void incrementCount() {
            count++;
        }
    }

    private static Pattern compileGlobPattern(String glob) {
        // Normalize the glob pattern the same way we normalize package names
        String normalizedGlob = glob.toLowerCase().replace('-', '_').replace('.', '_');
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < normalizedGlob.length(); i++) {
            char c = normalizedGlob.charAt(i);
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '+':
                case '^':
                case '$':
                case '|':
                case '\\':
                    regex.append("\\").append(c);
                    break;
                default:
                    regex.append(c);
                    break;
            }
        }
        regex.append("$");
        return Pattern.compile(regex.toString());
    }
}
