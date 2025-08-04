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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


@RequiredArgsConstructor
public class RpcRecipe extends ScanningRecipe<Integer> {
    private final transient RewriteRpc rpc;

    @Nullable
    private transient List<Recipe> recipeList;

    /**
     * The ID that the remote is using to refer to this recipe.
     */
    private final String remoteId;
    private final RecipeDescriptor descriptor;
    private final String editVisitor;

    @Nullable
    private final String scanVisitor;

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
        return descriptor.getContributors();
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
        if (scanVisitor == null) {
            return TreeVisitor.noop();
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof RpcCodec;
            }

            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                rpc.scan((SourceFile) tree, scanVisitor, ctx);
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Integer acc, ExecutionContext ctx) {
        return rpc.generate(remoteId, ctx);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Integer acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                return sourceFile instanceof RpcCodec;
            }

            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                return rpc.visit((SourceFile) tree, editVisitor, ctx);
            }
        };
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
        // This will merge data tables from the remote into the local context.
        //
        // When multiple recipes ran on the same RPC peer, they will all have been
        // adding to the same ExecutionContext instance on that peer, and so really
        // a CHANGE will only be returned for the first of any recipes on that peer.
        // It doesn't matter which one added data table entries, because they all share
        // the same view of the data tables.
        String id = ctx.getMessage("org.openrewrite.rpc.id");
        if (id != null) {
            rpc.getObject(id);
        }
    }
}
