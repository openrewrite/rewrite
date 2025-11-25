package org.openrewrite;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.SearchResult;

@Value
@EqualsAndHashCode(callSuper = false)
public class PreventIfAlreadyRan extends Recipe {

    @Option(displayName = "Fully qualified recipe name",
            description = "The fully qualified name of the recipe to prevent from running if it ran before.",
            example = "org.openrewrite.FindGitProvenance")
    @Nullable
    String fqrn;

    @Override
    public String getDisplayName() {
        return "";
    }

    @Override
    public String getDescription() {
        return "";
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
