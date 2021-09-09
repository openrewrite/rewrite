package org.openrewrite.polyglot;

import lombok.experimental.NonFinal;
import org.graalvm.polyglot.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;

import java.util.List;

import static org.graalvm.polyglot.Value.asValue;
import static org.openrewrite.polyglot.PolyglotUtils.invokeMemberOrElse;

public class PolyglotRecipe extends Recipe {

    private final Value options;
    private final Value constructor;

    @Nullable
    @NonFinal
    private volatile Value instance;

    public PolyglotRecipe(Value options, Value constructor) {
        this.options = options;
        this.constructor = constructor;
    }

    @Override
    public String getDisplayName() {
        return invokeMemberOrElse(getInstance(), "getDisplayName", asValue(options.getMember("displayName")))
                .asString();
    }

    @Override
    public String getDescription() {
        return invokeMemberOrElse(getInstance(), "getDescription", asValue(options.getMember("description")))
                .asString();
    }

    @Override
    public List<Recipe> getRecipeList() {
        return super.getRecipeList();
    }

    @Override
    public Recipe doNext(Recipe recipe) {
        return super.doNext(recipe);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PolyglotVisitor<>(getInstance(), super.getVisitor());
    }

    private synchronized Value getInstance() {
        if (instance == null) {
            instance = constructor.newInstance(options);
        }
        return instance;
    }
}
