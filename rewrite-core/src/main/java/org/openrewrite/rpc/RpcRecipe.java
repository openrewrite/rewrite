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
            recipeList = descriptor.getRecipeList().stream()
                    .map(r -> rpc.prepareRecipe(r.getName(), r.getOptions().stream()
                            .filter(opt -> opt.getValue() != null)
                            .collect(toMap(OptionDescriptor::getName, OptionDescriptor::getValue))))
                    .collect(toList());
        }
        return recipeList;
    }

    @Override
    public void onComplete(ExecutionContext ctx) {
        // Synchronize the final state of the remote's ExecutionContext. Data table
        // rows do not flow back over RPC: when a data table store is configured
        // (see DataTableExecutionContextView#setDataTableStoreConfig), the peer
        // streams its rows to its own files in the shared output directory as they
        // are inserted.
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
