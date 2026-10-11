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
import org.openrewrite.javascript.internal.MatchedDependency;
import org.openrewrite.javascript.internal.NodeCatalogs;
import org.openrewrite.javascript.internal.NodeDependencyEdits;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;


@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeDependencyVersion extends ScanningRecipe<NodeDependencyEdits> {

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);
    transient NodeDependencyEditsDeclined editsDeclined = new NodeDependencyEditsDeclined(this);
    transient NodeDependencyProtocolsSkipped protocolsSkipped = new NodeDependencyProtocolsSkipped(this);

    @Option(displayName = "Package name",
            description = "Exact package name to match. Mutually exclusive with `packagePattern`; " +
                    "at least one is required.",
            example = "lodash",
            required = false)
    @Nullable String packageName;

    @Option(displayName = "Package pattern",
            description = "Glob pattern matching package names (e.g., `@types/*`). " +
                    "Mutually exclusive with `packageName`; at least one is required.",
            example = "@types/*",
            required = false)
    @Nullable String packagePattern;

    @Option(displayName = "New version",
            description = "The new version constraint to set on matching dependencies.",
            example = "^5.0.0")
    String newVersion;

    @Override public String getDisplayName() { return "Upgrade npm dependency version"; }

    @Override public String getDescription() {
        return "Upgrades the version constraint of matching npm dependencies in `package.json` and regenerates the " +
                "lock file natively, without executing the package manager. Matching is by exact package name or " +
                "glob pattern. A version range is replaced whenever it differs from the new one, a `catalog:` " +
                "reference is upgraded in its catalog entry, and any other value is left as it is. Not safe to use " +
                "as a precondition: consults the package registry over the network and publishes per-project state " +
                "shared with other dependency recipes.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test(
                "options",
                "At least one of `packageName` or `packagePattern` must be specified.",
                this,
                r -> r.packageName != null || r.packagePattern != null));
    }

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
        Predicate<String> nameMatcher = buildNameMatcher();
        List<MatchedDependency> matches = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> scope :
                PackageJsonHelper.declaredDependencies(decision.getManifest()).entrySet()) {
            for (Map.Entry<String, String> dependency : scope.getValue().entrySet()) {
                String name = dependency.getKey();
                String value = dependency.getValue();
                if (!nameMatcher.test(name) || newVersion.equals(value)) {
                    continue;
                }
                Spec spec = Spec.ofDependencyOrNull(name, value);
                if (spec != null && spec.isVersionSpec()) {
                    matches.add(new MatchedDependency(name, scope.getKey(), value));
                } else if (spec == null || spec.getType() != Spec.Type.CATALOG ||
                           !followCatalog(decision, name, value)) {
                    String protocol = spec == null || spec.getProtocol() == null ? "" : spec.getProtocol();
                    String path = decision.getPath().toString();
                    decision.insertRow(protocolsSkipped, ctx, new NodeDependencyProtocolsSkipped.Row(path, path, name,
                            scope.getKey(), protocol, value, newVersion));
                }
            }
        }
        if (!matches.isEmpty()) {
            decision.editManifest(doc -> PackageJsonHelper.upgradeVersion(doc, matches, newVersion));
        }
    }

    private boolean followCatalog(NodeDependencyEdits.Decision decision, String name, String value) {
        NodeCatalogs catalogs;
        try {
            catalogs = decision.catalogs();
        } catch (Rejected e) {
            decision.decline(name + "'s catalog entry was not upgraded: " + e.getMessage());
            return true;
        }
        String catalog = catalogs == null ? null : catalogs.catalogOf(value);
        if (catalog == null) {
            return false;
        }
        if (catalogs.declined() != null) {
            decision.decline(name + "'s catalog entry was not upgraded: " + catalogs.declined());
            return true;
        }
        return decision.editCatalogs(c -> c.withSpecifier(catalog, name, newVersion));
    }

    private Predicate<String> buildNameMatcher() {
        if (packageName != null) {
            return packageName::equals;
        }
        Pattern p = PackageJsonHelper.compileGlobPattern(packagePattern);
        return s -> p.matcher(s).matches();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(NodeDependencyEdits acc) {
        return acc.visitor(this::decide);
    }
}
