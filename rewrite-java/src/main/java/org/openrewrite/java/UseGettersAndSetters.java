package org.openrewrite.java;

import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

import static org.openrewrite.internal.StringUtils.capitalize;

public class UseGettersAndSetters extends JavaIsoRefactorVisitor {

    private JavaType.Class classType;

    public UseGettersAndSetters() {
        setCursoringOn();
    }

    public void setClassType(JavaType.Class classType) {
        this.classType = classType;
    }

    @Override
    public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable) {
        if (variable.isField(getCursor())) {
            J.VariableDecls variableDecl = getCursor().firstEnclosing(J.VariableDecls.class);
            if (variableDecl != null && J.Modifier.hasModifier(variableDecl.getModifiers(), "public")) {
                String fieldName = variable.getSimpleName();
                andThen(new ScopedPrivateField(variableDecl));
                andThen(new GenerateGetter.Scoped(enclosingClass(), fieldName));
                andThen(new GenerateSetter.Scoped(enclosingClass(), fieldName));
                andThen(new ScopedUpdateReferences(fieldName, classType));
            }
        }

        // TODO change all sites where field was used to use getter or setter

        return super.visitVariable(variable);
    }

    public static class ScopedPrivateField extends JavaIsoRefactorVisitor {

        private final J.VariableDecls scope;

        public ScopedPrivateField(J.VariableDecls scope) {
            this.scope = scope;
        }

        @Override
        public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable) {
            if (scope.isScope(multiVariable)) {
                return multiVariable.withModifiers(J.Modifier.withModifiers(multiVariable.getModifiers(), "private"));
            }
            return super.visitMultiVariable(multiVariable);
        }
    }

    public static class ScopedUpdateReferences extends JavaRefactorVisitor {

        private final String fieldName;
        private final JavaType.Class classType;

        public ScopedUpdateReferences(String fieldName, JavaType.Class classType) {
            this.fieldName = fieldName;
            this.classType = classType;
            setCursoringOn();
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess) {
            if (matchesClass(fieldAccess.getTarget().getType()) && fieldAccess.getName().getSimpleName().equals(fieldName)) {
                Tree t = getCursor().getParentOrThrow().getTree();
                if (t instanceof J.Assign) {
                    J.Assign assign = (J.Assign) t;
                    andThen(new ScopedAssign(assign, fieldName));
                } else {
                    List<J> elements = treeBuilder.buildSnippet(getCursor().getParentOrThrow(),
                fieldAccess.getTarget().printTrimmed() + ".get" + capitalize(fieldName) + "()");
                    return elements.get(0).withFormatting(Formatting.format(" ", ""));
                }
            }
            return super.visitFieldAccess(fieldAccess);
        }


        private boolean matchesClass(@Nullable JavaType test) {
            JavaType.Class testClassType = TypeUtils.asClass(test);
            return testClassType != null && testClassType.getFullyQualifiedName().equals(classType.getFullyQualifiedName());
        }

        public static class ScopedAssign extends JavaRefactorVisitor {

            private final J.Assign scope;
            private final String fieldName;

            public ScopedAssign(J.Assign scope, String fieldName) {
                this.scope = scope;
                this.fieldName = fieldName;
                setCursoringOn();
            }

            @Override
            public J visitAssign(J.Assign assign) {
                if (scope.isScope(assign)) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) assign.getVariable();
                    List<J> elements = treeBuilder.buildSnippet(getCursor().getParentOrThrow(),
                            fieldAccess.getTarget().printTrimmed() + ".set" + capitalize(fieldName) + "(" + assign.getAssignment().printTrimmed() + ")");
                    return elements.get(0);
                }
                return super.visitAssign(assign);
            }
        }
    }

}
