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
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;
import org.openrewrite.json.tree.Json;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


@EqualsAndHashCode(callSuper = false)
@Value
public class ChangeDependency extends ScanningRecipe<NodeDependencyEdits> {

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);
    transient NodeDependencyEditsDeclined editsDeclined = new NodeDependencyEditsDeclined(this);
    transient NodeDependencyProtocolsSkipped protocolsSkipped = new NodeDependencyProtocolsSkipped(this);

    @Option(displayName = "Old package name",
            description = "The current name of the npm package to rename.",
            example = "lodash")
    String oldPackageName;

    @Option(displayName = "New package name",
            description = "The new name to use for the package.",
            example = "lodash-es")
    String newPackageName;

    @Option(displayName = "New version",
            description = "Optional new version constraint to set on the renamed package.",
            example = "^5.0.0",
            required = false)
    @Nullable String newVersion;

    @Option(displayName = "Scope",
            description = "The dependency scope: `dependencies`, `devDependencies`, etc. " +
                    "When omitted, all scopes are searched.",
            valid = {"dependencies", "devDependencies", "peerDependencies",
                    "optionalDependencies", "bundledDependencies"},
            example = "dependencies",
            required = false)
    @Nullable String scope;

    @Override public String getDisplayName() { return "Change npm dependency"; }
    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` -> `%s`", oldPackageName, newPackageName);
    }

    @Override public String getDescription() {
        return "Renames an npm dependency in `package.json` and optionally updates its version constraint. A " +
                "`catalog:` reference keeps its catalog, which gains the new name's entry. After modifying the " +
                "package.json, the lock file is regenerated natively, without executing the package manager. Not " +
                "safe to use as a precondition: consults the package registry over the network and publishes " +
                "per-project state shared with other dependency recipes.";
    }

    @Override
    public NodeDependencyEdits getInitialValue(ExecutionContext ctx) {
        return new NodeDependencyEdits(NodeDependencyEdits.rows(lockRegenerationFailures, editsDeclined,
                oldPackageName));
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
        Map<String, Boolean> keepValue = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> declared :
                PackageJsonHelper.declaredDependencies(decision.getManifest()).entrySet()) {
            String value = declared.getValue().get(oldPackageName);
            if (value == null || scope != null && !scope.equals(declared.getKey())) {
                continue;
            }
            Spec spec = Spec.ofDependencyOrNull(oldPackageName, value);
            if (spec != null && (spec.isVersionSpec() || spec.isLocation() && newVersion != null)) {
                keepValue.put(declared.getKey(), false);
            } else if (spec != null && spec.getType() == Spec.Type.CATALOG) {
                NodeCatalogs catalogs;
                try {
                    catalogs = decision.catalogs();
                } catch (Rejected e) {
                    String why = oldPackageName + " was not renamed: " + e.getMessage();
                    decision.warn(why);
                    decision.decline(why);
                    continue;
                }
                String catalog = catalogs == null ? null : catalogs.catalogOf(value);
                String entry = newVersion;
                if (catalog != null && entry == null) {
                    // an entry the new name already has stays, since other importers may use it
                    entry = catalogs.specifier(catalog, newPackageName);
                    entry = entry == null ? catalogs.specifier(catalog, oldPackageName) : entry;
                }
                String newEntry = entry;
                if (catalogs != null && catalogs.declined() != null) {
                    String why = oldPackageName + " was not renamed: " + catalogs.declined();
                    decision.warn(why);
                    decision.decline(why);
                } else if (catalogs != null && catalogs.isFlowStyle()) {
                    decline(decision, ctx, declared.getKey(), value, spec,
                            "its catalogs are written in flow style, which is not edited in place");
                } else if (catalog == null || entry == null) {
                    decline(decision, ctx, declared.getKey(), value, spec, "its catalog holds no entry for it");
                } else if (!decision.editCatalogs(c -> c.withEntry(catalog, newPackageName, newEntry))) {
                    decline(decision, ctx, declared.getKey(), value, spec,
                            "its catalog cannot take the new name's entry in place");
                } else {
                    keepValue.put(declared.getKey(), true);
                }
            } else {
                decline(decision, ctx, declared.getKey(), value, spec, spec != null && spec.isLocation() ?
                        "pass a newVersion to replace it with a registry version" :
                        "rename it where the value points first");
            }
        }
        if (!keepValue.isEmpty()) {
            decision.editManifest(doc -> rename(doc, keepValue));
        }
    }

    private Json.Document rename(Json.Document doc, Map<String, Boolean> keepValue) {
        Json.Document out = doc;
        for (Map.Entry<String, Boolean> declared : keepValue.entrySet()) {
            out = PackageJsonHelper.changeDependency(out, oldPackageName, newPackageName,
                    declared.getValue() ? null : newVersion, declared.getKey());
        }
        return out;
    }

    private void decline(NodeDependencyEdits.Decision decision, ExecutionContext ctx, String declaredScope,
                         String value, @Nullable Spec spec, String remedy) {
        String protocol = spec == null || spec.getProtocol() == null ? "" : spec.getProtocol();
        String path = decision.getPath().toString();
        decision.insertRow(protocolsSkipped, ctx, new NodeDependencyProtocolsSkipped.Row(path, path, oldPackageName,
                declaredScope, protocol, value, newVersion == null ? "" : newVersion));
        decision.warn(oldPackageName + " is declared as " + value + (protocol.isEmpty() ? "" : " (" + protocol +
                ")") + " and was left unchanged: " + remedy);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(NodeDependencyEdits acc) {
        return acc.visitor(this::decide);
    }
}
