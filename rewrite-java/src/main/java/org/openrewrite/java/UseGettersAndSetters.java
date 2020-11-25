package org.openrewrite.java;

import org.openrewrite.Formatting;
import org.openrewrite.Tree;
import org.openrewrite.Validated;
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

    public void setClassType(String classType) {
        this.classType = JavaType.Class.build(classType);
    }

    @Override
    public Validated validate() {
        return Validated.required("classType", classType);
    }

    @Override
    public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
        if (classType != null && classDecl.getType() != null
                && classType.getFullyQualifiedName().equals(classDecl.getType().getFullyQualifiedName())) {
            andThen(new Scoped(classDecl));
        }
        return classDecl;
    }

    public static class Scoped extends JavaIsoRefactorVisitor {

        private final J.ClassDecl scope;

        public Scoped(J.ClassDecl scope) {
            this.scope = scope;
            setCursoringOn();
        }

        @Override
        public J.ClassDecl visitClassDecl(J.ClassDecl classDecl) {
            if (scope.isScope(classDecl)) {
                return super.visitClassDecl(classDecl);
            }
            return classDecl;
        }

        @Override
        public J.VariableDecls.NamedVar visitVariable(J.VariableDecls.NamedVar variable) {
            if (variable.isField(getCursor())) {
                J.VariableDecls variableDecl = getCursor().firstEnclosing(J.VariableDecls.class);
                if (variableDecl != null && J.Modifier.hasModifier(variableDecl.getModifiers(), "public")) {
                    String fieldName = variable.getSimpleName();
                    andThen(new ChangePublicFieldToPrivate(variableDecl));
                    andThen(new GenerateGetter.Scoped(enclosingClass(), fieldName));
                    andThen(new GenerateSetter.Scoped(enclosingClass(), fieldName));
                    andThen(new UpdateReferences(fieldName, scope));
                }
            }
            return super.visitVariable(variable);
        }

        private static class ChangePublicFieldToPrivate extends JavaIsoRefactorVisitor {

            private final J.VariableDecls scope;

            public ChangePublicFieldToPrivate(J.VariableDecls scope) {
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

        private static class UpdateReferences extends JavaRefactorVisitor {

            private final String fieldName;
            private final J.ClassDecl classDecl;

            public UpdateReferences(String fieldName, J.ClassDecl classDecl) {
                this.fieldName = fieldName;
                this.classDecl = classDecl;
                setCursoringOn();
            }

            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess) {
                JavaType.Class targetClassType = TypeUtils.asClass(fieldAccess.getTarget().getType());
                if (classDecl != null
                        && classDecl.getType() != null
                        && targetClassType != null
                        && targetClassType.getFullyQualifiedName().equals(classDecl.getType().getFullyQualifiedName())
                        && fieldAccess.getName().getSimpleName().equals(fieldName)) {
                    Tree t = getCursor().getParentOrThrow().getTree();
                    if (t instanceof J.Assign) {
                        J.Assign assign = (J.Assign) t;
                        andThen(new ChangeAssignmentToSetter(assign, fieldName));
                    } else {
                        List<J> elements = treeBuilder.buildSnippet(getCursor().getParentOrThrow(),
                                fieldAccess.getTarget().printTrimmed() + ".get" + capitalize(fieldName) + "()");
                        return elements.get(0).withFormatting(Formatting.format(" ", ""));
                    }
                }
                return super.visitFieldAccess(fieldAccess);
            }

            private static class ChangeAssignmentToSetter extends JavaRefactorVisitor {

                private final J.Assign scope;
                private final String fieldName;

                public ChangeAssignmentToSetter(J.Assign scope, String fieldName) {
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

}
