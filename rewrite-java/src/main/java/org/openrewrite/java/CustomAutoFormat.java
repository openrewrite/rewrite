package org.openrewrite.java;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.With;
import org.openrewrite.*;
import org.openrewrite.java.format.AutoFormat;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Marker;

import java.util.UUID;

@Value
@EqualsAndHashCode(callSuper = false)
public class CustomAutoFormat extends Recipe {

    @Override
    public String getDisplayName() {
        return "Scentbird custom autoformatting recipe";
    }

    @Override
    public String getDescription() {
        return "Uses a fixed style to automatically format the pieces of the code for which we know that support is present.";
    }

    private static final String style =
            "type: specs.openrewrite.org/v1beta/style\n" +
                    "name: WrapMethodDeclaration\n" +
                    "displayName: Wrap method declaration arguments\n" +
                    "description: Formats the method declaration arguments with a continuation of 4 by default on their own new lines.\n" +
                    "styleConfigs:\n" +
                    "  - org.openrewrite.java.style.WrappingAndBracesStyle:\n" +
                    "      hardWrapAt: 120\n" +
                    "      methodDeclarationParameters:\n" +
                    "        wrap: ChopIfTooLong\n" +
                    "        alignWhenMultiline: true\n" +
                    "        openNewLine: true\n" +
                    "        closeNewLine: true\n" +
                    "      chainedMethodCalls:\n" +
                    "        wrap: ChopIfTooLong\n" +
                    "        builderMethods:\n" +
                    "          - builder\n" +
                    "          - newBuilder\n" +
                    "          - stream\n" +
                    "  - org.openrewrite.java.style.TabsAndIndentsStyle:\n" +
                    "      continuationIndent: 4\n";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> formatter = new AutoFormat(style).getVisitor();
        return Preconditions.check(new FindSourceFiles("**/*.java"), new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                if (cu.getMarkers().findFirst(Formatted.class).isPresent()) {
                    return cu;
                }
                return super.visitCompilationUnit(cu, ctx).withMarkers(cu.getMarkers().add(new Formatted()));
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                return super.visitMethodDeclaration(format(method, ctx)
                        .withPrefix(method.getPrefix()) // The prefix could also have changed by autoformatting (newline in between multiple methods etc.)
                        .withBody(method.getBody()), ctx); // Only do method declaration for now, not the body
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                return format(method, ctx);
            }

            private <T extends Tree> T format(T t, ExecutionContext ctx) {
                //noinspection unchecked
                return (T) formatter.visitNonNull(t, ctx, getCursor().getParent());
            }
        });
    }

    @Value
    @With
    @AllArgsConstructor
    private static class Formatted implements Marker {
        UUID id;

        public Formatted() {
            this.id = UUID.randomUUID();
        }
    }

}
