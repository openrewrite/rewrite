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
import org.openrewrite.javascript.internal.NodeDependencyEdits;
import org.openrewrite.javascript.internal.PackageJsonHelper;
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveDependency extends ScanningRecipe<NodeDependencyEdits> {

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);
    transient NodeDependencyEditsDeclined editsDeclined = new NodeDependencyEditsDeclined(this);

    @Option(displayName = "Package name",
            description = "The name of the npm package to remove (e.g., `lodash`, `@types/node`).",
            example = "lodash")
    String packageName;

    @Option(displayName = "Scope",
            description = "The dependency scope to remove from: `dependencies`, `devDependencies`, " +
                    "`peerDependencies`, `optionalDependencies`, or `bundledDependencies`. If not specified, removes " +
                    "from all scopes.",
            valid = {"dependencies", "devDependencies", "peerDependencies", "optionalDependencies",
                    "bundledDependencies"},
            example = "dependencies",
            required = false)
    @Nullable String scope;

    @Override public String getDisplayName() { return "Remove npm dependency"; }
    @Override public String getInstanceNameSuffix() { return String.format("`%s`", packageName); }

    @Override public String getDescription() {
        return "Remove an npm dependency from `package.json` and regenerate the lock file natively, without " +
                "executing the package manager. A pnpm workspace with `cleanupUnusedCatalogs` also loses the catalog " +
                "entries no project references any more. If the dependency does not exist in any scope, the recipe " +
                "is a no-op.";
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
        Set<String> scopes = new LinkedHashSet<>();
        for (Map.Entry<String, Map<String, String>> declared :
                PackageJsonHelper.declaredDependencies(decision.getManifest()).entrySet()) {
            if (declared.getValue().containsKey(packageName) && (scope == null || scope.equals(declared.getKey()))) {
                scopes.add(declared.getKey());
            }
        }
        if (!scopes.isEmpty()) {
            decision.editManifest(doc -> PackageJsonHelper.removeDependency(doc, packageName, scopes));
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(NodeDependencyEdits acc) {
        return acc.visitor(this::decide);
    }
}
