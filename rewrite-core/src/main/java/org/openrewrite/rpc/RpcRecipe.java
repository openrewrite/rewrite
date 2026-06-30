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
package org.openrewrite.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.config.OptionDescriptor;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.RecipeExample;
import org.openrewrite.rpc.request.PrepareRecipeResponse;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


@RequiredArgsConstructor
public class RpcRecipe extends ScanningRecipe<Integer> {
    @Getter
    private final transient RewriteRpc rpc;
    private transient @Nullable List<Recipe> recipeList;

    /**
     * The ID that the remote is using to refer to this recipe.
     */
    private final String remoteId;

    private final RecipeDescriptor descriptor;
    @Getter
    private final String editVisitor;
    /**
     * Composite of all editPreconditions resolved during PrepareRecipe. Exposed so that the
     * BatchVisit batching path in {@link org.openrewrite.scheduling.RecipeRunCycle} can
     * evaluate preconditions locally before adding a visitor to the batch — otherwise the
     * batch would dispatch the visit RPC for files that the precondition would have rejected.
     * The non-batch path uses {@link #getVisitor()}, which already wraps with
     * {@link org.openrewrite.Preconditions#check}.
     */
    @Getter
    private final @Nullable TreeVisitor<?, ExecutionContext> editPreconditionVisitor;
    @Getter
    private final @Nullable String scanVisitor;
    @Getter
    private final @Nullable TreeVisitor<?, ExecutionContext> scanPreconditionVisitor;

    /**
     * The prepared child recipe responses returned by the server as part of the whole-tree
     * prepare response. When non-null and non-empty, {@link #getRecipeList()} builds children
     * locally from these nodes instead of making individual PrepareRecipe RPC calls.
     */
    private final @Nullable List<PrepareRecipeResponse> childResponses;

    @Override
    public String getName() {
        return descriptor.getName();
    }

    @Override
    public String getDisplayName() {
        return descriptor.getDisplayName();
    }

    @Override
    public String getDescription() {
        return descriptor.getDescription();
    }

    @Override
    public Set<String> getTags() {
        return descriptor.getTags();
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return descriptor.getEstimatedEffortPerOccurrence();
    }

    @Override
    public List<RecipeExample> getExamples() {
        return descriptor.getExamples();
    }

    @Override
    public List<Contributor> getContributors() {
        // This is deprecated in RecipeDescriptor
        return emptyList();
    }

    @Override
    public List<Maintainer> getMaintainers() {
        return descriptor.getMaintainers();
    }

    @Override
    public Integer getInitialValue(ExecutionContext ctx) {
        return 0;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Integer acc) {
        return scanVisitor == null ? TreeVisitor.noop() : Preconditions.check(scanPreconditionVisitor, new RpcVisitor(rpc, scanVisitor));
    }

    @Override
    public Collection<? extends SourceFile> generate(Integer acc, ExecutionContext ctx) {
        return rpc.generate(remoteId, ctx);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Integer acc) {
        return Preconditions.check(editPreconditionVisitor, new RpcVisitor(rpc, editVisitor));
    }

    @Override
    public synchronized List<Recipe> getRecipeList() {
        if (recipeList == null) {
            if (childResponses != null) {
                // Whole-tree: children were prepared in the parent's response; build locally, no RPC.
                recipeList = childResponses.stream()
                        .map(rpc::recipeFromPrepareResponse)
                        .collect(toList());
            } else {
                // Fallback by-name path: peers whose servers don't yet return a prepared child tree
                // (Python/JS/Go until updated). The C# server always takes the branch above.
                // TODO(remove-fallback): delete this branch once every RPC server populates `recipeList`.
                recipeList = descriptor.getRecipeList().stream()
                        .map(r -> rpc.prepareRecipe(r.getName(), r.getOptions().stream()
                                .filter(opt -> opt.getValue() != null)
                                .collect(toMap(OptionDescriptor::getName, OptionDescriptor::getValue))))
                        .collect(toList());
            }
        }
        return recipeList;
    }

    @Override
    public void onComplete(ExecutionContext ctx) {
        // This will merge data tables from the remote into the local context.
        //
        // When multiple recipes ran on the same RPC peer, they will all have been
        // adding to the same ExecutionContext instance on that peer, and so really
        // a CHANGE will only be returned for the first of any recipes on that peer.
        //
        // It doesn't matter which one added data table entries, because they all share
        // the same view of the data tables.
        String id = ctx.getMessage("org.openrewrite.rpc.id");
        if (id != null) {
            rpc.getObject(id, null);
        }
    }

    @Override
    protected RecipeDescriptor createRecipeDescriptor() {
        return this.descriptor != null ? this.descriptor : super.createRecipeDescriptor();
    }
}
