package org.openrewrite.java;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.stream.Collectors;
import static org.openrewrite.Formatting.formatFirstPrefix;
import org.openrewrite.Tree;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class RemoveAnnotation {
    private RemoveAnnotation() {
    }

    public static class Scoped extends JavaIsoRefactorVisitor {
        private final Tree scope;
        private final JavaType.Class annotationType;

        public Scoped(Tree scope, String annotationTypeName, Expression... arguments) {
            this.scope = scope;
            this.annotationType = JavaType.Class.build(annotationTypeName);
            setCursoringOn();
        }

        @Override
        public Iterable<Tag> getTags() {
            return Tags.of("annotation.type", annotationType.getFullyQualifiedName());
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
            J.ClassDecl c = super.visitClassDecl(classDecl);

            if (scope.isScope(classDecl)) {
                List<J.Annotation> fixedAnnotations =  c.getAnnotations().
                    stream().
                    filter(
                        ann -> !TypeUtils.isOfClassType(
                            ann.getType(),
                            annotationType.getFullyQualifiedName()
                        )
                    ).collect(Collectors.toList());
                c = c.withAnnotations(fixedAnnotations);

                if (classDecl.getAnnotations().isEmpty()) {
                    String prefix = formatter.findIndent(0, c).getPrefix();
                    // special case, where a top-level class is often un-indented completely
                    String cdPrefix = c.getPrefix();
                    if (getCursor().getParentOrThrow().getTree() instanceof J.CompilationUnit &&
                        cdPrefix.substring(Math.max(cdPrefix.lastIndexOf('\n'), 0)).chars().noneMatch(p -> p == ' ' || p == '\t')) {
                        prefix = "\n";
                    }

                    if (!c.getModifiers().isEmpty()) {
                        c = c.withModifiers(formatFirstPrefix(c.getModifiers(), prefix));
                    } else if (c.getTypeParameters() != null) {
                        c = c.withTypeParameters(c.getTypeParameters().withPrefix(prefix));
                    } else {
                        c = c.withKind(c.getKind().withPrefix(prefix));
                    }
                }

                maybeRemoveImport(annotationType.getFullyQualifiedName());
            }

            return c;
        }

        @Override
        public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable) {
            J.VariableDecls v = super.visitMultiVariable(multiVariable);
            if (scope.isScope(multiVariable)) {
                Tree parent = getCursor().getParentOrThrow().getTree();
                boolean isMethodOrLambdaParameter = parent instanceof J.MethodDecl || parent instanceof J.Lambda;

                List<J.Annotation> fixedAnnotations = v.getAnnotations().
                    stream().
                    filter(
                        ann -> !TypeUtils.isOfClassType(
                            ann.getType(), annotationType.getFullyQualifiedName()
                        )
                    ).
                    collect(Collectors.toList());
                if (!isMethodOrLambdaParameter && multiVariable.getPrefix().chars().filter(c -> c == '\n').count() < 2) {
                    List<?> statements = enclosingBlock().getStatements();
                    for (int i = 1; i < statements.size(); i++) {
                        if (statements.get(i) == multiVariable) {
                            v = v.withPrefix("\n" + v.getPrefix());
                            break;
                        }
                    }
                }
                v = v.withAnnotations(fixedAnnotations);
                if (multiVariable.getAnnotations().isEmpty()) {
                    String prefix = isMethodOrLambdaParameter ? " " : formatter.format(enclosingBlock()).getPrefix();

                    if (!v.getModifiers().isEmpty()) {
                        v = v.withModifiers(formatFirstPrefix(v.getModifiers(), prefix));
                    } else {
                        //noinspection ConstantConditions
                        v = v.withTypeExpr(v.getTypeExpr().withPrefix(prefix));
                    }
                }
                maybeRemoveImport(annotationType.getFullyQualifiedName());
            }
            return v;
        }

        @Override
        public J.MethodDecl visitMethod(J.MethodDecl method) {
            J.MethodDecl m = super.visitMethod(method);

            if (scope.isScope(method)) {
                List<J.Annotation> fixedAnnotations = m.getAnnotations().
                    stream().
                    filter(
                        ann -> !TypeUtils.isOfClassType(
                            ann.getType(), annotationType.getFullyQualifiedName()
                        )
                    ).
                    collect(Collectors.toList());
                m = m.withAnnotations(fixedAnnotations);
                if (method.getAnnotations().isEmpty()) {
                    String prefix = formatter.findIndent(0, method).getPrefix();

                    if (!m.getModifiers().isEmpty()) {
                        m = m.withModifiers(formatFirstPrefix(m.getModifiers(), prefix));
                    } else if (m.getTypeParameters() != null) {
                        m = m.withTypeParameters(m.getTypeParameters().withPrefix(prefix));
                    } else if (m.getReturnTypeExpr() != null) {
                        m = m.withReturnTypeExpr(m.getReturnTypeExpr().withPrefix(prefix));
                    } else {
                        m = m.withName(m.getName().withPrefix(prefix));
                    }
                }
                maybeRemoveImport(annotationType.getFullyQualifiedName());
            }
            return m;
        }
    }
}
