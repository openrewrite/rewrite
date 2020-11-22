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
                ChangeFieldVisibility changeFieldVisibility = new ChangeFieldVisibility();
                changeFieldVisibility.setClassType(classType);
                changeFieldVisibility.setFieldName(fieldName);
                changeFieldVisibility.setVisibility("private");
                andThen(changeFieldVisibility);
                andThen(new GenerateGetter.Scoped(enclosingClass(), fieldName));
                andThen(new GenerateSetter.Scoped(enclosingClass(), fieldName));
            }
        }

        // TODO change all sites where field was used to use getter or setter

        return super.visitVariable(variable);
    }

}
