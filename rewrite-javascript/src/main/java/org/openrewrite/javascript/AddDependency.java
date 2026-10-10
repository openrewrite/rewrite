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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.javascript.internal.NodeCatalogs;
import org.openrewrite.javascript.internal.NodeDependencyEdits;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.internal.PnpmRelease;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.pnpm.InstallSome;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


@EqualsAndHashCode(callSuper = false)
@Value
public class AddDependency extends ScanningRecipe<NodeDependencyEdits> {

    private static final List<String> DEPENDENCY_FIELDS = Arrays.asList("dependencies", "devDependencies",
            "optionalDependencies", "peerDependencies");

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);
    transient NodeDependencyEditsDeclined editsDeclined = new NodeDependencyEditsDeclined(this);

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

    @Override public String getDisplayName() { return "Add npm dependency"; }
    @Override public String getInstanceNameSuffix() { return String.format("`%s`", packageName); }

    @Override public String getDescription() {
        return "Add an npm dependency to `package.json` and regenerate the lock file natively, without executing the " +
                "package manager. In a pnpm workspace with catalogs it writes what `pnpm add` writes under the " +
                "workspace's `catalogMode` and `saveCatalogName`. If the dependency already exists in any scope, the " +
                "recipe is a no-op. Not safe to use as a precondition: consults the package registry over the " +
                "network and publishes per-project state shared with other dependency recipes.";
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
        if (!matchesAdd(pkg)) {
            return;
        }
        if (PackageJsonHelper.isNonObjectMember(pkg, targetScope())) {
            declined(decision, "`" + targetScope() + "` is not an object, so " + packageName + " was not added");
            return;
        }
        NodeCatalogs catalogs = decision.catalogs();
        if (catalogs instanceof NodeCatalogs.Pnpm && catalogs.isFlowStyle()) {
            declined(decision, packageName + "@" + version + " was not added: the catalogs in " + catalogs.getPath() +
                             " are written in flow style, which is not read");
            return;
        }
        PackageManager pm = decision.getPackageManager();
        String value = catalogs instanceof NodeCatalogs.Pnpm ? pnpmAdd(decision, (NodeCatalogs.Pnpm) catalogs) :
                pm == PackageManager.Pnpm ? pnpmAdd(decision, PnpmRelease.of(PackageJsonHelper.printed(pkg), null)) :
                        version;
        if (value != null) {
            decision.editManifest(doc -> !matchesAdd(doc) ? doc :
                    saved(pm, PackageJsonHelper.addDependency(doc, packageName, value, targetScope())));
        }
    }

    // the manifest as each package manager's add saves it (add-<pm>-sorts-every-field, add-<pm>-beside-empty-field)
    private Json.Document saved(@Nullable PackageManager pm, Json.Document doc) {
        if (pm == null || pm == PackageManager.Bun) {
            return pm == null ? doc : PackageJsonHelper.sortMembers(doc, targetScope(), String::compareTo);
        }
        Comparator<String> order = pm == PackageManager.Npm ? LocaleCompare::en : String::compareTo;
        Json.Document sorted = doc;
        for (String field : DEPENDENCY_FIELDS) {
            sorted = PackageJsonHelper.sortMembers(sorted, field, order);
        }
        return pm == PackageManager.YarnClassic ? sorted : PackageJsonHelper.withoutEmptyObjects(sorted,
                DEPENDENCY_FIELDS);
    }

    private @Nullable String pnpmAdd(NodeDependencyEdits.Decision decision, PnpmRelease release) {
        if (release.declined() != null) {
            declined(decision, packageName + "@" + version + " was not added: " + release.declined());
            return null;
        }
        return version;
    }

    // what the pnpm release that runs writes, recorded: 9.15.9 the version, 10.34.6 and 11.2.2 by the catalogs
    private @Nullable String pnpmAdd(NodeDependencyEdits.Decision decision, NodeCatalogs.Pnpm catalogs) {
        SourceFile root = decision.manifestOf(catalogs.getPath().resolveSibling("package.json"));
        PnpmRelease release = (root == null ? catalogs : catalogs.forRoot(PackageJsonHelper.printed(root)))
                .getRelease();
        if (release.declined() != null || release.getMajor() == 9) {
            return pnpmAdd(decision, release);
        }
        InstallSome.Saved saved;
        try {
            saved = InstallSome.add(packageName, version, catalogs.getSettings());
        } catch (Rejected e) {
            declined(decision, packageName + "@" + version + " was not added: " +
                               LockOnlyInstall.PNPM.failure(e).getDetail());
            return null;
        }
        String catalog = saved.getCatalogName();
        if (catalog != null && !decision.editCatalogs(c -> c.withEntry(catalog, packageName, version))) {
            declined(decision, packageName + "@" + version + " was not added: its catalog in " + catalogs.getPath() +
                               " cannot take the entry in place");
            return null;
        }
        return saved.getBareSpecifier();
    }

    private static void declined(NodeDependencyEdits.Decision decision, String why) {
        decision.warn(why);
        decision.decline(why);
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
