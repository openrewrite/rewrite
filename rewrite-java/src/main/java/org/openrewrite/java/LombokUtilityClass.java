package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.J;

import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

/**
 * TODO: Check the following criteria:
 * - Lombok in dependencies
 * - All methods of class are static
 * - No instances of given class
 * - All static attributes are final
 * <p>
 * TODO: Perform the transformation:
 * - Add the annotation
 * - Remove static from all attributes and methods
 * <p>
 * TODO: Features to consider:
 * - Transformation: Add Lombok config if not present + supported configuration options for utility class
 * - Transformation: Replace instantiation with static calls to methods
 */
public class LombokUtilityClass extends Recipe {
    @Override
    public String getDisplayName() {
        return "Lombok UtilityClass";
    }

    @Override
    public String getDescription() {
        return "This recipe will check if any class is transformable (only static methods in class)" +
                " into the Lombok UtilityClass and will perform the change if applicable.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new LombokUtilityClassVisitor();
    }

    private static class LombokUtilityClassVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.ClassDeclaration visitClassDeclaration(
                final J.ClassDeclaration classDecl,
                final ExecutionContext executionContext
        ) {
            if (classDecl.getLeadingAnnotations().stream().anyMatch(a -> "UtilityClass".equals(a.getSimpleName()))) {
                return super.visitClassDeclaration(classDecl, executionContext);
            }

            maybeAddImport("lombok.experimental.UtilityClass", false);

            return JavaTemplate
                    .builder("@UtilityClass")
                    .imports("lombok.experimental.UtilityClass")
                    .build()
                    .apply(
                            getCursor(),
                            classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName))
                    );
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(
                final J.MethodDeclaration method,
                final ExecutionContext executionContext
        ) {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
            if (classDecl == null) {
                return super.visitMethodDeclaration(method, executionContext);
            }

            if (classDecl.getLeadingAnnotations().stream().noneMatch(a -> "UtilityClass".equals(a.getSimpleName()))) {
                return super.visitMethodDeclaration(method, executionContext);
            }

            if (!method.hasModifier(J.Modifier.Type.Static)) {
                return super.visitMethodDeclaration(method, executionContext);
            }

            return super.visitMethodDeclaration(
                    method.withModifiers(method.getModifiers().stream()
                            .filter(m -> m.getType() != J.Modifier.Type.Static)
                            .collect(Collectors.toList())),
                    executionContext
            );
        }
    }
}
