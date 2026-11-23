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
package org.openrewrite.golang.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.golang.GoModVisitor;
import org.openrewrite.golang.marker.GoResolutionResult;
import org.openrewrite.golang.marker.GoResolutionResult.ModuleRef;
import org.openrewrite.golang.marker.GoResolutionResult.Require;
import org.openrewrite.golang.marker.GoResolutionResult.ResolvedDependency;
import org.openrewrite.golang.table.GoDependenciesInUse;
import org.openrewrite.golang.tree.GoMod;
import org.openrewrite.golang.tree.GoModTree;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.util.*;

import static java.util.Collections.emptySet;

/**
 * Find direct and transitive Go module dependencies matching a module path pattern.
 * Reads the {@link GoResolutionResult} marker attached to each {@code go.mod} and places
 * search result markers on the module path of matching {@code require} directives.
 * A dependency matches when it directly matches the pattern, or (unless {@code onlyDirect})
 * when it transitively brings in a matching dependency.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class DependencyInsight extends Recipe {
    transient GoDependenciesInUse dependenciesInUse = new GoDependenciesInUse(this);

    @Option(displayName = "Module pattern",
            description = "A glob pattern to match Go module paths. Use `*` as a wildcard.",
            example = "github.com/google/*")
    String modulePattern;

    @Option(displayName = "Version",
            description = "Match only dependencies with the specified version. " +
                          "An exact version or a version range (see the [version selector syntax]" +
                          "(https://docs.openrewrite.org/reference/dependency-version-selectors)) may be used. " +
                          "All versions are searched by default.",
            required = false,
            example = "1.x")
    @Nullable
    String version;

    @Option(displayName = "Only direct",
            description = "If enabled, transitive dependencies will not be considered. All dependencies are searched by default.",
            required = false,
            example = "true")
    @Nullable
    Boolean onlyDirect;

    String displayName = "Go dependency insight";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s`", modulePattern);
    }

    String description = "Find direct and transitive Go module dependencies matching a module path pattern. " +
                         "Results include dependencies that either directly match or transitively include a matching dependency.";

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate();
        if (version != null) {
            v = v.and(Semver.validate(version, null));
        }
        return v;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        VersionComparator versionComparator = version != null ? Semver.validate(version, null).getValue() : null;

        return new GoModVisitor<ExecutionContext>() {
            private @Nullable GoResolutionResult resolution;
            private final Set<String> markRequires = new HashSet<>();
            private final Map<String, MatchInfo> matchedModules = new LinkedHashMap<>();

            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return super.isAcceptable(sourceFile, ctx) &&
                       sourceFile.getMarkers().findFirst(GoResolutionResult.class).isPresent();
            }

            @Override
            public GoModTree visitGoMod(GoMod goMod, ExecutionContext ctx) {
                resolution = goMod.getMarkers().findFirst(GoResolutionResult.class).orElse(null);
                if (resolution == null) {
                    return goMod;
                }
                markRequires.clear();
                matchedModules.clear();
                collectMatches();

                GoModTree g = super.visitGoMod(goMod, ctx);

                for (MatchInfo match : matchedModules.values()) {
                    dependenciesInUse.insertRow(ctx, new GoDependenciesInUse.Row(
                            resolution.getModulePath(),
                            resolution.getPath(),
                            match.modulePath,
                            match.version,
                            match.versionConstraint,
                            match.direct,
                            match.count
                    ));
                }
                return g;
            }

            @Override
            public GoModTree visitDirective(GoMod.Directive directive, ExecutionContext ctx) {
                GoMod.Directive d = (GoMod.Directive) super.visitDirective(directive, ctx);
                String modulePath = requireModulePath(d);
                if (modulePath != null && markRequires.contains(modulePath)) {
                    GoMod.Value first = d.getValues().get(0);
                    if (!first.getMarkers().findFirst(SearchResult.class).isPresent()) {
                        d = d.withValues(ListUtils.mapFirst(d.getValues(), v ->
                                v.withMarkers(v.getMarkers().addIfAbsent(new SearchResult(Tree.randomId(), null)))));
                    }
                }
                return d;
            }

            private @Nullable String requireModulePath(GoMod.Directive d) {
                if (d.getValues().isEmpty()) {
                    return null;
                }
                if ("require".equals(d.getKeyword())) {
                    return d.getValues().get(0).getText();
                }
                if (d.getKeyword().isEmpty()) {
                    GoMod.Block block = getCursor().firstEnclosing(GoMod.Block.class);
                    if (block != null && "require".equals(block.getKeyword())) {
                        return d.getValues().get(0).getText();
                    }
                }
                return null;
            }

            private void collectMatches() {
                if (resolution == null) {
                    return;
                }
                for (Require req : resolution.getRequires()) {
                    if (matches(req.getModulePath(), req.getVersion())) {
                        markRequires.add(req.getModulePath());
                        recordDirect(req);
                    } else if (!Boolean.TRUE.equals(onlyDirect)) {
                        Set<String> transitiveMatches = findTransitiveMatches(req.getModulePath(), new HashSet<>());
                        if (!transitiveMatches.isEmpty()) {
                            markRequires.add(req.getModulePath());
                            recordDirect(req);
                            for (String transitiveModule : transitiveMatches) {
                                recordTransitive(transitiveModule);
                            }
                        }
                    }
                }
            }

            private Set<String> findTransitiveMatches(String modulePath, Set<String> visited) {
                ResolvedDependency resolved = resolution == null ? null : resolution.findResolved(modulePath);
                if (resolved == null || resolved.getDeps() == null) {
                    return emptySet();
                }
                String key = resolved.getModulePath() + "@" + resolved.getVersion();
                if (!visited.add(key)) {
                    return emptySet();
                }
                Set<String> matches = new HashSet<>();
                for (ModuleRef ref : resolved.getDeps()) {
                    if (matches(ref.getModulePath(), ref.getVersion())) {
                        matches.add(ref.getModulePath());
                    }
                    matches.addAll(findTransitiveMatches(ref.getModulePath(), visited));
                }
                return matches;
            }

            private boolean matches(String modulePath, @Nullable String moduleVersion) {
                return StringUtils.matchesGlob(modulePath, modulePattern) && versionMatches(moduleVersion);
            }

            private boolean versionMatches(@Nullable String moduleVersion) {
                if (versionComparator == null) {
                    return true;
                }
                if (moduleVersion == null) {
                    return false;
                }
                String normalized = moduleVersion.startsWith("v") ? moduleVersion.substring(1) : moduleVersion;
                int plus = normalized.indexOf('+');
                if (plus > 0) {
                    normalized = normalized.substring(0, plus);
                }
                return versionComparator.isValid(null, normalized);
            }

            private void recordDirect(Require req) {
                MatchInfo existing = matchedModules.get(req.getModulePath());
                if (existing != null) {
                    existing.count++;
                    return;
                }
                ResolvedDependency resolved = resolution == null ? null : resolution.findResolved(req.getModulePath());
                String resolvedVersion = resolved != null ? resolved.getVersion() : req.getVersion();
                matchedModules.put(req.getModulePath(),
                        new MatchInfo(req.getModulePath(), resolvedVersion, req.getVersion(), true));
            }

            private void recordTransitive(String modulePath) {
                MatchInfo existing = matchedModules.get(modulePath);
                if (existing != null) {
                    existing.count++;
                    return;
                }
                ResolvedDependency resolved = resolution == null ? null : resolution.findResolved(modulePath);
                String resolvedVersion = resolved != null ? resolved.getVersion() : null;
                matchedModules.put(modulePath, new MatchInfo(modulePath, resolvedVersion, null, false));
            }
        };
    }

    private static class MatchInfo {
        final String modulePath;
        final @Nullable String version;
        final @Nullable String versionConstraint;
        final boolean direct;
        int count;

        MatchInfo(String modulePath, @Nullable String version, @Nullable String versionConstraint, boolean direct) {
            this.modulePath = modulePath;
            this.version = version;
            this.versionConstraint = versionConstraint;
            this.direct = direct;
            this.count = 1;
        }
    }
}
