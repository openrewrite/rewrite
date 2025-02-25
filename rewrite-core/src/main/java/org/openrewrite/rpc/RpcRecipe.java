package org.openrewrite.rpc;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.config.RecipeExample;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;


@RequiredArgsConstructor
public class RpcRecipe extends ScanningRecipe<Integer> {
    private final transient RewriteRpc rpc;

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
            public Tree preVisit(@NonNull Tree tree, ExecutionContext ctx) {
                rpc.scan((SourceFile) tree, scanVisitor, ctx);
                stopAfterPreVisit();
                return tree;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Integer acc, ExecutionContext ctx) {
        return rpc.generate(remoteId);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Integer acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(@NonNull Tree tree, ExecutionContext ctx) {
                Tree t = rpc.visit((SourceFile) tree, editVisitor, ctx);
                stopAfterPreVisit();
                return t;
            }
        };
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
        rpc.getObject(requireNonNull(ctx.getMessage("org.openrewrite.rpc.id")));
    }
}
