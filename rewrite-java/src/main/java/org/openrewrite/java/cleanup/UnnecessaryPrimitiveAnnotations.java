package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UnnecessaryPrimitiveAnnotations extends Recipe {
    private static final AnnotationMatcher CHECK_FOR_NULL_ANNOTATION_MATCHER = new AnnotationMatcher("@javax.annotation.CheckForNull");
    private static final AnnotationMatcher NULLABLE_ANNOTATION_MATCHER = new AnnotationMatcher("@javax.annotation.Nullable");

    @Override
    public String getDisplayName() {
        return "Remove Nullable and CheckForNull annotations from primitives";
    }

    @Override
    public String getDescription() {
        return "Remove `@Nullable` and `@CheckForNull` annotations from primitives since they can't be null.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-4682");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesType<>("javax.annotation.CheckForNull"));
                doAfterVisit(new UsesType<>("javax.annotation.Nullable"));
                return cu;
            }
        };
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {

        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                J.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
                if (cu != c) {
                    maybeRemoveImport("javax.annotation.CheckForNull");
                    maybeRemoveImport("javax.annotation.Nullable");
                }
                return c;
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, executionContext);
                if (md.getReturnTypeExpression() != null && md.getReturnTypeExpression().getType() instanceof JavaType.Primitive) {
                    md = maybeAutoFormat(md, md.withLeadingAnnotations(filterAnnotations(md.getLeadingAnnotations())), executionContext, getCursor().dropParentUntil(J.class::isInstance));
                }
                return md;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                J.VariableDeclarations varDecls = super.visitVariableDeclarations(multiVariable, executionContext);
                if (varDecls.getType() instanceof JavaType.Primitive) {
                    varDecls = varDecls.withLeadingAnnotations(filterAnnotations(varDecls.getLeadingAnnotations()));
                    if (varDecls.getLeadingAnnotations().isEmpty() && varDecls.getTypeExpression() != null) {
                        varDecls = varDecls.withTypeExpression(varDecls.getTypeExpression().withPrefix(varDecls.getPrefix()));
                    }
                }
                return varDecls;
            }

            private List<J.Annotation> filterAnnotations(List<J.Annotation> annotations) {
                return ListUtils.map(annotations, anno -> {
                    if (NULLABLE_ANNOTATION_MATCHER.matches(anno) || CHECK_FOR_NULL_ANNOTATION_MATCHER.matches(anno)) {
                        return null;
                    }
                    return anno;
                });
            }
        };
    }
}
