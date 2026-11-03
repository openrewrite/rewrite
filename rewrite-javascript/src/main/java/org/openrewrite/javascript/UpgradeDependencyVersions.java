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
import org.openrewrite.javascript.table.NodeDependencyEditsDeclined;
import org.openrewrite.javascript.table.NodeDependencyProtocolsSkipped;
import org.openrewrite.javascript.table.NodeLockRegenerationFailures;

import java.util.Collection;
import java.util.Map;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeDependencyVersions extends ScanningRecipe<NodeDependencyEdits> {

    transient NodeLockRegenerationFailures lockRegenerationFailures = new NodeLockRegenerationFailures(this);
    transient NodeDependencyEditsDeclined editsDeclined = new NodeDependencyEditsDeclined(this);
    transient NodeDependencyProtocolsSkipped protocolsSkipped = new NodeDependencyProtocolsSkipped(this);

    @Option(displayName = "Versions",
            description = "Package-name glob patterns mapped to target version constraints. All matching upgrades " +
                    "are decided together before regenerating the lock file.",
            example = "{\"@angular/*\": \"16.x\", \"typescript\": \"5.0.x\"}")
    Map<String, String> versions;

    @Option(displayName = "Reconcile npm peer dependencies",
            description = "Recover from stale npm lock peer conflicts only when the complete upgraded manifest " +
                    "resolves without a lock. The rebuilt lock must pass a subsequent plain install validation. " +
                    "Defaults to false; genuine peer conflicts leave the dependency edits unapplied.",
            required = false)
    @Nullable Boolean reconcileNpmPeerDependencies;

    @Override
    public String getDisplayName() {
        return "Upgrade npm dependency versions together";
    }

    @Override
    public String getDescription() {
        return "Upgrades matching npm dependencies as one coordinated edit and regenerates the lock file once. " +
                "Uses the same version-range, peer-range, protocol and catalog rules as `UpgradeDependencyVersion`. " +
                "Optionally reconciles stale npm lock peer conflicts after verifying the upgraded dependencies " +
                "resolve together. Not safe to use as a precondition: consults the package registry over the " +
                "network and publishes per-project state shared with other dependency recipes.";
    }

    @Override
    public NodeDependencyEdits getInitialValue(ExecutionContext ctx) {
        return new NodeDependencyEdits(NodeDependencyEdits.rows(lockRegenerationFailures, editsDeclined, null),
                Boolean.TRUE.equals(reconcileNpmPeerDependencies));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(NodeDependencyEdits acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
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
        for (Map.Entry<String, String> entry : versions.entrySet()) {
            new UpgradeDependencyVersion(null, entry.getKey(), entry.getValue())
                    .decide(decision, ctx, protocolsSkipped);
        }
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(NodeDependencyEdits acc) {
        return acc.visitor(this::decide);
    }
}
