package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.internal.lang.NonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceUUIDRandomId extends Recipe {
    @Option(displayName = "Fully Qualified Class Name",
            description = "A fully qualified class name indicating in which class to replace UUId.randomUUID() method to Tree.randomId() .",
            example = "com.yourorg.FooBar")
    @NonNull
    String fullyQualifiedClassName;

    @Override
    public String getDisplayName() {
        return "Replace UUID.randomUUID() with Tree.randomId()";
    }

    @Override
    public String getDescription() {
        return "This recipe replaces occurrences of UUID.randomUUID() with Tree.randomId().";
    }

    @JsonCreator
    public ReplaceUUIDRandomId(@NonNull @JsonProperty("fullyQualifiedClassName") String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("uuid-replacement");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public RecipeDescriptor createRecipeDescriptor() {
        // Customize if needed
        return super.createRecipeDescriptor();
    }

    @Override
    public @NotNull List<Recipe> getRecipeList() {
        return Collections.emptyList();
    }

    @Override
    public @NotNull TreeVisitor<J, ExecutionContext> getVisitor() {
        return new ReplaceUUIDRandomIdVisitor();
    }

    private static class ReplaceUUIDRandomIdVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.@NotNull Literal visitLiteral(J.@NotNull Literal literal, @NotNull ExecutionContext ctx) {
            J.Literal l = super.visitLiteral(literal, ctx);
            if (l.getValue() instanceof java.util.UUID) {
                return l.withValue(Tree.randomId());
            }
            return l;
        }
    }
}
