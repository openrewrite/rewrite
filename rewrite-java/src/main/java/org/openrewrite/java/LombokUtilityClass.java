package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;

import java.util.concurrent.atomic.AtomicInteger;
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
        return new LombokUtilityClassChangeVisitor();
    }

    private static class LombokUtilityClassCheckVisitor extends JavaIsoVisitor<AtomicInteger> {

        @Override
        public J.ClassDeclaration visitClassDeclaration(
                final J.ClassDeclaration classDecl,
                final AtomicInteger counter
        ) {
            if (classDecl.getLeadingAnnotations().stream().anyMatch(a -> "UtilityClass".equals(a.getSimpleName()))) {
                counter.getAndIncrement();
            }
            return super.visitClassDeclaration(classDecl, counter);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(
                final J.MethodDeclaration method,
                final AtomicInteger counter
        ) {
            if (!method.hasModifier(J.Modifier.Type.Static)) {
                counter.getAndIncrement();
            }
            return super.visitMethodDeclaration(method, counter);
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, AtomicInteger counter) {
            if (variable.isField(getCursor()) && !variable.getVariableType().hasFlags(Flag.Static)) {
                counter.getAndIncrement();
            }
            return super.visitVariable(variable, counter);
        }

        public static int countViolations(Tree t) {
            return new LombokUtilityClassCheckVisitor().reduce(t, new AtomicInteger(0)).get();
        }
    }

    private static class LombokUtilityClassChangeVisitor extends JavaIsoVisitor<ExecutionContext> {

        private int violations = 0;

        @Override
        public J.ClassDeclaration visitClassDeclaration(
                final J.ClassDeclaration classDecl,
                final ExecutionContext executionContext
        ) {
            this.violations = LombokUtilityClassCheckVisitor.countViolations(classDecl);

            if (violations == 0) {

                maybeAddImport("lombok.experimental.UtilityClass", false);

                return JavaTemplate
                        .builder("@UtilityClass")
                        .imports("lombok.experimental.UtilityClass")
                        .javaParser(JavaParser.fromJavaVersion().classpath("lombok"))
                        .build()
                        .apply(
                                getCursor(),
                                classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName))
                        );
            }

            return super.visitClassDeclaration(classDecl, executionContext);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(
                final J.MethodDeclaration method,
                final ExecutionContext executionContext
        ) {
            if (violations == 0) {
                return super.visitMethodDeclaration(
                        method.withModifiers(method.getModifiers().stream()
                                .filter(m -> m.getType() != J.Modifier.Type.Static)
                                .collect(Collectors.toList())),
                        executionContext
                );
            }

            return super.visitMethodDeclaration(method, executionContext);
        }
    }
}
