package org.openrewrite.java;

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class ChangeFieldVisibility extends JavaIsoRefactorVisitor {

    private JavaType.Class classType;
    private String fieldName;
    private String visibility;

    public void setClassType(JavaType.Class classType) {
        this.classType = classType;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public ChangeFieldVisibility() {
        setCursoringOn();
    }

    @Override
    public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable) {
        if (variable.isField(getCursor()) && matchesClass(enclosingClass().getType()) &&
                variable.getSimpleName().equals(fieldName)) {
            andThen(new Scoped(getCursor().firstEnclosingRequired(J.VariableDecls.class), visibility));
        }
        return super.visitVariable(variable);
    }

    private boolean matchesClass(@Nullable JavaType test) {
        JavaType.Class testClassType = TypeUtils.asClass(test);
        return testClassType != null && testClassType.getFullyQualifiedName().equals(classType.getFullyQualifiedName());
    }

    public static class Scoped extends JavaIsoRefactorVisitor {

        private final J.VariableDecls scope;
        private final String visibility;

        public Scoped(J.VariableDecls scope, String visibility) {
            this.scope = scope;
            this.visibility = visibility;
        }

        @Override
        public J.VariableDecls visitMultiVariable(J.VariableDecls multiVariable) {
            if (scope.isScope(multiVariable)) {
                return multiVariable.withModifiers(J.Modifier.withModifiers(multiVariable.getModifiers(), visibility));
            }
            return super.visitMultiVariable(multiVariable);
        }
    }

}
