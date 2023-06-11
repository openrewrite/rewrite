package org.openrewrite.java;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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
        return new ChangeVisitor();
    }


    private static class ChangeVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.ClassDeclaration visitClassDeclaration(
                final J.ClassDeclaration classDecl,
                final ExecutionContext executionContext
        ) {
            if (!CheckVisitor.shouldPerformChanges(classDecl)) {
                return super.visitClassDeclaration(classDecl, executionContext);
            }

            maybeAddImport("lombok.experimental.UtilityClass", false);

            return super.visitClassDeclaration(
                    JavaTemplate
                            .builder("@UtilityClass")
                            .imports("lombok.experimental.UtilityClass")
                            .javaParser(JavaParser.fromJavaVersion().classpath("lombok"))
                            .build()
                            .apply(
                                    getCursor(),
                                    classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName))
                            ),
                    executionContext
            );
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(
                final J.MethodDeclaration method,
                final ExecutionContext executionContext
        ) {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
            if (!CheckVisitor.shouldPerformChanges(classDecl)) {
                return super.visitMethodDeclaration(method, executionContext);
            }

            return super.visitMethodDeclaration(
                    method.withModifiers(method.getModifiers().stream()
                            .filter(m -> m.getType() != J.Modifier.Type.Static)
                            .collect(Collectors.toList())),
                    executionContext
            );
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(
                final J.VariableDeclarations.NamedVariable variable,
                final ExecutionContext executionContext
        ) {
            final J.ClassDeclaration classDecl = getCursor().firstEnclosingOrThrow(J.ClassDeclaration.class);
            if (!CheckVisitor.shouldPerformChanges(classDecl)) {
                return super.visitVariable(variable, executionContext);
            }

            JavaType.Variable vari = variable.getVariableType();
            Set<Flag> flags = new HashSet<>(vari.getFlags());
            flags.remove(Flag.Static);

            return super.visitVariable(
                    variable
                            .withName(variable.getName().withSimpleName(variable.getName().getSimpleName().toLowerCase()))
                            //.withVariableType(vari.withFlags(Collections.emptySet())),
                            .withVariableType(new JavaType.Variable(null, Flag.Final.getBitMask(), "", null, null, null)),
                    executionContext
            );
        }
    }

    private static class CheckVisitor extends JavaIsoVisitor<AtomicBoolean> {

        @Override
        public J.ClassDeclaration visitClassDeclaration(
                final J.ClassDeclaration classDecl,
                final AtomicBoolean shouldPerformChanges
        ) {
            if (classDecl.getLeadingAnnotations().stream().anyMatch(a -> "UtilityClass".equals(a.getSimpleName()))) {
                shouldPerformChanges.set(false);
            }
            return super.visitClassDeclaration(classDecl, shouldPerformChanges);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(
                final J.MethodDeclaration method,
                final AtomicBoolean shouldPerformChanges
        ) {
            if (!method.hasModifier(J.Modifier.Type.Static)) {
                shouldPerformChanges.set(false);
            }
            return super.visitMethodDeclaration(method, shouldPerformChanges);
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(
                final J.VariableDeclarations.NamedVariable variable,
                final AtomicBoolean shouldPerformChanges
        ) {
            if (variable.isField(getCursor())
                    && variable.getVariableType() != null
                    && !variable.getVariableType().hasFlags(Flag.Static)) {
                shouldPerformChanges.set(false);
            }
            return super.visitVariable(variable, shouldPerformChanges);
        }

        private static boolean shouldPerformChanges(final J.ClassDeclaration classDecl) {
            return new CheckVisitor().reduce(classDecl, new AtomicBoolean(true)).get();
        }
    }
}
