/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.table.ManagedDependencyGraph;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = false)
public class EffectiveManagedDependencies extends Recipe {
    transient ManagedDependencyGraph dependencyGraph = new ManagedDependencyGraph(this);

    @Override
    public String getDisplayName() {
        return "Effective dependencies";
    }

    @Override
    public String getDescription() {
        return "Emit the data of binary dependency relationships.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                MavenResolutionResult mrr = getResolutionResult();
                emitParent(mrr, ctx);
                return document;
            }
        };
    }

    private void emitParent(MavenResolutionResult mrr, ExecutionContext ctx) {
        if (mrr.getParent() != null) {
            dependencyGraph.insertRow(ctx, new ManagedDependencyGraph.Row(
                    String.format("%s:%s:%s", mrr.getPom().getGroupId(), mrr.getPom().getArtifactId(), mrr.getPom().getVersion()),
                    String.format("%s:%s:%s", mrr.getParent().getPom().getGroupId(),
                            mrr.getParent().getPom().getArtifactId(), mrr.getParent().getPom().getVersion())
            ));

            for (ResolvedManagedDependency managed : mrr.getPom().getDependencyManagement()) {
                dependencyGraph.insertRow(ctx, new ManagedDependencyGraph.Row(
                        String.format("%s:%s:%s", mrr.getPom().getGroupId(), mrr.getPom().getArtifactId(), mrr.getPom().getVersion()),
                        String.format("%s:%s:%s", managed.getGroupId(), managed.getArtifactId(), managed.getVersion())
                ));
            }
        }
    }
}
