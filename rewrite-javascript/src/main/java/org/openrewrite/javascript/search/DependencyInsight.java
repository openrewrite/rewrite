/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.javascript.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Dependency;
import org.openrewrite.javascript.marker.NodeResolutionResult.ResolvedDependency;
import org.openrewrite.javascript.table.NodeDependenciesInUse;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.json.tree.JsonKey;
import org.openrewrite.marker.SearchResult;

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;

/**
 * Find direct and transitive npm dependencies matching a package name pattern.
 * Results include dependencies that either directly match or transitively include
 * a matching dependency. Search result markers are placed on the dependency key
 * in package.json.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class DependencyInsight extends Recipe {
    transient NodeDependenciesInUse dependenciesInUse = new NodeDependenciesInUse(this);

    @Option(displayName = "Package name pattern",
            description = "A glob pattern to match npm package names. Use `*` as a wildcard.",
            example = "@types/*")
    String packageNamePattern;

    @Option(displayName = "Scope",
            description = "Match dependencies in the specified scope. All scopes are searched by default.",
            valid = {"dependencies", "devDependencies", "peerDependencies", "optionalDependencies", "bundledDependencies"},
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

    @Override
    public String getDisplayName() {
        return "Node.js dependency insight";
    }

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", packageNamePattern);
    }

    @Override
    public String getDescription() {
        return "Find direct and transitive npm dependencies matching a package name pattern. " +
               "Results include dependencies that either directly match or transitively include a matching dependency.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate();
        if (scope != null) {
            Set<String> validScopes = new HashSet<>(asList("dependencies", "devDependencies", "peerDependencies",
                    "optionalDependencies", "bundledDependencies"));
            v = v.and(Validated.test("scope", "scope is a valid npm dependency scope", scope, validScopes::contains));
        }
        return v;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern packageNameMatcher = compileGlobPattern(packageNamePattern);

        return new JsonIsoVisitor<ExecutionContext>() {
            private @Nullable NodeResolutionResult resolution;
            private @Nullable String currentSection;

            // Track which packages matched (for data table rows)
            private final Map<String, MatchInfo> matchedPackages = new HashMap<>();

            @Override
            public Json.Document visitDocument(Json.Document document, ExecutionContext ctx) {
                // Only process package.json files
                if (!document.getSourcePath().toString().endsWith("package.json")) {
                    return document;
                }

                Optional<NodeResolutionResult> nodeResolution = document.getMarkers()
                        .findFirst(NodeResolutionResult.class);

                if (!nodeResolution.isPresent()) {
                    return document;
                }

                resolution = nodeResolution.get();
                matchedPackages.clear();

                // Visit the document to mark matching dependencies
                Json.Document result = super.visitDocument(document, ctx);

                // Insert data table rows for all matched packages
                for (MatchInfo match : matchedPackages.values()) {
                    dependenciesInUse.insertRow(ctx, new NodeDependenciesInUse.Row(
                            resolution.getName(),
                            resolution.getPath(),
                            match.packageName,
                            match.version,
                            match.versionConstraint,
                            match.scope,
                            match.direct,
                            match.count,
                            match.license
                    ));
                }

                return result;
            }

            @Override
            public Json.Member visitMember(Json.Member member, ExecutionContext ctx) {
                Json.Member m = super.visitMember(member, ctx);

                if (resolution == null) {
                    return m;
                }

                String keyName = getKeyName(m.getKey());
                if (keyName == null) {
                    return m;
                }

                // Check if this member is a dependency section
                if (isDependencySection(keyName)) {
                    // Track that we're entering a dependency section
                    String previousSection = currentSection;
                    currentSection = keyName;
                    m = super.visitMember(member, ctx);
                    currentSection = previousSection;
                    return m;
                }

                // If we're inside a dependency section, check if this is a matching dependency
                if (currentSection != null && (scope == null || scope.equals(currentSection))) {
                    Dependency dep = findDependency(keyName, currentSection);
                    if (dep != null) {
                        // Check if this package directly matches
                        if (packageNameMatcher.matcher(keyName).matches()) {
                            recordMatch(dep, currentSection, true);
                            return markKey(m);
                        }

                        // If not only direct, check if this dependency has a matching transitive dependency
                        if (!Boolean.TRUE.equals(onlyDirect) && dep.getResolved() != null) {
                            Set<String> transitiveMatches = findTransitiveMatches(dep.getResolved(), new HashSet<>());
                            if (!transitiveMatches.isEmpty()) {
                                // Record all transitive matches for data table
                                for (String transitiveName : transitiveMatches) {
                                    recordTransitiveMatch(transitiveName);
                                }
                                return markKey(m);
                            }
                        }
                    }
                }

                return m;
            }

            private boolean isDependencySection(String name) {
                return "dependencies".equals(name) ||
                       "devDependencies".equals(name) ||
                       "peerDependencies".equals(name) ||
                       "optionalDependencies".equals(name) ||
                       "bundledDependencies".equals(name);
            }

            private @Nullable String getKeyName(JsonKey key) {
                if (key instanceof Json.Literal) {
                    String source = ((Json.Literal) key).getSource();
                    // Remove quotes from string literal
                    if (source.startsWith("\"") && source.endsWith("\"")) {
                        return source.substring(1, source.length() - 1);
                    }
                    return source;
                } else if (key instanceof Json.Identifier) {
                    return ((Json.Identifier) key).getName();
                }
                return null;
            }

            private @Nullable Dependency findDependency(String name, String section) {
                if (resolution == null) {
                    return null;
                }

                List<Dependency> deps = null;
                switch (section) {
                    case "dependencies":
                        deps = resolution.getDependencies();
                        break;
                    case "devDependencies":
                        deps = resolution.getDevDependencies();
                        break;
                    case "peerDependencies":
                        deps = resolution.getPeerDependencies();
                        break;
                    case "optionalDependencies":
                        deps = resolution.getOptionalDependencies();
                        break;
                    case "bundledDependencies":
                        deps = resolution.getBundledDependencies();
                        break;
                }

                if (deps == null) {
                    return null;
                }

                for (Dependency dep : deps) {
                    if (name.equals(dep.getName())) {
                        return dep;
                    }
                }
                return null;
            }

            private Set<String> findTransitiveMatches(ResolvedDependency resolved, Set<String> visited) {
                String key = resolved.getName() + "@" + resolved.getVersion();
                if (visited.contains(key)) {
                    return emptySet();
                }
                visited.add(key);

                Set<String> matches = new HashSet<>();

                // Check all dependency types of this resolved dependency
                List<Dependency> allDeps = new ArrayList<>();
                if (resolved.getDependencies() != null) {
                    allDeps.addAll(resolved.getDependencies());
                }
                if (resolved.getDevDependencies() != null) {
                    allDeps.addAll(resolved.getDevDependencies());
                }
                if (resolved.getPeerDependencies() != null) {
                    allDeps.addAll(resolved.getPeerDependencies());
                }
                if (resolved.getOptionalDependencies() != null) {
                    allDeps.addAll(resolved.getOptionalDependencies());
                }

                for (Dependency dep : allDeps) {
                    // Check if this transitive dependency matches
                    if (packageNameMatcher.matcher(dep.getName()).matches()) {
                        matches.add(dep.getName());
                    }

                    // Recursively check deeper transitive dependencies
                    if (dep.getResolved() != null) {
                        matches.addAll(findTransitiveMatches(dep.getResolved(), visited));
                    }
                }

                return matches;
            }

            private void recordMatch(Dependency dep, String section, boolean direct) {
                MatchInfo existing = matchedPackages.get(dep.getName());
                if (existing != null) {
                    existing.incrementCount();
                    return;
                }

                ResolvedDependency resolved = dep.getResolved();
                String resolvedVersion = resolved != null ? resolved.getVersion() : dep.getVersionConstraint();
                String license = resolved != null ? resolved.getLicense() : null;

                matchedPackages.put(dep.getName(), new MatchInfo(
                        dep.getName(),
                        resolvedVersion,
                        dep.getVersionConstraint(),
                        section,
                        direct,
                        license
                ));
            }

            private void recordTransitiveMatch(String packageName) {
                MatchInfo existing = matchedPackages.get(packageName);
                if (existing != null) {
                    existing.incrementCount();
                    return;
                }

                // Find the resolved dependency info from the resolution result
                if (resolution != null && resolution.getResolvedDependencies() != null) {
                    for (ResolvedDependency resolved : resolution.getResolvedDependencies()) {
                        if (packageName.equals(resolved.getName())) {
                            matchedPackages.put(packageName, new MatchInfo(
                                    packageName,
                                    resolved.getVersion(),
                                    null,
                                    "transitive",
                                    false,
                                    resolved.getLicense()
                            ));
                            return;
                        }
                    }
                }

                // Fallback if we can't find the resolved info
                matchedPackages.put(packageName, new MatchInfo(
                        packageName,
                        null,
                        null,
                        "transitive",
                        false,
                        null
                ));
            }

            private Json.Member markKey(Json.Member member) {
                JsonKey key = member.getKey();
                JsonKey markedKey = key.withMarkers(key.getMarkers().addIfAbsent(
                        new SearchResult(Tree.randomId(), null)));
                return member.withKey(markedKey);
            }
        };
    }

    private static class MatchInfo {
        final String packageName;
        final @Nullable String version;
        final @Nullable String versionConstraint;
        final String scope;
        final boolean direct;
        int count;
        final @Nullable String license;

        MatchInfo(String packageName, @Nullable String version, @Nullable String versionConstraint,
                  String scope, boolean direct, @Nullable String license) {
            this.packageName = packageName;
            this.version = version;
            this.versionConstraint = versionConstraint;
            this.scope = scope;
            this.direct = direct;
            this.count = 1;
            this.license = license;
        }

        void incrementCount() {
            count++;
        }
    }

    private static Pattern compileGlobPattern(String glob) {
        StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;
                case '?':
                    regex.append(".");
                    break;
                case '.':
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
