package org.openrewrite.java;

import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

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
                andThen(new Scoped(variableDecl));
                andThen(new GenerateGetter.Scoped(enclosingClass(), fieldName));
                andThen(new GenerateSetter.Scoped(enclosingClass(), fieldName));
            }
        }

        // TODO change all sites where field was used to use getter or setter

        return super.visitVariable(variable);
    }

    public static class Scoped extends JavaIsoRefactorVisitor {

        private final J.VariableDecls scope;

        public Scoped(J.VariableDecls scope) {
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

}
