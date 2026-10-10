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
import org.openrewrite.javascript.internal.LockFileRegeneration;
import org.openrewrite.javascript.internal.NodeCatalogs;
import org.openrewrite.javascript.internal.NodeDependencyEdits;
import org.openrewrite.javascript.internal.NodeCatalogs.Pnpm.CatalogMode;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;
import org.openrewrite.semver.NodeSemver;

import java.util.Collection;
import java.util.Map;


@EqualsAndHashCode(callSuper = false)
@Value
public class AddDependency extends ScanningRecipe<NodeDependencyEdits> {

    private static final String CATALOG = "catalog:";
    private static final String DEFAULT = "default";

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
        String value = catalogs instanceof NodeCatalogs.Pnpm ?
                pnpmAdd(decision, ctx, (NodeCatalogs.Pnpm) catalogs) : version;
        if (value != null) {
            decision.editManifest(doc -> matchesAdd(doc) ?
                    PackageJsonHelper.addDependency(doc, packageName, value, targetScope()) : doc);
        }
    }

    // what pnpm add writes: parseWantedDependencies' default-catalog match, installSome's catalogMode check and
    // resolveDependencyTree's saveCatalogName entry
    private @Nullable String pnpmAdd(NodeDependencyEdits.Decision decision, ExecutionContext ctx,
                                     NodeCatalogs.Pnpm catalogs) {
        String inDefault = catalogs.resolve(packageName, CATALOG);
        String wanted = version.equals(inDefault) ? CATALOG : version;
        String save = catalogs.getSaveCatalogName();
        CatalogMode mode = catalogs.getCatalogMode();
        if (mode != CatalogMode.MANUAL) {
            String perDep = save == null ? DEFAULT : save;
            String reference = CATALOG + (DEFAULT.equals(perDep) ? "" : perDep);
            String entry = catalogs.resolve(packageName, reference);
            boolean same = entry == null || wanted.equals(reference);
            if (!same && NodeSemver.validRange(wanted, NodeSemver.Options.DEFAULT) != null &&
                NodeSemver.validRange(entry, NodeSemver.Options.DEFAULT) != null) {
                try {
                    same = NodeSemver.compare(wanted, entry, NodeSemver.Options.DEFAULT) == 0;
                } catch (IllegalArgumentException e) {
                    return reject(decision, ctx, Rejected.crashes(LockFileRegeneration.Reason.RESOLUTION_REQUIRED,
                            packageName, "TypeError: " + e.getMessage()));
                }
            }
            if (same) {
                save = perDep;
            } else if (mode == CatalogMode.STRICT) {
                return reject(decision, ctx, Rejected.rejects(LockFileRegeneration.Reason.RESOLUTION_REQUIRED,
                        packageName, "[ERR_PNPM_CATALOG_VERSION_MISMATCH] Wanted dependency outside the version " +
                                     "range defined in catalog"));
            }
        }
        if (CATALOG.equals(wanted) || save == null || inDefault != null) {
            return wanted;
        }
        String catalog = save;
        if (!decision.editCatalogs(c -> c.withEntry(catalog, packageName, version))) {
            declined(decision, packageName + "@" + version + " was not added: its catalog in " + catalogs.getPath() +
                             " cannot take the entry in place");
            return null;
        }
        return CATALOG + (DEFAULT.equals(save) ? "" : save);
    }

    private @Nullable String reject(NodeDependencyEdits.Decision decision, ExecutionContext ctx, Rejected pnpm) {
        LockFileRegeneration.Failure failure = LockOnlyInstall.PNPM.failure(pnpm);
        decision.insertRow(lockRegenerationFailures, ctx, LockFileRegeneration.failureRow(decision.getPath(),
                failure, packageName));
        declined(decision, packageName + "@" + version + " was not added: " + failure.getDetail());
        return null;
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
