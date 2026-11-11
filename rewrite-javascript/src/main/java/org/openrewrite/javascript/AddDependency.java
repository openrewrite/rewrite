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
package org.openrewrite.javascript;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.javascript.internal.LockFileRegeneration;
import org.openrewrite.javascript.internal.NodeCatalogs;
import org.openrewrite.javascript.internal.NodeDependencyEdits;
import org.openrewrite.javascript.internal.NodeDependencyScan;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.internal.PnpmRelease;
import org.openrewrite.javascript.internal.lock.ManifestRules;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.semver.NodeSemver;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;


@EqualsAndHashCode(callSuper = false)
@Value
public class AddDependency extends ScanningRecipe<NodeDependencyEdits> {

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);
    transient NodeDependencyEditsDeclined editsDeclined = new NodeDependencyEditsDeclined(this);

    private static final List<String> RESOLVED_SCOPES = asList("dependencies", "devDependencies",
            "optionalDependencies", "peerDependencies");

    @Option(displayName = "Package name",
            description = "The name of the npm package to add (e.g., `lodash`, `@types/node`).",
            example = "lodash")
    String packageName;

    @Option(displayName = "Version",
            description = "The version constraint to set (e.g., `^5.0.0`, `~2.1.0`, `3.0.0`).",
            example = "^5.0.0")
    String version;

    @Option(displayName = "Scope",
            description = "The dependency scope: `dependencies`, `devDependencies`, `peerDependencies`, " +
                    "or `optionalDependencies`. Defaults to `dependencies`.",
            valid = {"dependencies", "devDependencies", "peerDependencies", "optionalDependencies"},
            example = "dependencies",
            required = false)
    @Nullable String scope;

    @Option(displayName = "Only if declaring",
            description = "Add the dependency only to a `package.json` that itself declares, in any scope, a package " +
                    "matching one of these glob patterns. Declarations are read as they stand before the recipes in a " +
                    "cycle edit them, so in a migration name the package being migrated away from. Leave unset to " +
                    "add the dependency to every `package.json`.",
            example = "jest, react-scripts",
            required = false)
    @Nullable List<String> onlyIfDeclaring;

    @Option(displayName = "Only if resolving",
            description = "Add the dependency only to a `package.json` that resolves every one of these `name@range` " +
                    "entries, split at the last `@`. A package not declared by the `package.json` is taken from the " +
                    "nearest enclosing `package.json` that declares it, as Node resolves it. An entry holds when the " +
                    "version the lock file resolves satisfies the range or, without one, when every version the " +
                    "declared constraint admits does, so `^16` does not satisfy `>=16.9.0 <18`. Declarations are read " +
                    "as they stand before the recipes in a cycle edit them. Leave unset to add the dependency " +
                    "regardless of versions.",
            example = "react@>=16.9.0 <18",
            required = false)
    @Nullable List<String> onlyIfResolving;

    public AddDependency(String packageName, String version, @Nullable String scope) {
        this(packageName, version, scope, null);
    }

    public AddDependency(String packageName, String version, @Nullable String scope,
                         @Nullable List<String> onlyIfDeclaring) {
        this(packageName, version, scope, onlyIfDeclaring, null);
    }

    @JsonCreator
    public AddDependency(String packageName, String version, @Nullable String scope,
                         @Nullable List<String> onlyIfDeclaring, @Nullable List<String> onlyIfResolving) {
        this.packageName = packageName;
        this.version = version;
        this.scope = scope;
        this.onlyIfDeclaring = onlyIfDeclaring;
        this.onlyIfResolving = onlyIfResolving;
    }

    @Override public String getDisplayName() { return "Add npm dependency"; }
    @Override public String getInstanceNameSuffix() { return String.format("`%s`", packageName); }

    @Override public String getDescription() {
        return "Add an npm dependency to `package.json` and regenerate the lock file natively, without executing the " +
                "package manager. In a pnpm workspace with catalogs it writes what `pnpm add` writes under the " +
                "workspace's `catalogMode` and `saveCatalogName`. If the dependency already exists in any scope, the " +
                "recipe is a no-op. Not safe to use as a precondition: consults the package registry over the " +
                "network and publishes per-project state shared with other dependency recipes.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> v = super.validate();
        if (onlyIfResolving != null) {
            for (String entry : onlyIfResolving) {
                v = v.and(Validated.test("onlyIfResolving", "entries are `name@range` with a valid npm semver range",
                        entry, e -> nameAndRange(e) != null));
            }
        }
        return v;
    }

    private String targetScope() { return scope == null ? "dependencies" : scope; }

    @Override
    public NodeDependencyEdits getInitialValue(ExecutionContext ctx) {
        return new NodeDependencyEdits(NodeDependencyEdits.rows(lockRegenerationFailures, editsDeclined,
                packageName));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(NodeDependencyEdits acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof SourceFile) {
                    acc.scan((SourceFile) tree);
                }
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(NodeDependencyEdits acc, ExecutionContext ctx) {
        return acc.decideAll(ctx, this::decide);
    }

    private void decide(NodeDependencyEdits.Decision decision, ExecutionContext ctx) {
        Json.Document pkg = decision.getManifest();
        if (!matchesAdd(pkg) || !declaresAMatch(decision) || !resolvesAll(decision)) {
            return;
        }
        if (PackageJsonHelper.isNonObjectMember(pkg, targetScope())) {
            declined(decision, ctx, "`" + targetScope() + "` is not an object, so " + packageName + " was not added");
            return;
        }
        PackageManager pm;
        NodeCatalogs catalogs;
        try {
            pm = decision.getPackageManager();
            catalogs = decision.catalogs();
        } catch (Rejected e) {
            declined(decision, ctx, packageName + "@" + version + " was not added: " + e.getMessage());
            return;
        }
        if (catalogs instanceof NodeCatalogs.Pnpm && catalogs.isFlowStyle()) {
            declined(decision, ctx, packageName + "@" + version + " was not added: the catalogs in " +
                                    catalogs.getPath() + " are written in flow style, which is not read");
            return;
        }
        ManifestRules rules = LockFileRegeneration.manifestRules(pm);
        String value = added(decision, ctx, rules, catalogs);
        if (value != null) {
            decision.editManifest(doc -> !matchesAdd(doc) ? doc :
                    saved(rules, PackageJsonHelper.addDependency(doc, packageName, value, targetScope())));
        }
    }

    // the manifest as each package manager's add saves it (add-<pm>-sorts-every-field, add-<pm>-beside-empty-field)
    private Json.Document saved(ManifestRules rules, Json.Document doc) {
        Json.Document sorted = doc;
        for (String field : rules.sortedOnAdd(targetScope())) {
            sorted = PackageJsonHelper.sortMembers(sorted, field, rules.addOrder());
        }
        return rules.keepsEmptyFieldsOnAdd() ? sorted : PackageJsonHelper.withoutEmptyObjects(sorted,
                ManifestRules.DEPENDENCY_FIELDS);
    }

    // what the release that runs writes, recorded: pnpm 9.15.9 the version, 10.34.6 and 11.2.2 by the catalogs
    private @Nullable String added(NodeDependencyEdits.Decision decision, ExecutionContext ctx, ManifestRules rules,
                                   @Nullable NodeCatalogs catalogs) {
        PnpmRelease release;
        if (catalogs instanceof NodeCatalogs.Pnpm) {
            String root = decision.manifestOf(ctx, catalogs.getPath().resolveSibling("package.json"));
            release = (root == null ? (NodeCatalogs.Pnpm) catalogs :
                    ((NodeCatalogs.Pnpm) catalogs).forRoot(root)).getRelease();
        } else {
            release = PnpmRelease.of(rules, PackageJsonHelper.printed(decision.getManifest()), null);
        }
        if (release != null && release.declined() != null) {
            declined(decision, ctx, packageName + "@" + version + " was not added: " + release.declined());
            return null;
        }
        if (release != null && release.getMajor() == 9) {
            return version;
        }
        if (catalogs != null && catalogs.declined() != null) {
            declined(decision, ctx, packageName + "@" + version + " was not added: " + catalogs.declined());
            return null;
        }
        ManifestRules.Saved saved;
        try {
            saved = rules.added(packageName, version, catalogs == null ? null : catalogs.getSettings(),
                    release == null ? null : release.getVersion());
        } catch (Rejected e) {
            declined(decision, ctx, packageName + "@" + version + " was not added: " + e.getMessage());
            return null;
        }
        String catalog = saved.getCatalogName();
        if (catalog != null && !decision.editCatalogs(c -> c.withEntry(catalog, packageName, version))) {
            declined(decision, ctx, packageName + "@" + version + " was not added: its catalog in " +
                                    requireNonNull(catalogs).getPath() + " cannot take the entry in place");
            return null;
        }
        return saved.getSpecifier();
    }

    private static void declined(NodeDependencyEdits.Decision decision, ExecutionContext ctx, String why) {
        decision.warn(why);
        decision.decline(ctx, why);
    }

    private boolean declaresAMatch(NodeDependencyEdits.Decision decision) {
        if (onlyIfDeclaring == null) {
            return true;
        }
        for (String glob : onlyIfDeclaring) {
            Pattern pattern = PackageJsonHelper.compileGlobPattern(glob);
            for (Map<String, String> declared : decision.scannedDeclarations().values()) {
                for (String name : declared.keySet()) {
                    if (pattern.matcher(name).matches()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean resolvesAll(NodeDependencyEdits.Decision decision) {
        if (onlyIfResolving == null) {
            return true;
        }
        for (String entry : onlyIfResolving) {
            Map.Entry<String, String> nameAndRange = nameAndRange(entry);
            if (nameAndRange == null || !resolves(decision, nameAndRange.getKey(), nameAndRange.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean resolves(NodeDependencyEdits.Decision decision, String name, String range) {
        NodeDependencyScan.Manifest declaring = decision.scannedDeclaring(name);
        if (declaring == null) {
            return false;
        }
        try {
            String resolved = declaring.resolvedVersion(name);
            if (resolved != null) {
                return NodeSemver.satisfies(resolved, range, NodeSemver.Options.LOOSE);
            }
            for (String scope : RESOLVED_SCOPES) {
                String constraint = declaring.getDeclared().getOrDefault(scope, emptyMap()).get(name);
                if (constraint != null) {
                    return NodeSemver.subset(constraint, range, NodeSemver.Options.LOOSE);
                }
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return false;
    }

    private static Map.@Nullable Entry<String, String> nameAndRange(@Nullable String entry) {
        if (entry == null) {
            return null;
        }
        String trimmed = entry.trim();
        int at = trimmed.lastIndexOf('@');
        if (at <= 0 || at == trimmed.length() - 1) {
            return null;
        }
        String range = trimmed.substring(at + 1);
        return NodeSemver.validRange(range, NodeSemver.Options.LOOSE) == null ? null :
                new AbstractMap.SimpleImmutableEntry<>(trimmed.substring(0, at), range);
    }

    private boolean matchesAdd(Json.Document pkg) {
        for (Map<String, String> declared : PackageJsonHelper.declaredDependencies(pkg).values()) {
            if (declared.containsKey(packageName)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(NodeDependencyEdits acc) {
        return acc.visitor(this::decide);
    }
}
