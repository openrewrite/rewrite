package org.openrewrite.java;

import lombok.Data;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.util.*;

public class AddScopeToInjectedClass extends ScanningRecipe<AddScopeToInjectedClass.Accumulator> {
    private static final Collection<String> typesPromptingScopeAddition = Arrays.asList("javax.inject.Inject");

    @Override
    public String getDisplayName() {
        return "Add scope annotation to injected classes";
    }

    @Override
    public String getDescription() {
        return "Add scope annotation to injected classes.";
    }

    private static boolean variableTypeRequiresScope(@Nullable JavaType.Variable memberVariable) {
        if (memberVariable == null) {
            return false;
        }

        for (JavaType.FullyQualified annotation : memberVariable.getAnnotations()) {
            if (typesPromptingScopeAddition.contains(annotation.getFullyQualifiedName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                // look for classes requiring scope definition
                if (tree instanceof J.CompilationUnit) {
                    J.CompilationUnit cu = (J.CompilationUnit) tree;
                    for (J.ClassDeclaration classDeclaration : cu.getClasses()) {
                        JavaType.FullyQualified type =(JavaType.FullyQualified) classDeclaration.getType();
                        for (JavaType.Variable variable : type.getMembers())
                        if (variableTypeRequiresScope(variable)) {
                            acc.getScopeTypes().add((JavaType.FullyQualified)variable.getType());
                        }
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof J.CompilationUnit) {
                    J.CompilationUnit cu = (J.CompilationUnit) tree;
                    for (J.ClassDeclaration aClass : cu.getClasses()) {
                        JavaType.FullyQualified type =(JavaType.FullyQualified) aClass.getType();
                        if (acc.getScopeTypes().contains(type)) {
                            return new AddScopeAnnotationVisitor().visit(cu, acc.getScopeTypes());
                        }
                    }
                }
                return tree;
            }
        };
    }

    private static class AddScopeAnnotationVisitor extends JavaIsoVisitor<Set<JavaType.FullyQualified>> {
        final String scopeAnnotationFqn = "javax.enterprise.context.Dependent";
        final AnnotationMatcher SCOPE_ANNOTATION_MATCHER = new AnnotationMatcher("@" + scopeAnnotationFqn);
        final JavaTemplate templ = JavaTemplate.builder("@Dependent")
                .imports(scopeAnnotationFqn)
                .javaParser(JavaParser.fromJavaVersion().dependsOn("package javax.enterprise.context; public @interface Dependent {}"))
                .build();

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, Set<JavaType.FullyQualified> scopeTypes) {
            if (!scopeTypes.contains(TypeUtils.asFullyQualified(classDecl.getType()))) {
                return classDecl;
            }
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, scopeTypes);
            if (cd.getLeadingAnnotations().stream().noneMatch(SCOPE_ANNOTATION_MATCHER::matches)) {
                cd = templ.apply(getCursor(), cd.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                maybeAddImport(scopeAnnotationFqn);
            }
            return cd;
        }
    }

    @Data
    static class Accumulator {
        Set<JavaType.FullyQualified> scopeTypes = new HashSet<>();
    }
}

