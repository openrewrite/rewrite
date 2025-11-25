/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class RunOnce extends Recipe {

    @Option(displayName = "Fully qualified recipe name",
            description = "The fully qualified name of the recipe to only run once." +
                          " Usually the name of the recipe this is put on as a precondition.",
            example = "org.openrewrite.FindGitProvenance")
    @Nullable
    String fqrn;

    @Override
    public String getDisplayName() {
        return "Run once precondition";
    }

    @Override
    public String getDescription() {
        return "This recipe can be used as a precondition to ensure that the specified recipe only makes changes once per execution cycle.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {

            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                for (Recipe recipe : ctx.getCycleDetails().getMadeChangesInThisCycle()) {
                    if (recipe.getDescriptor().getName().equals(fqrn)) {
                        return super.visit(tree, ctx);
                    }
                }
                return SearchResult.found(tree);
            }
        };
    }
}
